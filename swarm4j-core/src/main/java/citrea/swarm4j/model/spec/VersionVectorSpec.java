package citrea.swarm4j.model.spec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 17.09.2014
 *         Time: 22:07
 */
public final class VersionVectorSpec extends Spec {
    public static final VersionVectorSpec ZERO_VERSION_VECTOR = new VersionVectorSpec(VersionToken.ZERO_VERSION);

    private VersionToken[] tokens;

    public VersionVectorSpec(String specAsStr) {
        this(parse(specAsStr));
    }

    public VersionVectorSpec(VersionToken ... tokens) {
        super();
        this.tokens = tokens;
    }

    public VersionVectorSpec(SToken... tokens) {
        this(filterTokens(tokens));
    }

    private static VersionToken[] filterTokens(SToken[] tokens) {
        List<VersionToken> res = new ArrayList<VersionToken>(tokens.length);
        for (SToken tok : tokens) {
            if (tok.getQuant() == SQuant.VERSION) {
                res.add(tok.toVersionToken());
            }
        }
        return res.toArray(new VersionToken[res.size()]);
    }

    public VersionVectorSpec addTokens(VersionVectorSpec tokensToAdd) {
        return addTokens(tokensToAdd.tokens);
    }

    public VersionVectorSpec addTokens(VersionToken ... tokensToAdd) {
        final int thisLen = tokens.length;
        final int additionLen = tokensToAdd.length;
        final VersionToken[] tokensNew = new VersionToken[thisLen + additionLen];
        System.arraycopy(tokens, 0, tokensNew, 0, thisLen);
        System.arraycopy(tokensToAdd, 0, tokensNew, thisLen, additionLen);
        return new VersionVectorSpec(tokensNew);
    }

    public Iterator<VersionToken> getTokenIterator() {
        return new VersionTokenIterator(tokens);
    }

    @Override
    public List<SToken> listTokens() {
        return Arrays.<SToken>asList(tokens);
    }

    @Override
    public int getTokensCount() {
        return tokens.length;
    }
}
