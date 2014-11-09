package citrea.swarm4j.core.model;

import citrea.swarm4j.core.callback.OpRecipient;
import citrea.swarm4j.core.spec.IdToken;
import citrea.swarm4j.core.spec.TypeIdSpec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 16:27
 */
public interface SomeSyncable extends ReferringToPeer, OpRecipient {

    TypeIdSpec getTypeId();

    IdToken getId();
}
