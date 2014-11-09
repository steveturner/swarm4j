package citrea.swarm4j.core.spec;

import citrea.swarm4j.core.model.Syncable;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 24.08.2014
 *         Time: 22:32
 */
final class SyncableRef extends Spec {

    private final TypeIdSpec link;
    private final Syncable object;

    public SyncableRef(Syncable object) {
        super();
        this.link = object.getTypeId();
        this.object = object;
    }

    public Syncable getObject() {
        return object;
    }

    @Override
    public List<SToken> listTokens() {
        return this.link.listTokens();
    }

    @Override
    public int getTokensCount() {
        return this.link.getTokensCount();
    }
}
