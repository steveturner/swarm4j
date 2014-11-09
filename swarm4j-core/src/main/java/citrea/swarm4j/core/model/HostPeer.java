package citrea.swarm4j.core.model;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.callback.Uplink;
import citrea.swarm4j.core.pipe.OpChannel;
import citrea.swarm4j.core.pipe.UnsupportedProtocolException;
import citrea.swarm4j.core.spec.*;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 08.09.2014
 *         Time: 23:33
 */
public interface HostPeer extends Uplink, ReferringToPeer {

    public FullSpec newEventSpec(OpToken op);

    void accept(OpChannel stream);

    void connect(URI upstreamURI, long reconnectTimeout, int connectionAttempt) throws SwarmException, UnsupportedProtocolException;

    void connect(OpChannel upstream) throws SwarmException;

    void disconnect(IdToken peerId) throws SwarmException;

    void disconnect();
}
