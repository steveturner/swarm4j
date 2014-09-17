package citrea.swarm4j.model.spec;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02/11/13
 *         Time: 23:26
 */
public class SQuantTest {
    @Test
    public void testPrev() throws Exception {
        assertNull(SQuant.TYPE.prev());
        assertEquals(SQuant.TYPE, SQuant.ID.prev());
        assertEquals(SQuant.ID, SQuant.VERSION.prev());
        assertEquals(SQuant.VERSION, SQuant.OP.prev());
    }

    @Test
    public void testNext() throws Exception {
        assertEquals(SQuant.ID, SQuant.TYPE.next());
        assertEquals(SQuant.VERSION, SQuant.ID.next());
        assertEquals(SQuant.OP, SQuant.VERSION.next());
        assertNull(SQuant.OP.next());
    }

    @Test
    public void testByCode() throws Exception {
        assertEquals(SQuant.TYPE, SQuant.byCode('/'));
        assertEquals(SQuant.ID, SQuant.byCode('#'));
        assertEquals(SQuant.OP, SQuant.byCode('.'));
        assertEquals(SQuant.VERSION, SQuant.byCode('!'));
        assertNull(SQuant.byCode('a'));
    }

    @Test
    public void testByOrder() throws Exception {
        assertEquals(SQuant.TYPE, SQuant.byOrder(0));
        assertEquals(SQuant.ID, SQuant.byOrder(1));
        assertEquals(SQuant.VERSION, SQuant.byOrder(2));
        assertEquals(SQuant.OP, SQuant.byOrder(3));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testByOrderWrong() throws Exception {
        SQuant.byOrder(4);
    }
}
