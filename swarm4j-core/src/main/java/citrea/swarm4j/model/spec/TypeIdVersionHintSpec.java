package citrea.swarm4j.model.spec;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 06.10.2014
 *         Time: 21:40
 */
public class TypeIdVersionHintSpec extends Spec {

    public static final String ERROR_MALFORMED = "expecting [/Type][#id][!version][*hint] specifier";

    private final TypeToken type;
    private final IdToken id;
    private final VersionToken version;
    private final HintToken hint;
    private final int tokensCount;

    public TypeIdVersionHintSpec(String specAsStr, TypeToken defaultType) {
        super();
        SToken[] tokens = parse(specAsStr);
        Map<SQuant, SToken> tokensByQuant = tokensByQuants(tokens);
        TypeToken typeToken = (TypeToken) tokensByQuant.get(SQuant.TYPE);
        if (defaultType.equals(typeToken)) {
            tokensByQuant.remove(SQuant.TYPE);
            this.type = null;
        } else {
            this.type = typeToken;
        }
        this.id = (IdToken) tokensByQuant.get(SQuant.ID);
        VersionToken versionToken = (VersionToken) tokensByQuant.get(SQuant.VERSION);
        if (versionToken == null) {
            versionToken = VersionToken.ZERO_VERSION;
            tokensByQuant.put(SQuant.VERSION, versionToken);
        }
        this.version = versionToken;
        this.hint = (HintToken) tokensByQuant.get(SQuant.HINT);
        this.tokensCount = tokensByQuant.size();
    }

    public TypeIdVersionHintSpec(VersionToken version, HintToken hint) {
        this.type = null;
        this.id = null;
        if (version == null) {
            version = VersionToken.ZERO_VERSION;
        }
        this.version = version;
        this.hint = hint;
        this.tokensCount = this.hint != null ? 2 : 1;
    }

    public TypeIdVersionHintSpec(TypeIdSpec typeId, VersionToken version) {
        this.type = typeId.getType();
        this.id = typeId.getId();
        if (version == null) {
            version = VersionToken.ZERO_VERSION;
        }
        this.version = version;
        this.hint = null;
        this.tokensCount = 3;
    }

    private Map<SQuant, SToken> tokensByQuants(SToken... tokens) {
        Map<SQuant, SToken> res = new HashMap<SQuant, SToken>(4);
        for (SToken tok : tokens) {
            SQuant quant = tok.getQuant();
            if (res.containsKey(quant)) {
                throw new IllegalArgumentException(ERROR_MALFORMED + " (more than one token with "+ quant +"-quant)");
            }
            res.put(quant, tok);
        }
        return res;
    }

    @Override
    public List<SToken> listTokens() {
        List<SToken> res = new ArrayList<SToken>(tokensCount);
        if (type != null) {
            res.add(type);
        }
        if (id != null) {
            res.add(id);
        }
        res.add(version);
        if (hint != null) {
            res.add(hint);
        }
        return Collections.unmodifiableList(res);
    }

    public TypeToken getType() {
        return type;
    }

    public IdToken getId() {
        return id;
    }

    public VersionToken getVersion() {
        return version;
    }

    public HintToken getHint() {
        return hint;
    }

    @Override
    public int getTokensCount() {
        return tokensCount;
    }

}
