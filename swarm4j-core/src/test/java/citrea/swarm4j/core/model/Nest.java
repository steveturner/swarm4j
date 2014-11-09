package citrea.swarm4j.core.model;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.spec.IdToken;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 05.09.2014
 *         Time: 01:56
 */
class Nest extends SyncSet<Duck> {

    public Nest(IdToken id, Host host) throws SwarmException {
        super(id, host);
    }

    public Nest(Host host) throws SwarmException {
        this(null, host);
    }
}
