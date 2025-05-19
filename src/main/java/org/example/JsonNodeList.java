package org.example;

import org.example.cursor.DescentIndex;
import org.example.cursor.DescentKey;
import org.example.cursor.DescentStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class JsonNodeList extends JsonNode {

    public static class Builder implements JsonNodeBuilder {
        private final JsonNodeBuilder[] children;
        private @Nullable Sorter sortOrder;

        public Builder(JsonNodeBuilder[] children) {
            this.children = children;
        }

        // Optionally, set a sort order
        public Builder sorter(Sorter s) {
            this.sortOrder = s;
            return this;
        }

        @Override
        public JsonNodeList build(JsonNode parent, Cursor curToMe) {
            JsonNode root = null;
            if (null!=parent) root = parent.rootInfo.root;
            // This also builds all the children, recursively.
            JsonNodeList ret = new JsonNodeList(children, parent, curToMe, root, true);
            ret.sort(sortOrder);
            return ret;
        }
    }

    /** Iterate through our children, in display order. **/
    protected class JsonNodeListIterator implements JsonNodeIterator<Integer> {
        final JsonNodeList dad;
        final int displayIndex;
        final boolean includeAggregates;

        public JsonNodeListIterator(JsonNodeList dad, boolean includeAggregates) {
            this.dad = dad;
            this.includeAggregates = includeAggregates;
            if (this.dad.aggregate != null) {
                this.displayIndex = -1;
            } else {
                this.displayIndex = 0;
            }
        }

        public JsonNodeListIterator(JsonNodeList dad, int displayIndex, boolean includeAggregates) {
            this.dad = dad;
            this.includeAggregates = includeAggregates;
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
            JsonNodeIterator ret = new JsonNodeListIterator(dad, displayIndex + 1, includeAggregates);
            if (!includeAggregates) {
                while (ret.isAggregate()) {
                    ret = ret.next();
                }
            }
            return ret;
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

    List<Object> values;
    private JsonNode[] children;
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


    private JsonNodeList(JsonNodeBuilder[] newKids, JsonNode parent, Cursor curToMe, JsonNode root, boolean _ignored) {
        super(parent, curToMe, root);
        // The values are set from the JsonNodes.
        this.children = new JsonNode[newKids.length];
        for (int i=0; i < newKids.length; i++) {
            children[i] = newKids[i].build(this, this.asCursor().enterIndex(i));
        }
        this.values = new ArrayList<>();
        for (int i=0; i < newKids.length; i++) {
            values.add(children[i].getValue());
        }
        this.displayOrder = new int[values.size()];
        for (int i = 0; i< displayOrder.length; i++) {
            displayOrder[i] = i;
        }
        // when index = display order, these two arrays are the same.
        this.whereIsDiplayed = displayOrder;
        sortOrder = null;
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
    public @Nullable JsonNodeIterator iterateChildren(boolean includeAggregates) {
        if (children.length==0 && null==this.aggregate) return null;
        return new JsonNodeListIterator(this, includeAggregates);
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
        DescentStep step = childCursor.getStep();
        if (step instanceof DescentIndex) {
            DescentIndex di = (DescentIndex)step;
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
        } else if (step instanceof DescentKey) {
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
        DescentStep step = childCursor.getStep();
        if (step instanceof DescentIndex) {
            DescentIndex di = (DescentIndex)step;
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
        } else if (step instanceof DescentKey) {
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

    /**
     * change the parent node.
     */
    @Override
    public void reparent(JsonNode newParent, Cursor cursorToMe) {
        super.reparent(newParent, cursorToMe);
        for (int i=0; i<children.length; i++) {
            if (children[i] == null) continue;
            children[i].reparent(this, cursorToMe.enterIndex(i));
        }
    }

    @Override
    public @NotNull JsonNode replaceChild(Cursor toKid, JsonNodeBuilder kid) {

        if (!(toKid.getStep() instanceof DescentIndex)) {
            throw new RuntimeException("Cursor must point to a child, was expecting a numerical index. Got '" + toKid.toString() + "'");
        }
        int index = ((DescentIndex)toKid.getStep()).get();
        JsonNode oldKid = get(index);
        this.pinnedUnderMe -= oldKid.pinnedUnderMe;
        JsonNode newKid = kid.build(this, whereIAm.enterIndex(index));
        newKid.rootInfo = rootInfo;
        this.pinnedUnderMe += newKid.pinnedUnderMe;
        this.children[index] = newKid;
        return newKid;
    }

    @Override
    public void checkInvariants() throws InvariantException {
        super.checkInvariants();
        int pos = 0;
        for (int key: displayOrder) {
            if (whereIsDiplayed[key] != pos) {
                throw new InvariantException("displayOrder inconsistent with whereIsDisplayed for list at " + asCursor().toString());
            }
            pos++;
        }
        for (int i=0; i<this.children.length; i++) {
            if (this.children[i]==null) continue;
            Cursor toChild = this.children[i].whereIAm;
            DescentStep lastStep = toChild.getStep();
            if (lastStep instanceof DescentIndex) {
                int index = ((DescentIndex)lastStep).get();
                if (index != i) {
                    throw new InvariantException("Child " + i + " thinks it is child " + index + " at " + asCursor().toString());
                }
            } else {
                throw new InvariantException("Child " + i + " thinks it is child \"" + lastStep + " at " + asCursor().toString());
            }
        }
    }
}
