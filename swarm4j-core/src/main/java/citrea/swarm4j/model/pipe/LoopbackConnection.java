package citrea.swarm4j.model.pipe;

import citrea.swarm4j.model.Host;
import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.callback.Uplink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 22:36
 */
public class LoopbackConnection implements ConnectableOpChannel {

    private static AtomicInteger seq = new AtomicInteger(0);

    private static final Logger logger = LoggerFactory.getLogger(LoopbackConnection.class);

    private final int id;
    private Host uplink;
    private LoopbackConnection paired;
    private OpChannelListener sink;
    private boolean opened;

    public LoopbackConnection(Host uplink) {
        this.id = seq.incrementAndGet();
        this.uplink = uplink;
        this.paired = new LoopbackConnection(this);
    }

    public LoopbackConnection(LoopbackConnection paired) {
        this.id = paired.id;
        this.paired = paired;
    }

    @Override
    public void setSink(OpChannelListener sink) {
        this.sink = sink;
    }

    @Override
    public void sendMessage(String message) {
        if (!this.opened || !this.paired.opened) {
            logger.debug("{}.sendMessage({}) not opened", this, message);
            return;
        }

        logger.debug("{}.sendMessage({})", this, message);
        try {
            this.paired.sink.onMessage(message);
        } catch (SwarmException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void connect() {
        logger.debug("{}.connect()", this);
        if (this.uplink != null) {
            this.paired.opened = true;
            this.opened = true;
            this.uplink.accept(this.paired);
        }
    }

    @Override
    public void close() {
        logger.debug("{}.close()", this);
        if (this.opened) {
            this.opened = false;
            if (this.sink != null) {
                this.sink.onClose("channel closed");
            }
            this.paired.close();
        }
    }

    public OpChannel getPaired() {
        return paired;
    }

    @Override
    public String toString() {
        Uplink up = uplink != null ? uplink : paired.uplink;
        return "Loopback#" + id + "{" + (uplink == null ? "up, " : "down, ") + up.getTypeId() + "}";
    }
}
