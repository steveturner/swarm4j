package citrea.swarm4j.core.model.oplog;

import citrea.swarm4j.core.spec.VersionOpSpec;
import com.eclipsesource.json.JsonValue;


import java.util.Map;

/**
 * Implementation defines op-log compaction strategy
 *
 * Created with IntelliJ IDEA.
 * @author aleksisha
 *         Date: 05.09.2014
 *         Time: 01:03
 */
public interface LogDistillator {
    Map<String, JsonValue> distillLog(Map<VersionOpSpec, JsonValue> oplog);
}
