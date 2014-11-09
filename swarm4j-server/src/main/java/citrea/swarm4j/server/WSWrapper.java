package citrea.swarm4j.server;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.pipe.OpChannel;
import citrea.swarm4j.core.pipe.OpChannelListener;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 31/10/13
 *         Time: 17:58
 */
public class WSWrapper implements OpChannel {
    private static final Logger logger = LoggerFactory.getLogger(WSWrapper.class);

    private OpChannelListener sink;

    private final WebSocket ws;
    private final String pipeId;

    public WSWrapper(WebSocket ws, String pipeId) {
        this.ws = ws;
        this.pipeId = pipeId;
        logger.debug("{}.new", this);
    }

    @Override
    public void setSink(OpChannelListener sink) {
        this.sink = sink;
    }

    @Override
    public void sendMessage(String message) {
        logger.debug("{}.sendMessage({})", this, message);
        ws.send(message);
    }

    public void processMessage(String message) throws SwarmException {
        if (this.sink != null) {
            logger.debug("{}.processMessage({})", this, message);
            this.sink.onMessage(message);
        } else {
            logger.info("{}.processMessage({}): no sink, closing WebSocket", this, message);
            this.ws.close();
        }
    }

    @Override
    public void close() {
        logger.debug("{}.close()", this);
        if (sink != null) {
            sink.onClose(null);
        }
        ws.close();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WSWrapper wsWrapper = (WSWrapper) o;

        return ws.equals(wsWrapper.ws);
    }

    @Override
    public int hashCode() {
        return ws.hashCode();
    }

    @Override
    public String toString() {
        return "WS-" + this.pipeId;
    }
}
