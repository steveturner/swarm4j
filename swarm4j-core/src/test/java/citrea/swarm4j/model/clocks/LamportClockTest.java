package citrea.swarm4j.model.clocks;

import citrea.swarm4j.model.spec.VersionToken;
import org.junit.Test;

import static org.junit.Assert.*;

public class LamportClockTest {
    private static final String PROCESS_ID = "swarm~0";
    private static final String ZERO_TIME = "00000";

    @Test
    public void testIssueTimestamp() throws Exception {
        final Clock clock = new LamportClock(PROCESS_ID, ZERO_TIME);
        final int last = 5;
        assertEquals(
                "initialized correctly",
                new VersionToken(ZERO_TIME, PROCESS_ID),
                clock.getLastIssuedTimestamp()
        );
        for (int i = 1; i <= last; i++) {
            assertEquals(
                    "increment(" + i + ")",
                    new VersionToken("0000" + i, PROCESS_ID),
                    clock.issueTimestamp()
            );
        }
        assertEquals(
                "lastIssued ok",
                new VersionToken("0000" + last, PROCESS_ID),
                clock.getLastIssuedTimestamp()
        );
    }

    @Test
    public void testParseTimestamp() throws Exception {
        final Clock clock = new LamportClock(PROCESS_ID, ZERO_TIME);
        for (int i = 1; i <= 5; i++) {
            VersionToken ts = new VersionToken("0000" + i, PROCESS_ID);
            TimestampParsed tsParsed = clock.parseTimestamp(ts);
            assertEquals("parse(" + i + ")", new TimestampParsed(0, i), tsParsed);
        }
    }

    @Test
    public void testCheckTimestamp() throws Exception {
        final Clock clock = new LamportClock(PROCESS_ID, ZERO_TIME);
        for (int i = 1; i <= 3; i++) {
            assertTrue(clock.checkTimestamp(new VersionToken("0000" + (i * 2), PROCESS_ID)));
            assertEquals(
                    "see("+i+")",
                    new VersionToken("0000" + (i * 2 + 2), PROCESS_ID),
                    clock.issueTimestamp()
            );
        }
    }
}