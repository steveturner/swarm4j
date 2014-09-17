package citrea.swarm4j.model;

import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.FullSpec;
import citrea.swarm4j.model.spec.OpToken;
import com.eclipsesource.json.JsonValue;


/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 03.09.2014
 *         Time: 22:51
 */
public interface SubscriptionAware {
    public static final OpToken ON = new OpToken(".on");
    public static final OpToken REON = new OpToken(".reon");
    public static final OpToken OFF = new OpToken(".off");
    public static final OpToken REOFF = new OpToken(".reoff");

    void on(FullSpec spec, JsonValue filterValue, OpRecipient source) throws SwarmException;

    void reon(FullSpec spec, JsonValue base, OpRecipient source) throws SwarmException;

    void off(FullSpec spec, OpRecipient repl) throws SwarmException;

    void reoff(FullSpec spec, OpRecipient repl) throws SwarmException;

    // should be generated?
    void on(JsonValue evfilter, OpRecipient source) throws SwarmException;

    // should be generated?
    void off(OpRecipient source) throws SwarmException;
}
