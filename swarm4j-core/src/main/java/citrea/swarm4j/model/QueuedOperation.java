package citrea.swarm4j.model;

import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.Spec;

import com.eclipsesource.json.JsonValue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 16:37
 */
public class QueuedOperation {

    private OpRecipient peer;
    private Spec spec;
    private JsonValue value;

    public QueuedOperation(Spec spec, JsonValue value, OpRecipient peer) {
        this.spec = spec;
        this.value = value;
        this.peer = peer;
    }

    public Spec getSpec() {
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
