package citrea.swarm4j.model.clocks;

import citrea.swarm4j.model.spec.VersionToken;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MinutePreciseClockTest {
    private static final String PROCESS_ID = "swarm~0";

    @Test
    public void testIssueTimestamp() throws Exception {
        final long initialTime = 0L;
        final FakeMinutePreciseClock clock = new FakeMinutePreciseClock(initialTime);
        final int last = 5;
        assertEquals(
                "initialized correctly",
                new VersionToken("00000", PROCESS_ID),
                clock.getLastIssuedTimestamp()
        );
        for (int i = 1; i <= last; i++) {
            clock.tick();
            assertEquals(
                    "increment(" + clock.lastSeqSeen + ")",
                    new VersionToken("000" + i + "0", PROCESS_ID),
                    clock.issueTimestamp()
            );
        }
        for (int i = 1; i <= last; i++) {
            assertEquals(
                    "increment(" + clock.lastSeqSeen + ")",
                    new VersionToken("000" + last + i, PROCESS_ID),
                    clock.issueTimestamp()
            );
        }

        clock.lastSeqSeen = 64;
        for (int i = 1; i <= last; i++) {
            assertEquals(
                    "increment(" + clock.lastSeqSeen + ")",
                    new VersionToken("000" + last + "01" + i, PROCESS_ID),
                    clock.issueTimestamp()
            );
        }

        assertEquals(
                "lastIssued ok",
                new VersionToken("000" + last + "01" + last, PROCESS_ID),
                clock.getLastIssuedTimestamp()
        );
    }

    @Test
    public void testParseTimestamp() throws Exception {
        final long initialTime = 0L;
        final FakeMinutePreciseClock clock = new FakeMinutePreciseClock(initialTime);
        for (int i = 1; i <= 5; i++) {
            VersionToken ts = new VersionToken("000" + i + "0", PROCESS_ID);
            TimestampParsed tsParsed = clock.parseTimestamp(ts);
            assertEquals("parse(" + i + ", 0)", new TimestampParsed(i, 0), tsParsed);

            for (int j = 1; j <= 5; j++) {
                ts = new VersionToken("000" + i + j, PROCESS_ID);
                tsParsed = clock.parseTimestamp(ts);
                assertEquals("parse(" + i + ", " + j + ")", new TimestampParsed(i, j), tsParsed);
            }
        }
    }

    @Test
    public void testSeeTimestamp() throws Exception {
        final long initialTime = 0L;
        final FakeMinutePreciseClock clock = new FakeMinutePreciseClock(initialTime);

        for (int i = 1; i <= 4; i++) {
            clock.seeTimestamp(new VersionToken("000" + (i * 2) + "0", PROCESS_ID));
            assertEquals(
                    "see(" + (i * 2) + ")",
                    new VersionToken("000" + (i * 2) + "1", PROCESS_ID),
                    clock.issueTimestamp()
            );
        }

        for (int i = 1; i <= 4; i++) {
            clock.seeTimestamp(new VersionToken("000A" + "10" + (i * 2), PROCESS_ID));
            assertEquals(
                    "see("+ (i * 2) +")",
                    new VersionToken("000A" + "10" + (i * 2 + 1), PROCESS_ID),
                    clock.issueTimestamp()
            );
        }
    }

    public static class FakeMinutePreciseClock extends MinutePreciseClock {

        private long currentTime = 0L;

        public FakeMinutePreciseClock(long currentTime) {
            super(MinutePreciseClockTest.PROCESS_ID);
            this.currentTime = currentTime;
        }

        public void tick() {
            this.currentTime += MILLIS_IN_MINUTE;
        }

        @Override
        protected long getTimeInMillis() {
            return currentTime;
        }
    }
}