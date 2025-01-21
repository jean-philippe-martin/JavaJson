package org.example;

import org.jetbrains.annotations.Nullable;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

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
        if (key.endsWith("On") || key.endsWith("_epoch") || key.endsWith("_timestamp") || "timestamp".equalsIgnoreCase(key)) {
            // "CreatedOn", "bootedOn", etc. Assume we are getting a time value in epoch smth.
            try {
                double sinceEpoch = asDouble();
                long somethingSinceEpoch = (long)sinceEpoch;
                Date date = Conversions.epochToDate(somethingSinceEpoch);
                annotation = Conversions.dateToString(date);
            } catch (NumberFormatException | NullPointerException _x) {
                // value isn't a number, just do nothing
                return;
            }
        }
        if (key.endsWith("_at") && (value instanceof String)) {
            // try to interpret as a date
            SimpleDateFormat guess = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.ENGLISH);
            guess.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = guess.parse((String)value, new ParsePosition(0));
            if (date!=null) {
                // We were actually able to parse the date! Let's convert to local timezone.
                annotation = Conversions.dateToString(date);
            }
        }
    }

    public Double asDouble() throws NumberFormatException {
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
            return Double.parseDouble((String)value);
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
    public @Nullable JsonNodeIterator iterateChildren() {
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

}
