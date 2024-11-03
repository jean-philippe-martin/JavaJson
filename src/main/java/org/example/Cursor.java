package org.example;

import java.util.SequencedCollection;

public class Cursor {

    public class DescentStep {};

    public class DescentKey extends DescentStep {
        private final String key;

        public DescentKey(String key) {
            this.key = key;
        }

        public String get() {
            return this.key;
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

        public int get() {
            return this.index;
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
    private JsonState data = null;

    public Cursor() {
        this.step = null;
        this.parent = null;
    }

    private Cursor(DescentStep step, Cursor parent) {
        this.step = step;
        this.parent = parent;
    }

    public void setData(JsonState data) {
        if (this.data != null) throw new RuntimeException("Only set data once");
        this.data = data;
    }

    public JsonState getData() {
        return this.data;
    }

    public DescentStep getStep() {
        return this.step;
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
     * Go up the chain until we find a cursor that is a child of the passed state
     * or we reach the root cursor.
     */
    public Cursor truncate(JsonState state) {
        Cursor cur = this;
        while (cur.parent != null && cur.parent.getData() != state) {
            if (cur.parent==cur) break;
            cur = cur.parent;
        }
        return cur;
    }
}
