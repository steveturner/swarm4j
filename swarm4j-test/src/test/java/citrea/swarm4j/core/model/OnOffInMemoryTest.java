package citrea.swarm4j.core.model;

import citrea.swarm4j.core.spec.IdToken;
import citrea.swarm4j.core.storage.InMemoryStorage;
import citrea.swarm4j.core.storage.Storage;
import citrea.swarm4j.core.storage.StorageAdaptor;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 14.11.2014
 *         Time: 16:12
 */
public class OnOffInMemoryTest extends OnOffBaseTest {

    @Override
    protected Storage createServerStorage() {
        return new InMemoryStorage();
    }

    @Override
    protected Storage createClientStorage() {
        return null;
    }

    protected void cleanupServerStorage() {

    }

    protected void cleanupClientStorage() {

    }
}
