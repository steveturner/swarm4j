package citrea.swarm4j.core.meta.reflection;

import citrea.swarm4j.core.SwarmException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 22.08.2014
 *         Time: 16:10
 */
public class SwarmMethodInvocationException extends SwarmException {

    public SwarmMethodInvocationException(String message) {
        super(message);
    }

    public SwarmMethodInvocationException(String message, Throwable inner) {
        super(message, inner);
    }
}
