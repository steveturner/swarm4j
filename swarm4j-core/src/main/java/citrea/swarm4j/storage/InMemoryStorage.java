package citrea.swarm4j.storage;

import citrea.swarm4j.model.Host;
import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.*;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 /*
 * In a real storage implementation, state and log often go into
 * different backends, e.g. the state is saved to SQL/NoSQL db,
 * while the log may live in a key-value storage.
 * As long as the state has sufficient versioning info saved with
 * it (like a version vector), we may purge the log lazily, once
 * we are sure that the state is reliably saved. So, the log may
 * overlap with the state (some ops are already applied). That
 * provides some necessary resilience to workaround the lack of
 * transactions across backends.
 * In case third parties may write to the backend, go figure
 * some way to deal with it (e.g. make a retrofit operation).
 *
 * @author aleksisha
 *         Date: 26.08.2014
 *         Time: 00:33
 */
public class InMemoryStorage extends Storage {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryStorage.class);

    // TODO async storage
    private final Map<TypeIdSpec, Map<VersionOpSpec, JsonValue>> tails = new HashMap<TypeIdSpec, Map<VersionOpSpec, JsonValue>>();
    private final Map<TypeIdSpec, JsonObject> states = new HashMap<TypeIdSpec, JsonObject>();

    // TODO ??? storage listeners
    protected Map<Spec, List<OpRecipient>> listeners;

    public InMemoryStorage(IdToken id) {
        super(id);
        // many implementations do not push changes
        // so there are no listeners
        this.listeners = null;
    }

    @Override
    public void op(FullSpec spec, JsonValue val, OpRecipient source) throws SwarmException {
        if (!this.writeOp(spec, val, source)) {
            // The storage piggybacks on the object's state/log handling logic
            // First, it adds an op to the log tail unless the log is too long...
            // ...otherwise it sends back a subscription effectively requesting
            // the state, on state arrival zeroes the tail.
            source.deliver(spec.overrideOp(Syncable.REON), JsonValue.valueOf(Syncable.INIT.toString()), this);
        }
    }

    @Override
    protected void appendToLog(TypeIdSpec ti, JsonObject ver_op2val) throws SwarmException {
        throw new UnsupportedOperationException("Not supported for InMemoryStorage");
    }

    @Override
    public void patch(FullSpec spec, JsonValue patch) throws SwarmException {
        this.writeState(spec, patch);
    }

    @Override
    public TypeIdSpec getTypeId() {
        return new TypeIdSpec(Host.HOST, this.getPeerId());
    }

    @Override
    public void on(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
        TypeIdSpec ti = spec.getTypeId();

        if (this.listeners != null) {
            List<OpRecipient> ls = this.listeners.get(ti);
            if (ls == null) {
                ls = new ArrayList<OpRecipient>();
                this.listeners.put(ti, ls);
                ls.add(source);
            } else if (!ls.contains(source)) {
                ls.add(source);
            }
        }

        JsonObject state = this.readState(ti);
        if (state == null) {
            state = new JsonObject();
            state.set(Syncable.VERSION_FIELD, JsonValue.valueOf(SToken.ZERO_VERSION.toString()));
        }

        Map<VersionOpSpec, JsonValue> tail = this.readOps(ti);

        if (tail != null) {
            final JsonObject stateTail;
            JsonValue val = state.get(Syncable.TAIL_FIELD);
            if (val == null || !val.isObject()) {
                stateTail = new JsonObject();
            } else {
                stateTail = new JsonObject(val.asObject());
            }
            for (Map.Entry<VersionOpSpec, JsonValue> op : tail.entrySet()) {
                stateTail.set(op.getKey().toString(), op.getValue());
            }
            state.set(Syncable.TAIL_FIELD, stateTail);
        }
        VersionToken version = spec.getVersion();
        source.deliver(ti.fullSpec(version, Syncable.PATCH), state, this);
        source.deliver(ti.fullSpec(version, Syncable.REON), JsonValue.valueOf(Storage.stateVersionVector(state)), this);
    }

    @Override
    public void off(FullSpec spec, OpRecipient source) throws SwarmException {
        if (this.listeners == null) {
            return;
        }
        TypeIdSpec ti = spec.getTypeId();
        List<OpRecipient> ls = this.listeners.get(ti);
        if (ls == null) {
            return;
        }
        if (ls.contains(source)) {
            if (ls.size() == 1) {
                this.listeners.remove(ti);
            } else {
                ls.remove(source);
            }
        }
    }

    void writeState(FullSpec spec, JsonValue state) {
        TypeIdSpec ti = spec.getTypeId();
        if (state != null && state.isObject()) {
            this.states.put(ti, (JsonObject) state);
        }
        // tail is zeroed on state flush
        this.tails.put(ti, new HashMap<VersionOpSpec, JsonValue>());
    }

    boolean writeOp(FullSpec spec, JsonValue value, OpRecipient source) {
        TypeIdSpec ti = spec.getTypeId();
        VersionOpSpec vo = spec.getVersionOp();
        Map<VersionOpSpec, JsonValue> tail = this.tails.get(ti);
        if (tail == null) {
            tail = new HashMap<VersionOpSpec, JsonValue>();
            this.tails.put(ti, tail);
        }
        if (tail.containsKey(vo)) {
            logger.warn("op replay @storage");
        }
        tail.put(vo, value);
        int count = tail.size();
        return (count < 3);
    }

    protected JsonObject readState(TypeIdSpec ti) {
        JsonObject res = this.states.get(ti);
        if (res != null) { // clone to prevent changing
            res = JsonObject.readFrom(res.toString());
        }
        return res;
    }

    protected Map<VersionOpSpec, JsonValue> readOps(TypeIdSpec ti) {
        return this.tails.get(ti);
    }

}
