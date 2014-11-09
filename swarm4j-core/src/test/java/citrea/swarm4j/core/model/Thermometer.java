package citrea.swarm4j.core.model;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.model.annotation.SwarmField;
import citrea.swarm4j.core.model.annotation.SwarmType;
import citrea.swarm4j.core.spec.IdToken;
import com.eclipsesource.json.JsonObject;


/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 28.08.2014
 *         Time: 22:49
 */
@SwarmType()
public class Thermometer extends Model {

    @SwarmField()
    public int t;


    public Thermometer(IdToken id, Host host) throws SwarmException {
        super(id, host);
    }

    public Thermometer(JsonObject initialState, Host host) throws SwarmException {
        super(initialState, host);
    }
}
