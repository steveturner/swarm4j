package citrea.swarm4j.model.pipe;

import citrea.swarm4j.model.SwarmException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 15:49
 */
public interface OpChannelListener {

    public void onMessage(String message) throws SwarmException;
    public void onClose(String error);
}
