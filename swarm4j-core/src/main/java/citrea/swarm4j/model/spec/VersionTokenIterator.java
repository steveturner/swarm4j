package citrea.swarm4j.model.spec;

import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 21.06.2014
 *         Time: 20:54
 */
public class VersionTokenIterator implements Iterator<VersionToken> {

    private final SToken[] tokens;
    private int index = 0;

    public VersionTokenIterator(SToken[] tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean hasNext() {
        while (index < tokens.length && SQuant.VERSION != tokens[index].getQuant()) {
            index++;
        }
        return (index < tokens.length);
    }

    @Override
    public VersionToken next() {
        return index < tokens.length ? (VersionToken) tokens[index++] : null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("tokens removal is not supported");
    }
}
