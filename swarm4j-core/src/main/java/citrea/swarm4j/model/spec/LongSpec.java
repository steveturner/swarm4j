package citrea.swarm4j.model.spec;

import java.util.*;

/**
 * LongSpec is a Long Specifier, i.e. a string of quant+id tokens that may be
 * indeed very long (many megabytes). Ids are compressed using
 * dynamic dictionaries (codebooks) or "unicode numbers" (base-32768
 * encoding utilizing Unicode symbols as quasi-binary). Unicode
 * numbers are particularly handy for encoding timestamps. LongSpecs
 * may be assigned shared codebooks.
 *
 * @author aleksisha
 *         Date: 06.10.2014
 *         Time: 22:39
 */
public class LongSpec implements SomeSpec {

    public static final Set<SQuant> QUANTS_TO_CODE = new HashSet<SQuant>(Arrays.asList(SQuant.TYPE, SQuant.OP));
    public static final String RS_UNI2_TOK = "^[0-\u802f]{2}$";

    List<Chunk> chunks = new LinkedList<Chunk>();

    Map<String, String> encodeCodeBook = new HashMap<String, String>();
    Map<String, String> decodeCodeBook = new HashMap<String, String>();
    Map<String, Integer> lastCodes = new HashMap<String, Integer>();

    /**
     * Creates new empty LongSpec
     */
    public LongSpec() {
        this("", null, null);
    }

    /**
     * Creates new LongSpec
     *
     * A codebook is an object containing encode or decode tables and some stats.
     * It is OK to pass null as a codebooks; it gets initialized automatically.
     *
     * @param spec specifier as a string
     * @param encodeCodeBook code-book for encoding tokens, e.g. {"/Type": "/T"}
     * @param decodeCodeBook code-book for decoding tokens, e.g. {"/T": "/Type"}
     */
    public LongSpec(String spec, Map<String, String> encodeCodeBook, Map<String, String> decodeCodeBook) {
        if (encodeCodeBook != null) {
            this.encodeCodeBook.putAll(encodeCodeBook);
        }
        if (decodeCodeBook != null) {
            this.decodeCodeBook.putAll(decodeCodeBook);
        } else {
            // revert en to make de
            for (Map.Entry<String, String> e : this.encodeCodeBook.entrySet()) {
                this.decodeCodeBook.put(e.getValue(), e.getKey());
            }
        }
        lastCodes.put(SQuant.TYPE.toString(), 0x30);
        lastCodes.put(SQuant.ID.toString(), 0x30);
        lastCodes.put(SQuant.VERSION.toString(), 0x30);
        lastCodes.put(SQuant.OP.toString(), 0x30);
        lastCodes.put("+", 0x30);

        // For a larger document, a single LongSpec may be some megabytes long.
        // As we don't want to rewrite those megabytes on every keypress, we
        // divide data into chunks.
        if (spec != null) {
            this.add(spec);
        }
    }

    /**
     * Well, for many-MB LongSpecs this may take some time.
     */
    @Override
    public String asString() {
        StringBuilder ret = new StringBuilder();
        LongSpecIterator it = this.iterator(0);
        while (it.hasNext()) {
            ret.append(it.decode());
            it.next();
        }
        return ret.toString();
    }

    @Override
    public String toString() {
        return this.asString();
    }

    @Override
    public int getTokensCount() { // TODO .getTokensCount ?
        int len = 0;
        for (Chunk chunk : this.chunks) {
            len += chunk.tokensCount;
        }
        return len;
    }

    @Override
    public boolean isEmpty() {
        return this.chunks.isEmpty();
    }

    @Override
    public List<SToken> listTokens() {
        List<SToken> res = new ArrayList<SToken>(getTokensCount());
        LongSpecIterator it = this.iterator(0);
        while (it.hasNext()) {
            res.add(it.decode());
            it.next();
        }
        return res;
    }

    // T O K E N  C O M P R E S S I O N

    public String allocateCode(String tok) {
        String quant = String.valueOf(tok.charAt(0));
        //if (Spec.quants.indexOf(quant)===-1) {throw new Error('invalid token');}
        String en = null;
        Map<String, Integer> lc = lastCodes;
        if (lc.get(quant) < Character.codePointAt("z", 0)) { // pick a nice letter
            for(int i = 1; en == null && i < tok.length(); i++) {
                char x = tok.charAt(i);
                String e = quant + x;
                if (decodeCodeBook.get(e) == null) {
                    en = e;
                }
            }
        }
        while (en == null && lc.get(quant) < 0x802f) {
            int cp = lc.get(quant);
            cp ++;
            String y = new String(new int [] {cp}, 0, 1);
            lc.put(quant, cp);
            String mayUse = quant + y;
            if (!encodeCodeBook.containsKey(mayUse)) {
                en = mayUse;
            }
        }
        if (en == null) {
            if (tok.length() <= 3) {
                throw new IllegalArgumentException("out of codes");
            }
            en = tok;
        }
        encodeCodeBook.put(tok, en);
        decodeCodeBook.put(en, tok);
        return en;
    }

    // F O R M A T  C O N V E R S I O N

    /**
     * Always 2-char base2^15 coding for an int (0...2^30-1)
     */
    public static String int2uni(int i) {
        if (i < 0 /* always true: || i > 0x7fffffff */) {
            throw new Error("int is out of range");
        }
        int [] codePoints = new int[] {
                0x30 + (i >> 15),
                0x30 + (i & 0x7fff)
        };
        return new String(codePoints, 0, 2);
    }

    public static int uni2int(String uni) {
        // TODO optimize regexp execution
        if (uni == null || !uni.matches(RS_UNI2_TOK)) {
            throw new IllegalArgumentException("invalid unicode number");
        }
        return ((Character.codePointAt(uni, 0) - 0x30) << 15) |
                (Character.codePointAt(uni, 1) - 0x30);
    }

    // L O N G S P E C  A P I

    public LongSpecIterator iterator(int pos) {
        return new LongSpecIterator(this, pos);
    }

    public LongSpecIterator end() {
        LongSpecIterator e = new LongSpecIterator(this, 0);
        e.skip2chunk(this.chunks.size());
        return e;
    }

    /** Insert a token at a given position. */
    public void add(String tok, int afterPos) {
        this.iterator(afterPos).addBlock(tok);
    }

    public void add(String tok, LongSpecIterator afterPos) {
        this.iterator(afterPos.getIndex()).addBlock(tok);
    }

    public void add(String spec) {
        LongSpecIterator pos = this.end();
        pos.addBlock(spec);
    }

    public VersionToken tokenAt(int pos) {
        return this.iterator(pos).decode();
    }

    public int indexOf(VersionToken tok, int startAt) {
        LongSpecIterator iter = this.find(tok, startAt);
        return !iter.hasNext() ? -1 : iter.getIndex();
    }

    /**
     * The method finds the first occurence of a token, returns an
     * iterator.  While the internal format of an iterator is kind of
     * opaque, and generally is not recommended to rely on, that is
     * actually a regex match array. Note that it contains encoded tokens.
     * The second parameter is the position to start scanning from, passed
     * either as an iterator or an offset.
     * @param tok token to find
     * @param startIndex search starting at specified index
     */
    public LongSpecIterator find(VersionToken tok, int startIndex) {
        LongSpecIterator it = this.iterator(startIndex);
        while (it.hasNext()) {
            if (tok.equals(it.decode())) {
                return it;
            }
            it.next();
        }
        return it;
    }

    /**
     * Single chunk of encoded tokens
     */
    public static class Chunk {
        /**
         * string with encoded tokens
         */
        String chunk;
        /**
         * tokens count in this chunk
         */
        int tokensCount;

        public Chunk(String chunk, int length) {
            this.chunk = chunk;
            this.tokensCount = length;
        }
    }
}
