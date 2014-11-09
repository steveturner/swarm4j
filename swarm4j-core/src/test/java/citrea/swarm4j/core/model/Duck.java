package citrea.swarm4j.core.model;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.model.annotation.SwarmField;
import citrea.swarm4j.core.model.annotation.SwarmOperation;
import citrea.swarm4j.core.model.annotation.SwarmOperationKind;
import citrea.swarm4j.core.model.annotation.SwarmType;
import citrea.swarm4j.core.callback.OpRecipient;
import citrea.swarm4j.core.spec.*;
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

    private static final OpToken GROW = new OpToken(".grow");

    @SwarmField()
    public Integer age;

    @SwarmField()
    public Integer height;

    @SwarmField()
    public String mood = "neutral";

    public Duck(Host host2) throws SwarmException {
        super((IdToken) null, host2);
    }

    public Duck(IdToken id, Host host) throws SwarmException {
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
    public void grow(FullSpec spec, JsonValue by, OpRecipient source) {
        if (by != null && by.isNumber()) {
            int byAsInt = by.asInt();
            this.height += byAsInt;
        }
    }

    // should be generated
    public void grow(int by) throws SwarmException {
        trigger(GROW, JsonValue.valueOf(by));
    }
}
