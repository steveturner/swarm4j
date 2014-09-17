package citrea.swarm4j.model.spec;

import java.util.*;
import java.util.regex.Matcher;

/**
 * Specifier "/type#id.member!version"
 *
 * Created with IntelliJ IDEA.
 * @author aleksisha
 *         Date: 26/10/13
 *         Time: 15:02
 */
public class Spec implements Comparable<Spec> {

    private static final int AVERAGE_SPEC_LENGTH = 48;

    private final SpecToken[] tokens;
    private final String asString;

    public Spec(SpecToken... tokens) {
        int j = 0;
        for (int i = 0, l = tokens.length; i < l; i++) {
            if (tokens[i] != null) {
                if (j != i) {
                    tokens[j] = tokens[i];
                }
                j++;
            }
        }
        if (j < tokens.length) {
            tokens = cloneTokens(tokens, j);
        }
        this.tokens = tokens;
        StringBuilder sb = new StringBuilder(AVERAGE_SPEC_LENGTH);
        for (SpecToken tok : tokens) {
            sb.append(tok.toString());
        }
        this.asString = sb.toString();
    }

    public Spec(Spec copy) {
        this(cloneTokens(copy.tokens, copy.tokens.length));
    }

    public Spec(String specAsString) {
        this(Spec.parse(specAsString));
    }

    public final Spec addToken(SpecToken token) {
        SpecToken[] newTokens = cloneTokens(tokens, tokens.length + 1);
        newTokens[tokens.length] = token;
        return new Spec(newTokens);
    }

    public final Spec addToken(String token) {
        SpecToken[] tokensToAdd = new Spec(token).tokens;
        SpecToken[] newTokens = new SpecToken[tokens.length + tokensToAdd.length];
        System.arraycopy(tokens, 0, newTokens, 0, tokens.length);
        System.arraycopy(tokensToAdd, 0, newTokens, tokens.length, tokensToAdd.length);
        return new Spec(newTokens);
    }

    public Spec overrideToken(SpecToken overrideWith) {
        final SpecQuant q = overrideWith.getQuant();
        SpecToken[] newTokens = cloneTokens(tokens, tokens.length);
        boolean found = false;
        for (int i = 0, l = newTokens.length; i < l; i++) {
            SpecToken token = newTokens[i];
            if (token.getQuant() == q) {
                newTokens[i] = overrideWith;
                found = true;
                break;
            }
        }
        if (!found) {
            newTokens = cloneTokens(newTokens, newTokens.length + 1);
            newTokens[newTokens.length - 1] = overrideWith;
        }
        return new Spec(newTokens);
    }

    public Spec sort() {
        SpecToken[] newTokens = cloneTokens(tokens, tokens.length);
        Arrays.sort(newTokens, SpecToken.ORDER_BY_QUANT);
        return new Spec(newTokens);
    }

    public boolean isEmpty() {
        return tokens.length == 0;
    }

    public int getTokensCount() {
        return tokens.length;
    }

    public SpecToken getToken(int idx) {
        return (tokens.length >= idx + 1) ? tokens[idx] : null;
    }

    public SpecToken getToken(SpecQuant quant) {
        for (SpecToken token : tokens) {
            if (token.getQuant() == quant) {
                return token;
            }
        }
        return null;
    }

    public Iterator<SpecToken> getTokenIterator(SpecQuant quant) {
        return new TokenIterator(tokens, quant);
    }

    public Spec getTypeId() {
        return new Spec(getType(), getId());
    }

    public Spec getVersionOp() {
        return new Spec(getVersion(), getOp());
    }

    public SpecToken getType() {
        return getToken(SpecQuant.TYPE);
    }

    public SpecToken getId() {
        return getToken(SpecQuant.ID);
    }

    public SpecToken getVersion() {
        return getToken(SpecQuant.VERSION);
    }

    public SpecToken getOp() {
        return getToken(SpecQuant.OP);
    }


    public final SpecPattern getPattern() {
        switch (tokens.length) {
            case 4:
                for (int i = 0; i < tokens.length; i++) {
                    if (tokens[i].getQuant() != SpecQuant.byOrder(i)) {
                        return SpecPattern.UNKNOWN;
                    }
                }
                return SpecPattern.FULL;
            case 2:
                if (tokens[0].getQuant() == SpecQuant.TYPE &&
                        tokens[1].getQuant() == SpecQuant.ID) {
                    return SpecPattern.TYPE_ID;
                } else if (tokens[0].getQuant() == SpecQuant.VERSION &&
                        tokens[1].getQuant() == SpecQuant.OP) {
                    return SpecPattern.VERSION_OP;
                } else {
                    return SpecPattern.UNKNOWN;
                }
            default:
                return SpecPattern.UNKNOWN;
        }
    }

    public final boolean fits(Spec specFilter) {
        for (SpecToken tok : specFilter.tokens) {
            boolean found = false;
            for (SpecToken token : this.tokens) {
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
        return asString;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;

        if (o == null) return false;
        if (o instanceof String) {
            return o.equals(this.asString);
        }
        if (getClass() != o.getClass()) return false;

        Spec spec = (Spec) o;

        return asString.equals(spec.asString);
    }

    @Override
    public final int hashCode() {
        int len = this.tokens.length;
        int result = 0;
        for (SpecQuant q : SpecQuant.values()) {
            int idx = q.ordinal();
            result *= 31;
            if (idx < len) {
                result += this.tokens[idx].hashCode();
            }
        }
        return result;
    }

    @Override
    public final int compareTo(Spec spec) {
        return spec == null ? 1 : this.asString.compareTo(spec.asString);
    }

    private static SpecToken[] cloneTokens(SpecToken[] tokens, int count) {
        SpecToken[] res = new SpecToken[count];
        System.arraycopy(tokens, 0, res, 0, Math.min(count, tokens.length));
        return res;
    }

    public static SpecToken[] parse(String specAsString) {
        if (specAsString == null) {
            return new SpecToken[0];
        }

        Matcher matcher = SpecToken.RE_Q_TOK_EXT.matcher(specAsString);
        List<SpecToken> tokensList = new ArrayList<SpecToken>(4);
        while (matcher.find()) {
            SpecToken tok = new SpecToken(
                    SpecQuant.byCode(matcher.group(1)),
                    matcher.group(3),
                    matcher.group(4)
            );
            tokensList.add(tok);
        }

        return tokensList.toArray(new SpecToken[tokensList.size()]);
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
}
