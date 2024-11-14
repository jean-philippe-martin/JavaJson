package org.example;

import java.util.List;

public class JsonNodeList extends JsonNode {

    private final List<Object> values;
    private final JsonNode[] children;

    protected JsonNodeList(List<Object> values, JsonNode parent, Cursor curToMe, JsonNode root) {
        super(parent, curToMe, root);
        this.values = values;
        this.children = new JsonNode[values.size()];
    }

    public int size() {
        return this.values.size();
    }

    public JsonNode get(int index) {
        if (children[index]==null) {
            children[index] = JsonNode.fromObject(values.get(index), this, this.whereIAm.enterIndex(index), this.root);
        }
        return children[index];

    }

    @Override
    public JsonNode firstChild() {
        if (values.isEmpty()) return null;
        return get(0);
    }

    @Override
    public JsonNode lastChild() {
        if (values.isEmpty()) return null;
        return get(children.length-1);
    }

    @Override
    public JsonNode nextChild(Cursor childCursor) {
        childCursor = childCursor.truncate(this);
        if (childCursor.getData().parent!=this) {
            throw new RuntimeException("invalid cursor: should be a child of this JSON node");
        }
        switch (childCursor.getStep()) {
            case Cursor.DescentIndex di -> {
                int index = di.get();
                if (index+1>=this.values.size()) {
                    // out of bounds
                    return null;
                }
                return this.get(index+1);
            }
            default -> throw new RuntimeException("invalid cursor");
        }
    }

    @Override
    public JsonNode prevChild(Cursor childCursor) {
        childCursor = childCursor.truncate(this);
        if (childCursor.getData().parent!=this) {
            throw new RuntimeException("invalid cursor: should be a child of this JSON node");
        }
        switch (childCursor.getStep()) {
            case Cursor.DescentIndex di -> {
                int index = di.get();
                if (index<=0) {
                    // out of bounds
                    return null;
                }
                return this.get(index-1);
            }
            default -> throw new RuntimeException("invalid cursor");
        }
    }


}
