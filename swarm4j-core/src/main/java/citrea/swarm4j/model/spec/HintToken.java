package citrea.swarm4j.model.spec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 07.10.2014
 *         Time: 21:27
 */
public class HintToken extends SToken {

    public HintToken(String body) {
        super(SQuant.HINT, body);
    }

    public HintToken(String bare, String ext) {
        super(SQuant.HINT, bare, ext);
    }
}
