package citrea.swarm4j.model.callback;

import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;
import com.eclipsesource.json.JsonValue;


/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 22.06.2014
 *         Time: 00:33
 */
public class PendingUplink extends FilteringOpRecipient<Uplink> implements Uplink {

    private final Syncable object;
    private final SpecToken requestedVersion;

    public PendingUplink(Syncable object, Uplink original, SpecToken requestedVersion) {
        super(original);
        this.object = object;
        this.requestedVersion = requestedVersion;
    }

    @Override
    protected boolean filter(Spec spec, JsonValue value, OpRecipient source) {
        // only response for my request
        return requestedVersion.equals(spec.getVersion());
    }

    @Override
    protected void deliverInternal(Spec spec, JsonValue value, OpRecipient source) throws SwarmException {
        object.removeListener(this);
        object.addUplink(this.getInner());
    }

    @Override
    public Spec getTypeId() {
        return this.inner.getTypeId();
    }
}