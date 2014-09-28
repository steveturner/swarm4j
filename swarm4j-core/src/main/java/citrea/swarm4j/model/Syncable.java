package citrea.swarm4j.model;

import citrea.swarm4j.model.callback.*;
import citrea.swarm4j.model.annotation.SwarmOperation;
import citrea.swarm4j.model.annotation.SwarmOperationKind;
import citrea.swarm4j.model.meta.FieldMeta;
import citrea.swarm4j.model.meta.OperationMeta;
import citrea.swarm4j.model.meta.TypeMeta;
import citrea.swarm4j.model.oplog.LogDistillator;
import citrea.swarm4j.model.oplog.NoLogDistillator;
import citrea.swarm4j.model.spec.*;
import citrea.swarm4j.model.value.JSONUtils;
import citrea.swarm4j.util.ChainedIterators;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static citrea.swarm4j.model.spec.SToken.ZERO_VERSION;
import static citrea.swarm4j.model.spec.VersionVectorSpec.ZERO_VERSION_VECTOR;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 21.06.2014
 *         Time: 15:49
 */
public abstract class Syncable implements SomeSyncable, SubscriptionAware {

    public static final OpToken INIT = new OpToken(".init");
    public static final OpToken ERROR = new OpToken(".error");

    public static final String ID_FIELD = "_id";
    public static final String OPLOG_FIELD = "_oplog";
    public static final String TAIL_FIELD = "_tail";
    public static final String VERSION_FIELD = "_version";
    public static final String VECTOR_FIELD = "_vector";

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    // TODO fix listeners description
    // listeners represented as objects that have deliver() method
    // ...so uplinks is like [server1, server2, storage]
    // and listeners is like [view, other_listeners]
    // The most correct way to specify a version is the version vector,
    // but that one may consume more space than the data itself in some cases.
    // Hence, version is not a fully specified version vector (see version()
    // instead). version is essentially is the greatest operation timestamp
    // (Lamport-like, i.e. "time+source"), sometimes amended with additional
    // timestamps. Its main features:
    // (1) changes once the object's state changes
    // (2) does it monotonically (in the alphanumeric order sense)
    List<Uplink> uplinks = new ArrayList<Uplink>();
    List<OpRecipient> listeners = new ArrayList<OpRecipient>();
    LogDistillator logDistillator = new NoLogDistillator();

    Host host;
    TypeMeta typeMeta;
    private TypeToken type;
    private IdToken id;
    String version = null;
    private String vector = null;
    final Map<VersionOpSpec, JsonValue> oplog = new HashMap<VersionOpSpec, JsonValue>();

    protected Syncable(IdToken id, Host host) throws SwarmException {
        this.id = id;
        if (host == null) {
            if (!getClass().isAssignableFrom(Host.class)) {
                throw new IllegalArgumentException("host shouldn't be null");
            }
            // allowed for Host
            return;
        }
        this.setHost(host);
    }

    void setHost(Host host) throws SwarmException {
        this.host = host;
        this.typeMeta = this.getTypeMeta();
        this.type = this.typeMeta.getTypeToken();
        if (this.id == null) {
            this.id = this.host.time().convertToId();
            this.version = ZERO_VERSION.toString();
        }
        this.host.register(this);
        this.checkUplink();
    }

    /**
     * Applies a serialized operation (or a batch thereof) to this replica
     */
    @Override
    public synchronized void deliver(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
        if (logger.isDebugEnabled()) {
            logger.debug("{} <= ({}, {}, {})", this, spec, value, source);
        }
        if (value != null && value.isObject()) {
            value = JsonObject.unmodifiableObject(value.asObject());
        }
        String opver = spec.getVersion().toString();

        // sanity checks
        if (this.id == null) {
            source.deliver(spec.overrideOp(ERROR), JsonValue.valueOf("undead object invoked"), OpRecipient.NOOP);
            return;
        }

        String error = this.validate(spec, value);
        if (error != null && !"".equals(error)) {
            source.deliver(spec.overrideOp(ERROR), JsonValue.valueOf("invalid input, " + error), OpRecipient.NOOP);
            return;
        }

        if (!this.acl(spec, value, source)) {
            source.deliver(spec.overrideOp(ERROR), JsonValue.valueOf("access violation"), OpRecipient.NOOP);
            return;
        }

        //TODO if Swarm.debug
        //this.log(spec, value, this, source);

        try{
            OpToken op = spec.getOp();
            OperationMeta opMeta = this.typeMeta.getOperationMeta(op);
            if (opMeta == null) {
                this.unimplemented(spec, value, source);
                return;
            }

            switch (opMeta.getKind()) {
                case Logged:
                    if (this.isReplay(spec)) return; // it happens
                    // invoke the implementation

                    opMeta.invoke(this, spec, value, source);

                    // once applied, may remember in the log...
                    if (INIT.equals(op)) {
                        if (!this.oplog.isEmpty()) {
                            this.oplog.put(spec.getVersionOp(), value);
                        }
                        // this.version is practically a label that lets you know whether
                        // the state has changed. Also, it allows to detect some cases of
                        // concurrent change, as it is always set to the maximum version id
                        // received by this object. Still, only the full version vector may
                        // precisely and uniquely specify the current version (see version()).
                        if (this.isVersionOver(opver)) {
                            this.version = opver;
                        } else {
                            this.version += opver;
                        }
                    }
                    // ...and relay further to downstream replicas and various listeners
                    this.emit(spec, value, source);
                    break;

                case Neutral:
                    // invoke the implementation
                    opMeta.invoke(this, spec, value, source);
                    // and relay to listeners
                    this.emit(spec, value, source);
                    break;

                case Remote:
                    // TODO ???
                default:
                    this.unimplemented(spec, value, source);
            }
        } catch (Exception ex) {
            // log and rethrow; don't relay further; don't log
            logger.error("deliver({}, {}, {}) exception: ", spec, value, source, ex);
            this.error(spec, JsonValue.valueOf("method execution failed: " + ex.toString()), source);
        }
    }

    private boolean isVersionOver(String version) {
        return this.version == null || version.compareTo(this.version) > 0;
    }

    private TypeIdSpec typeId;
    /**
     * @return specifier "/Type#objid"
     */
    @Override
    public TypeIdSpec getTypeId() {
        if (typeId == null) {
            if (this.id != null && this.type != null) {
                typeId = new TypeIdSpec(this.type, this.id);
            } else {
                return new TypeIdSpec(TypeToken.NO_TYPE, IdToken.NO_ID);
            }
        }
        return typeId;
    }

    @Override
    public IdToken getId() {
        return id;
    }

    /**
     * Generates new specifier with unique version
     * @param op operation
     * @return {Spec}
     */
    public FullSpec newEventSpec(OpToken op) {
        return this.getTypeId().fullSpec(this.host.time(), op);
    }

    /**
     * Notify all the listeners of a state change (i.e. the operation applied).
     */
    void emit(FullSpec spec, JsonValue value, OpRecipient src) throws SwarmException {
        @SuppressWarnings("unchecked") Iterator<OpRecipient> it = new ChainedIterators<OpRecipient>(
                this.uplinks.iterator(),
                this.listeners.iterator()
        );
        OpToken op = spec.getOp();
        OperationMeta opMeta = this.typeMeta.getOperationMeta(op);
        if (opMeta == null) {
            throw new SwarmException("No method found: " + op.getBody());
        }
        boolean is_neutrals = opMeta.getKind() == SwarmOperationKind.Neutral;
        if (it.hasNext()) {
            List<OpRecipient> notify = new ArrayList<OpRecipient>();
            while (it.hasNext()) {
                OpRecipient l = it.next();
                // skip empties, deferreds and the source
                if (l == null || l == src) continue;
                if (is_neutrals && !(l instanceof OpFilter)) continue;
                if (l instanceof OpFilter && !((OpFilter) l).getOp().equals(op)) continue;

                notify.add(l);
            }
            for (OpRecipient l : notify) { // screw it I want my 'this'
                try {
                    l.deliver(spec, value, this);
                } catch (Exception ex) {
                    //TODO log console.error(ex.message, ex.stack);
                }
            }
        }
        /*TODO reactions
        var r = this._reactions[spec.op()];
        if (r) {
            r.constructor!==Array && (r = [r]);
            for (i = 0; i < r.length; i++) {
                r[i] && r[i].call(this, spec, value, src);
            }
        } */
    }

    protected void trigger(OpToken op, JsonValue params) throws SwarmException {
        FullSpec spec = this.newEventSpec(op);
        this.deliver(spec, params, OpRecipient.NOOP);
    }

    /**
     * Blindly applies a JSON changeset to this model.
     * @param fieldValues field values
     */
    void apply(JsonObject fieldValues) throws SwarmException {
        for (JsonObject.Member entry : fieldValues) {
            String fieldName = entry.getName();
            if (fieldName.startsWith("_")) {
                //special field: _version, _tail, _vector, _oplog
                continue;
            }
            this.setFieldValue(fieldName, entry.getValue());
        }
    }

    /**
     * @return the version vector for this object
     * @see citrea.swarm4j.model.spec.VersionVector
     */
    VersionVector version() {
        // distillLog() may drop some operations; still, those need to be counted
        // in the version vector; so, their Lamport ids must be saved in this.vector
        VersionVector map = new VersionVector();
        if (this.version != null) {
            map.add(this.version);
        }
        if (this.vector != null) {
            map.add(this.vector);
        }
        if (!this.oplog.isEmpty()) {
            for (VersionOpSpec op : this.oplog.keySet()) {
                map.add(op.getVersion());
            }
        }
        return map; // TODO return the object, let the consumer trim it to taste
    }

    /**
     * Produce the entire state or probably the necessary difference
     * to synchronize a replica which is at version *base*.
     * @return {{version:String, _tail:Object, *}} a state object
     * that must survive JSON.parse(JSON.stringify(obj))
     *
     * The size of a Model's distilled log is capped by the number of
     * fields in an object. In practice, that is a small number, so
     * Model uses its distilled log to transfer state (no snapshots).
     */
    JsonObject diff(VersionVectorSpec base) {
        this.distillLog(); // TODO optimize?
        JsonObject patch = new JsonObject();
        if (!base.isEmpty() && !ZERO_VERSION_VECTOR.equals(base)) {
            VersionVector map = new VersionVector(base);
            JsonObject tail = new JsonObject();
            for (Map.Entry<VersionOpSpec, JsonValue> op : this.oplog.entrySet()) {
                VersionOpSpec spec = op.getKey();
                if (!map.covers(spec.getVersion())) {
                    tail.set(spec.toString(), op.getValue());
                }
            }
            if (!tail.isEmpty()) {
                patch.set(Syncable.TAIL_FIELD, tail);
            }
        } else {
            JsonObject tail = new JsonObject();
            for (Map.Entry<VersionOpSpec, JsonValue> op : this.oplog.entrySet()) {
                tail.set(op.getKey().toString(), op.getValue());
            }
            patch.set(Syncable.VERSION_FIELD, JsonValue.valueOf(ZERO_VERSION.toString()));
            patch.set(Syncable.TAIL_FIELD, tail);
        }
        return patch;
    }

    Map<String, JsonValue> distillLog() {
        return this.logDistillator.distillLog(this.oplog);
    }

    /**
     * whether the update source (author) has all the rights necessary
     * @return {boolean}
     */
    protected boolean acl(FullSpec spec, JsonValue val, OpRecipient src) {
        return true;
    }

    /**
     * Check operation format/validity (recommendation: don't check against the current state)
     * @return '' if OK, error message otherwise.
     */
    protected String validate(FullSpec spec, JsonValue val) {
        // TODO add causal stability violation check  Swarm.EPOCH  (+tests)
        return "";
    }

    /**
     * whether this op was already applied in the past
     * @return {boolean}
     */
    protected final boolean isReplay(FullSpec spec) {
        if (this.version == null) return false;

        VersionToken opver = spec.getVersion();
        if (opver.toString().compareTo(this.version) > 0) return false;

        VersionOpSpec version_op = spec.getVersionOp();
        return this.oplog.containsKey(version_op) ||
                this.version().covers(opver);
    }

    /**
     * External objects (those you create by supplying an id) need first to query
     * the uplink for their state. Before the state arrives they are stateless.
     * @return {boolean}
     */
    protected boolean hasNoState() {
        return this.version == null;
    }

    protected void reset() {
        //TODO implement reset()
    }

    /**
     * Subscribe to the object's operations;
     * the upstream part of the two-way subscription
     *  on() with a full filter:
     *  @param spec /Mouse#Mickey!now.on
     *  @param filterValue !since.event
     *  @param source callback
     */
    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void on(FullSpec spec, JsonValue filterValue, OpRecipient source) throws SwarmException {   // WELL  on() is not an op, right?
        // if no listener is supplied then the object is only
        // guaranteed to exist till the next Host.gc() run
        if (source == null) return;

        // stateless objects fire no events; essentially, on() is deferred
        if (this.hasNoState() && this.isNotUplinked()) {
            this.addListener(
                    new OpFilter(
                            new Deferred(spec, filterValue, source),
                            REON
                    )
            );
            return; // defer this call till uplinks are waitForStart
        }

        if (!JSONUtils.isFalsy(filterValue)) {
            FilterSpec filter = new FilterSpec(filterValue.asString());
            VersionVectorSpec baseVersion = filter.getVersion();
            OpToken filter_by_op = filter.getOp();

            if (filter_by_op != null) {
                if (INIT.equals(filter_by_op)) {
                    JsonValue diff_if_needed = baseVersion != null ? this.diff(baseVersion) : JsonValue.NULL;
                    source.deliver(spec.overrideOp(INIT), diff_if_needed, this);
                    // use once()
                    return;
                }

                source = new OpFilter(source, filter_by_op);
            }

            if (baseVersion != null) {
                JsonObject diff = this.diff(baseVersion);
                if (!diff.isEmpty()) {
                    source.deliver(spec.overrideOp(INIT), diff, this); // 2downlink
                }
                source.deliver(spec.overrideOp(REON), JsonValue.valueOf(this.version().toString()), this);
            }
        }

        this.addListener(source);
        // TODO repeated subscriptions: send a diff, otherwise ignore
    }

    // should be generated?
    @Override
    public void on(JsonValue evfilter, OpRecipient source) throws SwarmException {
        this.on(newEventSpec(ON), evfilter, source);
    }

    /**
     * downstream reciprocal subscription
     */
    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void reon(FullSpec spec, JsonValue base, OpRecipient source) throws SwarmException {
        if (JSONUtils.isFalsy(base)) return;

        JsonObject diff = this.diff(new VersionVectorSpec(Spec.parse(base.asString())));
        if (diff.isEmpty()) return;

        source.deliver(spec.overrideOp(INIT), diff, this); // 2uplink
    }

    /** Unsubscribe */
    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void off(FullSpec spec, OpRecipient repl) throws SwarmException {
        this.removeListener(repl);
    }

    // should be generated?
    @Override
    public void off(OpRecipient source) throws SwarmException {
        this.deliver(this.newEventSpec(OFF), JsonValue.NULL, source);
    }

    /** Reciprocal unsubscription */
    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void reoff(FullSpec spec, OpRecipient source) throws SwarmException {
        this.removeListener(source);
        if (this.id != null) this.checkUplink();
    }

    /**
     * As all the event/operation processing is asynchronous, we
     * cannot simply throw/catch exceptions over the network.
     * This method allows to send errors back asynchronously.
     * Sort of an asynchronous complaint mailbox :)
     */
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void error(FullSpec spec, JsonValue value, OpRecipient source) {
        // TODO ??? source.deliver(error)
        this.log(spec.overrideOp(ERROR), value, this, source);
    }

    /**
     * A state of a Syncable CRDT object is transferred to a replica using
     * some combination of POJO state and oplog. For example, a simple LWW
     * object (Last Writer Wins, see Model below) uses its distilled oplog
     * as the most concise form. A CT document (Causal Trees) has a highly
     * compressed state, its log being hundred times heavier. Hence, it
     * mainly uses its plain state, but sometimes its log tail as well. The
     * format of the state object is POJO plus (optionally) special fields:
     * oplog, _tail, _vector, version (the latter flags POJO presence).
     * In either case, .state is only produced by diff() (+ by storage).
     * Any real-time changes are transferred as individual events.
     * @param spec specifier
     * @param state patch json
     * @param source source of operation
     */
    @SwarmOperation(kind = SwarmOperationKind.Logged)
    public void init(FullSpec spec, JsonValue state, OpRecipient source) throws SwarmException {
        Map<String, JsonValue> tail = new HashMap<String, JsonValue>();
        final TypeIdSpec typeid = spec.getTypeId();
        List<Uplink> uplinksBak = this.uplinks;
        List<OpRecipient> listenersBak = this.listeners;
        // prevent events from being fired
        this.uplinks = new ArrayList<Uplink>(0);
        this.listeners = new ArrayList<OpRecipient>(0);

            /*if (state._version === '!0') { // uplink knows nothing FIXME dubious
                if (!this._version) this._version = '!0';
            }*/

        if (state instanceof JsonObject) {
            JsonObject joState = (JsonObject) state;
            JsonValue state_version = joState.get(Syncable.VERSION_FIELD);
            if (state_version != null && state_version.isString()) {
                // local changes may need to be merged into the received state
                if (!this.oplog.isEmpty()) {
                    for (Map.Entry<VersionOpSpec, JsonValue> op : this.oplog.entrySet()) {
                        tail.put(op.getKey().toString(), op.getValue());
                    }
                    this.oplog.clear();
                }
                if (this.vector != null) {
                    this.vector = null;
                }
                // TODO zero everything
                /*
                for (FieldMeta field : this.typeMeta.getAllFields()) {
                    field.set(this, JsonValue.NULL);
                }
                */
                // set default values
                this.reset();

                this.apply(joState);
                this.version = state_version.asString();
                JsonValue state_oplog = joState.get(Syncable.OPLOG_FIELD);
                if (state_oplog instanceof JsonObject) {
                    JsonObject joOplog = (JsonObject) state_oplog;
                    for (JsonObject.Member spec_val : joOplog) {
                        this.oplog.put(new VersionOpSpec(spec_val.getName()), spec_val.getValue());
                    }
                }
                JsonValue state_vector = joState.get(Syncable.VECTOR_FIELD);
                if (state_vector != null && state_vector.isString()) {
                    this.vector = state_vector.asString();
                }
            }

            // add the received tail to the local one
            JsonValue stateTail = joState.get(Syncable.TAIL_FIELD);
            if (stateTail instanceof JsonObject) {
                JsonObject joStateTail = (JsonObject) stateTail;
                for (JsonObject.Member spec_val : joStateTail) {
                    tail.put(spec_val.getName(), spec_val.getValue());
                }
            }
        }

        // appply the combined tail to the new state
        String[] specs = tail.keySet().toArray(new String[tail.size()]);
        Arrays.sort(specs);
        // there will be some replays, but those will be ignored
        for (String a_spec : specs) {
            VersionOpSpec versionOp = new VersionOpSpec(a_spec);
            this.deliver(typeid.fullSpec(versionOp), tail.get(a_spec), source);
        }

        this.uplinks = uplinksBak;
        this.listeners = listenersBak;
    }

    /**
     * Uplink connections may be closed or reestablished so we need
     * to adjust every object's subscriptions time to time.
     */
    void checkUplink() throws SwarmException {
        List<Uplink> subscribeTo = this.host.getSources(this.getTypeId());
        // the plan is to eliminate extra subscriptions and to
        // establish missing ones; that only affects outbound subs
        List<Uplink> unsubscribeFrom = new ArrayList<Uplink>();
        for (Uplink up : this.uplinks) {
            if (up == null) {
                continue;
            }
            if (up instanceof PendingUplink) {
                up = ((PendingUplink) up).getInner();
            }
            int up_idx = subscribeTo.indexOf(up);
            if (up_idx > -1) {
                // is already subscribed or awaiting for response
                subscribeTo.remove(up_idx);
            } else {
                // don't need this uplink anymore
                unsubscribeFrom.add(up);
            }
        }
        // unsubscribe from old
        for (Uplink up : unsubscribeFrom) {
            up.deliver(this.newEventSpec(OFF), JsonValue.NULL, this);
        }
        // subscribe to the new
        for (Uplink new_uplink : subscribeTo) {
            if (new_uplink == null) {
                continue;
            }

            FullSpec onSpec = this.newEventSpec(ON);
            this.addUplink(new PendingUplink(this, new_uplink, onSpec.getVersion()));
            new_uplink.deliver(onSpec, JsonValue.valueOf(this.version().toString()), this);
        }
    }

    /**
     * returns a Plain Javascript Object with the state
     */
    public JsonObject getPOJO(boolean addVersionInfo) throws SwarmException {
        JsonObject pojo = new JsonObject();
        //TODO defaults
        for (FieldMeta field : this.typeMeta.getAllFields()) {
            JsonValue fieldValue = field.get(this);
            pojo.set(field.getName(), fieldValue);
        }
        if (addVersionInfo) {
            pojo.set(Syncable.ID_FIELD, JsonValue.valueOf(this.id.toString())); // not necassary
            pojo.set(Syncable.VERSION_FIELD, JsonValue.valueOf(this.version));
            if (this.vector != null) {
                pojo.set(Syncable.VECTOR_FIELD, JsonValue.valueOf(this.vector));
            }
            if (!this.oplog.isEmpty()) {
                JsonObject oplog = new JsonObject();
                for (Map.Entry<VersionOpSpec, JsonValue> op : this.oplog.entrySet()) {
                    oplog.set(op.getKey().toString(), op.getValue());
                }
                pojo.set(Syncable.OPLOG_FIELD, oplog); //TODO copy
            }
        }
        return pojo;
    }

    /**
     * Sometimes we get an operation we don't support; not normally
     * happens for a regular replica, but still needs to be caught
     * @param spec operation specifier
     * @param value operation params
     * @param source operation source
     */
    protected void unimplemented(Spec spec, JsonValue value, OpRecipient source) {
        logger.warn("{}.unimplemented({}, {}, {})", this, spec, value, source);
    }

    /**
     * Deallocate everything, free all resources.
     */
    public void close() throws SwarmException {
        // unsubscribe from uplinks
        Iterator<Uplink> itUplinks = uplinks.iterator();
        TypeIdSpec spec = this.getTypeId();
        while (itUplinks.hasNext()) {
            OpRecipient uplink = itUplinks.next();
            if (uplink instanceof Peer) {
                uplink.deliver(this.newEventSpec(OFF), JsonValue.NULL, this);
            }
            itUplinks.remove();
        }
        // notify listeners of object closing
        Iterator<OpRecipient> itListeners = listeners.iterator();
        while (itListeners.hasNext()) {
            // FIXME no version token in spec ???
            itListeners.next().deliver(spec.fullSpec(VersionToken.ZERO_VERSION, REOFF), JsonValue.NULL, this);
            itListeners.remove();
        }

        this.host.unregister(this);
    }

    /**
     * Once an object is not listened by anyone it is perfectly safe
     * to garbage collect it.
     */
    public void gc() throws SwarmException {
        if (uplinks.size() == 0 && listeners.size() == 0) {
            this.close();
        }
    }

    void log(FullSpec spec, JsonValue value, Syncable object, OpRecipient source) {
        if (ERROR.equals(spec.getOp())) {
            logger.warn("log: {}->{} {} {}", spec, value, object, source);
        } else {
            logger.debug("log: {}->{} {} {}", spec, value, object, source);
        }
    }

    public void once(JsonValue evfilter, OpRecipient fn) throws SwarmException {
        this.on(evfilter, new OnceOpRecipient(this, fn));
    }

    public void addUplink(Uplink uplink) {
        logger.debug("{}.addUplink({})", this, uplink);
        this.uplinks.add(uplink);
    }

    boolean isNotUplinked() {
        if (this.uplinks.isEmpty()) return true;

        for (Uplink peer : this.uplinks) {
            if (peer instanceof PendingUplink) {
                return true;
            }
        }
        return false;
    }

    final boolean hasUplink(Peer peer) {
        return this.uplinks.indexOf(peer) > -1;
    }

    final void addListener(OpRecipient listener) {
        logger.debug("{}.addListener({})", this, listener);
        this.listeners.add(listener);
    }

    public void removeListener(OpRecipient listener) {
        logger.debug("{}.removeListener({})", this, listener);

        @SuppressWarnings("unchecked") Iterator<OpRecipient> it = new ChainedIterators<OpRecipient>(
                this.uplinks.iterator(),
                this.listeners.iterator()
        );

        while (it.hasNext()) {
            OpRecipient l = it.next();
            if (l == listener) {
                it.remove();
                return;
            }

            // @see FilteringOpRecipient#equals() implementation
            if (l.equals(listener)) {
                logger.debug("{}.removeListener(): actualRemoved={}", this, l);
                it.remove();
                return;
            }
        }
    }

    final TypeMeta getTypeMeta() throws SwarmException {
        return this.host.getTypeMeta(this.getClass());
    }

    public final TypeToken getType() {
        return this.type;
    }

    @Override
    public final IdToken getPeerId() {
        return this.host == null ? null : this.host.getId();
    }

    @Override
    public String toString() {
        return getTypeId().toString();
    }

    public final JsonValue getFieldValue(String field) throws SwarmException {
        FieldMeta meta = getTypeMeta().getFieldMeta(field);
        if (meta == null) {
            return JsonValue.NULL;
        }
        return meta.get(this);
    }

    public final void setFieldValue(String field, JsonValue value) throws SwarmException {
        FieldMeta meta = getTypeMeta().getFieldMeta(field);
        if (meta == null) {
            logger.warn("{}.setFieldValue({}, {}): Trying to modify unknown field", this, field, value);
            return;
        }
        meta.set(this, value);
    }

    protected class Deferred extends FilteringOpRecipient<OpRecipient> {

        private final FullSpec spec;
        private final JsonValue filter;

        public Deferred(FullSpec spec, JsonValue filter, OpRecipient source) {
            super(source);
            this.spec = spec;
            this.filter = filter;
        }

        @Override
        public boolean filter(FullSpec spec, JsonValue value, OpRecipient source) {
            return !(Syncable.this.hasNoState() && Syncable.this.isNotUplinked());
        }

        @Override
        public void deliverInternal(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
            Syncable.this.removeListener(this);
            Syncable.this.deliver(this.spec, this.filter, this.getInner());
        }

        public OpRecipient getSource() {
            return getInner();
        }

        @Override
        public String toString() {
            return "" + Syncable.this.getTypeId() + ".Deferred{" +
                    "spec=" + spec +
                    ", filter=" + filter +
                    ", inner=" + inner +
                    '}';
        }
    }

}
