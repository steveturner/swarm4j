package citrea.swarm4j.client;

import citrea.swarm4j.core.pipe.ConnectableOpChannel;
import citrea.swarm4j.core.pipe.OpChannelFactory;
import citrea.swarm4j.core.pipe.UnsupportedProtocolException;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 14.09.2014
 *         Time: 12:28
 */
public class WebSocketChannelFactory implements OpChannelFactory {


    @Override
    public ConnectableOpChannel createChannel(URI uri) throws UnsupportedProtocolException {
        return new WSChannel(uri);
    }
}
