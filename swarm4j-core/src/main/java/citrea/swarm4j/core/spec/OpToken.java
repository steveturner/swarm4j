package citrea.swarm4j.core.spec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 17.09.2014
 *         Time: 22:03
 */
public class OpToken extends SToken {

    public OpToken(String body) {
        super(SQuant.OP, body);
    }

    public OpToken(String bare, String ext) {
        super(SQuant.OP, bare, ext);
    }
}
