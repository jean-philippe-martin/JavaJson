package org.example;

import org.example.cursor.DescentKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class JsonNodeValue<T> extends JsonNode {
    protected T value;

    // Use the builder to build complicated structures from the bottom up,
    // where you don't know in advance who the parent will be.
    public static class Builder<T> implements JsonNodeBuilder {
        protected T value;

        public Builder(T newValue) {
            this.value = newValue;
        }

        @Override
        public JsonNodeValue<T> build(JsonNode parent, Cursor curToMe) {
            JsonNode root = null;
            if (null!=parent) root = parent.rootInfo.root;
            return new JsonNodeValue<>(value, parent, curToMe, root);
        }
    }

    protected JsonNodeValue(T value, JsonNode parent, Cursor curToMe, JsonNode root) {
        super(parent, curToMe, root);
        this.value = value;
        this.folded = false;
        autoAnnotate();
    }

    private void autoAnnotate() {
        if (this.whereIAm==null || !(this.whereIAm.getStep() instanceof DescentKey)) return;
        String key = ((DescentKey)this.whereIAm.getStep()).get();
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
                double mins = asDouble();
                Conversions.UNITS unit = Conversions.bestUnit(mins, Conversions.UNITS.MINUTES);
                annotation = Conversions.toString(Conversions.convert(mins, Conversions.UNITS.MINUTES, unit), unit);
            } catch (NumberFormatException _x) {
                // value isn't a number, just do nothing
                return;
            }
        }
        if (key.endsWith("_hours")
            || key.endsWith("Hours")) {
            try {
                double hours = asDouble();
                Conversions.UNITS unit = Conversions.bestUnit(hours, Conversions.UNITS.HOURS);
                if (unit != Conversions.UNITS.HOURS) {
                    annotation = Conversions.toString(Conversions.convert(hours, Conversions.UNITS.HOURS, unit), unit);
                }
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
        if (key.endsWith("On") || key.endsWith("At")|| key.endsWith("_on") || key.endsWith("_at")
                || key.endsWith("_epoch") ||  key.endsWith("_timestamp") || key.endsWith("Timestamp")
                || "timestamp".equalsIgnoreCase(key)) {
            // "CreatedOn", "bootedOn", etc. Assume we are getting a time value in epoch smth.
            Date date = Conversions.nodeToDate(this);
            if (date!=null) {
                // We were actually able to parse the date! Let's convert to local timezone.
                annotation = Conversions.dateToString(date);
            }
        }
    }

    public @Nullable Double asDouble() throws NumberFormatException {
        double secs;
        if (value instanceof Double) {
            secs = (Double)value;
        } else if (value instanceof Integer) {
            secs = ((Integer)value).doubleValue();
        } else if (value instanceof Long) {
            secs = ((Long)value).doubleValue();
        } else if (value instanceof Float) {
            secs = ((Float)value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException _ex) {
                // not a number after all
                return null;
            }
        } else {
            // we don't recognize the type, don't know what to do with it
            return null;
        }
        return secs;
    }

    @Override
    public T getValue() {
        return this.value;
    }

    @Override
    public @Nullable JsonNodeIterator iterateChildren(boolean _includeAggregates) {
        return null;
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

    @Override
    public void sort(Sorter sorter) {
        // Nothing to do
    }

    @Override
    public void unsort() {
        // Nothing to do
    }

    @Override
    public @Nullable Sorter getSort() {
        return null;
    }

    @Override
    public @NotNull JsonNode replaceChild(Cursor toKid, JsonNodeBuilder kid) {
        throw new RuntimeException("no can do");
    }

}
