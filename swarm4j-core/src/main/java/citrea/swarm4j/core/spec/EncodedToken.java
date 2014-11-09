package citrea.swarm4j.core.spec;

/**
 * Encoded token
 * @see citrea.swarm4j.core.spec.LongSpecIterator
 *
 * @author aleksisha
 *         Date: 07.10.2014
 *         Time: 15:07
 */
public class EncodedToken extends SToken {

    public EncodedToken(SQuant quant, String bare, String processId) {
        super(quant, bare, processId);
    }

    public EncodedToken(SQuant quant) {
        super(quant, "", NO_AUTHOR);
    }
}
