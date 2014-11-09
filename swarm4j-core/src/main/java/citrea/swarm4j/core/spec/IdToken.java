package citrea.swarm4j.core.spec;

/**
 * Object ID token (format: "#objectId")
 *
 * @author aleksisha
 *         Date: 17.09.2014
 *         Time: 22:02
 */
public class IdToken extends SToken {
    public static final IdToken NO_ID = new IdToken("#NO_ID");

    public IdToken(String body) {
        super(SQuant.ID, body);
    }

    public IdToken(String bare, String ext) {
        super(SQuant.ID, bare, ext);
    }
}
