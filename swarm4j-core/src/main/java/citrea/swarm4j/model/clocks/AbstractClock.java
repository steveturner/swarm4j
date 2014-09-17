package citrea.swarm4j.model.clocks;

import citrea.swarm4j.model.spec.SpecQuant;
import citrea.swarm4j.model.spec.SpecToken;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 13.09.2014
 *         Time: 14:57
 */
public abstract class AbstractClock implements Clock {

    public static final String NO_INITIAL_TIME = "";

    // 2014-01-01 00:00:00.000
    static final long EPOCH = 61346664000000L;
    public static final Date EPOCH_DATE = new Date(EPOCH);

    final int timePartLen;
    final String id;

    SpecToken lastIssuedTimestamp;
    int lastSeqSeen;
    int lastTimeSeen;

    protected AbstractClock(String processId, int timePartLen) {
        this.id = processId;
        this.lastSeqSeen = -1;
        this.timePartLen = timePartLen;
    }

    @Override
    public SpecToken getLastIssuedTimestamp() {
        return lastIssuedTimestamp;
    }

    @Override
    public SpecToken issueTimestamp() {
        String baseTime = issueTimePart();
        String seqAsStr = generateNextSequencePart();
        this.lastIssuedTimestamp = new SpecToken(SpecQuant.VERSION, baseTime + seqAsStr, this.id);
        return this.lastIssuedTimestamp;
    }

    @Override
    public TimestampParsed parseTimestamp(SpecToken ts) {
        final String time_seq = ts.getBare();
        final int time, seq;
        if (timePartLen == 0) {
            time = 0;
            seq = SpecToken.base2int(time_seq);
        } else {
            String timePart = time_seq.substring(0, timePartLen);
            String seqPart = time_seq.substring(timePartLen);
            time = SpecToken.base2int(timePart);
            seq = parseSequencePart(seqPart);
        }
        return new TimestampParsed(time, seq);
    }

    protected abstract String issueTimePart();

    protected abstract String generateNextSequencePart();

    protected abstract int parseSequencePart(String seq);

    /**
     * Freshly issued Lamport logical timestamps must be greater than
     * any timestamps previously seen.
     */
    @Override
    public void seeTimestamp(SpecToken ts) {
        if (ts.compareTo(this.lastIssuedTimestamp) < 0) {
            return;
        }
        TimestampParsed parsed = this.parseTimestamp(ts);
        this.lastTimeSeen = parsed.time;
        this.lastSeqSeen = parsed.seq;
    }

}
