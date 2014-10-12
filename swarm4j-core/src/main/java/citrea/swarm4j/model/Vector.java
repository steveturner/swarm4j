package citrea.swarm4j.model;

import citrea.swarm4j.model.annotation.SwarmField;
import citrea.swarm4j.model.annotation.SwarmOperation;
import citrea.swarm4j.model.annotation.SwarmOperationKind;
import citrea.swarm4j.model.annotation.SwarmType;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.*;
import com.eclipsesource.json.JsonValue;

import java.util.*;

/**
 * In distributed environments, linear structures are tricky. It is always
 * recommended to use (sorted) Set as your default collection type. Still, in
 * some cases we need precisely a Vector, so here it is. Note that a vector can
 * not prune its mutation history for quite a while, so it is better not to
 * sort (reorder) it repeatedly. The perfect usage pattern is a growing vector+
 * add sort or no sort at all. If you need to re-shuffle a vector
 * differently or replace its contents, you'd better create a new vector.
 * So, you've been warned.
 * Vector is implemented on top of a LongSpec, so the API is very much alike.
 * The replication/convergence/correctness algorithm is Causal Trees.
 *
 * TODO support JSON types (as a part of ref-gen-refac)
 *
 * @author aleksisha
 *         Date: 06.10.2014
 *         Time: 21:30
 */
@SwarmType
public class Vector<T extends Syncable> extends Syncable {

    private static final OpToken IN = new OpToken("in");
    private static final OpToken RM = new OpToken("rm");
    protected List<T> objects = new ArrayList<T>();

    @SwarmField
    protected LongSpec order = new LongSpec();

    private final TypeToken objectType;
    private final ProxyListener proxy = new ProxyListener(this);

    protected Vector(TypeToken objectType, Host host) throws SwarmException {
        super(null, host);
        this.objectType = objectType;
    }

    protected Vector(IdToken id, TypeToken objectType, Host host) throws SwarmException {
        super(id, host);
        this.objectType = objectType;
    }

    // operations is our assembly language
    /**
     * add an object
     */
    @SwarmOperation(kind = SwarmOperationKind.Logged)
    public void in(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
        // we misuse specifiers to express the operation in
        // a compact non-ambiguous way
        TypeIdVersionHintSpec valueSpec = new TypeIdVersionHintSpec(value.asString(), objectType);
        VersionToken opid = spec.getVersion();
        VersionToken at = valueSpec.getVersion();
        if (opid.compareTo(at) <= 0) {
            throw new IllegalArgumentException("timestamps are messed up");
        }

        TypeToken typeToken = valueSpec.getType();
        if (typeToken == null) {
            typeToken = objectType;
        }
        IdToken idToken = valueSpec.getId();
        if (idToken == null) {
            throw new IllegalArgumentException("no #id token in specifier");
        }
        TypeIdSpec typeId = new TypeIdSpec(typeToken, idToken);
        LongSpecIterator pos = findPositionFor(opid, at);
        T obj = host.get(typeId);

        objects.add(pos.getIndex(), obj);
        order.add(opid.toString(), pos);

        obj.on(JsonValue.NULL, proxy);
    }

    /**
     * remove an object
     */
    @SwarmOperation(kind = SwarmOperationKind.Logged)
    public void rm(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {

        TypeIdVersionHintSpec valueSpec = new TypeIdVersionHintSpec(value.asString(), objectType);
        VersionToken target = valueSpec.getVersion();
        HintToken hintTok = valueSpec.getHint();
        int hint;
        if (hintTok == null) {
            hint = 0;
        } else {
            hint = SToken.base2int(hintTok.getBare());
        }
        LongSpecIterator at = order.find(target, Math.max(0, hint - 5));
        if (!at.hasNext()) {
            at = order.find(target, 0);
        }
        if (!at.hasNext()) {
            // this can only be explained by concurrent deletion
            // partial order can't break cause-and-effect ordering
            return;
        }
        Syncable obj = objects.get(at.getIndex());
        objects.remove(at.getIndex());
        at.erase(1);

        obj.off(proxy);
    }


    /* TODO reactions: {

        'init': function fillAll (spec,val,src) { // TODO: reactions, state init tests
            for(var i=this._order.iterator(); !i.hasNext(); i.next()) {
                var op = i.token() + '.in';
                var value = this._oplog[op];
                var obj = this.getObject(value);
                this.objects[i.index] = obj;
                obj.on(this._proxy);
            }
        }

    }, */

    public T getObject(TypeIdSpec spec) throws SwarmException {
        T obj = host.get(spec);
        return obj;
    }

    public int length() {
        return objects.size();
    }

    //  C A U S A L  T R E E S  M A G I C

    public LongSpecIterator findPositionFor(VersionToken opid, VersionToken parentId) { // FIXME protected methods && statics (entryType)
        if (parentId == null) {
            parentId = getParentOf(opid);
        }
        LongSpecIterator next;
        if (!VersionToken.ZERO_VERSION.equals(parentId)) {
            next = order.find(parentId, 0);
            if (!next.hasNext()) {
                next = findPositionFor(parentId, null);
            }
            next.next();
        } else {
            next = order.iterator(0);
        }
        // skip "younger" concurrent siblings
        while (next.hasNext()) {
            VersionToken nextId = next.decode();
            if (opid.compareTo(nextId) > 0) {
                break;
            }
            VersionToken subtreeId = inSubtreeOf(nextId, parentId);
            if (subtreeId == null || opid.compareTo(subtreeId) > 0) {
                break;
            }
            skipSubtree(next, subtreeId);
        }
        return next; // add before
    }

    public VersionToken getParentOf(VersionToken id) {
        JsonValue spec = oplog.get(new VersionOpSpec(id, IN));
        if (spec == null) {
            throw new Error("operation unknown: " + id);
        }
        return new TypeIdVersionHintSpec(spec.asString(), objectType).getVersion();
    }

    /**
     * returns the immediate child of the root node that is an ancestor
     * of the given node.
     */
    public VersionToken inSubtreeOf(VersionToken nodeId, VersionToken rootId) {
        VersionToken id = nodeId;
        VersionToken p = id;
        while (id != null && id.compareTo(rootId) > 0) {
            p = id;
            id = getParentOf(id);
        }
        if (id != null && id.equals(rootId)) {
            return p;
        } else {
            return null;
        }
    }

    public boolean isDescendantOf(VersionToken nodeId, VersionToken rootId) {
        VersionToken i = nodeId;
        while (i != null && i.compareTo(rootId) > 0) {
            i = getParentOf(i);
        }
        return i != null && i.equals(rootId);
    }

    public LongSpecIterator skipSubtree(LongSpecIterator iter, VersionToken root) {
        root = root != null ? root : iter.decode();
        do {
            iter.next();
        } while (iter.hasNext() && isDescendantOf(iter.decode(), root));
        return iter;
    }

    @Override
    protected String validate(FullSpec spec, JsonValue val) {
        // TODO ? ref op is known
        return super.validate(spec, val);
    }

    //  wrapper methods that convert into op calls above

    int indexOf(TypeIdSpec typeId) throws SwarmException {
        T object = getObject(typeId);
        return indexOf(object);
    }

    int indexOf(T object) throws SwarmException {
        return objects.indexOf(object);
    }

    int indexOf(TypeIdSpec typeId, int startAt) throws SwarmException {
        T object = getObject(typeId);
        return indexOf(object, startAt);
    }

    int indexOf(T object, int startAt) {
        return objects.subList(startAt, objects.size()).indexOf(object) + startAt;
    }

    /**
     * Assuming position 0 on the "left" and left-to-right writing, the
     * logic of causal tree insertion is
     * add(newEntry, parentWhichIsOnTheLeftSide).
     */
    public FullSpec add(TypeIdSpec typeId, int pos) throws SwarmException {
        VersionToken opid = pos == 0 ? VersionToken.ZERO_VERSION : order.tokenAt(pos - 1);
        FullSpec spec = newEventSpec(IN);
        deliver(spec, typeId.typeIdVersionHint(opid).toJson(), OpRecipient.NOOP);
        return spec;
    }

    public FullSpec add(T obj, int pos) throws SwarmException {
        return add(obj.getTypeId(), pos);
    }

    public FullSpec add(T obj, TypeIdSpec pos) throws SwarmException {
        return add(obj.getTypeId(), indexOf(pos));
    }

    public FullSpec add(TypeIdSpec typeId, TypeIdSpec pos) throws SwarmException {
        return add(typeId, indexOf(pos));
    }

    public FullSpec add(TypeIdSpec typeId) throws SwarmException {
        return add(typeId, order.getTokensCount());
    }

    public FullSpec add(T object) throws SwarmException {
        return add(object, order.getTokensCount());
    }

    public void insertAfter(TypeIdSpec spec, int pos) throws SwarmException {
        if (pos == -1) {
            pos = order.getTokensCount();
        }
        add(spec, pos);
    }

    public void insertAfter(TypeIdSpec spec, TypeIdSpec pos) throws SwarmException {
        insertAfter(spec, indexOf(pos));
    }

    public void remove(TypeIdSpec pos) throws SwarmException {
        remove(indexOf(pos));
    }

    public void remove(int pos) throws SwarmException {
        String hint = SToken.int2base(pos, 0);
        VersionToken version = order.tokenAt(pos);
        TypeIdVersionHintSpec val = new TypeIdVersionHintSpec(version, new HintToken(hint));
        deliver(newEventSpec(RM), val.toJson(), OpRecipient.NOOP);
    }

    public T get(int i) {
        return objects.get(i);
    }

    public void insertSorted(T obj, Comparator<? super T> cmp) {
        //TODO insertSorted
    }

    public void setOrder(Comparator<? super T> cmp) {
        //TODO setOrder
    }

    // object event relay: subscription
    public void onObjects(OpRecipient callback) {
        proxy.on(callback);
    }

    // object event relay: unsubscription
    public void offObjects(OpRecipient callback) {
        proxy.off(callback);
    }

    public void onObjectStateReady(OpRecipient callback) throws SwarmException { // TODO timeout ?
        final Checker checker = new Checker(callback);

        if (!hasNoState()) {
            checker.deliver(null, null, this);
        } else {
            once(JsonValue.valueOf("init"), checker);
        }
    }

    public Iterator<T> iterator() {
        return objects.iterator();
    }

    private class Checker implements OpRecipient {
        private final OpRecipient callback;

        private Checker(OpRecipient callback) {
            this.callback = callback;
        }

        @Override
        public void deliver(FullSpec spec, JsonValue value, OpRecipient source) throws SwarmException {
            for (Syncable obj : Vector.this.objects) {
                if (obj.hasNoState()) {
                    obj.once(JsonValue.valueOf("init"), this);
                    return;
                }
            }
            // all objects in collection their states
            callback.deliver(null, null, Vector.this);
        }
    }
}
