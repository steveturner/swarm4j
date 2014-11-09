package citrea.swarm4j.core.clocks;

import citrea.swarm4j.core.spec.SToken;
import citrea.swarm4j.core.spec.VersionToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 13.09.2014
 *         Time: 14:55
 */
public abstract class SomePreciseClock extends AbstractClock {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final int preciseInMillis;

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

    /**
     * Freshly issued timestamps must be greater than
     * any timestamps previously seen.
     */
    @Override
    public boolean checkTimestamp(VersionToken ts) {
        if (ts.compareTo(this.lastIssuedTimestamp) < 0) {
            return true;
        }
        TimestampParsed parsed = this.parseTimestamp(ts);
        if (parsed.time < this.lastTimeSeen) {
            return true;
        }
        int approxTime = this.getApproximateTime();
        if (parsed.time > approxTime + 1) {
            return false; // back to the future
        }
        this.lastTimeSeen = parsed.time;
        this.lastSeqSeen = parsed.seq;
        return true;
    }

    @Override
    public void adjustTime(long timeInMillis) {
        // TODO use min historical offset
        this.clockOffsetMs = timeInMillis - this.getTimeInMillis();
        int lastTS = this.lastTimeSeen;
        this.lastTimeSeen = 0;
        this.lastSeqSeen = 0;
        this.lastIssuedTimestamp = issueTimestamp();
        if (this.getApproximateTime() + 1 < lastTS) {
            logger.error("Risky clock reset: {}", this.lastIssuedTimestamp);
        }

    }

    protected final int getApproximateTime() {
        return (int) getTimeInMillis() / preciseInMillis;
    }

}
