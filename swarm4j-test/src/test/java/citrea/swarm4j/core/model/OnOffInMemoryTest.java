package citrea.swarm4j.core.model;

import citrea.swarm4j.core.spec.IdToken;
import citrea.swarm4j.core.storage.InMemoryStorage;
import citrea.swarm4j.core.storage.Storage;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 14.11.2014
 *         Time: 16:12
 */
public class OnOffInMemoryTest extends OnOffBaseTest {

    protected Storage createServerStorage(IdToken id) {
        Storage storage = new InMemoryStorage(id);
        storage.setAsync(true);
        return storage;
    }

    protected void cleanupServerStorage() {

    }

    protected void cleanupClientStorage() {

    }

    protected Storage createClientStorage(IdToken idToken) {
        //cacheStorage = new XInMemoryStorage(new SpecToken("#cache"));
        //cacheStorageThread = new Thread(cacheStorage);
        //cacheStorageThread.start();
        return null;
    }
}
