package citrea.swarm4j.model.spec;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.Calendar;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02/11/13
 *         Time: 23:44
 */
public class STokenTest {

    @Test
    public void testDate2ts() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(SToken.EPOCH);
        assertEquals("00000", SToken.date2ts(calendar.getTime()));

        calendar.add(Calendar.SECOND, 1);
        assertEquals("00001", SToken.date2ts(calendar.getTime()));

        calendar.add(Calendar.SECOND, 63);
        assertEquals("00010", SToken.date2ts(calendar.getTime()));
    }

    @Test
    public void testInt2base() throws Exception {
        assertEquals("0000000010", SToken.int2base(64, 10));
        assertEquals("00010", SToken.int2base(64, 5));
        assertEquals("00011", SToken.int2base(65, 5));
    }

    @Test
    public void testGetBody() throws Exception {
        SToken tok = new SToken("/Type");
        assertEquals("Type", tok.getBody());

        //token with ext part
        tok = new SToken("#bare+ext");
        assertEquals("bare+ext", tok.getBody());
    }

    @Test
    public void testGetBare() throws Exception {
        //token w/o ext part
        SToken tok = new SToken("#simple");
        assertEquals("simple", tok.getBare());

        //token with ext part
        tok = new SToken("#bare+ext");
        assertEquals("bare", tok.getBare());
    }

    @Test
    public void testGetExt() throws Exception {
        //token w/o ext part
        IdToken tok = new IdToken("simple");
        assertEquals("special <no_author> value when token w/o ext", SToken.NO_AUTHOR, tok.getProcessId());

        //token with ext part
        tok = new IdToken("bare+ext");
        assertEquals("ext", tok.getProcessId());
    }

    @Test
    public void testJoiningConstructor() throws Exception {
        SToken tok = new SToken(SQuant.VERSION, "bare1", "ext");
        assertEquals("produce correct token", "!bare1+ext", tok.toString());
        assertEquals("bare1+ext", tok.getBody());
        assertEquals("bare1", tok.getBare());
        assertEquals("ext", tok.getProcessId());
    }

    @Test
    public void testOverrideBare() throws Exception {
        SToken tok = new SToken("!bare1+ext");
        SToken tok2 = tok.overrideBare("bare2");
        assertNotSame("generates new SpecToken instance", tok, tok2);
        assertEquals("do not modify the object", "!bare1+ext", tok.toString());
        assertEquals("do not modify object body", "bare1+ext", tok.getBody());
        assertEquals("do not modify object bare", "bare1", tok.getBare());
        assertEquals("do not modify object ext", "ext", tok.getProcessId());
        assertEquals("produce correct token", "!bare2+ext", tok2.toString());
    }

    @Test
    public void testOverrideExt() throws Exception {
        SToken tok = new SToken("!bare1+ext1");
        SToken tok2 = tok.overrideExt("ext2");
        assertNotSame("generates new SpecToken instance", tok, tok2);
        assertEquals("do not modify the object", "!bare1+ext1", tok.toString());
        assertEquals("do not modify object bare", "bare1", tok.getBare());
        assertEquals("do not modify object ext", "ext1", tok.getProcessId());
        assertEquals("produce correct token", "!bare1+ext2", tok2.toString());
    }

    @Test
    public void testOverrideQuant() throws Exception {
        SToken tok = new SToken("/bare+ext");
        assertEquals("/bare+ext", tok.overrideQuant(SQuant.TYPE).toString());
        assertEquals("#bare+ext", tok.overrideQuant(SQuant.ID).toString());
        assertEquals("!bare+ext", tok.overrideQuant(SQuant.VERSION).toString());
        assertEquals(".bare+ext", tok.overrideQuant(SQuant.OP).toString());
    }

    @Test
    public void testEquals() throws Exception {
        SToken tok = new SToken("bare+ext");

        SToken tokEq = new SToken("bare+ext");
        assertEquals(tokEq, tok);

        SToken tokNotEq = new SToken("bare2+ext");
        assertFalse(tok.equals(tokNotEq));

        //noinspection EqualsBetweenInconvertibleTypes
        assertTrue("comparable with strings", tok.equals("bare+ext"));
    }
}
