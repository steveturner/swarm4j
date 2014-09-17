package citrea.swarm4j.model.value;

import com.eclipsesource.json.JsonValue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 16.09.2014
 *         Time: 00:00
 */
public class JSONUtils {

    public static boolean isFalsy(JsonValue value) {
        return value == null ||
                value.isNull() ||
                (value.isString() && value.asString().length() == 0) ||
                (value.isBoolean() && !value.asBoolean()) ||
                (value.isObject() && value.asObject().size() == 0) ||
                (value.isArray() && value.asArray().size() == 0) ||
                (value.isNumber() && value.asInt() == 0);
    }
}
