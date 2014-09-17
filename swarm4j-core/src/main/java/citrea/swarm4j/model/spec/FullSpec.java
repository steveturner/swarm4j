package citrea.swarm4j.model.spec;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 17.09.2014
 *         Time: 22:15
 */
public final class FullSpec extends Spec {

    private final TypeIdSpec typeId;
    private final VersionOpSpec versionOp;

    public FullSpec(TypeToken type, IdToken id, VersionToken version, OpToken op) {
        this(new TypeIdSpec(type, id), new VersionOpSpec(version, op));
    }

    public FullSpec(TypeToken type, IdToken id, VersionOpSpec versionOp) {
        this(new TypeIdSpec(type, id), versionOp);
    }

    public FullSpec(TypeIdSpec typeId, VersionToken version, OpToken op) {
        this(typeId, new VersionOpSpec(version, op));
    }

    public FullSpec(TypeIdSpec typeId, VersionOpSpec versionOp) {
        super();
        this.typeId = typeId;
        this.versionOp = versionOp;
    }

    public FullSpec(String specAsStr) {
        super();
        SToken[] tokens = parse(specAsStr);

        if (tokens.length != 4 ||
                tokens[0].getQuant() != SQuant.TYPE ||
                tokens[1].getQuant() != SQuant.ID ||
                tokens[2].getQuant() != SQuant.VERSION ||
                tokens[3].getQuant() != SQuant.OP) {
            throw new IllegalArgumentException("expecting /Type#id!ver.op specifier");
        }
        this.typeId = new TypeIdSpec((TypeToken) tokens[0], (IdToken) tokens[1]);
        this.versionOp = new VersionOpSpec((VersionToken) tokens[2], (OpToken) tokens[3]);
    }

    public TypeToken getType() {
        return typeId.getType();
    }

    public IdToken getId() {
        return typeId.getId();
    }

    public VersionToken getVersion() {
        return versionOp.getVersion();
    }

    public OpToken getOp() {
        return versionOp.getOp();
    }

    public TypeIdSpec getTypeId() {
        return typeId;
    }

    public VersionOpSpec getVersionOp() {
        return versionOp;
    }

    public FullSpec overrideId(IdToken idToken) {
        return new FullSpec(typeId.overrideId(idToken), versionOp);
    }

    public FullSpec overrideOp(OpToken opToken) {
        return new FullSpec(typeId, versionOp.overrideOp(opToken));
    }

    @Override
    public List<SToken> listTokens() {
        return Arrays.asList(typeId.getType(), typeId.getId(), versionOp.getVersion(), versionOp.getOp());
    }

    @Override
    public int getTokensCount() {
        return 4;
    }

    @Override
    public String asString() {
        return typeId.toString() + versionOp.toString();
    }
}