package org.example;

public class JsonStateValue<T> extends JsonState {
    protected T value;

    protected JsonStateValue(T value, JsonState parent, Cursor curToMe, JsonState root) {
        super(parent, curToMe, root);
        this.value = value;
    }

    public T getValue() {
        return this.value;
    }

    @Override
    public JsonState firstChild() {
        return null;
    }

    @Override
    public JsonState lastChild() {
        return null;
    }

    @Override
    public JsonState nextChild(Cursor pointingToAChild) {
        return null;
    }

    @Override
    public JsonState prevChild(Cursor pointingToAChild) {
        return null;
    }

}
