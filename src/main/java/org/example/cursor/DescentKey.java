package org.example.cursor;

import org.example.Cursor;
import org.example.JsonNode;
import org.example.JsonNodeMap;

public class DescentKey extends DescentStep {
    private final String key;

    public DescentKey(String key) {
        this.key = key;
    }

    @Override
    public boolean equals(Object rhs) {
        if (!(rhs instanceof DescentKey)) return false;
        if (key == null) return ((DescentKey) rhs).key == null;
        return key.equals(((DescentKey) rhs).key);
    }

    @Override
    public int hashCode() {
        if (null == key) return 0;
        return key.hashCode();
    }

    public String get() {
        return this.key;
    }

    public JsonNode apply(JsonNode node) {
        return ((JsonNodeMap) node).getChild(key);
    }

    public Cursor.DescentStyle getDescentStyle() {
        return Cursor.DescentStyle.A_KEY;
    }

    @Override
    public String toString() {
        return "." + this.key;
    }
}
