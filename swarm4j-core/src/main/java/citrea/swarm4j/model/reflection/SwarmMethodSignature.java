package citrea.swarm4j.model.reflection;


import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.FullSpec;
import com.eclipsesource.json.JsonValue;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 22.08.2014
 *         Time: 16:20
 */
public enum SwarmMethodSignature {

    NONE(),
    SPEC(FullSpec.class),
    SPEC_VALUE(FullSpec.class, JsonValue.class),
    SPEC_VALUE_SOURCE(FullSpec.class, JsonValue.class, OpRecipient.class),
    SPEC_SOURCE(FullSpec.class, OpRecipient.class),
    VALUE(JsonValue.class),
    VALUE_SOURCE(JsonValue.class, OpRecipient.class),
    SOURCE(OpRecipient.class);

    private final Class<?>[] paramTypes;

    private SwarmMethodSignature(Class<?> ... paramTypes) {
        this.paramTypes = paramTypes;
    }

    public Class<?>[] getParamTypes() {
        return paramTypes;
    }

    public static SwarmMethodSignature detect(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        SwarmMethodSignature res = null;
        for (SwarmMethodSignature sig : SwarmMethodSignature.values()) {
            if (Arrays.deepEquals(paramTypes, sig.getParamTypes())) {
                res = sig;
                break;
            }
        }
        return res;
    }
}
