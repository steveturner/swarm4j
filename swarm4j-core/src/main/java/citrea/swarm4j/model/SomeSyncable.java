package citrea.swarm4j.model;

import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.IdToken;
import citrea.swarm4j.model.spec.TypeIdSpec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 16:27
 */
interface SomeSyncable extends ReferringToPeer, OpRecipient {

    TypeIdSpec getTypeId();

    IdToken getId();
}
