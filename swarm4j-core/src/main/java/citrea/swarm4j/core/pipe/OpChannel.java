package citrea.swarm4j.core.pipe;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 17:58
 */
public interface OpChannel {

    void setSink(OpChannelListener sink);
    void sendMessage(String message);

    void close();
}
