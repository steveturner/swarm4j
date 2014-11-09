package citrea.swarm4j.core.meta;


import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.model.Syncable;
import com.eclipsesource.json.JsonValue;

/**
 * Model field wrapper interface.
 * Used to get/set field value as JsonValue instance.
 *
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 24.08.2014
 *         Time: 19:46
 */
public interface FieldMeta {
    String getName();
    JsonValue get(Syncable object) throws SwarmException;
    void set(Syncable object, JsonValue value) throws SwarmException;
}
