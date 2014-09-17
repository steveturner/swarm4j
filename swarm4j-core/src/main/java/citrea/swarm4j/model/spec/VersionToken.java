package citrea.swarm4j.model.spec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 17.09.2014
 *         Time: 22:03
 */
public class VersionToken extends SToken {

    public VersionToken(String body) {
        super(SQuant.VERSION, body);
    }

    public VersionToken(String bare, String ext) {
        super(SQuant.VERSION, bare, ext);
    }

    public IdToken convertToId() {
        return new IdToken(getBare(), getProcessId());
    }

    public VersionVectorSpec asSpec() {
        return new VersionVectorSpec(this);
    }
}
