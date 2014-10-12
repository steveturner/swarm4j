package citrea.swarm4j.model;

import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.*;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 03/11/13
 *         Time: 10:01
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HostTest {
    private static final Logger logger = LoggerFactory.getLogger(HostTest.class);

    private Host host;

    @Before
    public void setUp() throws Exception {
        XInMemoryStorage storage = new XInMemoryStorage(new IdToken("#dummy"));
        host = new Host(new IdToken("#gritzko"), storage);
        host.registerType(Duck.class);
        host.start();
        host.waitForStart();
    }

    @After
    public void tearDown() throws Exception {
        host.stop();
        host.close();
        host = null;
    }

    @Test
    public void testNewVersion() throws Exception {
        SToken ver1 = this.host.time();
        SToken ver2 = this.host.time();
        assertNotEquals(ver1, ver2);
    }

    @Test
    public void test2a_basic_listener() throws Exception {
        logger.info("2.a basic source func");
        // expect(5);

        // construct an object with an id provided; it will try to fetch
        // previously saved state for the id (which is none)
        final Duck huey = host.get(new TypeIdSpec("/Duck#hueyA"));
        Thread.sleep(10); // wait for initialization

        final int[] invoked = new int[] {0, 0};

        //ok(!huey._version); //storage is a?sync
        // listen to a field
        final OpRecipient lsfn2a = new OpRecipient() {
            @Override
            public void deliver(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
                // check spec
                Spec expectedSpec = new FullSpec(new TypeIdSpec("/Duck#hueyA"), spec.getVersion(), Model.SET);
                assertEquals(expectedSpec, spec);
                // check value
                assertTrue(value.isObject());
                JsonValue age = value.asObject().get("age");
                assertNotNull(age);
                assertFalse(age.isNull());
                assertEquals(1, age.asInt());

                // check version
                SToken version = spec.getVersion();
                assertEquals("gritzko", version.getProcessId());

                huey.off(this);
                // only the uplink remains and no listeners
                assertEquals(1, huey.uplinks.size());
                assertEquals(0, huey.listeners.size());
                invoked[1]++;
            }
        };

        final OpRecipient init2a = new OpRecipient() {
            @Override
            public void deliver(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
                JsonObject fieldValues = new JsonObject();
                fieldValues.set("age", 1);
                huey.set(fieldValues);
                invoked[0]++;
            }
        };

        huey.on(huey.newEventSpec(Syncable.ON), JsonValue.valueOf("age"), lsfn2a);
        huey.on(huey.newEventSpec(Syncable.ON), JsonValue.valueOf(".init"), init2a);

        assertEquals(1, invoked[0]);
        assertEquals(1, invoked[1]);
    }

    @Test
    public void test2b_create_by_id() throws Exception {
        logger.info("2.b create-by-id");
        // there is 1:1 spec-to-object correspondence;
        // an attempt of creating a second copy of a model object
        // will throw an exception
        Duck dewey1 = new Duck(new IdToken("#dewey"), host);
        // that's we resort to descendant() doing find-or-create
        Duck dewey2 = host.get(new TypeIdSpec("/Duck#dewey"));
        // must be the same object
        assertSame(dewey1, dewey2);
        assertEquals(Duck.class.getSimpleName(), dewey1.getType().getBody());
    }

    @Test
    public void test2c_version_ids() throws Exception {
        logger.info("2.c version ids");
        String ts1 = host.time().toString();
        Duck louie = host.get(new TypeIdSpec("/Duck#louie"));
        JsonObject fieldValues = new JsonObject();
        fieldValues.set("age", 3);
        louie.set(fieldValues);
        Thread.sleep(10);
        String ts2 = host.time().toString();
        assertTrue(ts1.compareTo(ts2) < 0);
        assertNotNull(louie.version);
        String vid = louie.version;
        assertTrue(ts1.compareTo(vid) < 0);
        assertTrue(ts2.compareTo(vid) > 0);
    }

    @Test
    public void test2d_pojos() throws Exception {
        logger.info("2.d pojos");
        Duck dewey = host.get(new TypeToken("Duck"));
        JsonObject fieldValues = new JsonObject();
        fieldValues.set("age", 2);
        dewey.set(fieldValues);
        JsonObject json = dewey.getPOJO(false);
        assertEquals("{\"height\":null,\"age\":2,\"mood\":\"neutral\"}", json.toString());
    }

    /* TODO reactions
    @Test
    public void test2e_reactions() throws Exception {
        logger.info("2.e reactions");
        Duck huey = host.get("/Duck#huey");
        var handle = Duck.addReaction('age', function reactionFn(spec,val) {
            logger.debug("yupee im growing");
            assertEquals(val.age,1);
            start();
        });
        //var version = host.time(), sp = '!'+version+'.set';
        huey.deliver(huey.newEventSpec('set'), {age:1});
        Duck.removeReaction(handle);
        assertEquals(Duck.prototype._reactions['set'].length,0); // no house cleaning :)
    }

    */
    @Test
    public void test2f_once() throws Exception {
        logger.info("2.f once");
        Duck huey = host.get(new TypeIdSpec("/Duck#huey"));
        Thread.sleep(10);

        RememberingRecipient listener = new RememberingRecipient();
        huey.once(JsonValue.valueOf("age"), listener);
        huey.set("age", JsonValue.valueOf(4));
        huey.set("age", JsonValue.valueOf(5));
        List<RememberingRecipient.Triplet> invocations = listener.getMemory();
        assertEquals(1, invocations.size());

        huey.set("age", JsonValue.valueOf(6));
        Thread.sleep(10);

        invocations = listener.getMemory();
        assertEquals(1, invocations.size());
    }

    @Test
    @Ignore
    public void test2g_custom_field_type() throws Exception {
        logger.info("2.g custom field type");
        Duck huey = host.get(new TypeIdSpec("/Duck#huey"));
        /*TODO custom field types
        huey.set({height:'32cm'});
        ok(Math.abs(huey.height.meters-0.32)<0.0001);
        var vid = host.time();
        host.deliver(new Spec('/Duck#huey!'+vid+'.set'),{height:'35cm'});
        ok(Math.abs(huey.height.meters-0.35)<0.0001);
        */
    }

    @Test
    public void test2h_state_init() throws Exception {
        logger.info("2.h state init");
        JsonObject initialState = JsonObject.readFrom("{\"age\":1,\"height\":4}");
        Duck factoryBorn = new Duck(initialState, host);
        Thread.sleep(10);
        assertEquals(4, factoryBorn.height.intValue());
        assertEquals(1, factoryBorn.age.intValue());
    }

    @Test
    public void test2i_batched_set() throws Exception {
        logger.info("2.i batched set");
        JsonObject fieldValues = JsonObject.readFrom("{\"age\":2,\"height\":5}");
        Duck nameless = new Duck(host);
        nameless.set(fieldValues);
        assertEquals(2, nameless.age.intValue());
        assertEquals(5, nameless.height.intValue());
        assertFalse(nameless.canDrink());
    }

    @Test
    public void test2j_basic_Set_functions() throws Exception {
        final Comparator<Duck> duckComparator = new Comparator<Duck>() {
            @Override
            public int compare(Duck duck, Duck duck2) {
                return duck.age - duck2.age;
            }
        };

        logger.info("2.j basic Set functions (string index)");

        JsonObject fieldValues;
        fieldValues = new JsonObject();
        fieldValues.set("age", 2);
        Duck hueyClone = new Duck(fieldValues, host);

        fieldValues = new JsonObject();
        fieldValues.set("age", 1);
        Duck deweyClone = new Duck(fieldValues, host);

        fieldValues = new JsonObject();
        fieldValues.set("age", 3);
        Duck louieClone = new Duck(fieldValues, host);

        Nest clones = new Nest(host);
        Thread.sleep(20);

        clones.add(louieClone);
        clones.add(hueyClone);
        clones.add(deweyClone);

        List<Duck> sibs = clones.list(duckComparator);
        assertEquals(3, sibs.size());
        assertSame(deweyClone, sibs.get(0));
        assertSame(hueyClone, sibs.get(1));
        assertSame(louieClone, sibs.get(2));

        JsonObject change = new JsonObject();
        change.set(hueyClone.getTypeId().toString(), Set.FALSE);
        clones.change(change);

        List<Duck> sibs2 = clones.list(duckComparator);
        assertEquals(2, sibs2.size());
        assertSame(deweyClone, sibs2.get(0));
        assertSame(louieClone, sibs2.get(1));
    }

    @Test
    public void test2k_distilled_log() throws Exception {
        logger.info("2.k distilled log");
        JsonObject fieldValues;

        Duck duckling1 = host.get(Duck.class);
        Thread.sleep(10);//wait for init

        fieldValues = new JsonObject();
        fieldValues.set("age", 1);
        duckling1.set(fieldValues);

        fieldValues = new JsonObject();
        fieldValues.set("age", 2);
        duckling1.set(fieldValues);

        duckling1.distillLog();
        assertEquals(1, duckling1.oplog.size());

        fieldValues = new JsonObject();
        fieldValues.set("age", 3);
        fieldValues.set("height", 30);
        duckling1.set(fieldValues);

        fieldValues = new JsonObject();
        fieldValues.set("age", 4);
        fieldValues.set("height", 40);
        duckling1.set(fieldValues);

        duckling1.distillLog();
        assertEquals(1, duckling1.oplog.size());

        fieldValues = new JsonObject();
        fieldValues.set("age", 5);
        duckling1.set(fieldValues);

        duckling1.distillLog();

        assertEquals(2, duckling1.oplog.size());
    }

    @Test
    public void test2l_patial_order() throws Exception {
        logger.info("2.l partial order");
        Duck duckling = new Duck(host);
        JsonObject fieldValues;

        FullSpec spec1 = duckling.getTypeId().fullSpec(new VersionToken("!time+user2"), Model.SET);
        fieldValues = new JsonObject();
        fieldValues.set("height", 2);
        duckling.deliver(spec1, fieldValues, OpRecipient.NOOP);

        FullSpec spec2 = duckling.getTypeId().fullSpec(new VersionToken("!time+user1"), Model.SET);
        fieldValues = new JsonObject();
        fieldValues.set("height", 1);
        duckling.deliver(spec2, fieldValues, OpRecipient.NOOP);

        Thread.sleep(10);

        assertEquals(2, duckling.height.intValue());
    }

    @Test
    public void test2m_init_push() throws Exception {
        logger.info("2.m init push");
        JsonObject fieldValues = new JsonObject();
        fieldValues.set("age", 105);
        final Duck scrooge = new Duck(fieldValues, host);
        final boolean[] inited = new boolean[] {false};
        scrooge.on(JsonValue.valueOf(".init"), new OpRecipient() {
            @Override
            public void deliver(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
                inited[0] = true;
            }
        });

        Thread.sleep(10);
        assertTrue(inited[0]);
    }

    @Test
    public void test2n_local_ON_OFF_listeners() throws Exception {
        logger.info("2.n local listeners for on/off");
        final RememberingRecipient[] counters = new RememberingRecipient[] {
                new RememberingRecipient(),
                new RememberingRecipient(),
                new RememberingRecipient(),
                new RememberingRecipient()
        };

        final Duck duck = new Duck(host);
        Thread.sleep(10);

        duck.on(JsonValue.valueOf(".on"), counters[0]);
        duck.on(JsonValue.valueOf(".init"), counters[1]);
        duck.on(JsonValue.valueOf(".reon"), counters[2]);
        host.on(JsonValue.valueOf("/Host#gritzko.on"), counters[3]);

        List<RememberingRecipient.Triplet> oplist;
        // (0)
        /* TODO fix ???
        oplist = counters[0].getMemory();
        assertEquals("triggered by duck.on(on), duck.on(init) and host.on", 3, oplist.size());
        for (RememberingRecipient.Triplet op : oplist) {
            assertEquals(Syncable.ON, op.spec.getOp());
        } */

        // (1)
        oplist = counters[1].getMemory();
        assertEquals("triggered by duck.on(init)", 1, oplist.size());
        assertSame(duck, oplist.get(0).source);
        assertNotNull(duck.version);

        // (2)
        // doesn't get triggered if the storage is sync
        oplist = counters[2].getMemory();
        assertEquals(1, oplist.size());
        assertEquals(Syncable.REON, oplist.get(0).spec.getOp());

        // (3)
        /* TODO fix ???
        oplist = counters[3].getMemory();
        assertEquals("triggered by host.on", 1, oplist.size());
        assertEquals(Syncable.ON, oplist.get(0).spec.getOp());
        */
    }
}
