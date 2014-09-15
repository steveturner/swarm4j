package citrea.swarm4j.model;

import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;

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

    public XInMemoryStorage(SpecToken id) {
        super(id);
    }

    @Override
    public JsonObject readState(Spec ti) {
        return super.readState(ti);
    }

    @Override
    public Map<Spec, JsonValue> readOps(Spec ti) {
        return super.readOps(ti);
    }
}
