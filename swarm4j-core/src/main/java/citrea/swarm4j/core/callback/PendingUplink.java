package citrea.swarm4j.core.callback;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.model.Syncable;
import citrea.swarm4j.core.spec.FullSpec;
import citrea.swarm4j.core.spec.SToken;
import citrea.swarm4j.core.spec.TypeIdSpec;
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
    private final SToken requestedVersion;

    public PendingUplink(Syncable object, Uplink original, SToken requestedVersion) {
        super(original);
        this.object = object;
        this.requestedVersion = requestedVersion;
    }

    @Override
    protected boolean filter(FullSpec spec, JsonValue value, OpRecipient source) {
        // only response for my request
        return requestedVersion.equals(spec.getVersion());
    }

    @Override
    protected void deliverInternal(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
        object.removeListener(this);
        object.addUplink(this.getInner());
    }

    @Override
    public TypeIdSpec getTypeId() {
        return this.inner.getTypeId();
    }
}
