package citrea.swarm4j.core.callback;

import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.spec.FullSpec;
import com.eclipsesource.json.JsonValue;


/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 23.08.2014
 *         Time: 23:10
 */
public abstract class FilteringOpRecipient<T extends OpRecipient> implements OpRecipient {
    protected final T inner;

    protected FilteringOpRecipient(T inner) {
        this.inner = inner;
    }

    protected abstract boolean filter(FullSpec spec, JsonValue value, OpRecipient source);

    protected void deliverInternal(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
        inner.deliver(spec, value, source);
    }

    @Override
    public final void deliver(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
        if (filter(spec, value, source)) {
            deliverInternal(spec, value, source);
        }
    }

    public T getInner() {
        return inner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o instanceof FilteringOpRecipient) {
            FilteringOpRecipient that = (FilteringOpRecipient) o;
            return this.inner.equals(that.inner);
        }

        return (o instanceof OpRecipient) && this.inner.equals(o);
    }

    @Override
    public int hashCode() {
        return inner.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "{" +
                "inner=" + inner +
                '}';
    }
}
