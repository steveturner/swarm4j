package citrea.swarm4j.core.model.value;

import com.eclipsesource.json.JsonValue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 24.08.2014
 *         Time: 20:51
 */
public interface TypeConverter<T> {

    JsonValue toJsonValue(T value);
    T fromJsonValue(JsonValue value);
}
