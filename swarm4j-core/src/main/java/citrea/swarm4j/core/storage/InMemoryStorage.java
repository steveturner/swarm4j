package citrea.swarm4j.core.storage;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.spec.*;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * In-memory storage implementation. Used mostly in tests.
 *
 * @author aleksisha
 *         Date: 26.08.2014
 *         Time: 00:33
 */
public class InMemoryStorage extends Storage {

    private final Logger logger = LoggerFactory.getLogger(InMemoryStorage.class);

    // in-mem storage
    private Map<TypeIdSpec, String> states = new HashMap<TypeIdSpec, String>();
    private Map<TypeIdSpec, Map<VersionOpSpec, String>> tails = new HashMap<TypeIdSpec, Map<VersionOpSpec, String>>();

    public InMemoryStorage(IdToken id) {
        super(id);
        // many implementations do not push changes
        // so there are no listeners
        listeners = null;
    }

    protected JsonObject readState(TypeIdSpec ti) throws SwarmException {
        String stateSerialized = states.get(ti);
        return stateSerialized == null ? null : JsonObject.readFrom(stateSerialized);
    }

    protected JsonObject readOps(TypeIdSpec ti) throws SwarmException {
        Map<VersionOpSpec, String> tailSerialized = tails.get(ti);
        JsonObject tail;
        if (tailSerialized == null) {
            tail = null;
        } else {
            tail = new JsonObject();
            for (Map.Entry<VersionOpSpec, String> entry : tailSerialized.entrySet()) {
                tail.set(entry.getKey().toString(), JsonObject.readFrom(entry.getValue()));
            }
        }
        return tail;
    }

    protected void writeState(TypeIdSpec ti, JsonValue state) throws SwarmException {
        states.put(ti, state.toString());
    }

    protected void writeOp(FullSpec spec, JsonValue value) throws SwarmException {
        TypeIdSpec ti = spec.getTypeId();
        VersionOpSpec vm = spec.getVersionOp();
        Map<VersionOpSpec, String> tail = tails.get(ti);
        if (tail == null) {
            tail = new HashMap<VersionOpSpec, String>();
            tails.put(ti, tail);
        }
        if (tail.containsKey(vm)) {
            logger.warn("op replay @storage {}", vm.toString(), new SwarmException("op replay"));
        }
        tail.put(vm, value.toString());
    }

    @Override
    protected void cleanUpCache(TypeIdSpec ti) {
        // never clean
    }
}
