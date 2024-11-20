package org.example;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.SequencedCollection;

public class JsonNodeMap extends JsonNode {

    private final LinkedHashMap<String, Object> kv;
    private final HashMap<String, JsonNode> children;


    protected JsonNodeMap(LinkedHashMap<String, Object> kv, JsonNode parent, Cursor curToMe, JsonNode root) {
        super(parent, curToMe, root);
        this.kv = kv;
        this.children = new HashMap<>();
    }

    public SequencedCollection<String> getKeysInOrder() {
        LinkedHashMap<String, Object> kv = (LinkedHashMap<String, Object>) this.kv;
        return kv.sequencedKeySet();
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
        SequencedCollection<String> keys = this.getKeysInOrder();
        if (keys.isEmpty()) return null;
        return getChild(keys.getFirst());
    }

    @Override
    public JsonNode lastChild() {
        SequencedCollection<String> keys = this.getKeysInOrder();
        if (keys.isEmpty()) return null;
        return getChild(keys.getLast());
    }

    @Override
    public JsonNode nextChild(Cursor childCursor) {
        childCursor = childCursor.truncate(this);
        Cursor.DescentStep step = childCursor.getStep();
        // if we found ourselves, then this must be a descentKey
        if (!(step instanceof Cursor.DescentKey)) return null;
        String key = ((Cursor.DescentKey)step).get();
        SequencedCollection<String> keys = this.getKeysInOrder();
        boolean found = false;
        for (String k : keys) {
            if (found) {
                // Get the cursor via the JsonState so that it has the data filled in.
                return getChild(k);
            }
            if (k.equals(key)) {
                found = true;
            }
        }
        JsonNode parent = childCursor.getData().getParent();
        if (parent==this) return null;
        return parent.nextChild(childCursor);
    }

    @Override
    public JsonNode prevChild(Cursor childCursor) {
        childCursor = childCursor.truncate(this);
        Cursor.DescentStep step = childCursor.getStep();
        // if we found ourselves, then this must be a descentKey
        if (!(step instanceof Cursor.DescentKey)) return null;
        String key = ((Cursor.DescentKey)step).get();
        SequencedCollection<String> keys = this.getKeysInOrder();
        boolean found = false;
        for (String k : keys.reversed()) {
            if (found) {
                // Get the cursor via the JsonState so that it has the data filled in.
                return getChild(k);
            }
            if (k.equals(key)) {
                found = true;
            }
        }
        return null;
    }


}
