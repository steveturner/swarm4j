package citrea.swarm4j.core.clocks;

import citrea.swarm4j.core.spec.VersionToken;

import java.util.Date;

/**
 * Pure logical-time Lamport clocks.
 *
 * @author aleksisha
 *         Date: 13.09.2014
 *         Time: 14:37
 */
public class LamportClock extends AbstractClock {

    private static final int SEQUENCE_PART_LENGTH = 5;

    public LamportClock(String processId, String initialTime) {
        super(processId, 0);

        VersionToken specToken = new VersionToken(initialTime, processId);
        // sometimes we assume our local clock has some offset
        if (NO_INITIAL_TIME.equals(specToken.getBare())) {
            specToken = new VersionToken(issueTimePart() + generateNextSequencePart(), id);
        } else {
            lastSeqSeen = 0;
        }
        this.lastIssuedTimestamp = specToken;
    }

    public LamportClock(String processId) {
        this(processId, NO_INITIAL_TIME);
    }

    @Override
    protected String issueTimePart() {
        return "";
    }

    @Override
    protected String generateNextSequencePart() {
        int seq = ++this.lastSeqSeen;
        return VersionToken.int2base(seq, SEQUENCE_PART_LENGTH);
    }

    @Override
    protected int parseSequencePart(String seq) {
        return VersionToken.base2int(seq);
    }

    /**
     * Freshly issued Lamport logical timestamps must be greater than
     * any timestamps previously seen.
     */
    @Override
    public boolean checkTimestamp(VersionToken ts) {
        TimestampParsed parsed = this.parseTimestamp(ts);
        if (parsed.seq >= this.lastSeqSeen) {
            this.lastSeqSeen = parsed.seq + 1;
        }
        return true;
    }

    @Override
    public void adjustTime(long timeInMillis) {
        // nothing to adjust
    }

    @Override
    public Date timestamp2date(VersionToken ts) {
        throw new UnsupportedOperationException("Lamport timestamp can't be converted to Date");
    }
}
