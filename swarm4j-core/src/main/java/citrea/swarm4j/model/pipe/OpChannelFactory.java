package citrea.swarm4j.model.pipe;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 14.09.2014
 *         Time: 12:02
 */
public interface OpChannelFactory {

    ConnectableOpChannel createChannel(URI uri) throws UnsupportedProtocolException;
}
