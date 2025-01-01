package org.example;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class JsonNodeMap extends JsonNode {

    private final LinkedHashMap<String, Object> kv;
    private final HashMap<String, JsonNode> children;

    // The keys, in display order
    private @NotNull String[] displayOrder;
    // For each key, where it stands in the display order.
    private @NotNull Map<String, Integer> whereIsDiplayed;


    protected JsonNodeMap(LinkedHashMap<String, Object> kv, JsonNode parent, Cursor curToMe, JsonNode root) {
        super(parent, curToMe, root);
        this.kv = kv;
        this.children = new HashMap<>();
        this.displayOrder = kv.sequencedKeySet().toArray(new String[0]);
        this.whereIsDiplayed = new HashMap<>();
        for (int i=0; i<displayOrder.length; i++) {
            whereIsDiplayed.put(displayOrder[i], i);
        }
    }

    public SequencedCollection<String> getKeysInOrder() {
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
            Object childJson = kv.get(key);
            if (null==childJson) throw new NoSuchElementException("No '"+key+"' child for " + whereIAm.toString());
            JsonNode child = JsonNode.fromObject(childJson, this, whereIAm.enterKey(key), root);
            this.children.put(key, child);
            return child;
        }
    }

    public int childCount() {
        return kv.size();
    }

    /** True if the userCursor is pointing to this key of ours. **/
    @Override
    public boolean isAtCursor(String key) {
        JsonNode hypothetical = getChild(key);
        return hypothetical.isAtCursor();
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
    public JsonNode nextChild(Cursor childCursor) {
        childCursor = childCursor.truncate(this);
        Cursor.DescentStep step = childCursor.getStep();
        // if we found ourselves, then this must be a descentKey
        if (!(step instanceof Cursor.DescentKey)) return null;
        String key = ((Cursor.DescentKey)step).get();
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
        Cursor.DescentStep step = childCursor.getStep();
        // if we found ourselves, then this must be a descentKey
        if (!(step instanceof Cursor.DescentKey)) return null;
        String key = ((Cursor.DescentKey)step).get();
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
            this.whereIsDiplayed = new HashMap<>();
            for (int i = 0; i < displayOrder.length; i++) {
                whereIsDiplayed.put(displayOrder[i], i);
            }
        } else {
            List<Object> values = kv.values().stream().toList();
            List<String> keys = kv.sequencedKeySet().stream().toList();
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
    }

    @Override
    public void unsort() {
        this.displayOrder = kv.sequencedKeySet().toArray(new String[0]);
        this.whereIsDiplayed = new HashMap<>();
        for (int i=0; i<displayOrder.length; i++) {
            whereIsDiplayed.put(displayOrder[i], i);
        }
    }

}
