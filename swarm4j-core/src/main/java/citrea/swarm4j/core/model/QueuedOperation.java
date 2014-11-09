package citrea.swarm4j.core.model;

import citrea.swarm4j.core.callback.OpRecipient;
import citrea.swarm4j.core.spec.FullSpec;

import com.eclipsesource.json.JsonValue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 16:37
 */
public class QueuedOperation {

    private final OpRecipient peer;
    private final FullSpec spec;
    private final JsonValue value;

    public QueuedOperation(FullSpec spec, JsonValue value, OpRecipient peer) {
        this.spec = spec;
        this.value = value;
        this.peer = peer;
    }

    public FullSpec getSpec() {
        return spec;
    }

    public JsonValue getValue() {
        return value;
    }

    public OpRecipient getPeer() {
        return peer;
    }

    @Override
    public String toString() {
        return spec.toString() + "->" + value.toString();
    }
}
