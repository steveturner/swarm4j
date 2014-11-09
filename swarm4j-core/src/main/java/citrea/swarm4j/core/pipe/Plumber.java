package citrea.swarm4j.core.pipe;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.spec.SToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Controls pipes reconnection and keep-alive
 *
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 08.09.2014
 *         Time: 20:08
 */
public final class Plumber implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final long keepAliveTimeout = 60000L;

    private Thread queueThread;
    private final DelayQueue<Event> events = new DelayQueue<Event>();

    public void start(SToken hostId) {
        new Thread(this, "Plumber" + hostId).start();
    }

    public void stop() {
        synchronized (this) {
            if (queueThread != null) {
                queueThread.interrupt();
            }
        }
    }

    @Override
    public void run() {
        synchronized (this) {
            if (queueThread != null) {
                throw new IllegalStateException("Can be started only once");
            }
            queueThread = Thread.currentThread();
        }

        logger.info("started");

        while (!queueThread.isInterrupted()) {
            try {
                Event event = events.take();
                event.run();
            } catch (InterruptedException e) {
                break;
            }
        }

        logger.info("finished");
    }

    @Override
    public String toString() {
        return "Plumber";
    }

    public void keepAlive(Pipe pipe) {
        long now = System.currentTimeMillis();
        events.put(new KeepAliveEvent(pipe, now + keepAliveTimeout >> 2)); // TODO + Math.random() * 100
    }

    public void reconnect(Pipe pipe) {
        long now = System.currentTimeMillis();
        // reconnection timeout doubles after each unsuccessful attempt
        // but not above maximum timeout (=30sec)
        long delta = Math.min(pipe.reconnectTimeout << Math.min(pipe.connectionAttempt, 8), 30000);
        events.put(new ReconnectEvent(pipe, now + delta));
    }

    private abstract class Event implements Delayed {
        final Pipe pipe;
        final long time;

        public Event(Pipe pipe, long time) {
            this.pipe = pipe;
            this.time = time;
        }

        public abstract void run();

        @Override
        public long getDelay(TimeUnit timeUnit) {
            long remaining = time - System.currentTimeMillis();
            return timeUnit.convert(remaining, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            final long delta;
            if (other instanceof Event) {
                Event otherEvent = (Event) other;
                delta = time - otherEvent.time;
            } else {
                delta = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
            }
            if (delta == 0L) {
                return 0;
            } else {
                return delta < 0 ? -1 : 1;
            }
        }
    }

    private class KeepAliveEvent extends Event {

        public KeepAliveEvent(Pipe pipe, long time) {
            super(pipe, time);
        }

        @Override
        public void run() {
            if (Pipe.State.CLOSED.equals(pipe.state)) {
                return;
            }
            long now = System.currentTimeMillis();
            long sinceReceived = now - pipe.lastReceivedTS;
            long sinceSend = now - pipe.lastSendTS;

            if (sinceSend > keepAliveTimeout >> 1) {
                pipe.sendMessage("{}");
            }
            if (sinceReceived > keepAliveTimeout) {
                pipe.close("channel timeout");
            }
            pipe.plumber.keepAlive(pipe);
        }
    }

    private class ReconnectEvent extends Event {

        public ReconnectEvent(Pipe pipe, long time) {
            super(pipe, time);
        }

        @Override
        public void run() {
            logger.debug("reconnecting {} attempt={}", pipe, pipe.connectionAttempt);
            if (pipe.uri != null) {
                try {
                    pipe.host.connect(pipe.uri, pipe.reconnectTimeout, pipe.connectionAttempt + 1);
                } catch (SwarmException e) {
                    pipe.close(e.getMessage());
                } catch (UnsupportedProtocolException e) {
                    pipe.close(e.getMessage());
                }
            }
        }
    }
}
