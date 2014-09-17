package citrea.swarm4j.model;

import citrea.swarm4j.model.spec.IdToken;

import citrea.swarm4j.model.spec.TypeIdSpec;
import citrea.swarm4j.model.spec.VersionOpSpec;
import citrea.swarm4j.storage.InMemoryStorage;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 28.08.2014
 *         Time: 00:54
 */
public class XInMemoryStorage extends InMemoryStorage {

    public XInMemoryStorage(IdToken id) {
        super(id);
    }

    @Override
    public JsonObject readState(TypeIdSpec ti) {
        return super.readState(ti);
    }

    @Override
    public Map<VersionOpSpec, JsonValue> readOps(TypeIdSpec ti) {
        return super.readOps(ti);
    }
}
