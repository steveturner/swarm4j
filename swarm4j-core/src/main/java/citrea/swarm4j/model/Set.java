package citrea.swarm4j.model;

import citrea.swarm4j.model.annotation.SwarmOperation;
import citrea.swarm4j.model.annotation.SwarmOperationKind;
import citrea.swarm4j.model.annotation.SwarmType;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.oplog.ModelLogDistillator;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecPattern;
import citrea.swarm4j.model.spec.SpecToken;
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

    public static final SpecToken CHANGE = new SpecToken(".change");
    public static final String VALID = "";

    public static final JsonValue TRUE = JsonValue.valueOf(1);
    public static final JsonValue FALSE = JsonValue.valueOf(0);

    private Map<Spec, T> objects = new HashMap<>();
    private OpRecipient proxy = new ObjectProxy();
    private List<ObjListener> objectListeners = new ArrayList<>();

    public Set(SpecToken id, Host host) throws SwarmException {
        super(id, host);
        this.logDistillator = new ModelLogDistillator();
    }

    /**
     * Both Model and Set are oplog-only; they never pass the state on the wire,
     * only the oplog; new replicas are booted with distilled oplog as well.
     * So, this is the only point in the code that mutates the state of a Set.
     */
    @SwarmOperation(kind = SwarmOperationKind.Logged)
    public void change(Spec spec, JsonValue value, OpRecipient source) throws SwarmException {
        value = this.distillOp(spec, value);
        if (!value.isObject()) return;

        JsonObject jo = (JsonObject) value;

        for (String keySpecStr : jo.names()) {
            Spec key_spec = new Spec(keySpecStr);
            if (TRUE.equals(jo.get(keySpecStr))) {
                T obj = this.host.get(key_spec);
                this.objects.put(key_spec, obj);
                obj.on(JsonValue.NULL, this.proxy);
            } else if (FALSE.equals(jo.get(keySpecStr))) {
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
        this.change(this.newEventSpec(CHANGE), changes, NOOP);
    }

    public void onObjects(JsonValue filter, OpRecipient callback) {
        this.objectListeners.add(new ObjListener(filter, callback));
    }

    public void offObjects(JsonValue filter, OpRecipient callback) {
        Iterator<ObjListener> it = this.objectListeners.iterator();
        while (it.hasNext()) {
            ObjListener item = it.next();
            if (filter.equals(item.filter) && callback.equals(item.listener)) {
                it.remove();
            }
        }
    }

    public void onObjectEvent(Spec spec, JsonValue value, OpRecipient source) throws SwarmException {
        Spec specFilter = new Spec(value.asString());
        for(ObjListener entry : this.objectListeners) {
            if (!entry.filter.isNull() && !spec.fits(specFilter)) {
                continue;
            }
            entry.listener.deliver(spec, value, source);
        }
    }

    @Override
    public String validate(Spec spec, JsonValue val) {
        if (!CHANGE.equals(spec.getOp())) {
            return VALID;
        }
        if (val.isObject()) {
            JsonObject jo = (JsonObject) val;
            for (String keySpecStr : jo.names()) {
                // member spec validity
                Spec key_spec = new Spec(keySpecStr);
                if (key_spec.getPattern() != SpecPattern.TYPE_ID) {
                    return "invalid spec: " + keySpecStr;
                }
            }
        }
        return VALID;
    }

    public JsonValue distillOp(Spec spec, JsonValue val) {
        if (this.version == null || this.version.compareTo(spec.getVersion().toString()) < 0) { //TODO check condition
            return val; // no concurrent op
        }
        Spec opkey = spec.getVersionOp();
        this.oplog.put(opkey, val);
        this.distillLog(); // may amend the value
        val = this.oplog.get(opkey);
        return (val == null ? JsonValue.NULL : val);
    }

    /**
     * Adds an object to the set.
     * @param obj the object  //TODO , its id or its specifier.
     */
    public void addObject(T obj) throws SwarmException {
        final Spec key_spec = obj.getTypeId();
        if (key_spec.getPattern() != SpecPattern.TYPE_ID) {
            throw new IllegalArgumentException("invalid spec: " + key_spec);
        }
        JsonObject changes = new JsonObject();
        changes.set(key_spec.toString(), TRUE);
        change(changes);
    }

    // FIXME reactions to emit .add, .remove

    public void removeObject(Spec key_spec) throws SwarmException {
        key_spec = key_spec.getTypeId();
        if (key_spec.getPattern() != SpecPattern.TYPE_ID) {
            throw new IllegalArgumentException("invalid spec: " + key_spec);
        }
        JsonObject changes = new JsonObject();
        changes.set(key_spec.toString(), FALSE);
        change(changes);
    }

    public void removeObject(T obj) throws SwarmException {
        removeObject(obj.getTypeId());
    }

    /**
     * @param key_spec key (specifier)
     * @return object by key
     */
    public T get(Spec key_spec) {
        key_spec = key_spec.getTypeId();
        if (key_spec.getPattern() != SpecPattern.TYPE_ID) {
            throw new IllegalArgumentException("invalid spec: " + key_spec);
        }
        return this.objects.get(key_spec);
    }

    /**
     * @param order ordering
     * @return sorted list of objects currently in set
     */
    public List<T> list(Comparator<T> order) {
        List<T> ret = new ArrayList<>();
        for (T obj : this.objects.values()) {
            ret.add(obj);
        }
        Collections.sort(ret, order);
        return ret;
    }

    public class ObjectProxy implements OpRecipient {

        @Override
        public void deliver(Spec spec, JsonValue value, OpRecipient source) throws SwarmException {
            Set.this.onObjectEvent(spec, value, source);
        }

        @Override
        public String toString() {
            return "" + Set.this.getTypeId() + ".ObjectProxy";
        }
    }

    private class ObjListener {
        JsonValue filter;
        OpRecipient listener;

        private ObjListener(JsonValue filter, OpRecipient listener) {
            this.filter = filter;
            this.listener = listener;
        }
    }
}
