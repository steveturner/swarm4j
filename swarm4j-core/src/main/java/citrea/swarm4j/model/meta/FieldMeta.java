package citrea.swarm4j.model.meta;


import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;
import com.eclipsesource.json.JsonValue;

/**
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
