package citrea.swarm4j.core.model.oplog;

import citrea.swarm4j.core.spec.Spec;
import citrea.swarm4j.core.spec.VersionOpSpec;
import citrea.swarm4j.core.model.value.JSONUtils;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;


import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 05.09.2014
 *         Time: 01:04
 */
public class ModelLogDistillator implements LogDistillator {

    /**
     * Removes redundant information from the log; as we carry a copy
     * of the log in every replica we do everything to obtain the minimal
     * necessary subset of it.
     * As a side effect, distillLog allows up to handle some partial
     * order issues (see _ops.set).
     *
     * @param oplog op-log
     * @return distilled log {spec:val}
     *
     * @see citrea.swarm4j.core.model.Model
     * @see citrea.swarm4j.core.model.SyncSet
     */
    @Override
    public Map<String, JsonValue> distillLog(Map<VersionOpSpec, JsonValue> oplog) {
        // explain
        final Map<String, JsonValue> cumul = new HashMap<String, JsonValue>();
        final Set<String> heads = new HashSet<String>();
        final Set<String> fieldsToRemove = new HashSet<String>(10);
        List<VersionOpSpec> sets = new ArrayList<VersionOpSpec>(oplog.keySet());
        Collections.sort(sets, Spec.ORDER_REVERSE);
        for (VersionOpSpec spec : sets) {
            JsonValue jsonVal = oplog.get(spec);
            if (jsonVal == null || !jsonVal.isObject()) continue;

            JsonObject jo = (JsonObject) jsonVal;
            boolean notEmpty = false;
            fieldsToRemove.clear();
            for (JsonObject.Member pair : jo) {
                final String field = pair.getName();
                if (cumul.containsKey(field)) {
                    fieldsToRemove.add(field);
                } else {
                    JsonValue fieldVal = pair.getValue();
                    cumul.put(field, fieldVal);
                    notEmpty = !JSONUtils.isFalsy(fieldVal); //store last value of the field
                }
            }
            if (!fieldsToRemove.isEmpty()) {
                // clone
                jo = JsonObject.readFrom(jo.toString());
                for (String field : fieldsToRemove) {
                    jo.remove(field);
                }
                oplog.put(spec, jo);
            }
            String source = spec.getVersion().getProcessId();
            if (!notEmpty) {
                if (heads.contains(source)) {
                    oplog.remove(spec);
                }
            }
            heads.add(source);
        }
        return cumul;
    }
}
