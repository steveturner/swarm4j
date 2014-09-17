package citrea.swarm4j.model.callback;

import citrea.swarm4j.model.spec.Spec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 20.08.2014
 *         Time: 12:18
 */
public interface Uplink extends OpRecipient {

    Spec getTypeId();
}
