package citrea.swarm4j.model.spec;

import com.eclipsesource.json.JsonValue;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 21.06.2014
 *         Time: 20:48
 */
public class VersionVector {

    private final Map<String, String> map = new HashMap<String, String>();

    public VersionVector(VersionVectorSpec vec) {
        this.add(vec);
    }

    public VersionVector(String vector) {
        this.add(new VersionVectorSpec(vector));
    }

    public VersionVector() {

    }

    public void add(VersionToken token) {
        String time = token.getBare();
        String source = token.getProcessId();
        String knownTime = getKnownTime(source);
        if (time.compareTo(knownTime) > 0) {
            this.map.put(source, time);
        }
    }

    public void add(VersionVectorSpec versionVector) {
        Iterator<VersionToken> it = versionVector.getTokenIterator();
        while (it.hasNext()) {
            add(it.next());
        }
    }

    public void add(String versionVector) {
        add(new VersionVectorSpec(versionVector));
    }

    private String getKnownTime(String source) {
        String res = this.map.get(source);
        if (res == null) { res = ""; }
        return res;
    }

    public boolean covers(VersionToken version) {
        String time = version.getBare();
        String source = version.getProcessId();
        String knownTime = getKnownTime(source);
        return time.compareTo(knownTime) <= 0;
    }

    public String maxTs() {
        String ts = null;
        for (Map.Entry<String, String> entry: this.map.entrySet()) {
            if (ts == null || ts.compareTo(entry.getValue()) < 0) {
                ts = entry.getValue();
            }
        }
        return ts;
    }

    public String toString(int top, String rot) {
        rot = "!" + rot;
        List<String> ret = new ArrayList<String>();
        for (Map.Entry<String, String> entry: this.map.entrySet()) {
            String ext = entry.getKey();
            String time = entry.getValue();
            ret.add("!" + time + (SToken.NO_AUTHOR.equals(ext) ? "" : "+" + ext));
        }
        Collections.sort(ret, STRING_REVERSE_ORDER);
        while (ret.size() > top || (ret.size() > 0 && ret.get(ret.size() - 1).compareTo(rot) <= 0)) {
            ret.remove(ret.size() - 1);
        }

        StringBuilder res = new StringBuilder();
        if (ret.size() > 0) {
            for (String item : ret) {
                res.append(item);
            }
        } else {
            res.append(SToken.ZERO_VERSION.toString());
        }
        return res.toString();
    }

    @Override
    public String toString() {
         return this.toString(10, "0");
    }

    public JsonValue toJson() {
        return JsonValue.valueOf(toString());
    }

    public String get(String ext) {
        String bare = this.map.get(ext);
        if (bare == null) {
            bare = "";
        }
        return bare;
    }

    private static final Comparator<String> STRING_REVERSE_ORDER = new Comparator<String>() {

        @Override
        public int compare(String s1, String s2) {
            return s2.compareTo(s1);
        }
    };
}
