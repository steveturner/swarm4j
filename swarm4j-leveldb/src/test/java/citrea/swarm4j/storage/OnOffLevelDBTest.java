package citrea.swarm4j.storage;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.model.OnOffBaseTest;
import citrea.swarm4j.core.spec.IdToken;
import citrea.swarm4j.core.storage.Storage;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;

public class OnOffLevelDBTest extends OnOffBaseTest {

    @Override
    protected Storage createServerStorage(IdToken id) throws SwarmException {
        LevelDBStorage storage = new LevelDBStorage(id, Iq80DBFactory.factory, "." + name.getMethodName());
        storage.setAsync(true);
        storage.setRoot(true);
        storage.setMaxLogSize(3L);
        return storage;
    }

    @Override
    protected void cleanupServerStorage() {
        File dir = new File("." + name.getMethodName());
        deleteDirectory(dir);
    }

    @Override
    protected Storage createClientStorage(IdToken idToken) {
        return null;
    }

    @Override
    protected void cleanupClientStorage() {

    }
}