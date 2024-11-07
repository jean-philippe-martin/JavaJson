package org.example;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.SequencedCollection;

public class JsonStateMap extends JsonState {

    private final LinkedHashMap<String, Object> kv;
    private final HashMap<String, JsonState> children;


    protected JsonStateMap(LinkedHashMap<String, Object> kv, JsonState parent, Cursor curToMe, JsonState root) {
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

    public JsonState getChild(String key) {
        if (this.children.containsKey(key)) {
            return this.children.get(key);
        } else {
            JsonState child = JsonState.fromObject(kv.get(key), this, whereIAm.enterKey(key), root);
            this.children.put(key, child);
            return child;
        }
    }

    /** Whether this child is folded **/
    public boolean getChildFolded(String key) {
        if (!this.children.containsKey(key)) return false;
        return getChild(key).getFolded();
    }

    @Override
    public JsonState firstChild() {
        SequencedCollection<String> keys = this.getKeysInOrder();
        if (keys.isEmpty()) return null;
        return getChild(keys.getFirst());
    }

    @Override
    public JsonState lastChild() {
        SequencedCollection<String> keys = this.getKeysInOrder();
        if (keys.isEmpty()) return null;
        return getChild(keys.getLast());
    }

    @Override
    public JsonState nextChild(Cursor childCursor) {
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
        JsonState parent = childCursor.getData().getParent();
        if (parent==this) return null;
        return parent.nextChild(childCursor);
    }

    @Override
    public JsonState prevChild(Cursor childCursor) {
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
