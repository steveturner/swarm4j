package citrea.swarm4j.storage;

import citrea.swarm4j.model.spec.VersionOpSpec;
import com.eclipsesource.json.JsonObject;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 07.11.2014
 *         Time: 17:16
 */
public class SingleObjectData {

    JsonObject state;
    Map<VersionOpSpec, String> tail;
}
