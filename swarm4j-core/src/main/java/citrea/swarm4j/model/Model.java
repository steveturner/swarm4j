package citrea.swarm4j.model;

import citrea.swarm4j.model.callback.FieldChangeOpRecipient;
import citrea.swarm4j.model.callback.OpFilter;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.annotation.SwarmOperation;
import citrea.swarm4j.model.annotation.SwarmOperationKind;
import citrea.swarm4j.model.meta.FieldMeta;
import citrea.swarm4j.model.oplog.ModelLogDistillator;
import citrea.swarm4j.model.spec.*;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;


import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 29/10/13
 *         Time: 01:01
 */
public class Model extends Syncable {

    public static final SpecToken SET = new SpecToken(".set");

    /**
     * Model (LWW key-value object)
     * @param id object id
     * @param host swarm host object bound to
     */
    public Model(SpecToken id, Host host) throws SwarmException {
        super(id, host);
        this.logDistillator = new ModelLogDistillator();
    }

    public Model(JsonValue initialState, Host host) throws SwarmException {
        super(null, host);
        this.logDistillator = new ModelLogDistillator();
        this.set(initialState);
    }

    /**  init modes:
     *    1  fresh id, fresh object
     *    2  known id, stateless object
     *    3  known id, state boot
     */

    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    @Override
    public void on(Spec spec, JsonValue base, OpRecipient source) throws SwarmException {
        //  support the model.on('field',callback_fn) pattern
        if (!base.isNull() && base.isString()) {
            String possibleFieldName = base.asString();
            FieldMeta fieldMeta = getTypeMeta().getFieldMeta(possibleFieldName);
            if (fieldMeta != null) {
                //TODO check if field exists with a given name
                base = JsonValue.NULL;
                source = new OpFilter(new FieldChangeOpRecipient(source, possibleFieldName), SET);
            }
        }
        // this will delay response if we have no state yet
        super.on(spec, base, source);
    }

        /*init: function (spec,snapshot,host) {
         if (this._version && this._version!=='0')
         return; // FIXME tail FIXME
         snapshot && this.apply(snapshot);
         Syncable._pt.__init.apply(this,arguments);
         }*/

    // TODO remove unnecessary value duplication
    protected void packState(JsonValue state) {
    }

    protected void unpackState(JsonValue state) {
    }

    /**
     * This barebones Model class implements just one kind of an op:
     * set({key:value}). To implment your own ops you need to understand
     * implications of partial order as ops may be applied in slightly
     * different orders at different replicas. This implementation
     * may resort to distillLog() to linearize ops.
     */
    @SwarmOperation(kind = SwarmOperationKind.Logged)
    public void set(Spec spec, JsonValue value) throws SwarmException {
        Spec verOp = spec.getVersionOp();
        String version = verOp.getVersion().toString();
        if (this.version == null || this.version.compareTo(version) < 0) {
            this.oplog.put(verOp, value);
            this.distillLog(); // may amend the value
            value = this.oplog.get(verOp);
        }

        if (value != null && value.isObject()) {
            this.apply((JsonObject) value);
        }
    }

    // TODO should be generated
    public void set(JsonValue newFieldValues) throws SwarmException {
        this.deliver(this.newEventSpec(SET), newFieldValues, OpRecipient.NOOP);
    }

    public void set(String fieldName, JsonValue value) throws SwarmException {
        JsonObject fieldValues = new JsonObject();
        fieldValues.set(fieldName, value);
        this.set(fieldValues);
    }

    public void fill(String key) throws SwarmException { // TODO goes to Model to support references
        Spec spec = new Spec(this.getFieldValue(key).asString()).getTypeId();
        if (spec.getPattern() != SpecPattern.TYPE_ID) {
            throw new SwarmException("incomplete spec");
        }
        throw new UnsupportedOperationException("not supported yet");
        //this.setFieldValue(key, new SyncableRefJson(new SyncableRef(this.host.get(spec))));
    }

    /**
     * Generate .set operation after some of the model fields were changed
     * TODO write test for Model.save()
     */
    public void save() throws SwarmException {
        Map<String, JsonValue> cumul = this.distillLog();
        JsonObject changes = new JsonObject();
        JsonObject pojo = this.getPOJO(false);
        for (String field : pojo.names()) {
            JsonValue currentFieldValue = this.getFieldValue(field);
            if (!currentFieldValue.equals(cumul.get(field))) {
                // TODO nesteds
                changes.set(field, currentFieldValue);
            }
        }
        for (String field : cumul.keySet()) {
            if (pojo.get(field).isNull()) {
                changes.set(field, JsonValue.NULL); // JSON has no undefined
            }
        }

        this.set(changes);
    }

    @Override
    public String validate(Spec spec, JsonValue value) {
        if (!SET.equals(spec.getOp())) {
            // no idea
            return "";
        }

        if (!(value instanceof JsonObject)) {
            return "";
        }

        for (String key : ((JsonObject) value).names()) {
            FieldMeta fieldMeta = typeMeta.getFieldMeta(key);
            if (fieldMeta == null) {
                return "bad field name";
            }
        }
        return "";
    }

    //TODO reactions
    /*
    // Model may have reactions for field changes as well as for 'real' ops/events
    // (a field change is a .set operation accepting a {field:newValue} map)
    public static void addReaction(String methodOrField, fn) {
        var proto = this.prototype;
        if (typeof (proto[methodOrField]) === 'function') { // it is a field name
            return Syncable.addReaction.call(this, methodOrField, fn);
        } else {
            var wrapper = function (spec, val) {
                if (methodOrField in val) {
                    fn.apply(this, arguments);
                }
            };
            wrapper._rwrap = true;
            return Syncable.addReaction.call(this, 'set', wrapper);
        }
    }*/

    public JsonValue getFieldValue(String fieldName) {
        //TODO getFieldValue
        return JsonValue.NULL;
    }

    public void setFieldValue(String fieldName, JsonValue value) {
        //TODO setFieldValue
    }

}
