package citrea.swarm4j.core.pipe;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 14.09.2014
 *         Time: 12:07
 */
public class OpChannelFactoryRegistry implements OpChannelFactory {

    private final Map<String, OpChannelFactory> supportedFactories = new HashMap<String, OpChannelFactory>();

    public void registerFactory(String scheme, OpChannelFactory factory) {
        supportedFactories.put(scheme, factory);
    }

    @Override
    public ConnectableOpChannel createChannel(URI uri) throws UnsupportedProtocolException {
        final String scheme = uri.getScheme();
        OpChannelFactory factory = supportedFactories.get(scheme);
        if (factory == null) throw new UnsupportedProtocolException(scheme);

        return factory.createChannel(uri);
    }
}
