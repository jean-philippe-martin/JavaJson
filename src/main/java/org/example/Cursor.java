package org.example;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Cursor {

    public abstract class DescentStep {
        // Apply this step to the given node (must have the correct type).
        public abstract JsonNode apply(JsonNode node);
    };

    public class DescentKey extends DescentStep {
        private final String key;

        public DescentKey(String key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object rhs) {
            if (!(rhs instanceof DescentKey)) return false;
            if (key==null) return ((DescentKey)rhs).key==null;
            return key.equals(((DescentKey)rhs).key);
        }

        @Override
        public int hashCode() {
            if (null==key) return 0;
            return key.hashCode();
        }

        public String get() {
            return this.key;
        }

        public JsonNode apply(JsonNode node) {
            return ((JsonNodeMap) node).getChild(key);
        }

        @Override
        public String toString() {
            return "." + this.key;
        }
    }

    public class DescentIndex extends DescentStep {
        private final int index;

        public DescentIndex(int index) {
            this.index = index;
        }

        @Override
        public boolean equals(Object rhs) {
            if (!(rhs instanceof DescentIndex)) return false;
            return index  == (((DescentIndex)rhs).index);
        }

        @Override
        public int hashCode() {
            return index;
        }

        public int get() {
            return this.index;
        }

        public JsonNode apply(JsonNode node) {
            return ((JsonNodeList) node).get(index);
        }


        @Override
        public String toString() {
            return "[" + this.index + "]";
        }
    }

    private final DescentStep step;
    private final Cursor parent;
    // This is set immediately after making the cursor, and then no longer
    // modified.
    private JsonNode data = null;

    public Cursor() {
        this.step = null;
        this.parent = null;
    }

    // copy ctor
    protected Cursor(Cursor old, Cursor fork) {
        this.step = old.step;
        this.parent = old.parent;
        this.data = old.data;
    }

    private Cursor(DescentStep step, Cursor parent) {
        this.step = step;
        this.parent = parent;
    }

    public void setData(JsonNode data) {
        if (this.data != null) throw new RuntimeException("Only set data once");
        this.data = data;
    }

    public JsonNode getData() {
        return this.data;
    }

    public DescentStep getStep() {
        return this.step;
    }

    public List<DescentStep> asListOfSteps() {
        if (null==parent || parent==this) {
            // no parent, empty list
            return new ArrayList<DescentStep>();
        }
        List<DescentStep> ret = parent.asListOfSteps();
        ret.add(step);
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        addToString(sb);
        return sb.toString();
    }

    protected void addToString(StringBuilder sb) {
        if (null==step) return;
        if (this.parent != this) {
            this.parent.addToString(sb);
        }
        sb.append(step.toString());
    }

    public Cursor enterKey(String key) {
        return new Cursor(new DescentKey(key), this);
    }

    public Cursor enterIndex(int index) {
        return new Cursor(new DescentIndex(index), this);
    }

    /**
     * True if this cursor selects the location indicated by node.
     *
     * @param node a JSON node
     * @param fork a parent whose children are all selected (or null)
     */
    public boolean selects(JsonNode node, @Nullable Cursor fork) {
        if (null==node) return false;
        if (getData()==node) return true;
        if (null==fork) return false;
        String forkStr = fork.toString();
        String here = this.toString();
        String there = node.whereIAm.toString();
        if (!there.startsWith(forkStr)) return false;
        // remove one step from "here" and "there" after the pivot
        // and check if they're the same.

        Cursor myAncestors = this;
        Cursor itsAncestors = node.whereIAm;

        while (myAncestors!=null && itsAncestors!=null) {
            if (myAncestors.step == null) {
                return itsAncestors.step == null;
            } else {
                if (itsAncestors.step==null) return false;
            }

            // types must match all the way up
            if (myAncestors.step.getClass() != itsAncestors.step.getClass()) return false;
            // now either same value, or we're at pivot.
            DescentStep itsStep = (DescentStep) itsAncestors.step;
            if (!myAncestors.step.equals(itsAncestors.step)) {
                if (myAncestors.parent != fork) return false;
                return true;
            }

            Cursor dad = myAncestors.parent;
            if (dad==myAncestors) return true;
            myAncestors = dad;
            dad = itsAncestors.parent;
            if (dad==itsAncestors) return false;
            itsAncestors = dad;
        }
        return true;
    }

    /**
     * Go up the chain until we find a cursor that is a child of the passed state
     * or we reach the root cursor.
     */
    public Cursor truncate(JsonNode state) {
        Cursor cur = this;
        while (cur.parent != null && cur.parent.getData() != state) {
            if (cur.parent==cur) break;
            cur = cur.parent;
        }
        return cur;
    }

}
