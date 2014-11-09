package citrea.swarm4j.core.callback;

import citrea.swarm4j.core.model.ReferringToPeer;
import citrea.swarm4j.core.spec.IdToken;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 21.06.2014
 *         Time: 16:33
 */
public interface Peer extends Uplink, ReferringToPeer {

    void setPeerId(IdToken id);
}
