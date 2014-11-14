package citrea.swarm4j.core.storage;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.spec.*;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;

/**
 * An improvised filesystem-based storage implementation.
 * Objects are saved into separate files in a hashed directory
 * tree. Ongoing operations are streamed into a log file.
 * One can go surprisingly far with this kind of an approach.
 * https://news.ycombinator.com/item?id=7872239
 *
 * @author aleksisha
 *         Date: 25.08.2014
 *         Time: 00:45
 *
 * TODO async io
 */
public class FileStorage extends Storage {

    public static final Logger logger = LoggerFactory.getLogger(FileStorage.class);

    public static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    private final String dir;

    private FileChannel logFile = null;

    private Map<TypeIdSpec, Map<VersionOpSpec, JsonValue>> tails = new HashMap<TypeIdSpec, Map<VersionOpSpec, JsonValue>>();

    public FileStorage(IdToken id, String dir) throws SwarmException {
        super(id);

        this.dir = dir;
        File storageRoot = new File(dir);
        if (!storageRoot.exists()) {
            if (!storageRoot.mkdir()) {
                throw new SwarmException("Can't create directory: " + storageRoot.getName());
            }
        }
    }

    @Override
    public void start() throws SwarmException {
        loadLog();
        rotateLog(false);

        super.start();
    }

    @Override
    protected void writeState(TypeIdSpec ti, JsonValue state) throws SwarmException {
        String stateFileName = buildStateFileName(ti);
        int pos = stateFileName.lastIndexOf("/");
        String dir = stateFileName.substring(0, pos);
        File folder = new File(dir);
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                throw new SwarmException("Error creating state-folder: " + dir);
            }
        }
        // finally, send JSON to the file
        try {
            RandomAccessFile stateFile = null;
            try {
                stateFile = new RandomAccessFile(stateFileName, "rw");
                String json = state.toString();
                ByteBuffer buf = ByteBuffer.wrap(json.getBytes(CHARSET_UTF8));

                FileChannel channel = stateFile.getChannel();
                channel.position(0);
                while (buf.hasRemaining()) {
                    channel.write(buf);
                }
                channel.force(false);
                channel.truncate(buf.capacity());
            } finally {
                if (stateFile != null) {
                    stateFile.close();
                }
            }
        } catch (IOException e) {
            throw new SwarmException(e.getMessage(), e);
        }
        tails.remove(ti);
    }

    @Override
    protected JsonObject readState(TypeIdSpec ti) throws SwarmException {
        String stateFileName = buildStateFileName(ti);

        // read in the state
        BufferedReader stateFileReader;
        try {
            stateFileReader = new BufferedReader(new FileReader(stateFileName));
        } catch (FileNotFoundException e) {
            return null;
        }

        // load state
        JsonObject state = new JsonObject();
        try {
            StringBuilder stateData = new StringBuilder();
            String line;
            while ((line = stateFileReader.readLine()) != null) {
                stateData.append(line);
            }
            state = JsonObject.readFrom(stateData.toString());
        } catch (IOException e) {
            throw new SwarmException("IO error: " + e.getMessage(), e);
        } finally {
            try {
                stateFileReader.close();
            } catch (IOException e) {
                // ignore
            }
        }
        return state;
    }

    private boolean saveInTail(FullSpec spec, JsonValue value) {
        TypeIdSpec ti = spec.getTypeId();
        VersionOpSpec vo = spec.getVersionOp();
        Map<VersionOpSpec, JsonValue> tail = tails.get(ti);
        if (tail == null) {
            tail = new HashMap<VersionOpSpec, JsonValue>();
            tails.put(ti, tail);
        }

        if (!tail.containsKey(vo)) {
            tail.put(vo, value);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void writeOp(FullSpec spec, JsonValue value) throws SwarmException {
        if (!saveInTail(spec, value)) {
            logger.warn("{}.writeOp({}, {}) op replay", this, spec, value);
            return ;
        }

        String json = ",\n\"" + spec.toString() + "\":\t" + value.toString();
        ByteBuffer buf = ByteBuffer.wrap(json.getBytes(CHARSET_UTF8));
        // append JSON to the log file
        try {
            while (buf.hasRemaining()) {
                logFile.write(buf);
            }
        } catch (IOException e) {
            throw new SwarmException("Error writing operation to log: " + spec.toString(), e);
        }
    }

    @Override
    protected void cleanUpCache(TypeIdSpec ti) {
        tails.remove(ti);
    }

    @Override
    protected JsonObject readOps(TypeIdSpec ti) throws SwarmException {
        Map<VersionOpSpec, JsonValue> tail = tails.get(ti);
        JsonObject res;
        if (tail == null) {
            res = null;
        } else {
            res = new JsonObject();
            for (Map.Entry<VersionOpSpec, JsonValue> entry : tail.entrySet()) {
                res.set(entry.getKey().toString(), entry.getValue());
            }
        }
        return res;
    }

    private String buildLogFileName() {
        return dir + "/_log";
    }

    private String buildStateFileName(TypeIdSpec spec) {
        return dir + "/" +
                spec.getType().getBody() + "/" +
                spec.getId().getBody(); // TODO hashing (issue: may break FAT caching?)
    }

    // Once the current log file exceeds some size, we start a new one.
    // Once all ops are saved in object-state files, a log file is rm'ed.
    void rotateLog(boolean noOpen) throws SwarmException {
        try {
            if (logFile != null) {
                logFile.write(ByteBuffer.wrap("}".getBytes(CHARSET_UTF8)));
                logFile.force(false);
                logFile.close();
                logFile = null;
            }

            if (noOpen) {
                return;
            }

            File file = new File(buildLogFileName());
            if (file.exists()) {
                File bak = new File(file.getAbsolutePath() + ".bak");
                if (bak.exists()) {
                    if (!bak.delete()) {
                        throw new SwarmException("Can't remove file: " + bak.getAbsolutePath());
                    }
                }
                if (!file.renameTo(bak)) {
                    throw new SwarmException("Can't rename log file \"" + file.getAbsolutePath() + "\" into \"" + bak.getAbsolutePath() + "\"");
                }
            }
            JsonObject json = new JsonObject();
            for (Map.Entry<TypeIdSpec, Map<VersionOpSpec, JsonValue>> entry: tails.entrySet()) {
                JsonObject tailObj = new JsonObject();
                for (Map.Entry<VersionOpSpec, JsonValue> vo_val: entry.getValue().entrySet()) {
                    tailObj.set(vo_val.getKey().toString(), vo_val.getValue());
                }
                json.set(entry.getKey().toString(), tailObj);
            }
            String open_json = "{\"\":\n" + json.toString(); // open-ended JSON

            RandomAccessFile fw = new RandomAccessFile(buildLogFileName(), "rw");
            logFile = fw.getChannel();
            logFile.write(ByteBuffer.wrap(open_json.getBytes(CHARSET_UTF8)));

        } catch (IOException e) {
            throw new SwarmException("Error rotating log: " + e.getMessage(), e);
        }
    }

    /**
     * Load log on startup.
     * Object-state files will be read on demand but we can't seek inside
     * log file so load it as this.tails
     */
    void loadLog() throws SwarmException {
        final String logFilename = buildLogFileName();
        File logFile = new File(logFilename);
        if (!logFile.exists()) {
            return;
        }

        ByteBuffer buf = ByteBuffer.allocate(4096);
        try {
            StringBuilder json = new StringBuilder();
            CharsetDecoder decoder = CHARSET_UTF8.newDecoder();
            RandomAccessFile file = null;
            try {
                file = new RandomAccessFile(logFile, "r");
                FileChannel channel = file.getChannel();
                buf.clear();
                while (channel.read(buf) > 0) {
                    buf.flip();
                    json.append(decoder.reset().decode(buf));
                    buf.clear();
                }
            } finally {
                if (file != null) {
                    file.close();
                }
            }

            JsonObject tails_read;
            try {
                tails_read = JsonObject.readFrom(json.toString());
            } catch (Exception ex) {
                json.append("}");
                tails_read = JsonObject.readFrom(json.toString());
            }

            tails.clear();

            JsonValue all = tails_read.get("");
            if (all != null && all.isObject()) {
                for (JsonObject.Member typeId_tail : all.asObject()) {
                    TypeIdSpec ti = new TypeIdSpec(typeId_tail.getName());
                    JsonValue value = typeId_tail.getValue();
                    Map<VersionOpSpec, JsonValue> vo2val = new HashMap<VersionOpSpec, JsonValue>();
                    if (value != null && value.isObject()) {
                        for (JsonObject.Member versionOp_val : value.asObject()) {
                            VersionOpSpec vo = new VersionOpSpec(versionOp_val.getName());
                            vo2val.put(vo, versionOp_val.getValue());
                        }
                        tails.put(ti, vo2val);
                    } else {
                        logger.warn("{}.fillTails() object={} tail is not an object: {}", this, ti, value);
                    }
                }
            }
            tails_read.remove("");

            for (JsonObject.Member spec_val : tails_read) {
                final FullSpec spec = new FullSpec(spec_val.getName());
                saveInTail(spec, spec_val.getValue());
            }
        } catch (IOException e) {
            throw new SwarmException("Error loading log: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() throws SwarmException {
        if (logFile != null) {
            rotateLog(true);
        }
        tails.clear();
        super.close();
    }
}
