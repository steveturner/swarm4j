package citrea.swarm4j.core.model;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.spec.IdToken;
import citrea.swarm4j.core.storage.FileStorage;
import citrea.swarm4j.core.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 14.11.2014
 *         Time: 16:16
 */
public class OnOffWithFileStorageTest extends OnOffBaseTest {

    private static final Logger logger = LoggerFactory.getLogger(OnOffWithFileStorageTest.class);

    @Override
    protected Storage createServerStorage(IdToken id) throws SwarmException {
        FileStorage storage = new FileStorage(id, "." + name.getMethodName());
        storage.setMaxLogSize(3L);
        storage.setRoot(true);
        storage.setAsync(true);
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
        // do nothing
    }
}
