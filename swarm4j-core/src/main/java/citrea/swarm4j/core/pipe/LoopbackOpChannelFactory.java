package citrea.swarm4j.core.pipe;

import citrea.swarm4j.core.model.Host;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 14.09.2014
 *         Time: 15:25
 */
public class LoopbackOpChannelFactory implements OpChannelFactory {
    public static final String SCHEME = "loopback";

    private final Host uplink;

    public LoopbackOpChannelFactory(Host uplink) {
        this.uplink = uplink;
    }

    @Override
    public ConnectableOpChannel createChannel(URI uri) throws UnsupportedProtocolException {
        return new LoopbackConnection(this.uplink);
    }
}
