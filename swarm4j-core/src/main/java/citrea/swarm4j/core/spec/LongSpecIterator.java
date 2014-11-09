package citrea.swarm4j.core.spec;

import java.util.Iterator;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unfortunately, LongSpec cannot be made a simple array because tokens are
 * not fixed-width in the general case. Some tokens are dictionary-encoded
 * into two-symbol segments, e.g. ".on" --> ".o". Other tokens may need 6
 * symbols to encode, e.g. "!timstse+author~ssn" -> "!tss+a".
 * Also, iterators opportunistically use sequential compression. Namely,
 * tokens that differ by +1 are collapsed into quant-only sequences:
 * "!abc+s!abd+s" -> "!abc+s!"
 * So, locating and iterating becomes less-than-trivial. Raw string offsets
 * better not be exposed in the external interface; hence, we need iterators.
 * {
 *   offset:5,       // char offset in the string (chunk)
 *   index:1,        // index of the entry (token)
 *   en: "!",        // the actual matched token (encoded)
 *   chunkIndex:0,   // index of the chunk
 *   de: "!timst00+author~ssn", // decoded token
 *   seqstart: "!ts0+a", // first token of the sequence (encoded)
 *   seqoffset: 3        // offset in the sequence
 * }
 *
 *
 * @author aleksisha
 *         Date: 07.10.2014
 *         Time: 00:33
 */
public class LongSpecIterator implements Iterator<String> {


    private final LongSpec owner;

    /** previous full (non-collapsed) token */
    private SToken prevFull;

    /** count of collapsed tokens */
    private int prevCollapsed = 0;

    /** last match result */
    private MatchResult match;

    /** the chunk we are currently in */
    private int chunkIndex = 0;

    /** token index within the chunk */
    private int tokenIndexInChunk;

    /** token index (position "before the first token") */
    private int tokenIndexGlobal;


    /**
     *
     * @param owner our LongSpec
     * @param index token index (position "before the first token")
     */
    public LongSpecIterator(LongSpec owner, int index) {
        this.owner = owner;         // our LongSpec
        /*this.chunkIndex = 0;
        this.tokenIndexInChunk = -1;
        this.prevFull = undefined;
        //  seqStart IS the previous match or prev match is trivial
        this.prevCollapsed = 0;
        */
        this.skip2chunk(0);
        if (index > 0) {
            this.skip(index);
        }
    }

    public LongSpecIterator(LongSpecIterator original) {
        this.owner = original.owner;
        this.skip2chunk(0);
        if (original.tokenIndexGlobal > 0) {
            this.tokenIndexGlobal = original.tokenIndexGlobal;
        }
    }

    /**
     * The method converts a (relatively) verbose Base64 specifier into an
     * internal compressed format.  Compressed tokens are also
     * variable-getTokensCount; the getTokensCount of the token depends on the encoding
     * method used.
     * 1 unicode symbol: dictionary-encoded (up to 2^15 entries for each quant),
     * 2 symbols: simple timestamp base-2^15 encoded,
     * 3 symbols: timestamp+seq base-2^15,
     * 4 symbols: long-number base-2^15,
     * 5 symbols and more: unencoded original (fallback).
     * As long as two sequential unicoded entries differ by +1 in the body
     * of the token (quant and extension being the same), we use sequential
     * compression. The token is collapsed (only the quant is left).
     */
    public String encode(SToken de) {
        final String tok = de.toString();
        final SQuant quant = de.getQuant();
        final String bare = de.getBare();
        final String ext = de.getProcessId();

        SToken pm = this.prevFull; // this one is en
        String enBare;
        String enExt = null;
        if (ext.length() > 0) {
            enExt = this.owner.encodeCodeBook.get("+" + ext);
            if (enExt == null) {
                enExt = this.owner.allocateCode("+" + ext);
            }
        }
        boolean maySeq = pm != null &&
                quant == pm.getQuant() &&
                (pm.getProcessId().equals(enExt));
        boolean haveSeq;
        String seqBare = "";
        int int1, int2;
        String uni1, uni2;

        //var expected = head + (counter===-1?'':Spec.int2base(counter+inc,1)) + tail;
        switch (bare.length()) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                if (LongSpec.QUANTS_TO_CODE.contains(quant) ||
                        (this.owner.encodeCodeBook.containsKey(tok))) {
                    // 1 symbol by the codebook
                    enBare = this.owner.encodeCodeBook.get(quant.code + bare);
                    if (enBare == null) {
                        enBare = this.owner.allocateCode(quant.code + bare);
                    }
                    enBare = enBare.substring(1); // FIXME separate codebooks 4 quants
                    if (maySeq) {// seq coding for dictionary-coded
                        seqBare = enBare;
                    }
                } else {
                    // verbatim
                    enBare = bare;
                    seqBare = enBare;
                }
                break;

            case 5: // 2-symbol base-2^15
                int i = SToken.base2int(bare);
                enBare = LongSpec.int2uni(i);
                if (maySeq && pm.getBare().length() == 2) {
                    seqBare = LongSpec.int2uni(i - this.prevCollapsed - 1);
                }
                break;

            case 7: // 3-symbol base-2^15
                int1 = SToken.base2int(bare.substring(0, 5));
                int2 = SToken.base2int(bare.substring(5, 7));
                uni1 = LongSpec.int2uni(int1);
                uni2 = LongSpec.int2uni(int2).substring(1);
                enBare = uni1 + uni2;
                if (maySeq && pm.getBare().length() == 3) {
                    seqBare = uni1 + LongSpec.int2uni(int2 - this.prevCollapsed - 1).substring(1, 2);
                }
                break;

            case 10: // 4-symbol 60-bit long number
                int1 = SToken.base2int(bare.substring(0, 5));
                int2 = SToken.base2int(bare.substring(5, 10));
                uni1 = LongSpec.int2uni(int1);
                uni2 = LongSpec.int2uni(int2);
                enBare = uni1 + uni2;
                if (maySeq && pm.getBare().length() == 4) {
                    seqBare = uni1+LongSpec.int2uni(int2 - this.prevCollapsed - 1);
                }
                break;

            default: // verbatim
                enBare = bare;
                seqBare = enBare;

        }
        haveSeq = pm != null && pm.getBare().equals(seqBare);
        return haveSeq ?
                quant.toString() :
                (quant.toString() + enBare + enExt);
    }

    /**
     * Decode a compressed specifier back into base64.
     */
    public VersionToken decode() {
        if (this.match == null) {
            return null;
        }
        EncodedToken token = TokMatcher.buildToken(this.match);
        final SQuant res_quant = token.getQuant();
        if (!SQuant.VERSION.equals(res_quant)) { //TODO support other tokens???
            throw new IllegalArgumentException("not a version token");
        }

        final String bare;
        final String processId;
        if (token.getBare().length() == 0 && prevFull != null) {
            if (prevFull.getBare().length() == 1) {
                bare = prevFull.getBare();
            } else {
                int l_1 = prevFull.getBare().length() - 1;
                int i = Character.codePointAt(prevFull.getBare(), l_1);
                bare = prevFull.getBare().substring(0, l_1) + new String(new int [] {i + this.prevCollapsed + 1}, 0, 1);
            }
            processId = prevFull.getProcessId();
        } else {
            bare = token.getBare();
            processId = token.getProcessId();
        }

        final String res_bare;
        switch (bare.length()) {
            case 1:
                res_bare = this.owner.decodeCodeBook.get(token.getQuant().toString() + bare).substring(1); // TODO sep codebooks
            break;

            case 2: {
                int int1 = LongSpec.uni2int(bare);
                res_bare = SToken.int2base(int1, 5);
            }
            break;

            case 3: {
                int int1 = LongSpec.uni2int(bare.substring(0, 2));
                int int2 = LongSpec.uni2int('0' + bare.substring(2, 3));
                res_bare = SToken.int2base(int1, 5) + SToken.int2base(int2, 2);
            }
            break;

            case 4: {
                int int1 = LongSpec.uni2int(bare.substring(0, 2));
                int int2 = LongSpec.uni2int(bare.substring(2, 4));
                res_bare = SToken.int2base(int1, 5) + SToken.int2base(int2, 5);
            }
            break;

            default:
                res_bare = bare;
        }

        final String res_process;
        if (SToken.NO_AUTHOR.equals(processId)) {
            res_process = processId;
        } else {
            res_process = this.owner.decodeCodeBook.get("+" + processId);
        }

        return new VersionToken(res_bare, res_process);
    }

    @Override
    public boolean hasNext() {
        return this.match != null || this.chunkIndex < this.owner.chunks.size();
    }

    @Override
    public String next() {

        if (!this.hasNext()) {
            return null;
        }

        final int tokenEndPos = match == null ? 0 : match.end();
        LongSpec.Chunk specChunk = this.owner.chunks.get(this.chunkIndex);
        String chunk = specChunk.chunk;

        if (chunk.length() == tokenEndPos) {
            this.chunkIndex++;
            this.tokenIndexInChunk = 0;

            if (this.match != null) {
                EncodedToken tok = TokMatcher.buildToken(this.match);
                if (tok.getBare().length() > 1) {
                    this.prevFull = tok;
                    this.prevCollapsed = 0;
                } else {
                    this.prevCollapsed++;
                }
            } else { // empty
                this.prevFull = null;
                this.prevCollapsed = 0;
            }
            this.match = null;
            this.tokenIndexGlobal++;
            if (!this.hasNext()) {
                return chunk;
            }
        }

        if (match != null) {
            EncodedToken tok = TokMatcher.buildToken(match);
            if (tok.getBare().length() > 1) {
                this.prevFull = tok;
                this.prevCollapsed = 0;
            } else {
                this.prevCollapsed ++;
            }
        }

        MatchResult mr = TokMatcher.nextToken(chunk, tokenEndPos);
        if (mr != null) {
            this.match = mr;
            this.tokenIndexGlobal++;
            this.tokenIndexInChunk++;
            return mr.group(TokMatcher.G_TOK);
        } else {
            return null;
        }
    }

    @Override
    public void remove() {
        erase(1);
    }

    public int getIndex() {
        return tokenIndexGlobal;
    }

    public int skip(int count) {
        // TODO may implement fast-skip of seq-compressed spans
        List<LongSpec.Chunk> chunks = this.owner.chunks;
        count = count > 0 ? count : 1;
        int left = count;
        LongSpec.Chunk specChunk = chunks.get(this.chunkIndex);
        int leftInChunk = specChunk.tokensCount - this.tokenIndexInChunk;
        if (leftInChunk <= count) { // skip chunks
            left -= leftInChunk; // skip the current chunk
            int c = this.chunkIndex + 1; // how many extra chunks to skip
            while (c < chunks.size() && left > chunks.get(c).tokensCount) {
                left -= chunks.get(++c).tokensCount;
            }
            this.skip2chunk(c);
        }
        if (this.chunkIndex < chunks.size()) {
            while (left > 0) {
                this.next();
                left--;
            }
        }
        return count - left;
    }

    /**
     * Irrespectively of the current state of the iterator moves it to the
     * first token in the chunkIndex specified; chunkIndex===undefined moves it to
     * the hasNext() position (one after the last token).
     */
    public void skip2chunk(int chunk) {
        List<LongSpec.Chunk> chunks = this.owner.chunks;
        if (chunk < 0) {
            chunk = chunks.size();
        }
        this.tokenIndexGlobal = 0;
        for(int c = 0; c < chunk; c++) { // TODO perf pick the current value
            this.tokenIndexGlobal += chunks.get(c).tokensCount;
        }
        this.tokenIndexInChunk = 0;
        this.chunkIndex = chunk;
        if (chunk < chunks.size()) {
            MatchResult mr = TokMatcher.nextToken(chunks.get(this.chunkIndex).chunk, 0);
            if (mr != null) {
                this.match = mr;
            } else {
                this.match = null;
            }
        } else {
            this.match = null;
        }
        if (chunk > 0) {
            // (1) chunks must not be empty;
            // (2) a chunkIndex starts with a full token
            String prev = chunks.get(chunk - 1).chunk;
            int j = 0;
            while (SQuant.byCode(prev.substring(prev.length() - 1 - j, prev.length() - j)) != null) {
                j++;
            }
            this.prevCollapsed = j;
            int k = 0;
            while (SQuant.byCode(prev.substring(prev.length() - 1 - j - k)) != null) {
                k++;
            }
            MatchResult mr = TokMatcher.nextToken(prev, prev.length() - 1 - j - k);
            if (mr != null) {
                this.prevFull = TokMatcher.buildToken(mr);
            } else {
                this.prevFull = null;
            }
        } else {
            this.prevFull = null;
            this.prevCollapsed = 0;
        }
    }

    /**
     * As sequential coding is encapsulated in LongSpec.Iterator,
     * inserts are done by Iterator as well.
     */
    public void insert(SToken de) {
        // insertBefore
        String insStr = this.encode(de);

        boolean brokenSeq = match != null &&
                match.group(TokMatcher.G_TOK) != null &&
                match.group(TokMatcher.G_TOK).length() == 1;

        List<LongSpec.Chunk> chunks = this.owner.chunks;
        if (this.chunkIndex == chunks.size()) { // hasNext(), append
            if (chunks.size() > 0) {
                int ind = this.chunkIndex - 1;
                LongSpec.Chunk specChunk = chunks.get(ind);
                specChunk.chunk += insStr;
                specChunk.tokensCount++;
            } else {
                chunks.add(new LongSpec.Chunk(insStr, 1));
                this.chunkIndex++;
            }
        } else {
            LongSpec.Chunk specChunk = chunks.get(this.chunkIndex);
            String chunkStr = specChunk.chunk;
            String preEq = chunkStr.substring(0, this.match.start());
            String postEq = chunkStr.substring(this.match.start());
            final int lastIndex;
            if (brokenSeq) {
                SToken me = this.decode();
                this.prevFull = null;
                String en = this.encode(me);
                specChunk.chunk = preEq + insStr + en + postEq.substring(1);
                lastIndex = preEq.length() + insStr.length();
            } else {
                specChunk.chunk = preEq + insStr + /**/ postEq;
                lastIndex = match.start() + insStr.length();
            }
            this.match = TokMatcher.nextToken(specChunk.chunk, lastIndex);
            specChunk.tokensCount++;
            this.tokenIndexInChunk++;
        }
        this.tokenIndexGlobal++;
        if (insStr.length() > 1) {
            MatchResult mr = TokMatcher.nextToken(insStr, 0);
            if (mr != null) {
                this.prevFull = TokMatcher.buildToken(mr);
            }
            this.prevCollapsed = 0;
        } else {
            this.prevCollapsed++;
        }

        // may split chunks
        // may join chunks
    }

    public void addBlock(String de) { // insertBefore
        for (SToken tok : Spec.parse(de)) {
            this.insert(tok);
        }
    }

    public void erase(int count) {
        if (!this.hasNext()) {
            return;
        }
        count = count > 0 ? count : 1;
        List<LongSpec.Chunk> chunks = this.owner.chunks;
        // remember offsets
        int fromChunk = this.chunkIndex;
        int fromOffset = this.match.start();
        int fromChunkIndex = this.tokenIndexInChunk; // TODO clone USE 2 iterators or i+c

        count = this.skip(count); // checked for runaway skip()
        // the iterator now is at the first-after-erased pos

        int tillChunk = this.chunkIndex;
        int tillOffset = this.match == null ? 0 : this.match.start(); // hasNext()

        boolean collapsed = match != null &&
                match.group(TokMatcher.G_TOK) != null &&
                match.group(TokMatcher.G_TOK).length() == 1;

        // splice strings, adjust indexes
        if (fromChunk == tillChunk) {
            LongSpec.Chunk specChunk = chunks.get(this.chunkIndex);
            String chunk = specChunk.chunk;
            String pre = chunk.substring(0,fromOffset);
            String post = chunk.substring(tillOffset);
            if (collapsed) { // sequence is broken now; needs expansion
                post = this.decode() + post.substring(1);
            }
            specChunk.chunk = pre + post;
            specChunk.tokensCount -= count;
            this.tokenIndexInChunk -= count;
        } else {  // FIXME refac, more tests (+wear)
            if (fromOffset == 0) {
                fromChunk--;
            } else {
                LongSpec.Chunk specChunk = chunks.get(fromChunk);
                specChunk.chunk = specChunk.chunk.substring(0, fromOffset);
                specChunk.tokensCount = fromChunkIndex;
            }
            int midChunks = tillChunk - fromChunk - 1;
            if (midChunks > 0) { // wipe'em out
                //for(var c=fromChunk+1; c<tillChunk; c++) ;
                for (int i = 0; i < midChunks; i++) {
                    chunks.remove(fromChunk + 1);
                }
            }
            if (tillChunk < chunks.size() && tillOffset > 0) {
                LongSpec.Chunk specChunk = chunks.get(tillChunk);
                specChunk.chunk = specChunk.chunk.substring(this.match.start());
                specChunk.tokensCount -= this.tokenIndexInChunk;
                this.tokenIndexInChunk = 0;
            }
        }
        this.tokenIndexGlobal -= count;

    }

    @Override
    public LongSpecIterator clone() {
        LongSpecIterator copy = new LongSpecIterator(this);
        copy.chunkIndex = this.chunkIndex;
        copy.match = this.match;
        copy.tokenIndexGlobal = this.tokenIndexGlobal;
        return copy;
    }


    private static class TokMatcher {
        // encoded alphabet
        public static final String RS_TOK_ENCODED = "[0-\\u802f]+";

        // also matches collapsed quant-only tokens
        public static final String RS_Q_TOK_EXT_ENCODED = "([Q])((=)(?:\\+(=))?)?"
                .replaceAll("Q", Matcher.quoteReplacement(SQuant.allCodes))
                .replaceAll("=", Matcher.quoteReplacement(RS_TOK_ENCODED));

        public static final Pattern RE_Q_TOK_EXT_ENCODED = Pattern.compile(RS_Q_TOK_EXT_ENCODED, Pattern.DOTALL);

        // named groups of RE_Q_TOK_EXT_ENCODED regexp
        public static final int G_TOK = 0;
        public static final int G_QUANT = 1;
        public static final int G_BARE = 3;
        public static final int G_PROC = 4;

        public static MatchResult nextToken(final String input, final int startAt) {
            Matcher m = RE_Q_TOK_EXT_ENCODED.matcher(input);
            if (m.find(startAt)) {
                return m.toMatchResult();
            } else {
                return null;
            }
        }

        public static EncodedToken buildToken(MatchResult mr) {
            if (mr.groupCount() > G_QUANT) {
                return new EncodedToken(SQuant.byCode(mr.group(G_QUANT)), mr.group(G_BARE), mr.group(G_PROC));
            } else {
                return new EncodedToken(SQuant.byCode(mr.group(G_QUANT)));
            }
        }
    }
}
