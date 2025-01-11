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
