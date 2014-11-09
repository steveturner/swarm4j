package citrea.swarm4j.core.callback;

import citrea.swarm4j.core.spec.FullSpec;
import com.eclipsesource.json.JsonValue;


/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 23.08.2014
 *         Time: 22:59
 */
public class FieldChangeOpRecipient extends FilteringOpRecipient<OpRecipient> {

    private final String fieldName;

    public FieldChangeOpRecipient(OpRecipient inner, String fieldName) {
        super(inner);
        this.fieldName = fieldName;
    }

    @Override
    public boolean filter(FullSpec spec, JsonValue value, OpRecipient source) {
        return value.isObject() &&
                value.asObject().names().contains(this.fieldName);
    }

    @Override
    public String toString() {
        return "FieldChangeOpRecipient{" +
                "fieldName='" + fieldName + "\', " +
                "inner=" + inner +
                '}';
    }
}
