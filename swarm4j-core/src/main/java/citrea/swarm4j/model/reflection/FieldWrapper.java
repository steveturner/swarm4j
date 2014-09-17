package citrea.swarm4j.model.reflection;


import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.meta.FieldMeta;
import com.eclipsesource.json.JsonValue;

import java.lang.reflect.Field;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 24.08.2014
 *         Time: 10:20
 */
public class FieldWrapper implements FieldMeta {

    private final Field field;

    public FieldWrapper(Field field) {
        this.field = field;
    }

    @Override
    public void set(Syncable object, JsonValue value) throws SwarmException {
        //TODO JsonValue <-> raw-value converter
        Class<?> type = field.getType();
        try {
            if (type.isAssignableFrom(JsonValue.class)) {
                field.set(object, value);
            } else if (String.class.isAssignableFrom(type)) {
                field.set(object, value.asString());
            } else if (Number.class.isAssignableFrom(type)) {
                if (type.isAssignableFrom(Integer.class)) {
                    field.set(object, value.asInt());
                } else if (type.isAssignableFrom(Long.class)) {
                    field.set(object, value.asLong());
                } else if (type.isAssignableFrom(Float.class)) {
                    field.set(object, value.asFloat());
                } else if (type.isAssignableFrom(Double.class)) {
                    field.set(object, value.asDouble());
                } else {
                    throw new IllegalArgumentException("Unsupported number type: " + type.getSimpleName());
                }
            } else if (Boolean.class.isAssignableFrom(type)) {
                field.set(object, value.asBoolean());
            } else if (type.isPrimitive()) {
                if (type.isAssignableFrom(int.class)) {
                    field.set(object, value.asInt());
                } else if (type.isAssignableFrom(long.class)) {
                    field.set(object, value.asLong());
                } else if (type.isAssignableFrom(boolean.class)) {
                    field.set(object, value.asBoolean());
                } else {
                    throw new IllegalArgumentException("Unsupported primitive type: " + type.getSimpleName());
                }
            } else if (type.isEnum()) {
                // TODO what is right way to find Enum item?
                for (Object item : type.getEnumConstants()) {
                    if (item.equals(value.asString())) {
                        field.set(object, item);
                        break;
                    }
                }
            } else {
                // TODO add Date fields support
                throw new SwarmException("Unsupported field type: " + type.getSimpleName());
            }
        } catch (IllegalAccessException e) {
            throw new SwarmException(e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return this.field.getName();
    }

    @Override
    public JsonValue get(Syncable object) throws SwarmException {
        try {
            Object rawValue = field.get(object);
            if (rawValue == null) {
                return JsonValue.NULL;
            }
            if (rawValue instanceof String) {
                return JsonValue.valueOf((String) rawValue);
            }
            if (rawValue instanceof Integer) {
                return JsonValue.valueOf((Integer) rawValue);
            }
            if (rawValue instanceof Long) {
                return JsonValue.valueOf((Long) rawValue);
            }
            if (rawValue instanceof Boolean) {
                return JsonValue.valueOf((Boolean) rawValue);
            }
            if (rawValue instanceof Double) {
                return JsonValue.valueOf((Double) rawValue);
            }
            if (rawValue instanceof Float) {
                return JsonValue.valueOf((Float) rawValue);
            }
            return JsonValue.valueOf(rawValue.toString());

        } catch (IllegalAccessException e) {
            throw new SwarmException(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return "FieldWrapper{" +
                "field=" + field.getName() +
                '}';
    }
}
