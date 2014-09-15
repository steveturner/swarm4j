package citrea.swarm4j.model.pipe;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 14.09.2014
 *         Time: 12:06
 */
public class UnsupportedProtocolException extends Exception {

    public UnsupportedProtocolException(String protocol) {
        super("Unsupported protocol: " + protocol);
    }
}
