package citrea.swarm4j.core.pipe;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.spec.Spec;
import com.eclipsesource.json.JsonValue;


/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 16:32
 */
public class InvalidHandshakeSwarmException extends SwarmException {
    public InvalidHandshakeSwarmException(String message) {
        super(message);
    }

    public InvalidHandshakeSwarmException(Spec spec, JsonValue value) {
        this(spec.toString() + "->" + value.toString());
    }
}
