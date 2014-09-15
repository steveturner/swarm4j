package citrea.swarm4j.storage;

import citrea.swarm4j.model.Host;
import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;

import com.eclipsesource.json.JsonArray;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An improvised filesystem-based storage implementation.
 * Objects are saved into separate files in a hashed directory
 * tree. Ongoing operations are streamed into a log file.
 * One can go surprisingly far with this kind of an approach.
 * https://news.ycombinator.com/item?id=7872239
 *
 * v load:   preload existing log chunks
 *   on:     load state, add tail, send, reon base=????
 *   patch:  state=> save state; log=> append
 *   op:     append
 *   unload: may flush all states
 * v onflush:new flush
 *
 * Difference: with/without direct access to the state.
 * Will not request state from the client side anyway.
 *
 * @author aleksisha
 *         Date: 25.08.2014
 *         Time: 00:45
 */
public class FileStorage extends Storage {

    public static final Logger logger = LoggerFactory.getLogger(FileStorage.class);

    public static final int MAX_LOG_SIZE = 1 << 15;

    private Map<Spec, Map<Spec, JsonValue>> tails = new HashMap<>();
    private String dir;
    private int logCount;
    private Queue<String> dirtyQueue = new ArrayDeque<>();
    private FileChannel logFile = null;
    private int logSize = 0;
    private long pulling;


    public FileStorage(SpecToken id, String dir) throws SwarmException {
        super(id);
        this.host = null; //will be set during Host creation

        this.dir = dir;
        File storageRoot = new File(dir);
        if (!storageRoot.exists()) {
            if (!storageRoot.mkdir()) {
                throw new SwarmException("Can't create directory: " + storageRoot.getName());
            }
        }
        File storageLogs = new File(dir + "/_log");
        if (!storageLogs.exists()) {
            if (!storageLogs.mkdir()) {
                throw new SwarmException("Can't create logs directory: " + storageLogs.getName());
            }
        }
        this.id = new SpecToken("#file"); //path.basename(dir);

        this.logCount = 0;
        this.loadTails();
        this.rotateLog();
    }

    @Override
    protected void appendToLog(Spec ti, JsonObject verop2val) throws SwarmException {
        Map<Spec, JsonValue> tail = this.tails.get(ti);
        if (tail == null) {
            tail = new HashMap<>();
            this.tails.put(ti, tail);
        }
        // stash ops in RAM (can't seek in the log file so need that)
        for (String verop : verop2val.names()) {
            tail.put(new Spec(verop), verop2val.get(verop));
        }
        // queue the object for state flush
        this.dirtyQueue.add(ti.toString());
        // serialize the op as JSON
        JsonObject o = new JsonObject();
        o.set(ti.toString(), verop2val);  // TODO annoying
        String buf = o.toString() + ",\n";
        ByteBuffer bbuf = ByteBuffer.wrap(buf.getBytes());
        // append JSON to the log file
        try {
            while (bbuf.hasRemaining()) {
                this.logFile.write(bbuf);
            }
        } catch (IOException e) {
            throw new SwarmException("Error writing operation to log: " + ti.toString(), e);
        }
        this.logSize += bbuf.capacity();
        if (this.logSize > MAX_LOG_SIZE) {
            this.rotateLog();
        }
        // We flush objects to files one at a time to keep HDD seek rates
        // at reasonable levels; if something fails we don't get stuck for
        // more than 1 second.
        if (this.pulling == 0 || this.pulling < new Date().getTime() - 1000) {
            this.pullState(ti);
        }
    }

    public void pullState(Spec ti) throws SwarmException {
        String spec;
        while ((spec = this.dirtyQueue.poll()) != null) {
            if (spec.matches("\\d+")) {
                // TODO ??? String cleared = this.logFileName(Integer.valueOf(spec));
                // FIXME we should not delete the file before the state will be flushed to the disk
            } else if (this.tails.containsKey(new Spec(spec))) {
                break; // flush it
            }
        }
        if (spec == null) {
            // all states flushed
            return;
        }
        this.pulling = new Date().getTime();
        // Request the host to send us the full state patch.
        // Only a live object can integrate log tail into the state,
        // so we use this trick. As object lifecycles differ in Host
        // and FileStorage we can't safely access the object directly.
        final Spec onSpec = ti.addToken(this.time()).addToken(Syncable.ON);
        this.host.deliver(onSpec, JsonValue.valueOf(".init!0"), this);
    }

    @Override
    public void patch(Spec spec, JsonValue patchVal) throws SwarmException {
        if (!(patchVal instanceof JsonObject)) return;
        JsonObject patch = (JsonObject) patchVal;
        Spec ti = spec.getTypeId();
        if (patch.get("_version").isNull()) { // no full state, just the tail
            JsonValue tail = patch.get("_tail");
            if (tail.isObject()) {
                this.appendToLog(ti, tail.asObject());
            }
            return;
        }
        // in the [>on <patch1 <reon >patch2] handshake pattern, we
        // are currently at the patch2 stage, so the state in this
        // patch also includes the tail which was sent in patch1
        this.tails.remove(ti);

        String stateFileName = this.stateFileName(ti);
        int pos = stateFileName.lastIndexOf("/");
        String dir = stateFileName.substring(0, pos);
        File folder = new File(dir);
        // I believe FAT is cached (no disk seek) so existsSync()
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                throw new SwarmException("Error creating state-folder: " + dir);
            }
        }
        // finally, send JSON to the file
        try {
            try (RandomAccessFile stateFile = new RandomAccessFile(stateFileName, "w")) {
                String json = patch.toString();
                ByteBuffer buf = ByteBuffer.wrap(json.getBytes(Charset.forName("UTF-8")));

                FileChannel channel = stateFile.getChannel();
                channel.position(0);
                while (buf.hasRemaining()) {
                    channel.write(buf);
                }
                channel.force(false);
                channel.truncate(buf.capacity());
            }

            this.pulling = 0;
            this.pullState(ti); // may request next object

        } catch (IOException e) {
            throw new SwarmException(e.getMessage(), e);
        }
    }

    public String logFileName(int count) {
        return this.dir + "/_log/log" + SpecToken.int2base(count, 8);
    }

    public int parseLogFileName(String fileName) {
        Pattern p = Pattern.compile("/.*?(\\w{8})$/");
        Matcher m = p.matcher(fileName);
        if (!m.matches()) {
            throw new IllegalArgumentException("Wrong log file name: " + fileName);
        }
        return SpecToken.base2int(m.group(1));
    }

    public String stateFileName(Spec spec) {
        return this.dir + "/" +
                spec.getType().getBody() + "/" +
                spec.getId().getBody(); // TODO hashing (issue: may break FAT caching?)
    }

    // Once the current log file exceeds some size, we start a new one.
    // Once all ops are saved in object-state files, a log file is rm'ed.
    public void rotateLog() throws SwarmException {
        try {
            if (this.logFile != null) {
                this.logFile.force(false);
                this.logFile.close();
                this.dirtyQueue.add(String.valueOf(this.logCount));
            }
            RandomAccessFile fw = new RandomAccessFile(this.logFileName(++this.logCount), "a");
            this.logFile = fw.getChannel();
            this.logSize = 0;
        } catch (IOException e) {
            throw new SwarmException("Error rotating log: " + e.getMessage(), e);
        }
    }

    @Override
    public void on(Spec spec, JsonValue base, OpRecipient replica) throws SwarmException {
        Spec ti = spec.getTypeId();
        String stateFileName = this.stateFileName(ti);

        // read in the state
        BufferedReader stateFileReader;
        try {
            stateFileReader = new BufferedReader(new FileReader(stateFileName));
        } catch (FileNotFoundException e) {
            stateFileReader = null;
        }

        // load state
        JsonObject state = new JsonObject();
        if (stateFileReader == null) {
            state.set("_version", JsonValue.valueOf(SpecToken.ZERO_VERSION.toString()));
        } else {
            try {
                StringBuilder stateData = new StringBuilder();
                String line;
                while ((line = stateFileReader.readLine()) != null) {
                    stateData.append(line);
                }
                state = JsonObject.readFrom(stateData.toString());
                Map<Spec, JsonValue> tail = this.tails.get(ti);
                if (tail != null) {
                    JsonValue state_tail_val = state.get("_tail");
                    JsonObject state_tail;
                    if (state_tail_val.isObject()) {
                        state_tail = state_tail_val.asObject();
                    } else {
                        state_tail = new JsonObject();
                    }
                    for (Map.Entry<Spec, JsonValue> op : tail.entrySet()) {
                        state_tail.set(op.getKey().toString(), op.getValue());
                    }
                    state.set("_tail", state_tail);
                }

            } catch (IOException e) {
                throw new SwarmException("IO error: " + e.getMessage(), e);
            } finally {
                try {
                    stateFileReader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        Spec tiv = ti.addToken(spec.getVersion());
        replica.deliver(tiv.addToken(Syncable.PATCH), state, this);
        String versionVector = Storage.stateVersionVector(state);
        replica.deliver(tiv.addToken(Syncable.REON), JsonValue.valueOf(versionVector), this);
    }

    @Override
    public void off(Spec spec, OpRecipient src) throws SwarmException {
        // if (this.tails[ti]) TODO half-close
        src.deliver(spec.overrideToken(Syncable.REON), JsonValue.NULL, this);
    }

    /**
     * Load all existing log files on startup.
     * Object-state files will be read on demand but we can't seek inside
     * log files so load 'em as this.tails
     */
    public void loadTails() throws SwarmException {
        String path = this.dir + "/_log";
        File logsFolder = new File(path);
        if (!logsFolder.isDirectory()) {
            throw new IllegalStateException("Path is not a directory: " + logsFolder.getAbsolutePath());
        }
        File[] logFiles = logsFolder.listFiles();
        if (logFiles == null) {
            return;
        }

        ByteBuffer buf = ByteBuffer.allocate(4096);
        try {
            for (File logFile : logFiles) {
                int count = this.parseLogFileName(logFile.getName());
                this.logCount = Math.max(count, this.logCount);

                StringBuilder json = new StringBuilder();
                json.append("[");

                CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
                try (RandomAccessFile file = new RandomAccessFile(logFile, "r")) {
                    FileChannel channel = file.getChannel();
                    buf.clear();
                    while (channel.read(buf) > 0) {
                        buf.flip();
                        json.append(decoder.reset().decode(buf));
                        buf.clear();
                    }
                }

                json.append("{}]");

                JsonArray arr = JsonArray.readFrom(json.toString());
                for (int i = 0, l = arr.size(); i < l; i++) {
                    JsonValue block = arr.get(i);
                    if (block.isObject()) {
                        JsonObject blockObj = block.asObject();
                        for (String tidoid : blockObj.names()) {
                            Map<Spec, JsonValue> tail = this.tails.get(new Spec(tidoid));
                            JsonValue ops = blockObj.get(tidoid);
                            if (tail == null) {
                                tail = new HashMap<>();
                                this.tails.put(new Spec(tidoid), tail);
                                this.dirtyQueue.add(tidoid);
                            }
                            if (!ops.isObject()) continue;

                            JsonObject opsObj = ops.asObject();
                            for (String vidop : opsObj.names()) {
                                tail.put(new Spec(vidop), opsObj.get(vidop));
                            }
                        }
                    }
                    this.dirtyQueue.add(String.valueOf(this.logCount));
                }
            }
        } catch (IOException e) {
            throw new SwarmException("Error loading log: " + e.getMessage(), e);
        }
    }

    public Spec getTypeId() {
        return new Spec(Host.HOST, id);
    }

}
