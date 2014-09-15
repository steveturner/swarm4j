package citrea.swarm4j.model.pipe;

import citrea.swarm4j.model.*;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.callback.Peer;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 15:49
 *
 */
public class Pipe implements OpChannelListener, OpRecipient, Peer {
    public static Logger logger = LoggerFactory.getLogger(Pipe.class);

    private static final AtomicInteger idSeq = new AtomicInteger(0);

    public static final Set<SpecToken> SUBSCRIPTION_OPERATIONS = new HashSet<>(Arrays.asList(
            Syncable.ON,
            Syncable.REON
    ));

    private final int id;
    protected final Plumber plumber;
    protected State state;
    protected long lastRecvTS = -1L;
    protected long lastSendTS = -1L;

    protected HostPeer host;
    protected URI uri;
    protected OpChannel channel;
    protected SpecToken peerId;
    protected Boolean isOnSent = null;
    protected long reconnectTimeout;
    protected int connectionAttempt;

    public Pipe(HostPeer host, Plumber plumber) {
        this.id = idSeq.incrementAndGet();
        this.host = host;
        this.plumber = plumber;
        this.state = State.NEW;
        this.connectionAttempt = 0;
    }

    @Override
    public void onMessage(String message) throws SwarmException {
        if (logger.isDebugEnabled()) {
            logger.debug("{} << {}", this, message);
        }
        this.lastRecvTS = new Date().getTime();
        for (Map.Entry<Spec, JsonValue> op : parse(message).entrySet()) {
            switch (state) {
                case NEW:
                    processHandshake(op.getKey(), op.getValue());
                    break;
                case HANDSHAKEN:
                    host.deliver(op.getKey(), op.getValue(), this);
                    break;
                case CLOSED:
                    logger.warn("{}.onMessage() but pipe closed", this);
                    break;
            }
        }
    }

    @Override
    public void onClose(String error) {
        logger.debug("{}.onClose({})", this, error);
        this.channel.setSink(null);
        this.channel = null;
        this.close(error);
    }

    @Override
    public void deliver(Spec spec, JsonValue value, OpRecipient source) throws SwarmException {
        String message = Pipe.serialize(spec, value);

        sendMessage(message);

        if (Host.HOST.equals(spec.getType())) {
            final SpecToken op = spec.getOp();
            if (Syncable.ON.equals(op)) {
                this.isOnSent = true;
            } else if (Syncable.REON.equals(op)) {
                this.isOnSent = false;
            } else if (Syncable.OFF.equals(op)) {
                this.close(null);
            } else if (Syncable.REOFF.equals(op)) {
                this.isOnSent = null;
            }
        }
    }

    protected void sendMessage(String message) {
        if (logger.isDebugEnabled()) {
            logger.debug("{} >> {}", this, message);
        }
        if (channel == null) {
            return;
        }
        channel.sendMessage(message);
        lastSendTS = new Date().getTime();
    }

    protected void processHandshake(Spec spec, JsonValue value) throws SwarmException {
        if (spec == null) {
            throw new IllegalArgumentException("handshake has no spec");
        }
        if (!Host.HOST.equals(spec.getType())) {
            throw new InvalidHandshakeSwarmException(spec, value);
        }
        if (this.host.getPeerId().equals(spec.getId())) {
            throw new SelfHandshakeSwarmException(spec, value);
        }
        SpecToken op = spec.getOp();
        Spec evspec = spec.overrideToken(this.host.getPeerId()).sort();

        if (SUBSCRIPTION_OPERATIONS.contains(op)) { // access denied TODO
            this.setPeerId(spec.getId());
            this.host.deliver(evspec, value, this);
        } else {
            throw new InvalidHandshakeSwarmException(spec, value);
        }
    }

    public void bindChannel(OpChannel channel) {
        this.channel = channel;
        this.channel.setSink(this);
    }

    public void setReconnectionUri(URI upstreamURI) {
        this.uri = upstreamURI;
    }

    public void close(String error) {
        logger.info("{}.close({})", this, error != null ? error : "");
        if (error != null && this.uri != null && !State.CLOSED.equals(state)) {
            plumber.reconnect(this);
        }
        if (State.HANDSHAKEN.equals(state)) {
            if (this.isOnSent != null) {
                // emulate normal off
                Spec offspec = this.host.newEventSpec(this.isOnSent ? Syncable.OFF : Syncable.REOFF);
                try {
                    this.host.deliver(offspec, JsonValue.NULL, this);
                } catch (SwarmException e) {
                    logger.warn("{}.close(): Error delivering {} to host", this, offspec);
                }
            }
            this.state = State.CLOSED; // can't pass any more messages
        }
        // TODO plumber.stopKeepAlive ?
        if (this.channel != null) {
            try {
                this.channel.close();
            } catch (Exception e) {
                // ignore
            }
            this.channel = null;
        }
    }

    @Override
    public void setPeerId(SpecToken id) {
        this.peerId = id;
        // now we know remote peer id  it means pipe is handshaken
        this.state = State.HANDSHAKEN;
        // reset connection attempt
        this.connectionAttempt = 0;
        // start sending keep-alive
        this.plumber.keepAlive(this);
        logger.info("{} handshaken", this);
    }

    @Override
    public SpecToken getPeerId() {
        return peerId;
    }

    private Spec typeId = null;

    @Override
    public Spec getTypeId() {
        if (typeId == null && !State.NEW.equals(state)) {
            typeId = new Spec(Host.HOST, peerId);
        }
        return typeId;
    }

    @Override
    public String toString() {
        return "" + host.getPeerId() +
                "-(" + id + ")-" +
                (!State.NEW.equals(state) ? peerId : "?");
    }

    public void setReconnectTimeout(long reconnectTimeout) {
        this.reconnectTimeout = reconnectTimeout;
    }

    public OpChannel getChannel() {
        return channel;
    }

    public void setConnectionAttempt(int connectionAttempt) {
        this.connectionAttempt = connectionAttempt;
    }

    //TODO configurable serializer
    public static String serialize(Spec spec, JsonValue value) throws SwarmException {
        JsonObject payload = new JsonObject();
        payload.set(spec.toString(), value);
        return payload.toString();
    }

    public static SortedMap<Spec, JsonValue> parse(String message) {

        JsonObject bundle = JsonObject.readFrom(message);

        // sort operations by spec
        SortedMap<Spec, JsonValue> operations = new TreeMap<>(Spec.ORDER_NATURAL);
        for (String specStr : bundle.names()) {
            final Spec spec = new Spec(specStr);
            final JsonValue value = bundle.get(specStr);
            operations.put(spec, value);
        }
        return operations;
    }

    /**
     * Created with IntelliJ IDEA.
     *
     * @author aleksisha
     *         Date: 07.09.2014
     *         Time: 23:48
     */
    public static enum State {
        NEW, HANDSHAKEN, CLOSED
    }
}
