package org.example;

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

    public static String toString(double value, UNITS unit) {
        return String.format("%.2f %s", value, Conversions.toString(unit));
    }

    private static String toString(UNITS unit) {
        return switch (unit) {
            case UNITS.MILLISECONDS -> "ms";
            case UNITS.SECONDS -> "s";
            case UNITS.MINUTES -> "min";
            case UNITS.HOURS -> "h";
            case UNITS.DAYS -> "d";
        };
    }

    private static double secondsIn(double value, UNITS sourceUnit) {
        return switch (sourceUnit) {
            case UNITS.MILLISECONDS -> value / 1000.0;
            case UNITS.SECONDS -> value;
            case UNITS.MINUTES -> value * 60;
            case UNITS.HOURS -> value * 3600;
            case UNITS.DAYS -> value * 3600 * 24;
        };
    }
}
