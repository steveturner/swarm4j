package citrea.swarm4j.model.spec;

import citrea.swarm4j.model.Syncable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 03/11/13
 *         Time: 00:34
 */
public class SpecTest {

    @Test
    public void testOverrideToken() throws Exception {
        FullSpec spec, spec2;
        //shortening
        spec = new FullSpec("/Type#id!version.op");

        spec2 = spec.overrideOp(new OpToken("op2"));
        assertNotSame(spec, spec2);
        assertEquals("/Type#id!version.op2", spec2.toString());

        spec2 = spec.overrideId(new IdToken("id2"));
        assertNotSame(spec, spec2);
        assertEquals("/Type#id2!version.op", spec2.toString());

        //extending TODO test TypeIdSpec.fullSpec()
    }

    @Test
    public void testIsEmpty() throws Exception {
        Spec emptySpec = Spec.parseSpec("");
        assertTrue(emptySpec.isEmpty());
    }

    @Test
    public void testGetTokensCount() throws Exception {
        Spec spec;
        spec = new FullSpec("/Type#id!version.operation");
        assertEquals(4, spec.getTokensCount());
        spec = new FilterSpec("/Type#id!version");
        assertEquals(3, spec.getTokensCount());
        spec = new TypeIdSpec("/Type#id");
        assertEquals(2, spec.getTokensCount());
        spec = new VersionOpSpec("!version.operation");
        assertEquals(2, spec.getTokensCount());
        spec = new FilterSpec("/Type");
        assertEquals(1, spec.getTokensCount());
        spec = new FilterSpec("");
        assertEquals(0, spec.getTokensCount());
    }

    @Test
    public void testGetToken() throws Exception {
        FullSpec spec;
        spec = new FullSpec("/Type#id!version.op");
        assertEquals("/Type", spec.getType().toString());
        assertEquals("#id", spec.getId().toString());
        assertEquals("!version", spec.getVersion().toString());
        assertEquals(".op", spec.getOp().toString());
    }

    @Test
    public void testGetTypeId() throws Exception {
        FullSpec spec = new FullSpec("/Type#id!version.op");
        assertEquals("/Type#id", spec.getTypeId().toString());
    }

    @Test
    public void testGetVersionOp() throws Exception {
        FullSpec spec = new FullSpec("/Type#id!version.op");
        assertEquals("!version.op", spec.getVersionOp().toString());
    }

    @Test
    public void testGetTokenIterator() throws Exception {
        VersionVectorSpec spec = new VersionVectorSpec("!v1+s1!v2+s2!v3+s2");
        Iterator<VersionToken> it = spec.getTokenIterator();
        List<SToken> tokens = new ArrayList<SToken>();
        while (it.hasNext()) {
            tokens.add(it.next());
        }
        assertEquals(3, tokens.size());
        assertEquals(new VersionToken("!v1+s1"), tokens.get(0));
        assertEquals(new VersionToken("!v2+s2"), tokens.get(1));
        assertEquals(new VersionToken("!v3+s2"), tokens.get(2));
    }

    @Test
    public void testSort() throws Exception {
        final String rightOrdered = "/Type#id!ver.op";
        Spec spec = Spec.parseSpec(rightOrdered);
        // leave correct order
        assertEquals(rightOrdered, spec.toString());
        // fix order
        spec = Spec.parseSpec("#id/Type!ver.op");
        assertEquals(rightOrdered, spec.toString());
        spec = Spec.parseSpec("#id!ver/Type.op");
        assertEquals(rightOrdered, spec.toString());
        spec = Spec.parseSpec(".op!ver#id/Type");
        assertEquals(rightOrdered, spec.toString());
    }

    @Test
    public void testToString() throws Exception {
        FullSpec spec = new FullSpec(
                new TypeIdSpec("/Mouse#s1"),
                new VersionToken("!8oJOb03+s1~0"),
                Syncable.ON
        );
        assertEquals("/Mouse#s1!8oJOb03+s1~0.on", spec.toString());
    }

    @Test
    public void testEquals() throws Exception {
        FullSpec spec = new FullSpec("/Mouse#s1!8oJOb03+s1~0.on");
        //noinspection EqualsBetweenInconvertibleTypes
        assertTrue("comparing to string", spec.equals("/Mouse#s1!8oJOb03+s1~0.on"));
    }
}
