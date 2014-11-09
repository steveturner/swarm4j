package citrea.swarm4j.core.model;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.callback.OpRecipient;
import citrea.swarm4j.core.spec.FullSpec;
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

    private final List<Triplet> memory = new ArrayList<Triplet>();

    @Override
    public void deliver(FullSpec spec, JsonValue value, OpRecipient listener) throws SwarmException {
        Triplet triplet = new Triplet(spec, value, listener);
        memory.add(triplet);
    }

    public List<Triplet> getMemory() {
        return memory;
    }

    public static class Triplet {
        public final FullSpec spec;
        public final JsonValue value;
        public final OpRecipient source;

        public Triplet(FullSpec spec, JsonValue value, OpRecipient source) {
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
