package org.example;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class JsonStateList extends JsonState {

    private final List<Object> values;
    private final JsonState[] children;

    protected JsonStateList(List<Object> values, JsonState parent, Cursor curToMe, JsonState root) {
        super(parent, curToMe, root);
        this.values = values;
        this.children = new JsonState[values.size()];
    }

    public int size() {
        return this.values.size();
    }

    public JsonState get(int index) {
        if (children[index]==null) {
            children[index] = JsonState.fromObject(values.get(index), this, this.whereIAm.enterIndex(index), this.root);
        }
        return children[index];

    }

    @Override
    public JsonState firstChild() {
        if (values.isEmpty()) return null;
        return get(0);
    }

    @Override
    public JsonState lastChild() {
        if (values.isEmpty()) return null;
        return get(children.length-1);
    }

    @Override
    public JsonState nextChild(Cursor childCursor) {
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
    public JsonState prevChild(Cursor childCursor) {
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
