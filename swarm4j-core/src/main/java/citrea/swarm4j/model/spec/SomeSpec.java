package citrea.swarm4j.model.spec;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 13.10.2014
 *         Time: 01:21
 */
public interface SomeSpec {
    List<SToken> listTokens();

    String asString();

    boolean isEmpty();

    int getTokensCount();
}
