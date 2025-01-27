package org.example;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Conversions {
    public enum UNITS {
        MILLISECONDS,
        SECONDS,
        MINUTES,
        HOURS,
        DAYS
    }

    public static double convert(double value, UNITS source, UNITS destination) {
        double seconds = secondsIn(value, source);
        double oneSecondIs = 1.0 / secondsIn(1, destination);
        return seconds * oneSecondIs;
    }

    public static UNITS bestUnit(double value, UNITS source) {
        UNITS bestUnit = UNITS.MILLISECONDS;
        double bestScore = Double.MAX_VALUE;
        for (UNITS unit : UNITS.values()) {
            double score = Math.abs(Math.log10(convert(value, source, unit)));
            if (score<bestScore) {
                bestScore = score;
                bestUnit = unit;
            }
        }
        return bestUnit;
    }

    public static @NotNull String toString(double value, UNITS unit) {
        return String.format("%.2f %s", value, Conversions.toString(unit));
    }

    /** guesses if it's seconds, milliseconds, or microseconds since epoch. */
    public static @NotNull Date epochToDate(long sinceEpoch) {
        if (sinceEpoch >= 100000000000000L) {
            // large number, assume microseconds (1E-6s) since epoch
            sinceEpoch = (long) (sinceEpoch / 1e6);
        } else if (sinceEpoch < 10000000000L) {
            // small number, must be seconds since epoch
            sinceEpoch = sinceEpoch * 1000;
        }
        // Now we have ms since epoch, can convert to a Date
        return new Date(sinceEpoch);
    }

    // convert either a string or a seconds/ms/us-since-epoch.
    public static @Nullable Date nodeToDate(@Nullable JsonNodeValue node) {
        if (null==node) return null;
        try {
            double sinceEpoch = node.asDouble();
            long somethingSinceEpoch = (long)sinceEpoch;
            return Conversions.epochToDate(somethingSinceEpoch);
        } catch (NumberFormatException | NullPointerException _x) {
            // value isn't a number. Maybe it's a string?
            if (!(node.getValue() instanceof String)) return null;
            return stringToDate((String)node.getValue());
        }
    }

    // parse a date if it's just the right way
    public static @Nullable Date stringToDate(@Nullable String maybeDate) {
        if (null==maybeDate) return null;
        // try to interpret as a date
        SimpleDateFormat guess = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.ENGLISH);
        guess.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = guess.parse(maybeDate, new ParsePosition(0));
        return date;
    }

    // But importantly, string in the format & timezone we want.
    public static @NotNull String dateToString(@NotNull Date date) {
        SimpleDateFormat ourFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSS Z", java.util.Locale.ENGLISH);
        // no need, we already default to the local timezone
//        ZoneId ourTimeZone = ZonedDateTime.now().getZone();
//        ourFormat.setTimeZone(TimeZone.getTimeZone(ourTimeZone));
        return ourFormat.format(date);
    }

    private static @NotNull String toString(UNITS unit) {
        switch (unit) {
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            case MINUTES:
                return "min";
            case HOURS:
                return "h";
            case DAYS:
                return "d";
            default:
                throw new IllegalArgumentException();
        }
    }

    private static double secondsIn(double value, UNITS sourceUnit) {
        switch (sourceUnit) {
            case MILLISECONDS:
                return value / 1000.0;
            case SECONDS:
                return value;
            case MINUTES:
                return value * 60;
            case HOURS:
                return value * 3600;
            case DAYS:
                return value * 3600 * 24;
        };
        throw new IllegalArgumentException();
    }
}
