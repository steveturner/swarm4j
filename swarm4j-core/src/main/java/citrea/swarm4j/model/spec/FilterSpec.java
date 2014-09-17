package citrea.swarm4j.model.spec;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 18.09.2014
 *         Time: 00:39
 */
public class FilterSpec extends Spec {

    private final TypeToken type;
    private final IdToken id;
    private final OpToken op;
    private final VersionVectorSpec version;
    private final int tokensCount;

    public FilterSpec(String specAsStr) {
        this(parse(specAsStr));
    }

    public FilterSpec(SToken... tokens) {
        TypeToken type = null;
        IdToken id = null;
        VersionToken version = null;
        OpToken op = null;
        int count = 0;
        List<VersionToken> versionTokens = new ArrayList<VersionToken>(tokens.length);
        for (SToken token : tokens) {
            switch (token.getQuant()) {
                case TYPE:
                    if (type != null) {
                        raiseIllegalArgumentException("malformed specifier (second /Type token):", tokens, token);
                    }
                    type = (TypeToken) token;
                    count++;
                    break;
                case ID:
                    if (id != null) {
                        raiseIllegalArgumentException("malformed specifier (second #id token):", tokens, token);
                    }
                    id = (IdToken) token;
                    count++;
                    break;
                case OP:
                    if (op != null) {
                        raiseIllegalArgumentException("malformed specifier (second .op token):", tokens, token);
                    }
                    op = (OpToken) token;
                    count++;
                    break;
                case VERSION:
                    versionTokens.add((VersionToken) token);
                    count++;
                    break;
            }
        }
        this.type = type;
        this.id = id;
        this.op = op;
        this.version = new VersionVectorSpec(versionTokens.toArray(new VersionToken[versionTokens.size()]));
        this.tokensCount = count;
    }

    private void raiseIllegalArgumentException(String message, SToken[] tokens, SToken wrong) {
        StringBuilder sb = new StringBuilder(message);
        sb.append(" ");
        for (SToken tok : tokens) {
            if (tok == wrong) {
                sb.append(" >>");
                sb.append(tok.toString());
                sb.append("<< ");
            } else {
                sb.append(tok.toString());
            }
        }
        throw new IllegalArgumentException(sb.toString());
    }

    public TypeToken getType() {
        return type;
    }

    public IdToken getId() {
        return id;
    }

    public VersionVectorSpec getVersion() {
        return version;
    }

    public OpToken getOp() {
        return op;
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
        res.addAll(version.listTokens());
        if (op != null) {
            res.add(op);
        }
        return res;
    }

    @Override
    public int getTokensCount() {
        return tokensCount;
    }
}
