package org.example;

import org.example.cursor.DescentKey;
import org.example.cursor.DescentStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class JsonNodeMap extends JsonNode {

    public static class Builder implements JsonNodeBuilder {
        private final LinkedHashMap<String, JsonNodeBuilder> children;
        private @Nullable Sorter sortOrder;

        public Builder(LinkedHashMap<String, JsonNodeBuilder> children) {
            this.children = children;
        }

        // Optionally, set a sort order
        public JsonNodeMap.Builder sorter(Sorter s) {
            this.sortOrder = s;
            return this;
        }

        @Override
        public JsonNodeMap build(JsonNode parent, Cursor curToMe) {
            JsonNode root = null;
            if (null!=parent) root = parent.rootInfo.root;
            // This also builds all the children, recursively.
            JsonNodeMap ret = new JsonNodeMap(children, parent, curToMe, root, true);
            ret.sort(sortOrder);
            return ret;
        }
    }

    /** Iterate through our children, in display order. **/
    public class JsonNodeMapIterator implements JsonNodeIterator<String> {
        final JsonNodeMap dad;
        final int displayIndex;

        public JsonNodeMapIterator(JsonNodeMap dad) {
            this.dad = dad;
            if (this.dad.aggregate != null) {
                this.displayIndex = -1;
            } else {
                this.displayIndex = 0;
            }
        }

        public JsonNodeMapIterator(JsonNodeMap dad, int displayIndex) {
            this.dad = dad;
            this.displayIndex = displayIndex;
        }

        @Override
        public @NotNull JsonNode get() {
            if (displayIndex==-1) {
                return dad.aggregate;
            }
            return dad.getChild(dad.displayOrder[displayIndex]);
        }

        @Override
        public @NotNull String key() {
            if (displayIndex==-1) {
                return dad.aggregateComment;
            }
            return dad.displayOrder[displayIndex];
        }

        @Override
        public boolean isAggregate() {
            return displayIndex<0;
        }

        @Override
        public @Nullable JsonNodeIterator next() {
            if (displayIndex + 1 >= dad.childCount()) return null;
            return new JsonNodeMap.JsonNodeMapIterator(dad, displayIndex + 1);
        }
    }

    private final LinkedHashMap<String, Object> kv;
    private final HashMap<String, JsonNode> children;

    // The keys, in display order
    private @NotNull String[] displayOrder;
    // For each key, where it stands in the display order.
    private @NotNull Map<String, Integer> whereIsDiplayed;

    private @Nullable Sorter sortOrder = null;


    /** Normal constructor, from the result of parsing JSON. */
    protected JsonNodeMap(LinkedHashMap<String, Object> kv, JsonNode parent, Cursor curToMe, JsonNode root) {
        super(parent, curToMe, root);
        this.kv = kv;
        this.children = new HashMap<>();
        this.displayOrder = kv.keySet().toArray(new String[0]);
        this.whereIsDiplayed = new HashMap<>();
        for (int i=0; i<displayOrder.length; i++) {
            whereIsDiplayed.put(displayOrder[i], i);
        }
        sortOrder = null;
    }

    // See the builder
    private JsonNodeMap(LinkedHashMap<String, JsonNodeBuilder> newKids, JsonNode parent, Cursor curToMe, JsonNode root, boolean _ignored) {
        super(parent, curToMe, root);
        // So you better not call getValue now. But childCount() will work.
        this.kv = new LinkedHashMap<>();
        for (String key : newKids.keySet()) {
            kv.put(key, null);
        }
        this.children = new HashMap<String, JsonNode>();
        for (Map.Entry<String, JsonNodeBuilder> e : newKids.entrySet()) {
            JsonNode child = e.getValue().build(this, this.asCursor().enterKey(e.getKey()));
            this.children.put(e.getKey(), child);
            kv.put(e.getKey(), child.getValue());
        }

        this.displayOrder = this.children.keySet().toArray(new String[0]);
        this.whereIsDiplayed = new HashMap<>();
        for (int i=0; i<displayOrder.length; i++) {
            whereIsDiplayed.put(displayOrder[i], i);
        }
        sortOrder = null;
    }

    public Collection<String> getKeysInOrder() {
        return new ArrayList<>(List.of(displayOrder));
    }

    @Override
    public Object getValue() {
        return kv.clone();
    }

    public Object getValue(String key) {
        return kv.get(key);
    }

    public JsonNode getChild(String key) {
        if (this.children.containsKey(key)) {
            return this.children.get(key);
        } else {
            if (!kv.containsKey(key)) throw new NoSuchElementException("No '"+key+"' child for " + whereIAm.toString());
            Object childJson = kv.get(key);
            JsonNode child = JsonNode.fromObject(childJson, this, whereIAm.enterKey(key), root);
            this.children.put(key, child);
            return child;
        }
    }

    public int childCount() {
        return kv.size();
    }

    public void setChildAggregateComment(String key, String comment) {
        getChild(key).aggregateComment = comment;
    }

    /** True if the userCursor is pointing to this key of ours. **/
    @Override
    public boolean isAtCursor(String key) {
        try {
            JsonNode hypothetical = getChild(key);
            return hypothetical.isAtCursor();
        } catch (NoSuchElementException _ex) {
            // if we don't have that child, then clearly it's not at the cursor.
            return false;
        }
    }

    /** Whether this child is folded **/
    public boolean getChildFolded(String key) {
        if (!this.children.containsKey(key)) return false;
        return getChild(key).getFolded();
    }

    @Override
    public JsonNode firstChild() {
        if (displayOrder.length==0) return null;
        return getChild(displayOrder[0]);
    }

    @Override
    public JsonNode lastChild() {
        if (displayOrder.length==0) return null;
        return getChild(displayOrder[displayOrder.length-1]);
    }

    @Override
    public @Nullable JsonNodeIterator iterateChildren() {
        if (childCount()==0) return null;
        return new JsonNodeMapIterator(this);
    }

    @Override
    public JsonNode nextChild(Cursor childCursor) {
        childCursor = childCursor.truncate(this);
        DescentStep step = childCursor.getStep();
        // if we found ourselves, then this must be a descentKey
        if (!(step instanceof DescentKey)) return null;
        String key = ((DescentKey)step).get();
        if (!whereIsDiplayed.containsKey(key)) {
            return null;
        }
        int displayIndex = whereIsDiplayed.get(key);
        if (displayIndex+1 >= displayOrder.length) {
            // we're at the end
            JsonNode parent = childCursor.getData().getParent();
            if (parent==this) return null;
            return parent.nextChild(childCursor);
        }
        return getChild(displayOrder[displayIndex+1]);
    }

    @Override
    public JsonNode prevChild(Cursor childCursor) {
        childCursor = childCursor.truncate(this);
        DescentStep step = childCursor.getStep();
        // if we found ourselves, then this must be a descentKey
        if (!(step instanceof DescentKey)) return null;
        String key = ((DescentKey)step).get();
        if (!whereIsDiplayed.containsKey(key)) {
            return null;
        }
        int displayIndex = whereIsDiplayed.get(key);
        if (displayIndex == 0) {
            // we're at the beginning already
            return null;
        }
        return getChild(displayOrder[displayIndex-1]);
    }

    @Override
    public void sort(Sorter sorter) {
        if (null==sorter) {
            unsort();
            return;
        }

        if (sorter.getSortkeys()) {
            ArrayList<String> keys = new ArrayList<String>(getKeysInOrder());
            keys.sort(sorter);
            this.displayOrder = keys.toArray(new String[0]);
        } else {
            List<Object> values = kv.values().stream().collect(Collectors.toList());
            List<String> keys = kv.keySet().stream().collect(Collectors.toList());
            List<Integer> indices = new ArrayList<>();
            for (int i=0; i<values.size(); i++) {
                indices.add(i);
            }
            SorterList<Object> sl = new SorterList<>(sorter, values);
            int[] sortedIndices = indices.stream().sorted(sl).mapToInt(i->i).toArray();
            this.displayOrder = new String[indices.size()];
            for (int i : indices) {
                displayOrder[i] = keys.get(sortedIndices[i]);
            }
        }
        this.whereIsDiplayed = new HashMap<>();
        for (int i = 0; i < displayOrder.length; i++) {
            whereIsDiplayed.put(displayOrder[i], i);
        }
        sorter.pack();
        sortOrder = sorter;
    }

    @Override
    public void unsort() {
        this.displayOrder = kv.keySet().toArray(new String[0]);
        this.whereIsDiplayed = new HashMap<>();
        for (int i=0; i<displayOrder.length; i++) {
            whereIsDiplayed.put(displayOrder[i], i);
        }
        sortOrder = null;
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
        var it = this.iterateChildren();
        while (it!=null) {
            it.get().reparent(this, cursorToMe.enterKey((String)it.key()));
            it = it.next();
        }
    }

    @Override
    public @NotNull JsonNode replaceChild(Cursor toKid, JsonNodeBuilder kid) {
        if (toKid.getParent() != whereIAm) {
            throw new RuntimeException("Cursor must point to a child. Got '" + toKid.toString() + "'");
        }
        if (!(toKid.getStep() instanceof DescentKey)) {
            throw new RuntimeException("Cursor must point to a child, was expecting a string index. Got '" + toKid.toString() + "'");
        }
        String key = ((DescentKey)toKid.getStep()).get();
        JsonNode oldKid = getChild(key);
        this.pinnedUnderMe -= oldKid.pinnedUnderMe;
        JsonNode newKid = kid.build(this, whereIAm.enterKey(key));
        this.pinnedUnderMe += newKid.pinnedUnderMe;
        this.children.put(key, newKid);
        return newKid;
    }
}
