package citrea.swarm4j.model.oplog;

import citrea.swarm4j.model.spec.Spec;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;


import java.util.*;
import java.util.Set;

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
     * of the log in every replica we do everythin to obtain the minimal
     * necessary subset of it.
     * As a side effect, distillLog allows up to handle some partial
     * order issues (see _ops.set).
     * @see citrea.swarm4j.model.Model#set(Spec, JsonValue)
     * @return {*} distilled log {spec:true}
     */
    @Override
    public Map<String, JsonValue> distillLog(Map<Spec, JsonValue> oplog) {
        // explain
        Map<String, JsonValue> cumul = new HashMap<>();
        Map<String, Boolean> heads = new HashMap<>();
        List<Spec> sets = new ArrayList<>(oplog.keySet());
        Collections.sort(sets);
        Collections.reverse(sets);
        for (Spec spec : sets) {
            JsonValue jsonVal = oplog.get(spec);
            if (!(jsonVal instanceof JsonObject)) continue;

            JsonObject jo = (JsonObject) jsonVal;
            boolean notempty = false;
            Set<String> fieldsToRemove = new HashSet<>();
            for (String field : jo.names()) {
                if (cumul.containsKey(field)) {
                    fieldsToRemove.add(field);
                } else {
                    JsonValue fieldVal = jo.get(field);
                    cumul.put(field, fieldVal);
                    notempty = !fieldVal.isNull(); //store last value of the field
                }
            }
            for (String field : fieldsToRemove) {
                jo.remove(field);
            }
            String source = spec.getVersion().getExt();
            if (!notempty) {
                if (heads.containsKey(source)) {
                    oplog.remove(spec);
                }
            }
            heads.put(source, true);
        }
        return cumul;
    }
}
