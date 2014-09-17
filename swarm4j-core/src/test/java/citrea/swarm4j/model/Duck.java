package citrea.swarm4j.model;

import citrea.swarm4j.model.annotation.SwarmField;
import citrea.swarm4j.model.annotation.SwarmOperation;
import citrea.swarm4j.model.annotation.SwarmOperationKind;
import citrea.swarm4j.model.annotation.SwarmType;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 26.08.2014
 *         Time: 21:39
 */
@SwarmType("Duck")
public class Duck extends Model {

    private static final SpecToken GROW = new SpecToken(".grow");

    @SwarmField()
    public Integer age;

    @SwarmField()
    public Integer height;

    @SwarmField()
    public String mood = "neutral";

    public Duck(Host host2) throws SwarmException {
        super((SpecToken) null, host2);
    }

    public Duck(SpecToken id, Host host) throws SwarmException {
        super(id, host);
    }

    public Duck(JsonObject initialState, Host host2) throws SwarmException {
        super(initialState, host2);
    }

    // Simply a regular convenience method
    public boolean canDrink() {
        return this.age >= 18; // Russia
    }

    public String validate(Spec spec, JsonValue value) {
        return ""; // :|
        //return spec.op()!=='set' || !('height' in val);
        //throw new Error("can't set height, may only grow");
    }

    @SwarmOperation(kind = SwarmOperationKind.Logged)
    public void grow(Spec spec, JsonValue by, OpRecipient source) {
        if (by != null && by.isNumber()) {
            int byAsInt = by.asInt();
            this.height += byAsInt;
        }
    }

    // should be generated
    public void grow(int by) throws SwarmException {
        Spec growSpec = this.newEventSpec(GROW);
        this.deliver(growSpec, JsonValue.valueOf(by), OpRecipient.NOOP);
    }
}
