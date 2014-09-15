package citrea.swarm4j.model;

import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.Spec;
import com.eclipsesource.json.JsonValue;


import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 03/11/13
 *         Time: 10:29
 */
public class RememberingRecipient implements OpRecipient {

    private List<Triplet> memory = new ArrayList<>();

    @Override
    public void deliver(Spec spec, JsonValue value, OpRecipient listener) throws SwarmException {
        Triplet triplet = new Triplet(spec, value, listener);
        memory.add(triplet);
    }

    public List<Triplet> getMemory() {
        return memory;
    }

    public static class Triplet {
        public final Spec spec;
        public final JsonValue value;
        public final OpRecipient source;

        public Triplet(Spec spec, JsonValue value, OpRecipient source) {
            this.spec = spec;
            this.value = value;
            this.source = source;
        }
    }

    @Override
    public String toString() {
        return "RememberingRecipient{" + memory.size() + " operations remembered}";
    }
}
