package citrea.swarm4j.core.spec;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 17.09.2014
 *         Time: 21:59
 */
public final class TypeIdSpec extends Spec {

    private final TypeToken type;
    private final IdToken id;

    public TypeIdSpec(TypeIdSpec typeId) {
        this(typeId.getType(), typeId.getId());
    }

    public TypeIdSpec(TypeToken type, IdToken id) {
        this.type = type;
        this.id = id;
    }

    public TypeIdSpec(String specAsStr) {
        super();
        SToken[] tokens = parse(specAsStr);
        if (tokens.length != 2 ||
                tokens[0].getQuant() != SQuant.TYPE ||
                tokens[1].getQuant() != SQuant.ID) {
            throw new IllegalArgumentException("expecting /Type#id specifier");
        }
        this.type = (TypeToken) tokens[0];
        this.id = (IdToken) tokens[1];
    }

    public TypeToken getType() {
        return type;
    }

    public IdToken getId() {
        return id;
    }

    public FullSpec fullSpec(VersionOpSpec versionOp) {
        return new FullSpec(this, versionOp);
    }

    public FullSpec fullSpec(VersionToken version, OpToken op) {
        return new FullSpec(this, version, op);
    }

    @Override
    public List<SToken> listTokens() {
        return Arrays.asList(type, id);
    }

    @Override
    public int getTokensCount() {
        return 2;
    }

    public TypeIdSpec overrideId(IdToken idToken) {
        return new TypeIdSpec(type, idToken);
    }

    @Override
    public String asString() {
        return type.toString() + id.toString();
    }

    public TypeIdVersionHintSpec typeIdVersionHint(VersionToken versionToken) {
        return new TypeIdVersionHintSpec(this, versionToken);
    }
}
