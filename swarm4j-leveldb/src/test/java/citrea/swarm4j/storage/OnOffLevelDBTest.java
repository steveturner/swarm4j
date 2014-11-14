package citrea.swarm4j.storage;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.model.OnOffBaseTest;
import citrea.swarm4j.core.storage.Storage;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;

public class OnOffLevelDBTest extends OnOffBaseTest {

    @Override
    protected Storage createServerStorage() throws SwarmException {
        return new LevelDBStorage(Iq80DBFactory.factory, "." + name.getMethodName());
    }

    @Override
    protected void cleanupServerStorage() {
        deleteDirectory(new File("." + name.getMethodName()));
    }

    @Override
    protected Storage createClientStorage() {
        return null;
    }

    @Override
    protected void cleanupClientStorage() {

    }
}