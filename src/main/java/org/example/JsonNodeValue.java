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
        if (key.endsWith("_seconds") || key.endsWith("_sec")
                || key.endsWith("Seconds") || key.endsWith("Sec")) {
            try {
                double secs = asDouble();
                Conversions.UNITS unit = Conversions.bestUnit(secs, Conversions.UNITS.SECONDS);
                annotation = Conversions.toString(Conversions.convert(secs, Conversions.UNITS.SECONDS, unit), unit);
            } catch (NumberFormatException _x) {
                // value isn't a number, just do nothing
                return;
            }
        }
        if (key.endsWith("_minutes") || key.endsWith("_mins")
            || key.endsWith("Minutes") || key.endsWith("Mins")) {
            try {
                double secs = asDouble();
                Conversions.UNITS unit = Conversions.bestUnit(secs, Conversions.UNITS.MINUTES);
                annotation = Conversions.toString(Conversions.convert(secs, Conversions.UNITS.SECONDS, unit), unit);
            } catch (NumberFormatException _x) {
                // value isn't a number, just do nothing
                return;
            }
        }
        if (key.endsWith("_hours")
            || key.endsWith("Hours")) {
            try {
                double secs = asDouble();
                Conversions.UNITS unit = Conversions.bestUnit(secs, Conversions.UNITS.HOURS);
                annotation = Conversions.toString(Conversions.convert(secs, Conversions.UNITS.SECONDS, unit), unit);
            } catch (NumberFormatException _x) {
                // value isn't a number, just do nothing
                return;
            }
        }
        if (key.endsWith("_days") || key.endsWith("Days")) {
            try {
                double secs = asDouble();
                Conversions.UNITS unit = Conversions.bestUnit(secs, Conversions.UNITS.DAYS);
                annotation = Conversions.toString(Conversions.convert(secs, Conversions.UNITS.SECONDS, unit), unit);
            } catch (NumberFormatException _x) {
                // value isn't a number, just do nothing
                return;
            }
        }
    }

    public Double asDouble() throws NumberFormatException {
        double secs;
        if (value instanceof Double d) {
            secs = d;
        } else if (value instanceof Integer i) {
            secs = i.doubleValue();
        } else if (value instanceof Float f) {
            secs = f.doubleValue();
        } else if (value instanceof String s) {
            return Double.parseDouble(s);
        } else {
            // we don't recognize the type, don't know what to do with it
            return null;
        }
        return secs;
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
