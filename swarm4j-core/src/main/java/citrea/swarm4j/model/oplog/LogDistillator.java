package citrea.swarm4j.model.oplog;

import citrea.swarm4j.model.spec.VersionOpSpec;
import com.eclipsesource.json.JsonValue;


import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 05.09.2014
 *         Time: 01:03
 */
public interface LogDistillator {
    Map<String, JsonValue> distillLog(Map<VersionOpSpec, JsonValue> oplog);
}
