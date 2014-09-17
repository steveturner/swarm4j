package citrea.swarm4j.model.callback;

import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.spec.FullSpec;
import com.eclipsesource.json.JsonValue;


/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 22.06.2014
 *         Time: 00:59
 */
public class OnceOpRecipient extends FilteringOpRecipient<OpRecipient> {

    private final Syncable obj;

    public OnceOpRecipient(Syncable obj, OpRecipient inner) {
        super(inner);
        this.obj = obj;
    }

    @Override
    protected boolean filter(FullSpec spec, JsonValue value, OpRecipient source) {
        return true;
    }

    @Override
    protected void deliverInternal(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
        super.deliverInternal(spec, value, source);
        obj.off(this);
    }

    @Override
    public String toString() {
        return "OnceOpRecipient{" +
                "objTypeId=" + obj.getTypeId() +
                '}';
    }
}
