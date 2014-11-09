package citrea.swarm4j.core.spec;

/**
 * Type token (format: "/SomeType")
 *
 * @author aleksisha
 *         Date: 17.09.2014
 *         Time: 22:01
 */
public class TypeToken extends SToken {

    public static final TypeToken NO_TYPE = new TypeToken("/NOT_INITED");

    public TypeToken(String body) {
        super(SQuant.TYPE, body);
    }

    public TypeToken(String bare, String ext) {
        super(SQuant.TYPE, bare, ext);
    }
}
