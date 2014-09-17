package citrea.swarm4j.model.clocks;

import citrea.swarm4j.model.spec.VersionToken;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 09.09.2014
 *         Time: 17:15
 */
public interface Clock {

    VersionToken getLastIssuedTimestamp();

    VersionToken issueTimestamp();

    TimestampParsed parseTimestamp(VersionToken ts);

    void seeTimestamp(VersionToken ts);

    Date timestamp2date(VersionToken ts);
}
