package citrea.swarm4j.model.spec;

import java.util.*;
import java.util.regex.Matcher;

/**
 * Base class for all types of Specifiers
 *
 * Created with IntelliJ IDEA.
 * @author aleksisha
 *         Date: 26/10/13
 *         Time: 15:02
 */
public abstract class Spec implements Comparable<Spec> {

    private static final int AVERAGE_SPEC_LENGTH = 48;
    private static final EmptySpec EMPTY_SPEC = new EmptySpec();

    private String str = null;

    protected Spec() {
    }

    public abstract List<SToken> listTokens();

    public String asString() {
        List<SToken> tokens = listTokens();
        StringBuilder sb = new StringBuilder(AVERAGE_SPEC_LENGTH);
        for (SToken tok : tokens) {
            if (tok == null) {
                throw new IllegalArgumentException("malformed specifier (token is null)");
            }
            sb.append(tok.toString());
        }
        return sb.toString();
    }

    public final boolean isEmpty() {
        return getTokensCount() == 0;
    }

    public final boolean fits(Spec specFilter) {
        for (SToken tok : specFilter.listTokens()) {
            boolean found = false;
            for (SToken token : this.listTokens()) {
                if (tok.equals(token)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final String toString() {
        if (str == null) {
            str = asString();
        }
        return str;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;

        if (o == null) return false;
        if (o instanceof String) {
            return o.equals(this.toString());
        }
        if (getClass() != o.getClass()) return false;

        Spec spec = (Spec) o;

        return toString().equals(spec.toString());
    }

    @Override
    public final int hashCode() {
        return toString().hashCode();
    }

    @Override
    public final int compareTo(Spec spec) {
        return spec == null ? 1 : this.toString().compareTo(spec.toString());
    }

    public static SToken[] parse(String specAsString) {
        if (specAsString == null) {
            return new SToken[0];
        }

        Matcher matcher = SToken.RE_Q_TOK_EXT.matcher(specAsString);
        List<SToken> tokensList = new ArrayList<SToken>(4);
        while (matcher.find()) {
            final SToken tok;
            final String bare = matcher.group(3);
            final String ext = matcher.group(4);
            switch (SQuant.byCode(matcher.group(1))) {
                case TYPE:
                    tok = new TypeToken(bare, ext);
                    break;
                case ID:
                    tok = new IdToken(bare, ext);
                    break;
                case VERSION:
                    tok = new VersionToken(bare, ext);
                    break;
                case OP:
                    tok = new OpToken(bare, ext);
                    break;
                default:
                    throw new IllegalArgumentException("unknown token quant: " + matcher.group(1));
            }
            tokensList.add(tok);
        }

        boolean hasTypeOrIdOrOperation = false;
        for (SToken token : tokensList) {
            switch (token.getQuant()) {
                case TYPE:
                case ID:
                case OP:
                    hasTypeOrIdOrOperation = true;
                    break;
            }
        }
        if (hasTypeOrIdOrOperation) {
            Collections.sort(tokensList, SToken.ORDER_BY_QUANT);
        }
        return tokensList.toArray(new SToken[tokensList.size()]);
    }

    public static Spec parseSpec(String specAsStr) {
        SToken[] tokens = parse(specAsStr);
        int tokensCount = tokens.length;
        if (tokensCount == 0) {
            return EMPTY_SPEC;
        } else {
            if (tokens[0] instanceof TypeToken) {
                TypeToken type = (TypeToken) tokens[0];
                if (tokensCount > 1) {
                    if (tokens[1] instanceof IdToken) {
                        IdToken id = (IdToken) tokens[1];
                        if (tokensCount == 2) {
                            return new TypeIdSpec(type, id);
                        } else if (tokens[2] instanceof VersionToken) {
                            VersionToken version = (VersionToken) tokens[2];
                            if (tokensCount > 3) {
                                if (tokens[3] instanceof OpToken) {
                                    OpToken op = (OpToken) tokens[3];
                                    if (tokensCount == 4) {
                                        return new FullSpec(type, id, version, op);
                                    } else {
                                        throw new IllegalArgumentException("malformed specifier (/Type#id!version.op ???): " + specAsStr);
                                    }
                                } else {
                                    throw new IllegalArgumentException("malformed specifier (/Type#id!version NOOP): " + specAsStr);
                                }
                            } else {
                                throw new IllegalArgumentException("malformed specifier (/Type#id!version NOOP): " + specAsStr);
                            }
                        } else {
                            throw new IllegalArgumentException("malformed specifier (/Type#id NOVER): " + specAsStr);
                        }
                    } else {
                        throw new IllegalArgumentException("malformed specifier (/Type NOID): " + specAsStr);
                    }
                } else {
                    throw new IllegalArgumentException("malformed specifier (/Type ???): " + specAsStr);
                }
            } else if (tokens[0] instanceof VersionToken) {
                VersionToken version = (VersionToken) tokens[0];
                if (tokensCount > 1) {
                    if (tokens[1] instanceof OpToken) {
                        OpToken op = (OpToken) tokens[1];
                        if (tokensCount == 2) {
                            return new VersionOpSpec(version, op);
                        } else {
                            throw new IllegalArgumentException("malformed specifier (!version.op ???): " + specAsStr);
                        }
                    } else if (tokens[1] instanceof VersionToken) {
                        boolean ok = true;
                        for (int i = 2; i < tokensCount; i++) {
                            if (!(tokens[i] instanceof VersionToken)) {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) {
                            return new VersionVectorSpec((VersionToken[]) tokens);
                        } else {
                            throw new IllegalArgumentException("malformed specifier (!ver1!ver2... ???): " + specAsStr);
                        }
                    } else {
                        throw new IllegalArgumentException("malformed specifier (!version ???): " + specAsStr);
                    }
                } else {
                    throw new IllegalArgumentException("malformed specifier (!version NOOP): " + specAsStr);
                }
            } else {
                throw new IllegalArgumentException("malformed specifier (should start from /Type or !version): " + specAsStr);
            }
        }
    }

    public static final Comparator<Spec> ORDER_NATURAL = new Comparator<Spec>() {

        @Override
        public int compare(Spec left, Spec right) {
            if (left == null) {
                return right == null ? 0 : -1;
            } else {
                if (right == null) {
                    return 1;
                }
                return left.compareTo(right);
            }
        }
    };

    public static final Comparator<Spec> ORDER_REVERSE = Collections.reverseOrder(ORDER_NATURAL);

    public abstract int getTokensCount();

    /**
     * Empty specifier contains no tokens
     */
    private static final class EmptySpec extends Spec {

        private EmptySpec() {
        }

        @Override
        public List<SToken> listTokens() {
            return Collections.emptyList();
        }

        @Override
        public int getTokensCount() {
            return 0;
        }
    }
}
