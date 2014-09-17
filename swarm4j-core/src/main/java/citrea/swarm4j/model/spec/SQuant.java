package citrea.swarm4j.model.spec;

import java.util.HashMap;
import java.util.Map;

/**
 * Possible quants in specifier
 *
 * @see Spec
 *
 * Created with IntelliJ IDEA.
 * @author aleksisha
 *         Date: 26/10/13
 *         Time: 21:51
 */
public enum SQuant {
    TYPE('/'),
    ID('#'),
    VERSION('!'),
    OP('.');

    private static final Map<Character, SQuant> code2item;
    private static final SQuant[] allInOrder;
    public static final String allCodes;

    static {
        code2item = new HashMap<Character, SQuant>(4);
        code2item.put(TYPE.code, TYPE);
        code2item.put(OP.code, OP);
        code2item.put(ID.code, ID);
        code2item.put(VERSION.code, VERSION);

        allInOrder = SQuant.values();

        String codes = "";
        for (SQuant q : SQuant.values()) {
            codes += q.code;
        }
        allCodes = codes;

        for (int i = 1; i < allInOrder.length; i++) {
            allInOrder[i - 1].next = allInOrder[i];
            allInOrder[i].prev = allInOrder[i - 1];
        }
    }

    public final char code;

    private SQuant prev = null;
    private SQuant next = null;

    private SQuant(char code) {
        this.code = code;
    }

    public SQuant prev() {
        return prev;
    }

    public SQuant next() {
        return next;
    }

    @Override
    public String toString() {
        return String.valueOf(code);
    }

    public static SQuant byCode(Character q) {
        return code2item.get(q);
    }

    public static SQuant byCode(String q) {
        return code2item.get(q.charAt(0));
    }

    public static SQuant byOrder(int order) {
        return allInOrder[order];
    }
}
