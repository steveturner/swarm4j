package citrea.swarm4j.core.model;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.pipe.LoopbackOpChannelFactory;
import citrea.swarm4j.core.spec.*;

import citrea.swarm4j.core.storage.Storage;
import citrea.swarm4j.core.storage.StorageAdaptor;
import org.junit.*;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 28.08.2014
 *         Time: 22:48
 */
public abstract class BaseClientServerTest {

    private static Logger logger = LoggerFactory.getLogger(BaseClientServerTest.class);

    @Rule
    public TestName name = new TestName();

    private static final IdToken SERVER = new IdToken("#" + Host.SERVER_HOST_ID_PREFIX + "up");
    private static final IdToken CLIENT = new IdToken("#client");
    private static final IdToken DUMMY_STORAGE_ID = new IdToken("#dummy");
    private static final int RECONNECT_TIMEOUT = 10;

    //TODO cache-storage private Thread cacheStorageThread;

    //TODO cache-storage private XInMemoryStorage cacheStorage;

    protected Host server;
    protected Host client;

    protected abstract Storage createServerStorage() throws SwarmException;

    protected abstract Storage createClientStorage();

    protected abstract void setupServerStorageAdaptor(StorageAdaptor adaptor);

    protected abstract void setupClientStorageAdaptor(StorageAdaptor adaptor);

    protected abstract void cleanupServerStorage();

    protected abstract void cleanupClientStorage();

    protected abstract Set<Class<? extends Syncable>> getClassesToRegister();

    protected abstract void setupServerHost(Host host) throws SwarmException;

    protected void setupClientHost(Host host) throws SwarmException {
        host.registerChannelFactory(LoopbackOpChannelFactory.SCHEME, new LoopbackOpChannelFactory(server));
    }

    @Before
    public void setUp() throws Exception {
        Storage dummyStorage = createServerStorage();

        StorageAdaptor serverStorageAdaptor = new StorageAdaptor(DUMMY_STORAGE_ID, dummyStorage);
        setupServerStorageAdaptor(serverStorageAdaptor);

        server = new Host(SERVER, serverStorageAdaptor);
        setupServerHost(server);
        server.start();
        server.waitForStart();

        Storage cacheStorage = createClientStorage();
        StorageAdaptor cacheStorageAdaptor;
        if (cacheStorage != null) {
            cacheStorageAdaptor = new StorageAdaptor(new IdToken("#cache"), cacheStorage);
            setupClientStorageAdaptor(cacheStorageAdaptor);
        } else {
            cacheStorageAdaptor = null;
        }
        client = new Host(CLIENT, cacheStorageAdaptor);
        setupClientHost(client);
        client.start();
        client.waitForStart();

        client.connect(new URI(LoopbackOpChannelFactory.SCHEME + "://server"), RECONNECT_TIMEOUT, 0);
    }

    @After
    public void tearDown() throws Exception {
        client.close();
        client.stop();
        client = null;

        server.close();
        server.stop();
        server = null;
        cleanupClientStorage();
        cleanupServerStorage();
    }


    protected static void deleteDirectory(File dir) {
        // TODO use FileUtils.deleteDirectory(dir); from apache commons-io
        File[] dir_files = dir.listFiles();
        if (dir_files != null) {
            for (File file : dir_files) {
                if (file.isDirectory()) {
                    File[] subdir_files = file.listFiles();
                    if (subdir_files != null) {
                        for (File in_file : subdir_files) {
                            if (!in_file.delete()) {
                                logger.warn("File wasn't deleted: {}", in_file.getName());
                            }
                        }
                    }
                }
                if (!file.delete()) {
                    logger.warn("File or directory wasn't deleted: {}", file.getName());
                }
            }
        }
        if (!dir.delete()) {
            logger.warn("Directory wasn't deleted: {}", dir.getName());
        }
    }

}
