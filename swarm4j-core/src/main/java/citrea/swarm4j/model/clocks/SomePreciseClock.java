package citrea.swarm4j.model.clocks;

import citrea.swarm4j.model.spec.SToken;
import citrea.swarm4j.model.spec.VersionToken;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 13.09.2014
 *         Time: 14:55
 */
public abstract class SomePreciseClock extends AbstractClock {

    private final int preciseInMillis;

    private long clockOffsetMs;

    protected SomePreciseClock(String processId, String initialTime, int timePartLen, int preciseInMillis) {
        super(processId, timePartLen);
        this.preciseInMillis = preciseInMillis;

        // sometimes we assume our local clock has some offset

        // although we try hard to use wall clock time, we must
        // obey Lamport logical clock rules, in particular our
        // timestamps must be greater than any other timestamps
        // previously seen

        if (NO_INITIAL_TIME.equals(initialTime)) {
            this.clockOffsetMs = 0;
            initialTime = issueTimePart() + generateNextSequencePart();
        }
        this.lastIssuedTimestamp = new VersionToken(initialTime, id);
        this.clockOffsetMs = parseTimestamp(this.lastIssuedTimestamp).time - this.getTimeInMillis();
        this.seeTimestamp(this.lastIssuedTimestamp);
    }

    @Override
    protected String issueTimePart() {
        int res = this.getApproximateTime();
        if (this.lastTimeSeen > res) {
            res = this.lastTimeSeen;
        }
        if (res > this.lastTimeSeen) {
            this.lastSeqSeen = -1;
        }
        this.lastTimeSeen = res;

        return SToken.int2base(res, timePartLen);
    }

    @Override
    public Date timestamp2date(VersionToken ts) {
        TimestampParsed parsed = parseTimestamp(ts);
        long millis = parsed.time * preciseInMillis + EPOCH;
        return new Date(millis);
    }

    protected final int getApproximateTime() {
        return (int) getTimeInMillis() / preciseInMillis;
    }

    long getTimeInMillis() {
        long millis = System.currentTimeMillis();
        millis -= EPOCH;
        millis += this.clockOffsetMs;
        return millis;
    }
}
