package citrea.swarm4j.core.callback;

import citrea.swarm4j.core.spec.FullSpec;
import citrea.swarm4j.core.spec.SToken;
import com.eclipsesource.json.JsonValue;


/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 18.08.2014
 *         Time: 16:58
 */
public class OpFilter extends FilteringOpRecipient<OpRecipient> {

    private final SToken op;

    public OpFilter(OpRecipient inner, SToken op) {
        super(inner);
        this.op = op;
    }

    @Override
    public boolean filter(FullSpec spec, JsonValue value, OpRecipient source) {
        return op.equals(spec.getOp());
    }

    public SToken getOp() {
        return op;
    }

    @Override
    public String toString() {
        return "OpFilter{" +
                "op=" + op +
                ", inner=" + inner +
                '}';
    }
}
