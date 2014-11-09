package citrea.swarm4j.core.model;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.model.annotation.SwarmField;
import citrea.swarm4j.core.model.annotation.SwarmType;
import citrea.swarm4j.core.callback.OpRecipient;
import citrea.swarm4j.core.spec.*;
import citrea.swarm4j.core.storage.InMemoryStorage;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VectorTest {

    @SwarmType
    public static class Agent extends Model {
        public static final TypeToken TYPE = new TypeToken("/Agent");

        @SwarmField
        public String name;
        @SwarmField
        public int num;
        @SwarmField
        public String weapon;
        @SwarmField
        public String dressCode;

        public Agent(IdToken id, Host host) throws SwarmException {
            super(id, host);
        }

        public Agent(JsonObject initialState, Host host) throws SwarmException {
            super(initialState, host);
        }
    }

    public static class AgentVector extends Vector<Agent> {

        public AgentVector(Host host) throws SwarmException {
            super(Agent.TYPE, host);
        }

        public AgentVector(IdToken id, Host host) throws SwarmException {
            super(id, Agent.TYPE, host);
        }

    }

    private Host vhost;
    private Agent smith;
    private Agent jones;
    private Agent brown;

    @Before
    public void setUp() throws SwarmException, InterruptedException {
        InMemoryStorage storage = new InMemoryStorage(new IdToken("#dummy"));
        storage.setAsync(false);

        vhost = new Host(new IdToken("#matrix"), storage);
        vhost.setAsync(false);
        vhost.start();
        vhost.waitForStart();

        JsonObject init = new JsonObject();
        init.set("name", "Smith");
        init.set("num", 1);
        smith = new Agent(init, vhost);

        init = new JsonObject();
        init.set("name", "Jones");
        init.set("num", 2);
        jones = new Agent(init, vhost);

        init = new JsonObject();
        init.set("name", "Brown");
        init.set("num", 3);
        brown = new Agent(init, vhost);

    }

    @After
    public void cleanup() {
        vhost.stop();
    }

    @Test
    public void test7a_init_vector() throws SwarmException {
        AgentVector vec = new AgentVector(vhost);
        vec.add(smith);
        vec.add(jones);
        vec.add(brown, jones.getTypeId());
        assertArrayEquals(new Agent[] {smith, brown, jones}, vec.objects.toArray(new Agent[vec.objects.size()]));
    }

/*test("7.b ordered add", function (test) {
    env.localhost = vhost;
    var vector = new Vector();
    function order(a,b) {
        return a.num - b.num;
    }
    vector.setOrder(order);
    vector.add(jones);
    vector.add(smith);
    vector.add(brown);
    checkOrder(vec);
});*/

    @Test
    public void test7c_insert_remove() throws SwarmException {
        AgentVector vec = new AgentVector(vhost);
        // default object type
        vec.add(smith); // => [smith]
        vec.add(brown, smith.getTypeId()); // => [brown, smith]
        vec.remove(smith.getTypeId()); // => [brown]
        vec.add(jones.getTypeId()); // => [brown, jones]
        vec.add(smith.getTypeId(), brown.getTypeId()); // => [smith, brown, jones]
        assertArrayEquals(new Agent[] {smith, brown, jones}, vec.objects.toArray());
    }

    @Test
    public void test7d_concurrent_insert() throws SwarmException {
        OpRecipient cb = new OpRecipient() {
            @Override
            public void deliver(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
                throw new SwarmException("what?");
            }
        };

        AgentVector vec = new AgentVector(new IdToken("#vecid"), vhost);
        VersionToken smithOp = vec.add(smith).getVersion();
        vec.deliver(new FullSpec("/AgentVector#vecid!2after+src1.in"), jones.getTypeId().typeIdVersionHint(smithOp).toJson(), cb);
        vec.deliver(new FullSpec("/AgentVector#vecid!1before+src2.in"), brown.getTypeId().typeIdVersionHint(smithOp).toJson(), cb);
        assertArrayEquals(new Agent[] {smith, jones, brown}, vec.objects.toArray());
        assertEquals(smithOp.toString() + "!2after+src1!1before+src2", vec.order.toString());

        AgentVector vec2 = new AgentVector(new IdToken("vecid2"), vhost);
        VersionToken smithOp2 = vec2.add(smith, 0).getVersion();
        vec2.deliver(new FullSpec("/AgentVector#vecid2!1before+src2.in"), brown.getTypeId().typeIdVersionHint(smithOp2).toJson(), cb);
        vec2.deliver(new FullSpec("/AgentVector#vecid2!2after+src1.in"), jones.getTypeId().typeIdVersionHint(smithOp2).toJson(), cb);

        assertArrayEquals(new Agent[] {smith, jones, brown}, vec2.objects.toArray());
        assertEquals(vec2.order.toString(), smithOp2+"!2after+src1!1before+src2");
    }

    @Test
    public void test7e_dead_point() throws SwarmException {
        AgentVector vec = new AgentVector(vhost);
        // keeps
        vec.add(smith);
        VersionToken pos = vec.order.tokenAt(0); // !time
        vec.remove(smith.getTypeId());
        vec.deliver(vec.getTypeId().fullSpec(new VersionOpSpec("!2after+src1.in")), jones.getTypeId().typeIdVersionHint(pos).toJson(), OpRecipient.NOOP);
        vec.deliver(vec.getTypeId().fullSpec(new VersionOpSpec("!1before+src2.in")), brown.getTypeId().typeIdVersionHint(pos).toJson(), OpRecipient.NOOP);
        vec.add(smith.getTypeId(), jones.getTypeId());
        assertArrayEquals(new Agent[] {smith, jones, brown}, vec.objects.toArray());
    }

/*test("7.f splits: O(N^2) prevention", function (test) {
    // ONE! MILLION! ENTRIES!
    env.localhost = vhost;
    var vec = new Vector();
    // add 1mln entries at pos i%length
    // TODO O(N^0.5) offset anchors
});*/

/*test("7.g log compaction", function (test) {   TODO HORIZON
    // values essentially reside in the _oplog
    // compaction only brings benefit on numerous repeated rewrites
    // over long periods of time (>EPOCH)
    env.localhost = vhost;
    var vec = new Vector();
    // /Type#elem ( !pos (offset)? )?
}); */

    @Test
    public void test7h_duplicates() throws SwarmException {
        AgentVector vec = new AgentVector(vhost);
        vec.add(smith);
        vec.add(smith.getTypeId()); // take that :)
        assertArrayEquals(new Agent[]{smith, smith}, vec.objects.toArray(new Agent[vec.objects.size()]));
    }

    @Test
    public void test7l_event_relay() throws SwarmException {
        final List<String> ids = new ArrayList<String>();
        AgentVector vec = new AgentVector(vhost);
        vec.add(smith);
        vec.add(smith);
        vec.add(smith);
        vec.onObjects(new OpRecipient() {
            @Override
            public void deliver(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
                ids.add(((Agent) source).name);
            }
            @Override
            public String toString() {
                return "test7l_id_collector";
            }
        });

        JsonObject fieldValues = new JsonObject();
        fieldValues.set("weapon", "bug");
        smith.set(fieldValues);

        assertArrayEquals(new String[]{"Smith"}, ids.toArray(new String[ids.size()]));

        vec.remove(1);
        //vec.move(1,0);
        ids.clear();
        fieldValues = new JsonObject();
        fieldValues.set("weapon", "mighty fist");
        smith.set(fieldValues);

        assertArrayEquals(new String[]{"Smith"}, ids.toArray(new String[ids.size()]));
    }

    @Test
    public void test7i_implements_List() throws SwarmException {
        AgentVector vec = new AgentVector(vhost);
        vec.add(smith.getTypeId());
        vec.add(smith.getTypeId());
        vec.add(smith.getTypeId());
        vec.add(brown.getTypeId());
        assertEquals(3, vec.indexOf(brown.getTypeId()));
        assertEquals(3, vec.indexOf(brown));
        assertEquals(0, vec.indexOf(smith.getTypeId()));
        assertEquals(1, vec.indexOf(smith.getTypeId(), 1));
        //vec.splice(1,2,jones);
        //checkOrder(vec);
    }

    /*test("7.j sugary API", function (test) {
        AgentVector vec = new Vector();
        vec.add(jones);
        vec.insertAfter(smith,jones);
        vec.insertBefore(brown,smith);
        vec.move("smith",0);
        checkOrder(vec);
        var i = vec.iterator();
        assertEquals(i.object.name,"Smith");
        i.next();
        assertEquals(i.object.name,"Jones");
        i.next();
        assertEquals(i.object.name,"Brown");
    });*/

    /*test("7.k long Vector O(N^2)", function (test){
        AgentVector vec = new Vector();
        var num = 500, bignum = num << 1; // TODO 1mln entries (need chunks?)
        for(var i=0; i<bignum; i++) { // mooore meee!!!
            vec.append(smith);
        }
        for(var i=bignum-1; i>0; i-=2) {
            vec.remove(i);
        }
        assertEquals(vec.length(), bignum>>1);
        assertEquals(vec.objects[0].name,"Smith");
        assertEquals(vec.objects[num-1].name,"Smith");
    });*/
}
