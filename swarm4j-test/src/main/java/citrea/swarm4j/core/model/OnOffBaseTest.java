package citrea.swarm4j.core.model;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.callback.OpRecipient;
import citrea.swarm4j.core.callback.Uplink;
import citrea.swarm4j.core.pipe.Pipe;
import citrea.swarm4j.core.spec.FullSpec;
import citrea.swarm4j.core.spec.IdToken;
import citrea.swarm4j.core.spec.TypeIdSpec;
import citrea.swarm4j.core.storage.Storage;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 14.11.2014
 *         Time: 15:51
 */
public abstract class OnOffBaseTest extends BaseClientServerTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseClientServerTest.class);

    protected abstract Storage createServerStorage(IdToken id) throws SwarmException;

    protected abstract Storage createClientStorage(IdToken idToken);

    protected abstract void cleanupServerStorage();

    protected abstract void cleanupClientStorage();

    protected Set<Class<? extends Syncable>> getClassesToRegister() {
        Set<Class<? extends Syncable>> res = new HashSet<Class<? extends Syncable>>();
        res.add(Thermometer.class);
        return res;
    }

    protected void setupServerHost(Host host) throws SwarmException {
        host.setAsync(true);
        Set<Class<? extends Syncable>> classesToRegister = getClassesToRegister();
        for (Class<? extends Syncable> cls : classesToRegister) {
            host.registerType(cls);
        }
    }

    protected void setupClientHost(Host host) throws SwarmException {
        host.setAsync(true);
        Set<Class<? extends Syncable>> classesToRegister = getClassesToRegister();
        for (Class<? extends Syncable> cls : classesToRegister) {
            host.registerType(cls);
        }
        super.setupClientHost(host);
    }

    @Test
    public void test3a_serialized_on_reon() throws SwarmException, InterruptedException {
        logger.info("3.a serialized on, reon");
        // that's the default server.getSources = function () {return [storage]};
        final TypeIdSpec THERM_ID = new TypeIdSpec("/Thermometer#room");

        client.on(JsonValue.valueOf(THERM_ID.toString() + Syncable.INIT.toString()), new OpRecipient() {
            @Override
            public void deliver(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
                Thermometer obj = (Thermometer) client.objects.get(THERM_ID);
                JsonObject fieldValues = new JsonObject();
                fieldValues.set("t", 22);
                obj.set(fieldValues);
            }

            @Override
            public String toString() {
                return "init-listener";
            }
        });
        Thread.sleep(100);

        Thermometer o = (Thermometer) server.objects.get(THERM_ID);
        Assert.assertNotNull(o);
        Assert.assertEquals(22, o.t);
    }

    @Test
    public void test3b_pipe_reconnect_backoff() throws SwarmException, InterruptedException {
        logger.info("3.b pipe reconnect, backoff");
        Thermometer thermometer = server.get(Thermometer.class);
        final AtomicInteger counter = new AtomicInteger(0);

        // OK. The idea is to connect/disconnect it 100 times then
        // check that the state is OK, there are no zombie listeners
        // no objects/hosts, log is 1 record long (distilled) etc

        client.on(JsonValue.valueOf(thermometer.getTypeId().toString() + Model.SET.toString()), new OpRecipient() {
            @Override
            public void deliver(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
                logger.debug("{} <= ({}, {}, {})", this, spec, value, source);
                if (Model.SET.equals(spec.getOp())) {
                    for (Uplink connection : client.getSources(client.getTypeId())) {
                        if (connection instanceof Pipe) {
                            ((Pipe) connection).getChannel().close();
                        }
                    }
                    counter.incrementAndGet();
                }
            }

            @Override
            public String toString() {
                return "ThermometerListener";
            }
        });

        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            JsonObject fieldValues = new JsonObject();
            fieldValues.set("t", i);
            thermometer.set(fieldValues);
        }

        Thread.sleep(500);
        Assert.assertEquals("reconnected 10 times", 10, counter.get());
    }

    // TODO disconnection events
    @Test
    @Ignore
    public void test3c_disconnection_events() throws SwarmException, InterruptedException {
        logger.info("3.c Disconnection events");

        final AtomicInteger counter = new AtomicInteger(0);

        Thread.sleep(100);

        client.on(Syncable.REOFF.toJson(), new OpRecipient() {
            @Override
            public void deliver(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
                Assert.assertSame(source, client);
                Assert.assertTrue(source instanceof Host);
                Assert.assertFalse(!((Host) source).isNotUplinked());
                counter.incrementAndGet();
            }

            @Override
            public String toString() {
                return "reoff-listener";
            }
        });

        client.on(Syncable.REON.toJson(), new OpRecipient() {
            @Override
            public void deliver(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
                Assert.assertEquals(spec.getId(), client.getId());
            }

            @Override
            public String toString() {
                return "reon-listener";
            }
        });

        Thread.sleep(100);
        client.disconnect(server.getPeerId());

        Thread.sleep(100);
        Assert.assertEquals(3, counter.get());
    }
}
