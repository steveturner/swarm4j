package citrea.swarm4j.model.meta;


import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.annotation.SwarmOperationKind;
import citrea.swarm4j.model.reflection.SwarmMethodInvocationException;
import citrea.swarm4j.model.spec.Spec;
import com.eclipsesource.json.JsonValue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 24.08.2014
 *         Time: 17:43
 */
public interface OperationMeta {

    SwarmOperationKind getKind();
    void invoke(Syncable object, Spec spec, JsonValue value, OpRecipient source) throws SwarmMethodInvocationException;
}
