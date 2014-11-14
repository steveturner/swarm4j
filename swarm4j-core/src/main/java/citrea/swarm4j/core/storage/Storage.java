package citrea.swarm4j.core.storage;

import citrea.swarm4j.core.model.Host;
import citrea.swarm4j.core.model.QueuedOperation;
import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.model.Syncable;
import citrea.swarm4j.core.callback.OpRecipient;
import citrea.swarm4j.core.callback.Peer;
import citrea.swarm4j.core.spec.*;

import citrea.swarm4j.core.model.value.JSONUtils;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import static citrea.swarm4j.core.model.SubscriptionAware.OFF;
import static citrea.swarm4j.core.model.SubscriptionAware.ON;
import static citrea.swarm4j.core.model.SubscriptionAware.REON;
import static citrea.swarm4j.core.model.Syncable.INIT;
import static citrea.swarm4j.core.spec.SToken.ZERO_VERSION;

/**
 * Abstract base class for all data storage implementations
 *
 * In a real storage implementation, state and log often go into
 * different backends, e.g. the state is saved to SQL/NoSQL db,
 * while the log may live in a key-value storage.
 * As long as the state has sufficient versioning info saved with
 * it (like a version vector), we may purge the log lazily, once
 * we are sure that the state is reliably saved. So, the log may
 * overlap with the state (some ops are already applied). That
 * provides some necessary resilience to workaround the lack of
 * transactions across backends.
 * In case third parties may write to the backend, go figure
 * some way to deal with it (e.g. make a retrofit operation).
 *
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 25.08.2014
 *         Time: 00:55
 */
public abstract class Storage implements Peer, Runnable {

    private final Logger logger = LoggerFactory.getLogger(Storage.class);

    public final BlockingQueue<QueuedOperation> queue = new LinkedBlockingQueue<QueuedOperation>();

    private Thread queueThread;
    private final CountDownLatch started = new CountDownLatch(1);

    private boolean root = true;
    private long maxLogSize = 1000L;
    protected IdToken id;
    private boolean async = false;

    protected Map<TypeIdSpec, List<OpRecipient>> listeners = null;

    private Map<TypeIdSpec, Long> counts = new HashMap<TypeIdSpec, Long>();

    protected Storage(IdToken id) {
        this.id = id;
    }

    /**
     * Setup weather the storage can create empty model instances
     * when no data found in the underlying database
     * or should it wait for initial state
     * @param root true – create new model instances, false – wait
     */
    public final void setRoot(boolean root) {
        this.root = root;
    }

    public final void setAsync(boolean async) {
        this.async = async;
    }

    public final void setMaxLogSize(long maxLogSize) {
        this.maxLogSize = maxLogSize;
    }

    @Override
    public final void deliver(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
        if (queueThread != null && queueThread != Thread.currentThread()) {
            // queue
            try {
                queue.put(new QueuedOperation(spec, value, source));
            } catch (InterruptedException e) {
                throw new SwarmException(e.getMessage(), e);
            }
        } else {
            logger.debug("{}.deliver({}, {}, {})", this, spec, value, source);
            final SToken op = spec.getOp();
            if (ON.equals(op)) {
                on(spec, value, source);
            } else if (OFF.equals(op)) {
                off(spec, source);
            } else if (INIT.equals(op)) {
                init(spec, value);
            } else {
                op(spec, value, source);
            }
        }
    }

    private void on(final FullSpec spec, JsonValue value, final OpRecipient source) throws SwarmException {
        final TypeIdSpec ti = spec.getTypeId();

        if (listeners != null) {
            List<OpRecipient> list = listeners.get(ti);
            if (list == null) {
                list = new ArrayList<OpRecipient>();
                listeners.put(ti, list);
            }
            list.add(source);
        }

        JsonObject state = readState(ti);
        JsonObject tail = readOps(ti);
        if (state == null) {
            if (root) {// && !spec.token('#').ext) {
                // make 0 state for a global object TODO move to Host
                state = new JsonObject();
                state.set(Syncable.VERSION_FIELD, ZERO_VERSION.toJson());
            }
        }
        if (tail != null) {
            if (state == null) {
                state = new JsonObject();
            }
            JsonValue state_tail = state.get(Syncable.TAIL_FIELD);
            if (JSONUtils.isFalsy(state_tail)) {
                state.set(Syncable.TAIL_FIELD, tail);
            } else {
                JsonObject state_tail_obj = state_tail.asObject();
                for (String versionOp: tail.names()) {
                    state_tail_obj.set(versionOp, tail.get(versionOp));
                }
            }
        }
        FullSpec reonSpec = ti.fullSpec(spec.getVersion(), REON);
        if (state != null) {
            FullSpec initSpec = ti.fullSpec(spec.getVersion(), INIT);
            source.deliver(initSpec, state, this);
            source.deliver(reonSpec, stateVersionVector(state), this);
            // TODO and the tail
        } else {
            // state unknown
            source.deliver(reonSpec, ZERO_VERSION.toJson(), this);
        }
    }

    private void off(FullSpec spec, OpRecipient source) {
        TypeIdSpec ti = spec.getTypeId();
        if (listeners != null) {
            List<OpRecipient> ls = listeners.get(ti);
            ls.remove(source);
            if (ls.isEmpty()) {
                listeners.remove(ti);
            }
        }
        cleanUpCache(ti);
    }

    private void init(FullSpec spec, final JsonValue state) {
        final TypeIdSpec ti = spec.getTypeId();

        try {
            writeState(ti, state);
        } catch (SwarmException ex) {
            logger.error("State dump error: {}", ex.getMessage(), ex);
        }

    }

    private void op(FullSpec spec, JsonValue val, OpRecipient source) throws SwarmException {
        TypeIdSpec ti = spec.getTypeId();
        try {
            writeOp(spec, val);
        } catch (SwarmException ex) {
            logger.error("Error writing op");
            close();
        }

        Long logSize = counts.get(ti);
        if (logSize == null) {
            logSize = 1L;
        } else {
            logSize++;
        }

        if (logSize > maxLogSize) {
            // The storage piggybacks on the object's state/log handling logic
            // First, it adds an op to the log tail unless the log is too long...
            // ...otherwise it sends back a subscription effectively requesting
            // the state, on state arrival zeroes the tail.
            counts.remove(ti);
            source.deliver(spec.overrideOp(REON), new VersionOpSpec(ZERO_VERSION, INIT).toJson(), this);
        } else {
            counts.put(ti, logSize);
        }
    }

    protected abstract JsonObject readState(TypeIdSpec ti) throws SwarmException;

    protected abstract JsonObject readOps(TypeIdSpec ti) throws SwarmException;

    protected abstract void writeState(TypeIdSpec spec, JsonValue state) throws SwarmException;

    protected abstract void writeOp(FullSpec spec, JsonValue value) throws SwarmException;

    protected abstract void cleanUpCache(TypeIdSpec ti);

    public void close() throws SwarmException {
    }

    /**
     * Derive version vector from a state of a Syncable object.
     * This is not a method as it needs to be applied to a flat JSON object.
     * @see citrea.swarm4j.core.model.Syncable#version()
     * @see citrea.swarm4j.core.spec.VersionVector
     * @return string representation of SpecMap
     */
    public static JsonValue stateVersionVector(JsonObject state) {
        StringBuilder str = new StringBuilder();
        JsonValue version = state.get(Syncable.VERSION_FIELD);
        if (!version.isNull()) {
            str.append(version.asString());
        }
        JsonValue vector = state.get(Syncable.VECTOR_FIELD);
        if (vector != null && vector.isString()) {
            str.append(version.asString());
        }
        JsonValue oplog = state.get(Syncable.OPLOG_FIELD);
        if (oplog != null && oplog.isObject()) {
            for (String spec : oplog.asObject().names()) {
                str.append(spec);
            }
        }
        JsonValue tail = state.get(Syncable.TAIL_FIELD);
        if (tail != null && tail.isObject()) {
            for (String spec : tail.asObject().names()) {
                str.append(spec);
            }
        }
        return new VersionVector(str.toString()).toJson();
    }

    @Override
    public TypeIdSpec getTypeId() {
        return new TypeIdSpec(Host.HOST, getPeerId());
    }

    @Override
    public IdToken getPeerId() {
        return id;
    }

    @Override
    public final void setPeerId(IdToken id) {
        this.id = id;
    }

    public void waitForStart() throws InterruptedException {
        started.await();
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
        started.countDown();
        try {
            while (!queueThread.isInterrupted()) {
                QueuedOperation op = queue.take();
                if (op == null) continue;

                try {
                    deliver(op.getSpec(), op.getValue(), op.getPeer());
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

    public void start() throws SwarmException {
        logger.info("{}.start()", this);
        if (async) {
            new Thread(this, "S" + getPeerId().toString()).start();
        } else {
            started.countDown();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + getPeerId();
    }

    public void stop() throws SwarmException {
        logger.info("{}.stop()", this);
        synchronized (this) {
            if (queueThread != null) {
                queueThread.interrupt();
            }
            this.close();
        }
    }
}
