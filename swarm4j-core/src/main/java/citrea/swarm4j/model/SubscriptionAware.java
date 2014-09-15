package citrea.swarm4j.model;

import citrea.swarm4j.model.annotation.SwarmOperation;
import citrea.swarm4j.model.annotation.SwarmOperationKind;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;
import com.eclipsesource.json.JsonValue;


/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 03.09.2014
 *         Time: 22:51
 */
public interface SubscriptionAware {
    public static final SpecToken ON = new SpecToken(".on");
    public static final SpecToken REON = new SpecToken(".reon");
    public static final SpecToken OFF = new SpecToken(".off");
    public static final SpecToken REOFF = new SpecToken(".reoff");

    void on(Spec spec, JsonValue filterValue, OpRecipient source) throws SwarmException;

    void reon(Spec spec, JsonValue base, OpRecipient source) throws SwarmException;

    void off(Spec spec, OpRecipient repl) throws SwarmException;

    void reoff(Spec spec, OpRecipient repl) throws SwarmException;

    // should be generated?
    void on(JsonValue evfilter, OpRecipient source) throws SwarmException;

    // should be generated?
    void off(OpRecipient source) throws SwarmException;
}
