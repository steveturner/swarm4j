package citrea.swarm4j.model.meta;

import citrea.swarm4j.model.Host;
import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.spec.IdToken;
import citrea.swarm4j.model.spec.OpToken;
import citrea.swarm4j.model.spec.TypeToken;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 24.08.2014
 *         Time: 17:43
 */
public interface TypeMeta {
    Class<? extends Syncable> getType();
    TypeToken getTypeToken();
    Syncable newInstance(IdToken id, Host host) throws SwarmException;

    FieldMeta getFieldMeta(String fieldName);
    Collection<FieldMeta> getAllFields();

    OperationMeta getOperationMeta(OpToken op);
    OperationMeta getOperationMeta(String opName);

    String getDescription();
}
