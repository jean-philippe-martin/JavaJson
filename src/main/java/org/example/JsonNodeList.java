package org.example;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JsonNodeList extends JsonNode {

    /** Iterate through our children, in display order. **/
    protected class JsonNodeListIterator implements JsonNodeIterator<Integer> {
        final JsonNodeList dad;
        final int displayIndex;

        public JsonNodeListIterator(JsonNodeList dad) {
            this.dad = dad;
            if (this.dad.aggregate != null) {
                this.displayIndex = -1;
            } else {
                this.displayIndex = 0;
            }
        }

        public JsonNodeListIterator(JsonNodeList dad, int displayIndex) {
            this.dad = dad;
            this.displayIndex = displayIndex;
        }

        @Override
        public @NotNull JsonNode get() {
            if (displayIndex==-1) {
                return dad.aggregate;
            }
            return dad.get(dad.displayOrder[displayIndex]);
        }

        @Override
        public @NotNull Integer key() {
            return displayIndex;
        }

        @Override
        public boolean isAggregate() {
            return displayIndex<0;
        }

        @Override
        public @Nullable JsonNodeIterator next() {
            if (displayIndex + 1 >= dad.childCount()) return null;
            return new JsonNodeListIterator(dad, displayIndex + 1);
        }
    }

    // The JsonNodeList holds a JSON array, like "[ "hello", 1, 2]".
    // The deal is that we want the UI to be able to reorder the elements for display,
    // but we need to still know where things were originally so that cursors
    // don't need to be modified and yet still point to the same thing after
    // the display order is changed.
    // For that reason, we don't reorder the actual list of values but
    // instead maintain a "display order" of who's displayed when.
    // Everything else that talks about an "index" uses the original index.

    final List<Object> values;
    private final JsonNode[] children;
    // display index -> list index
    private int[] displayOrder;
    // list index -> display index
    private int[] whereIsDiplayed;
    private @Nullable Sorter sortOrder = null;

    /** We assume that the passed values list is never modified. **/
    protected JsonNodeList(List<Object> values, JsonNode parent, Cursor curToMe, JsonNode root) {
        super(parent, curToMe, root);
        this.values = values;
        this.displayOrder = new int[values.size()];
        for (int i = 0; i< displayOrder.length; i++) {
            displayOrder[i] = i;
        }
        // when index = display order, these two arrays are the same.
        this.whereIsDiplayed = displayOrder;
        this.children = new JsonNode[values.size()];
        this.sortOrder = null;
    }

    public int size() {
        return this.values.size();
    }


    public int[] getIndexesInOrder() {
        return displayOrder.clone();
    }

    public JsonNode get(int index) {
        if (index==-1) {
            return this.aggregate;
        }
        if (children[index]==null) {
            children[index] = JsonNode.fromObject(values.get(index), this, this.whereIAm.enterIndex(index), this.root);
        }
        return children[index];

    }

    @Override
    public Object getValue() {
        List<Object> ret = new ArrayList(values);
        return ret;
    }

    @Override
    public @Nullable JsonNodeIterator iterateChildren() {
        if (children.length==0 && null==this.aggregate) return null;
        return new JsonNodeListIterator(this);
    }


    @Override
    public JsonNode firstChild() {
        if (values.isEmpty()) return null;
        if (null!=aggregate) {
            return aggregate;
        }
        return get(displayOrder[0]);
    }

    @Override
    public JsonNode lastChild() {
        if (values.isEmpty()) return null;
        return get(displayOrder[displayOrder.length-1]);
    }

    public int childCount() {
        return children.length;
    }

    @Override
    public JsonNode nextChild(Cursor childCursor) {
        childCursor = childCursor.truncate(this);
        if (childCursor.getData().parent!=this) {
            throw new RuntimeException("invalid cursor: should be a child of this JSON node");
        }
        Cursor.DescentStep step = childCursor.getStep();
        if (step instanceof Cursor.DescentIndex) {
            Cursor.DescentIndex di = (Cursor.DescentIndex)step;
            int index = di.get();
            if (index<0) {
                // special case: aggregate data. For those we don't change the order.
                return get(displayOrder[0]);
            }
            // convert from the index in the original order (what's in the cursor)
            // to the index in display order (the order we want to iterate in)
            int displayed = whereIsDiplayed[index];
            if (displayed+1>=displayOrder.length) {
                // out of bounds
                return null;
            }
            return get(displayOrder[displayed+1]);
        } else if (step instanceof Cursor.DescentKey) {
            // ok that was the aggregate, moving on to index 0.
            return get(displayOrder[0]);
        }
        throw new RuntimeException("invalid cursor");
    }

    @Override
    public JsonNode prevChild(Cursor childCursor) {
        childCursor = childCursor.truncate(this);
        if (childCursor.getData().parent!=this) {
            throw new RuntimeException("invalid cursor: should be a child of this JSON node");
        }
        Cursor.DescentStep step = childCursor.getStep();
        if (step instanceof Cursor.DescentIndex) {
            Cursor.DescentIndex di = (Cursor.DescentIndex)step;
            int index = di.get();
            if (index<0) {
                // that was aggregate, we're now out of bounds
                return null;
            }
            // convert from the index in the original order (what's in the cursor)
            // to the index in display order (the order we want to iterate in)
            int displayed = whereIsDiplayed[index];
            if (displayed<=0) {
                if (null!=aggregate) {
                    return get(-1);
                }
                // out of bounds
                return null;
            }
            return get(displayOrder[displayed-1]);
        } else if (step instanceof Cursor.DescentKey) {
            // ok that was the aggregate, we're done then.
            return null;
        }
        throw new RuntimeException("invalid cursor");
    }

    @Override
    public void sort(Sorter sorter) {
        if (null==sorter) {
            unsort();
            return;
        }
        SorterList<Object> sl = new SorterList<>(sorter, values);
        displayOrder = Arrays.stream(displayOrder).boxed().sorted(sl).mapToInt(i->i).toArray();
        if (whereIsDiplayed==displayOrder) {
            whereIsDiplayed = new int[displayOrder.length];
        }
        for (int pos=0; pos<displayOrder.length; pos++) {
            int index = displayOrder[pos];
            whereIsDiplayed[index] = pos;
        }
        sorter.pack();
        this.sortOrder = sorter;
    }

    @Override
    public void unsort() {
        for (int i = 0; i< displayOrder.length; i++) {
            displayOrder[i] = i;
        }
        // when index = display order, these two arrays are the same.
        this.whereIsDiplayed = displayOrder;
        this.sortOrder = null;
    }

    @Override
    public @Nullable Sorter getSort() {
        return this.sortOrder;
    }

}
