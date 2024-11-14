package org.example;

public class JsonNodeValue<T> extends JsonNode {
    protected T value;

    protected JsonNodeValue(T value, JsonNode parent, Cursor curToMe, JsonNode root) {
        super(parent, curToMe, root);
        this.value = value;
        autoAnnotate();
    }

    private void autoAnnotate() {
        if (this.whereIAm==null || !(this.whereIAm.getStep() instanceof Cursor.DescentKey)) return;
        String key = ((Cursor.DescentKey)this.whereIAm.getStep()).get();
        if (key.endsWith("_seconds")) {
            double secs = (double) value;
            Conversions.UNITS unit = Conversions.bestUnit(secs, Conversions.UNITS.SECONDS);
            annotation = Conversions.toString(Conversions.convert(secs, Conversions.UNITS.SECONDS, unit), unit);
        }
    }

    public T getValue() {
        return this.value;
    }

    @Override
    public JsonNode firstChild() {
        return null;
    }

    @Override
    public JsonNode lastChild() {
        return null;
    }

    @Override
    public JsonNode nextChild(Cursor pointingToAChild) {
        return null;
    }

    @Override
    public JsonNode prevChild(Cursor pointingToAChild) {
        return null;
    }

}
