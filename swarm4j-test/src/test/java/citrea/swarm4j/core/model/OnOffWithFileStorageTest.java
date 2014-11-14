package citrea.swarm4j.core.model;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.spec.IdToken;
import citrea.swarm4j.core.storage.FileStorage;
import citrea.swarm4j.core.storage.Storage;
import citrea.swarm4j.core.storage.StorageAdaptor;
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
    protected Storage createServerStorage() throws SwarmException {
        return new FileStorage("." + name.getMethodName());
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
        // do nothing
    }
}
