package citrea.swarm4j.core.storage;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.spec.FullSpec;
import citrea.swarm4j.core.spec.TypeIdSpec;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 14.11.2014
 *         Time: 18:16
 */
public interface Storage {

    void open() throws SwarmException;

    void close() throws SwarmException;

    JsonObject readState(TypeIdSpec ti) throws SwarmException;

    JsonObject readOps(TypeIdSpec ti) throws SwarmException;

    void writeState(TypeIdSpec ti, JsonValue state) throws SwarmException;

    void writeOp(FullSpec spec, JsonValue value) throws SwarmException;

    void cleanUpCache(TypeIdSpec ti);
}
