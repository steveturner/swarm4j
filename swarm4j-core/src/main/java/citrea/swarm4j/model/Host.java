package citrea.swarm4j.model;

import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.callback.Peer;
import citrea.swarm4j.model.callback.Uplink;
import citrea.swarm4j.model.annotation.SwarmOperation;
import citrea.swarm4j.model.annotation.SwarmOperationKind;
import citrea.swarm4j.model.clocks.Clock;
import citrea.swarm4j.model.clocks.SecondPreciseClock;
import citrea.swarm4j.model.hash.HashFunction;
import citrea.swarm4j.model.hash.SimpleHash;
import citrea.swarm4j.model.meta.TypeMeta;
import citrea.swarm4j.model.pipe.*;
import citrea.swarm4j.model.reflection.ReflectionTypeMeta;
import citrea.swarm4j.model.value.JSONUtils;
import citrea.swarm4j.storage.Storage;
import citrea.swarm4j.model.spec.*;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.net.URI;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 21.06.2014
 *         Time: 15:51
 */
public class Host extends Syncable implements HostPeer, Runnable {
    public static final TypeToken HOST = new TypeToken("/Host");
    private final Map<SToken, TypeMeta> knownTypes = new HashMap<SToken, TypeMeta>();

    private final BlockingQueue<QueuedOperation> queue = new LinkedBlockingQueue<QueuedOperation>();
    private final CountDownLatch started = new CountDownLatch(1);
    private Thread queueThread;

    private final Map<TypeIdSpec, Peer> sources = new HashMap<TypeIdSpec, Peer>();
    final Map<TypeIdSpec, Syncable> objects = new HashMap<TypeIdSpec, Syncable>();
    private Storage storage = null;

    // config
    private final Clock clock;
    private final Plumber plumber = new Plumber();
    private final OpChannelFactoryRegistry connectionFactory = new OpChannelFactoryRegistry();
    private final HashFunction hashFn = new SimpleHash();
    private boolean async = false;

    public Host(IdToken id, Storage storage) throws SwarmException {
        super(id, null);
        this.setHost(this);
        this.clock = new SecondPreciseClock(id.getBare());
        if (storage != null) {
            this.storage = storage;
            this.sources.put(this.getTypeId(), storage);
        }
    }

    public Host(IdToken id) throws SwarmException {
        this(id, null);
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    @Override
    void checkUplink() throws SwarmException {
        //do nothing for host
    }

    @Override
    public void deliver(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {

        if (queueThread != null && queueThread != Thread.currentThread()) {
            // queue
            try {
                if (value != null && value.isObject()) {
                    // value must be immutable, so prevent further modifications
                    value = JsonObject.unmodifiableObject(value.asObject());
                }
                queue.put(new QueuedOperation(spec, value, source));
            } catch (InterruptedException e) {
                throw new SwarmException(e.getMessage(), e);
            }
        } else {
            // process
            if (HOST.equals(spec.getType())) {
                super.deliver(spec, value, source);
            } else {
                logger.debug("{} <= ({}, {}, {})", this, spec, value, source);
                TypeIdSpec typeid = spec.getTypeId();
                Syncable obj = this.get(typeid);
                if (obj != null) {
                    obj.deliver(spec, value, source);
                }
            }
        }
    }

    public <T extends Syncable> T get(Class<T> type) throws SwarmException {
        TypeMeta typeMeta = getTypeMeta(type);
        return newInstance(typeMeta, null);
    }

    public <T extends Syncable> T get(TypeToken type) throws SwarmException {
        TypeMeta typeMeta = getTypeMeta(type);
        return newInstance(typeMeta, null);
    }

    public <T extends Syncable> T get(TypeIdSpec spec) throws SwarmException {
        T res;
        //noinspection unchecked
        res = (T) this.objects.get(spec);
        if (res != null) {
            return res;
        }

        TypeToken type = spec.getType();
        if (type == null) {
            throw new SwarmException("invalid spec (expecting \"/Type#id\" or \"/Type\")");
        }

        TypeMeta typeMeta = getTypeMeta(type);
        return newInstance(typeMeta, spec.getId());
    }

    private <T extends Syncable> T newInstance(TypeMeta typeMeta, IdToken id) throws SwarmException {
        @SuppressWarnings("unchecked") T res = (T) typeMeta.newInstance(id, this);
        //TODO defaults obj.apply(JsonValue.NULL);
        return res;
    }

    void addSource(FullSpec spec, Peer peer) throws SwarmException {
        logger.debug("{}.addSource({}, {})", this, spec, peer);
        //TODO their time is off so tell them so  //FIXME ???
        Peer old = this.sources.get(peer.getTypeId());
        if (old != null) {
            old.deliver(this.newEventSpec(OFF), JsonValue.NULL, this);
        }

        this.sources.put(peer.getTypeId(), peer);
        if (ON.equals(spec.getOp())) {
            peer.deliver(this.newEventSpec(REON), JsonValue.valueOf(this.clock.getTimeInMillis()), this); // TODO offset
        }

        for (Syncable obj: this.objects.values()) {
            obj.checkUplink();
        }

        this.emit(spec, JsonValue.NULL, peer); // PEX hook
    }

    @Override
    protected String validate(FullSpec spec, JsonValue val) {
        if (!HOST.equals(spec.getType()) && !this.clock.checkTimestamp(spec.getVersion())) {
            return "invalid timestamp " + spec;
        }
        return super.validate(spec, val);
    }

    /**
     * Host forwards on() calls to local objects to support some
     * shortcut notations, like
     *          host.on('/Mouse',callback)
     *          host.on('/Mouse.init',callback)
     *          host.on('/Mouse#Mickey',callback)
     *          host.on('/Mouse#Mickey.init',callback)
     *          host.on('/Mouse#Mickey!baseVersion',repl)
     *          host.on('/Mouse#Mickey!base.x',trackfn)
     * The target object may not exist beforehand.
     * Note that the specifier is actually the second 3sig parameter
     * (value). The 1st (spec) reflects this /Host.on invocation only.
     */
    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void on(FullSpec spec, JsonValue evfilter, OpRecipient source) throws SwarmException {
        if (JSONUtils.isFalsy(evfilter)) {// the subscriber needs "all the events"
            if (!(source instanceof Peer)) {
                throw new IllegalArgumentException("evfilter is empty but source is not a channel");
            }
            this.addSource(spec, (Peer) source);
            return;
        }

        String possibleHostMethodName = evfilter.asString();
        if (possibleHostMethodName != null && possibleHostMethodName.startsWith(SQuant.OP.toString())) {
            possibleHostMethodName = possibleHostMethodName.substring(1);
        }
        TypeToken typeToken;
        IdToken objId;
        if (getTypeMeta().getOperationMeta(possibleHostMethodName) != null) { //this Host operation listening
            typeToken = this.getType(); // "/Host#id"
            objId = this.getId();
        } else {
            FilterSpec objon = new FilterSpec(evfilter.asString());
            typeToken = objon.getType();
            objId = objon.getId();
            if (typeToken == null) throw new IllegalArgumentException("no type mentioned");
        }

        if (HOST.equals(typeToken)) {
            super.on(spec, evfilter, source);
        } else {
            if (objId == null) {
                objId = spec.getVersion().toIdToken();
            }
            FullSpec fullObjOn = new FullSpec(typeToken, objId, spec.getVersion(), ON);
            this.deliver(fullObjOn, evfilter, source);
        }
    }

    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void reon(FullSpec spec, JsonValue timeInMillis, OpRecipient source) throws SwarmException {
        if (!HOST.equals(spec.getType())) throw new IllegalArgumentException("/NotHost");
        /// well.... TODO
        if (!(source instanceof Peer)) throw new IllegalArgumentException("src is not a Peer");
        if (!timeInMillis.isNumber()) throw new IllegalArgumentException("value must be numeric");
        this.clock.adjustTime(timeInMillis.asLong());
        this.addSource(spec, (Peer) source);
    }

    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void off(FullSpec spec, OpRecipient src) throws SwarmException {
        if (!(src instanceof Peer)) throw new IllegalArgumentException("src is not a Peer");

        FullSpec reoffSpec = new FullSpec(
                ((Peer) src).getTypeId(),
                this.clock.issueTimestamp(),
                REOFF
        );
        src.deliver(reoffSpec, JsonValue.NULL, this);
        this.removeSource(spec.getTypeId(), (Peer) src);
    }

    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void reoff(FullSpec spec, OpRecipient source) throws SwarmException {
        if (!(source instanceof Peer)) throw new IllegalArgumentException("src is not a Peer");
        this.removeSource(spec.getTypeId(), (Peer) source);
    }

    private void removeSource(TypeIdSpec spec, Peer peer) throws SwarmException {
        if (!HOST.equals(spec.getType())) throw new IllegalArgumentException("/NotHost");

        if (this.sources.get(peer.getTypeId()) != peer) {
            //TODO log console.error('channel unknown', channel._id); //throw new Error
            return;
        }
        logger.debug("{}.removeSource({}, {})", this, spec, peer);
        this.sources.remove(peer.getTypeId());
        for (Map.Entry<TypeIdSpec, Syncable> sp : this.objects.entrySet()) {
            Syncable obj = sp.getValue();
            if (HOST.equals(obj.getType())) continue;

            obj.off(peer);
            if (obj.hasUplink(peer)) {
                obj.checkUplink();
            }
        }
    }

    VersionToken time() {
        return this.clock.issueTimestamp();
    }

    /**
     * Returns an array of sources (caches,storages,uplinks,peers)
     * a given replica should be subscribed to. This default
     * implementation uses a simple consistent hashing scheme.
     * Note that a client may be connected to many servers
     * (peers), so the uplink selection logic is shared.
     * @param spec some object specifier
     * @return list of currently available uplinks for specified object
     */
    List<Uplink> getSources(TypeIdSpec spec) {

        List<Uplink> uplinks = new ArrayList<Uplink>();
        int mindist = Integer.MAX_VALUE;
        Pattern rePeer = Pattern.compile("^swarm~"); // peers, not clients
        String target = spec.getId().getBody();
        Uplink closestPeer = null;

        String thisHostId = this.getId().getBody();
        Matcher m = rePeer.matcher(thisHostId);
        if (m.find()) {
            mindist = this.hashDistance(thisHostId, target);
            closestPeer = this.storage;
        } else if (this.storage != null) {
            uplinks.add(this.storage); // client-side cache
        }

        for (Map.Entry<TypeIdSpec, Peer> entry : this.sources.entrySet()) {
            String id = entry.getKey().getId().getBody();
            m = rePeer.matcher(id);
            if (!m.find()) continue;

            int dist = this.hashDistance(id, target);
            if (dist < mindist) {
                closestPeer = entry.getValue();
                mindist = dist;
            }
        }
        if (closestPeer != null) uplinks.add(0, closestPeer);
        return uplinks;
    }

    @Override
    boolean isNotUplinked() {
        for (TypeIdSpec peerId : this.sources.keySet()) {
            if (peerId.getId().getBare().startsWith("swarm~")) {
                return false;
            }
        }
        return true;
    }

    Syncable register(Syncable obj) {
        TypeIdSpec spec = obj.getTypeId();
        Syncable res = this.objects.get(spec);
        if (res == null) {
            this.objects.put(spec, obj);
            res = obj;
        }
        return res;
    }

    void unregister(Syncable obj) {
        TypeIdSpec spec = obj.getTypeId();
        // TODO unsubscribe from the uplink - swarm-scale gc
        if (this.objects.containsKey(spec)) {
            this.objects.remove(spec);
        }
    }

    // TODO Host event relay + PEX

    public void registerType(Class<? extends Syncable> type) throws SwarmException {
        TypeMeta typeMeta = new ReflectionTypeMeta(type);
        logger.info("registerType: {}", typeMeta.getDescription());
        this.knownTypes.put(new TypeToken(type.getSimpleName()), typeMeta);
    }

    TypeMeta getTypeMeta(SToken typeToken) throws SwarmException {
        TypeMeta res = knownTypes.get(typeToken);
        if (res == null) {
            throw new SwarmException("Unknown type: " + typeToken.toString());
        }
        return res;
    }

    public TypeMeta getTypeMeta(Class<? extends Syncable> type) throws SwarmException {
        TypeToken typeToken = new TypeToken(type.getSimpleName());
        TypeMeta res = knownTypes.get(typeToken);
        if (res != null) {
            return res;
        }

        try {
            this.registerType(type);
        } catch (SwarmException e) {
            throw new IllegalArgumentException("Error registering type: " + type.getName(), e);
        }
        return getTypeMeta(typeToken);
    }

    public void registerChannelFactory(String scheme, OpChannelFactory factory) {
        this.connectionFactory.registerFactory(scheme, factory);
    }

    @Override
    public void accept(OpChannel channel) {
        logger.info("{}.accept({})", this, channel);
        Pipe pipe = new Pipe(this, plumber);
        pipe.bindChannel(channel);
    }

    @Override
    public void connect(URI upstreamURI, long reconnectTimeout, int connectionAttempt) throws SwarmException, UnsupportedProtocolException {
        logger.info("{}.connect({}, {})", this, upstreamURI.toString(), reconnectTimeout);
        Pipe pipe = new Pipe(this, plumber);
        pipe.setReconnectionUri(upstreamURI);
        pipe.setReconnectTimeout(reconnectTimeout);
        pipe.setConnectionAttempt(connectionAttempt);
        final ConnectableOpChannel channel = this.connectionFactory.createChannel(upstreamURI);
        pipe.bindChannel(channel);
        channel.connect();
        pipe.deliver(newEventSpec(ON), JsonValue.NULL, this);
    }

    @Override
    public void connect(OpChannel upstream) throws SwarmException {
        Pipe pipe = new Pipe(this, plumber);
        pipe.bindChannel(upstream);
        pipe.deliver(newEventSpec(ON), JsonValue.NULL, this);
    }

    @Override
    public void disconnect(IdToken peerId) throws SwarmException {
        TypeIdSpec searchFor = new TypeIdSpec(HOST, peerId);
        for (TypeIdSpec id : sources.keySet()) {
            if (id.equals(searchFor)) {
                Peer peer = sources.get(id);
                // normally, .off is sent by a downlink
                peer.deliver(peer.getTypeId().fullSpec(this.time(), OFF), JsonValue.NULL, NOOP);
            }
        }
    }

    @Override
    public void disconnect() {
        for (Map.Entry<TypeIdSpec, Peer> entry : sources.entrySet()) {
            OpRecipient peer = entry.getValue();
            if (peer instanceof Pipe) {
                ((Pipe) peer).close(null);
            }
        }
    }

    int hashDistance(Object ipeer, Object obj) {
        int obj_hash;
        String peer;
        if (obj instanceof Number) {
            obj_hash = ((Number) obj).intValue();
        } else if (obj instanceof SomeSyncable) {
            obj_hash = hashFn.calc(((SomeSyncable) obj).getTypeId().getId().getBody());
        } else {
            obj_hash = hashFn.calc(obj.toString());
        }

        if (ipeer instanceof SomeSyncable) {
            peer = ((SomeSyncable) ipeer).getId().getBody();
        } else if (ipeer instanceof TypeIdSpec) {
            peer = ((TypeIdSpec) ipeer).getId().getBody();
        } else if (ipeer instanceof FullSpec) {
            peer = ((FullSpec) ipeer).getId().getBody();
        } else {
            peer = ipeer.toString();
        }

        int dist = Integer.MAX_VALUE;
        for (int i = 0; i < 3; i++) { //TODO 3 ~ HASH_POINTS
            int hash = hashFn.calc(peer + ":" + i);
            dist = Math.min(dist, hash ^ obj_hash);
        }
        return dist;
    }

    @Override
    public void run() {
        synchronized (this) {
            if (queueThread != null) {
                throw new IllegalStateException("Can't run the single host more than once");
            }
            queueThread = Thread.currentThread();
        }

        logger.info("started");
        this.started.countDown();
        try {
            while (!queueThread.isInterrupted()) {
                QueuedOperation op = queue.take();
                if (op == null) continue;

                try {
                    this.deliver(op.getSpec(), op.getValue(), op.getPeer());
                } catch (SwarmException e) {
                    //TODO fatal exception
                    logger.warn("Error processing operation: {}", op, e);
                }
            }
        } catch (InterruptedException e) {
            // ignore
        }

        logger.info("finished");
    }

    public void start() throws SwarmException {
        logger.info("{}.start()", this);
        if (this.storage != null) {
            this.storage.start();
        }
        this.plumber.start(getId());
        if (this.async) {
            new Thread(this, getPeerId().toString()).start();
        } else {
            this.started.countDown();
        }
    }

    public void waitForStart() throws InterruptedException {
        this.started.await();
        if (this.storage != null) {
            this.storage.waitForStart();
        }
    }

    public void stop() {
        logger.info("{}.stop()", this);
        synchronized (this) {
            if (queueThread != null) {
                queueThread.interrupt();
            }
        }
        if (this.storage != null) {
            this.storage.stop();
        }
        this.plumber.stop();
    }

}
