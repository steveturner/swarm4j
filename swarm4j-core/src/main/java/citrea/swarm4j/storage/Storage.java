package citrea.swarm4j.storage;

import citrea.swarm4j.model.Host;
import citrea.swarm4j.model.QueuedOperation;
import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.callback.Peer;
import citrea.swarm4j.model.clocks.Clock;
import citrea.swarm4j.model.clocks.SecondPreciseClock;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.VersionVector;
import citrea.swarm4j.model.spec.SpecToken;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 25.08.2014
 *         Time: 00:55
 */
public abstract class Storage implements Peer, Runnable {

    protected Logger logger = LoggerFactory.getLogger(Storage.class);

    public final BlockingQueue<QueuedOperation> queue = new LinkedBlockingQueue<>();
    private Thread queueThread;

    protected Host host;
    protected Clock clock;
    protected SpecToken id;

    protected Storage(SpecToken id) {
        this.id = id;
        // TODO allow to setup clock
        this.clock = new SecondPreciseClock(id.getBare());
    }

    public void setHost(Host host) {
        if (this.host != null) throw new IllegalStateException("host can be set only once");

        this.host = host;
    }

    /**
     * Returns an unique Lamport timestamp on every invocation.
     * Swarm employs 30bit integer Unix-like timestamps starting epoch at
     * 1 Jan 2010. Timestamps are encoded as 5-char base64 tokens; in case
     * several events are generated by the same process at the same second
     * then sequence number is added so a timestamp may be more than 5
     * chars. The id of the Host (+user~session) is appended to the ts.
     */
    public SpecToken time() {
        return this.clock.issueTimestamp();
    }

    @Override
    public void deliver(Spec spec, JsonValue value, OpRecipient source) throws SwarmException {
        if (queueThread != Thread.currentThread()) {
            // queue
            try {
                queue.put(new QueuedOperation(spec, value, source));
            } catch (InterruptedException e) {
                throw new SwarmException(e.getMessage(), e);
            }
        } else {
            logger.debug("{}.deliver({}, {}, {})", this, spec, value, source);
            final SpecToken op = spec.getOp();
            if (Syncable.ON.equals(op)) {
                this.on(spec, value, source);
            } else if (Syncable.OFF.equals(op)) {
                this.off(spec, source);
            } else if (Syncable.PATCH.equals(op)) {
                this.patch(spec, value);
            } else {
                this.op(spec, value, source);
            }
        }
    }

    protected abstract void on(Spec spec, JsonValue value, OpRecipient source) throws SwarmException;

    protected abstract void off(Spec spec, OpRecipient source) throws SwarmException;

    protected abstract void patch(Spec spec, JsonValue patch) throws SwarmException;

    public void op(Spec spec, JsonValue val, OpRecipient source) throws SwarmException {
        Spec ti = spec.getTypeId();
        Spec vo = spec.getVersionOp();
        JsonObject o = new JsonObject();
        o.set(vo.toString(), val);
        this.appendToLog(ti, o);
    }

    protected abstract void appendToLog(Spec ti, JsonObject verop2val) throws SwarmException;

    /**
     * Derive version vector from a state of a Syncable object.
     * This is not a method as it needs to be applied to a flat JSON object.
     * @see citrea.swarm4j.model.Syncable#version()
     * @see citrea.swarm4j.model.spec.VersionVector
     * @return string representation of SpecMap
     */
    public static String stateVersionVector(JsonObject state) {
        StringBuilder str = new StringBuilder();
        JsonValue version = state.get("_version");
        if (!version.isNull()) {
            str.append(version.asString());
        }
        JsonValue vector = state.get("_vector");
        if (vector != null && vector.isString()) {
            str.append(version.asString());
        }
        JsonValue oplog = state.get("_oplog");
        if (oplog != null && oplog.isObject()) {
            for (String spec : oplog.asObject().names()) {
                str.append(spec);
            }
        }
        JsonValue tail = state.get("_tail");
        if (tail != null && tail.isObject()) {
            for (String spec : tail.asObject().names()) {
                str.append(spec);
            }
        }
        return new VersionVector(str.toString()).toString();
    }

    @Override
    public SpecToken getPeerId() {
        return id;
    }

    @Override
    public void setPeerId(SpecToken id) {
        this.id = id;
    }

    public synchronized boolean ready() {
        return queueThread != null;
    }

    @Override
    public void run() {
        synchronized (this) {
            if (queueThread != null) {
                throw new IllegalStateException("Can't run the single host more than once");
            }
            queueThread = Thread.currentThread();
        }

        logger.info("started");
        try {
            while (!queueThread.isInterrupted()) {
                QueuedOperation op = queue.take();
                if (op == null) continue;

                try {
                    this.deliver(op.getSpec(), op.getValue(), op.getPeer());
                } catch (SwarmException e) {
                    //TODO fatal exception
                    logger.warn("Error processing operation: {}", op, e);
                }
            }
        } catch (InterruptedException e) {
            //ignore
        }
        logger.info("finished");
    }

    public void start() {
        logger.info("{}.start()", this);
        new Thread(this, "Stor" + this.getPeerId().toString())
                .start();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + getPeerId();
    }

    public void stop() {
        logger.info("{}.stop()", this);
        synchronized (this) {
            if (queueThread != null) {
                queueThread.interrupt();
            }
        }
    }
}
