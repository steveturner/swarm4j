package citrea.swarm4j.model;

import citrea.swarm4j.model.annotation.SwarmOperation;
import citrea.swarm4j.model.annotation.SwarmOperationKind;
import citrea.swarm4j.model.annotation.SwarmType;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.oplog.ModelLogDistillator;
import citrea.swarm4j.model.spec.*;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;


import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 05.09.2014
 *         Time: 00:48
 */
@SwarmType
public class Set<T extends Syncable> extends Syncable {

    public static final OpToken CHANGE = new OpToken(".change");
    public static final String VALID = "";

    public static final JsonValue TRUE = JsonValue.valueOf(1);
    public static final JsonValue FALSE = JsonValue.valueOf(0);

    private final Map<TypeIdSpec, T> objects = new HashMap<TypeIdSpec, T>();
    private final ProxyListener proxy = new ProxyListener(this);

    public Set(IdToken id, Host host) throws SwarmException {
        super(id, host);
        this.logDistillator = new ModelLogDistillator();
    }

    /**
     * Both Model and Set are oplog-only; they never pass the state on the wire,
     * only the oplog; new replicas are booted with distilled oplog as well.
     * So, this is the only point in the code that mutates the state of a Set.
     */
    @SwarmOperation(kind = SwarmOperationKind.Logged)
    public void change(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
        value = this.distillOp(spec, value);
        if (!value.isObject()) return;

        JsonObject jo = (JsonObject) value;

        for (JsonObject.Member entry : jo) {
            String keySpecStr = entry.getName();
            TypeIdSpec key_spec = new TypeIdSpec(keySpecStr);
            JsonValue val = entry.getValue();
            if (TRUE.equals(val)) {
                T obj = this.host.get(key_spec);
                this.objects.put(key_spec, obj);
                obj.on(JsonValue.NULL, this.proxy);
            } else if (FALSE.equals(val)) {
                T obj = this.objects.get(key_spec);
                if (obj != null) {
                    obj.off(this.proxy);
                    this.objects.remove(key_spec);
                }
            } else {
                logger.warn("unexpected value: {}->{}", spec, value);
            }
        }
    }

    // should be generated?
    public void change(JsonObject changes) throws SwarmException {
        this.deliver(this.newEventSpec(CHANGE), changes, NOOP);
    }

    public void onObjects(OpRecipient callback) {
        this.proxy.on(callback);
    }

    public void offObjects(OpRecipient callback) {
        this.proxy.off(callback);
    }

    @Override
    public String validate(FullSpec spec, JsonValue val) {
        if (!CHANGE.equals(spec.getOp())) {
            return VALID;
        }
        if (val.isObject()) {
            JsonObject jo = (JsonObject) val;
            for (String keySpecStr : jo.names()) {
                // member spec validity
                try {
                    new TypeIdSpec(keySpecStr);
                } catch (IllegalArgumentException e) {
                    return "invalid spec: " + keySpecStr;
                }
            }
        }
        return VALID;
    }

    JsonValue distillOp(FullSpec spec, JsonValue val) {
        if (this.version == null || this.version.compareTo(spec.getVersion().toString()) < 0) { //TODO check condition
            return val; // no concurrent op
        }
        VersionOpSpec opkey = spec.getVersionOp();
        this.oplog.put(opkey, val);
        this.distillLog(); // may amend the value
        val = this.oplog.get(opkey);
        return (val == null ? JsonValue.NULL : val);
    }

    /**
     * Adds an object to the set.
     * @param obj the object  //TODO , its id or its specifier.
     */
    public void add(T obj) throws SwarmException {
        final TypeIdSpec key_spec = obj.getTypeId();
        JsonObject changes = new JsonObject();
        changes.set(key_spec.toString(), TRUE);
        change(changes);
    }

    public void add(TypeIdSpec typeId) throws SwarmException {
        JsonObject changes = new JsonObject();
        changes.set(typeId.toString(), TRUE);
        change(changes);
    }

    // FIXME reactions to emit .add, .remove

    public void remove(TypeIdSpec key_spec) throws SwarmException {
        JsonObject changes = new JsonObject();
        changes.set(key_spec.toString(), FALSE);
        change(changes);
    }

    public void remove(T obj) throws SwarmException {
        remove(obj.getTypeId());
    }

    /**
     * @param key_spec key (specifier)
     * @return object by key
     */
    public T get(TypeIdSpec key_spec) {
        return this.objects.get(key_spec);
    }

    /**
     * @param order ordering
     * @return sorted list of objects currently in set
     */
    public List<T> list(Comparator<T> order) {
        List<T> ret = this.list();
        Collections.sort(ret, order);
        return ret;
    }

    public List<T> list() {
        return new ArrayList<T>(this.objects.values());
    }
}
