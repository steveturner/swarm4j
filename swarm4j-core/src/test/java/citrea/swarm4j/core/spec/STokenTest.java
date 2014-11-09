package citrea.swarm4j.core.spec;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

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
        SToken tok = new TypeToken("/Type");
        assertEquals("Type", tok.getBody());

        //token with ext part
        tok = new IdToken("#bare+ext");
        assertEquals("bare+ext", tok.getBody());
    }

    @Test
    public void testGetBare() throws Exception {
        //token w/o ext part
        SToken tok = new IdToken("#simple");
        assertEquals("simple", tok.getBare());

        //token with ext part
        tok = new IdToken("#bare+ext");
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
        SToken tok = new VersionToken("bare1", "ext");
        assertEquals("produce correct token", "!bare1+ext", tok.toString());
        assertEquals("bare1+ext", tok.getBody());
        assertEquals("bare1", tok.getBare());
        assertEquals("ext", tok.getProcessId());
    }

    @Test
    public void testOverrideQuant() throws Exception {
        SToken tok = new TypeToken("/bare+ext");
        assertEquals("/bare+ext", tok.overrideQuant(SQuant.TYPE).toString());
        assertEquals("#bare+ext", tok.overrideQuant(SQuant.ID).toString());
        assertEquals("!bare+ext", tok.overrideQuant(SQuant.VERSION).toString());
        assertEquals(".bare+ext", tok.overrideQuant(SQuant.OP).toString());
    }

    @Test
    public void testEquals() throws Exception {
        SToken tok = new TypeToken("bare+ext");

        SToken tokEq = new TypeToken("bare+ext");
        assertEquals("same body and quant", tokEq, tok);

        SToken tokNotEq = new TypeToken("bare2+ext");
        assertFalse("different body", tok.equals(tokNotEq));

        tokNotEq = new IdToken("bare+ext");
        assertFalse("different quant", tok.equals(tokNotEq));

        //noinspection EqualsBetweenInconvertibleTypes
        assertTrue("comparable with strings", tok.equals("/bare+ext"));
    }
}
