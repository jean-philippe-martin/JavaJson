package org.example.cursor;

import org.example.Cursor;
import org.example.JsonNode;

import java.util.Objects;

// This is meant for multicursor
public class DescentAll extends DescentStep {
    private final Cursor.DescentStyle style;

    public DescentAll(Cursor.DescentStyle style) {
        if (style != Cursor.DescentStyle.ALL_INDICES && style != Cursor.DescentStyle.ALL_KEYS)
            throw new RuntimeException("wrong descent style");
        this.style = style;
    }

    public JsonNode apply(JsonNode node) {
        throw new RuntimeException("Cannot get one cursor from descending a wildcard step.");
    }

    /** get the first child of the node, or null if none exist **/
    public JsonNode applyFirst(JsonNode node) {
        var it = node.iterateChildren();
        while (it!=null) {
            if (it.isAggregate()) {
                it = it.next();
                continue;
            }
            return it.get();
        }
        return null;
    }

    public Cursor.DescentStyle getDescentStyle() {
        return style;
    }

    @Override
    public String toString() {

        if (style == Cursor.DescentStyle.ALL_INDICES) return "[*]";
        return ".*";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DescentAll)) return false;
        DescentAll that = (DescentAll) o;
        return style == that.style;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(style);
    }
}
