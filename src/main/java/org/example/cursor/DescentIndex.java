package org.example.cursor;

import org.example.Cursor;
import org.example.JsonNode;
import org.example.JsonNodeList;

public class DescentIndex extends DescentStep {
    private final int index;

    public DescentIndex(int index) {
        this.index = index;
    }

    @Override
    public boolean equals(Object rhs) {
        if (!(rhs instanceof DescentIndex)) return false;
        return index == (((DescentIndex) rhs).index);
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

    public Cursor.DescentStyle getDescentStyle() {
        return Cursor.DescentStyle.AN_INDEX;
    }

    @Override
    public String toString() {
        return "[" + this.index + "]";
    }
}
