package citrea.swarm4j.storage;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.spec.FullSpec;
import citrea.swarm4j.core.spec.IdToken;
import citrea.swarm4j.core.spec.TypeIdSpec;
import citrea.swarm4j.core.spec.VersionOpSpec;
import citrea.swarm4j.core.storage.Storage;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import org.iq80.leveldb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 10.11.2014
 *         Time: 18:56
 */
public class LevelDBStorage extends Storage {

    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    private final Logger logger = LoggerFactory.getLogger(LevelDBStorage.class);

    private final DBFactory dbFactory;
    private final String filename;
    private DB db;

    private final Map<TypeIdSpec, Set<VersionOpSpec>> tails = new HashMap<TypeIdSpec, Set<VersionOpSpec>>();

    protected LevelDBStorage(IdToken id, DBFactory dbFactory, String filename) {
        super(id);
        this.dbFactory = dbFactory;
        this.filename = filename;
    }

    @Override
    public void start() throws SwarmException {
        // open level db
        Options opts = new Options();
        opts.createIfMissing(true);
        opts.comparator(new SpecDBComparator());
        try {
            db = dbFactory.open(new File(filename), opts);
        } catch (IOException e) {
            throw new SwarmException("Error opening leveldb: " + e.getMessage(), e);
        }
        super.start();
    }

    @Override
    public void close() throws SwarmException {
        // close level db
        try {
            db.close();
        } catch (IOException e) {
            throw new SwarmException("Error closing leveldb: " + e.getMessage(), e);
        }
        super.close();
    }

    @Override
    protected JsonObject readState(TypeIdSpec ti) throws SwarmException {
        logger.debug("{}.readState({})", this, ti);
        byte[] key = ti.toString().getBytes(CHARSET_UTF8);
        ReadOptions opts = new ReadOptions();
        opts.fillCache(false);
        try {
            byte[] state_bytes = db.get(key, opts);
            if (state_bytes != null) {
                String state = new String(state_bytes, CHARSET_UTF8);
                return JsonObject.readFrom(state);
            } else {
                return null;
            }
        } catch (DBException e) {
            throw new SwarmException("ReadState db error: " + e.getMessage(), e);
        }
    }

    @Override
    protected JsonObject readOps(TypeIdSpec ti) throws SwarmException {
        logger.debug("{}.readOps({})", this, ti);

        byte[] since = (ti.toString() + " ").getBytes(CHARSET_UTF8);
        String tillStr = (ti.toString() + "0");

        Set<VersionOpSpec> tail = tails.get(ti);
        if (tail == null) {
            tail = new HashSet<VersionOpSpec>();
            tails.put(ti, tail);
        }

        ReadOptions ro = new ReadOptions();
        ro.fillCache(false);
        DBIterator it = db.iterator(ro);
        JsonObject res = new JsonObject();
        try {
            for (it.seek(since); it.hasNext(); it.next()) {
                Map.Entry<byte[], byte[]> entry = it.peekNext();
                String specStr = new String(entry.getKey(), CHARSET_UTF8);
                String valStr = new String(entry.getValue(), CHARSET_UTF8);
                if (specStr.compareTo(tillStr) >= 0) {
                    break;
                }
                tail.add(new VersionOpSpec(specStr));
                res.set(specStr, JsonValue.valueOf(valStr));
            }
        } finally {
            try {
                it.close();
            } catch (IOException e) {
                throw new SwarmException("Error closing iterator: " + e.getMessage(), e);
            }
        }
        return res;
    }

    @Override
    protected void writeOp(FullSpec spec, JsonValue value) throws SwarmException {
        logger.debug("{}.writeOp({}, {})", this, spec, value);
        String json = value.toString();
        TypeIdSpec ti = spec.getTypeId();
        Set<VersionOpSpec> tail = tails.get(ti);
        if (tail == null) {
            tail = new HashSet<VersionOpSpec>();
            tails.put(ti, tail);
        }
        tail.add(spec.getVersionOp());

        WriteOptions opts = new WriteOptions();
        opts.sync(true);
        this.db.put(spec.toString().getBytes(CHARSET_UTF8), json.getBytes(CHARSET_UTF8), opts);
    }

    @Override
    protected void cleanUpCache(TypeIdSpec ti) {
        tails.remove(ti);
    }

    @Override
    protected void writeState(TypeIdSpec ti, JsonValue state) throws SwarmException {
        logger.debug("{}.writeState({}, {})", this, ti, state);
        if (db == null) {
            throw new SwarmException("The storage is not open");
        }

        String json = state.toString();
        Set<VersionOpSpec> tail = tails.get(ti);

        List<byte []> cleanup = new ArrayList<byte[]>();
        if (tail != null) {
            for (VersionOpSpec vo : tail) {
                cleanup.add(vo.toString().getBytes(CHARSET_UTF8));
            }
        }

        WriteOptions opts = new WriteOptions();
        opts.sync(true);
        db.put(ti.toString().getBytes(CHARSET_UTF8), json.getBytes(), opts);

        WriteBatch batch =  db.createWriteBatch();
        for (byte [] vo : cleanup) {
            batch.delete(vo);
        }
        db.write(batch);
        tails.remove(ti);
    }


    public static class SpecDBComparator implements DBComparator {
        @Override
        public String name() {
            return "spec";
        }

        @Override
        public byte[] findShortestSeparator(byte[] start, byte[] limit) {
            return start;
        }

        @Override
        public byte[] findShortSuccessor(byte[] key) {
            return key;
        }

        @Override
        public int compare(byte[] key1, byte[] key2) {
            return new String(key1, CHARSET_UTF8).compareTo(new String(key2, CHARSET_UTF8));
        }
    }
}
