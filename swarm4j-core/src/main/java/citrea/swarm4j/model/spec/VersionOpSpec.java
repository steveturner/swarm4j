package citrea.swarm4j.model.spec;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 17.09.2014
 *         Time: 21:59
 */
public final class VersionOpSpec extends Spec {

    private final VersionToken version;
    private final OpToken op;

    public VersionOpSpec(VersionOpSpec versionOp) {
        this(versionOp.getVersion(), versionOp.getOp());
    }

    public VersionOpSpec(VersionToken version, OpToken op) {
        super();
        this.version = version;
        this.op = op;
    }

    public VersionOpSpec(String specAsStr) {
        super();
        SToken[] tokens = parse(specAsStr);
        if (tokens.length != 2 ||
                tokens[0].getQuant() != SQuant.VERSION ||
                tokens[1].getQuant() != SQuant.OP) {
            throw new IllegalArgumentException("expecting !version.operation specifier");
        }
        this.version = (VersionToken) tokens[0];
        this.op = (OpToken) tokens[1];
    }

    public VersionToken getVersion() {
        return version;
    }

    public OpToken getOp() {
        return op;
    }

    @Override
    public List<SToken> listTokens() {
        return Arrays.asList(version, op);
    }

    @Override
    public int getTokensCount() {
        return 2;
    }

    public FullSpec fullSpec(TypeIdSpec typeId) {
        return new FullSpec(typeId, this);
    }

    public FullSpec fullSpec(TypeToken type, IdToken id) {
        return new FullSpec(type, id, this);
    }

    public VersionOpSpec overrideOp(OpToken opToken) {
        return new VersionOpSpec(version, opToken);
    }

    @Override
    public String asString() {
        return version.toString() + op.toString();
    }
}
