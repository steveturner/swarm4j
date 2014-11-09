package citrea.swarm4j.core.callback;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.model.Syncable;
import citrea.swarm4j.core.spec.FullSpec;
import com.eclipsesource.json.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
* Created with IntelliJ IDEA.
*
* @author aleksisha
*         Date: 07.10.2014
*         Time: 20:36
*/
public class ProxyListener implements OpRecipient {

    private Syncable owner;
    private List<OpRecipient> callbacks = new ArrayList<OpRecipient>();

    public ProxyListener(Syncable owner) {
        this.owner = owner;
    }

    @Override
    public void deliver(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
        for (OpRecipient cb : callbacks) {
            cb.deliver(spec, value, source);
        }
    }

    public void on(OpRecipient cb) {
        this.callbacks.add(cb);
    }

    public void off(OpRecipient cb) {
        this.callbacks.remove(cb);
    }

    @Override
    public String toString() {
        return "" + owner.getTypeId() + ".Proxy";
    }
}
