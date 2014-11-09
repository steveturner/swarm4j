package citrea.swarm4j.core.model.oplog;

import citrea.swarm4j.core.spec.VersionOpSpec;
import com.eclipsesource.json.JsonValue;


import java.util.Collections;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 05.09.2014
 *         Time: 01:08
 */
public class NoLogDistillator implements LogDistillator {
    @Override
    public Map<String, JsonValue> distillLog(Map<VersionOpSpec, JsonValue> oplog) {
        return Collections.emptyMap();
    }
}
