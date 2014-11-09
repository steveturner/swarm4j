package citrea.swarm4j.core.clocks;

import citrea.swarm4j.core.spec.VersionToken;

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
    protected long clockOffsetMs;

    VersionToken lastIssuedTimestamp;
    int lastSeqSeen;
    int lastTimeSeen;

    protected AbstractClock(String processId, int timePartLen) {
        this.id = processId;
        this.lastSeqSeen = -1;
        this.timePartLen = timePartLen;
        this.clockOffsetMs = 0L;
    }

    @Override
    public VersionToken getLastIssuedTimestamp() {
        return lastIssuedTimestamp;
    }

    @Override
    public VersionToken issueTimestamp() {
        String baseTime = issueTimePart();
        String seqAsStr = generateNextSequencePart();
        this.lastIssuedTimestamp = new VersionToken(baseTime + seqAsStr, this.id);
        return this.lastIssuedTimestamp;
    }

    @Override
    public TimestampParsed parseTimestamp(VersionToken ts) {
        final String time_seq = ts.getBare();
        final int time, seq;
        if (timePartLen == 0) {
            time = 0;
            seq = VersionToken.base2int(time_seq);
        } else {
            String timePart = time_seq.substring(0, timePartLen);
            String seqPart = time_seq.substring(timePartLen);
            time = VersionToken.base2int(timePart);
            seq = parseSequencePart(seqPart);
        }
        return new TimestampParsed(time, seq);
    }

    protected abstract String issueTimePart();

    protected abstract String generateNextSequencePart();

    protected abstract int parseSequencePart(String seq);

    @Override
    public long getTimeInMillis() {
        long millis = System.currentTimeMillis();
        millis -= EPOCH;
        millis += this.clockOffsetMs;
        return millis;
    }
}
