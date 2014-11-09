package citrea.swarm4j.client;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.pipe.ConnectableOpChannel;
import citrea.swarm4j.core.pipe.OpChannelListener;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 14.09.2014
 *         Time: 12:47
 */
public class WSChannel implements ConnectableOpChannel {
    private final Logger logger = LoggerFactory.getLogger(WSChannel.class);

    private final Queue<String> buffer = new LinkedList<String>();
    private final URI uri;
    private boolean open;
    private OpChannelListener sink;
    private WebSocketClient ws;

    public WSChannel(URI uri) {
        this.uri = uri;
        this.open = false;
    }

    @Override
    public void setSink(OpChannelListener sink) {
        this.sink = sink;
    }

    @Override
    public synchronized void sendMessage(String message) {
        if (this.open) {
            ws.send(message);
        } else {
            buffer.add(message);
        }
    }

    @Override
    public void connect() {
        this.ws = new WebSocketClient(this.uri, new Draft_10(), null, 3000) {
            @Override
            public void onOpen(ServerHandshake handshakeData) {
                WSChannel.this.onOpen(handshakeData);
            }

            @Override
            public void onMessage(String message) {
                WSChannel.this.onMessage(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                WSChannel.this.onClose(code, reason, remote);
            }

            @Override
            public void onError(Exception ex) {
                WSChannel.this.onError(ex);
            }
        };
        this.ws.connect();
    }

    @Override
    public void close() {
        this.ws.close();
    }

    public synchronized void onOpen(ServerHandshake handshakeData) {
        logger.debug("{}.onOpen({}, {})", handshakeData.getHttpStatus(), handshakeData.getHttpStatusMessage());
        while (!buffer.isEmpty()) {
            ws.send(buffer.poll());
        }
        this.open = true;
    }

    public void onMessage(String message) {
        if (this.sink == null) return;

        logger.debug("{}.onMessage({})", this, message);
        try {
            this.sink.onMessage(message);
        } catch (SwarmException e) {
            //TODO log exception
            this.close();
        }
    }

    public void onClose(int code, String reason, boolean remote) {
        if (sink == null) return;

        logger.debug("{}.onClose({}, {})", code, reason);
        if (CloseFrame.NORMAL == code) {
            sink.onClose(null);
        } else {
            sink.onClose(reason);
        }
    }

    public void onError(Exception ex) {
        logger.warn("{}.onError({})", this, ex.getMessage(), ex);
        //wait for onClose: this.sink.onClose(ex.getMessage());
    }

    @Override
    public String toString() {
        return "WS{" + this.uri + "}";
    }
}
