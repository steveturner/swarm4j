package citrea.swarm4j.core.pipe;

import citrea.swarm4j.core.spec.Spec;
import com.eclipsesource.json.JsonValue;


/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 16:34
 */
public class SelfHandshakeSwarmException extends InvalidHandshakeSwarmException {
    public SelfHandshakeSwarmException(Spec spec, JsonValue value) {
        super(spec, value);
    }
}
