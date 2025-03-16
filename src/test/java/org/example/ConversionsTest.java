package org.example;



import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.Date;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConversionsTest {

    @Test
    public void testConversion1() {
        double ret = Conversions.convert(20, Conversions.UNITS.MINUTES, Conversions.UNITS.SECONDS);
        assertEquals(20*60.0, ret, 0.01);
    }

    @Test
    public void testConversion2() {
        double ret = Conversions.convert(20, Conversions.UNITS.HOURS, Conversions.UNITS.MINUTES);
        assertEquals(20*60.0, ret, 0.01);
    }

    @Test
    public void testConversion3() {
        double ret = Conversions.convert(20, Conversions.UNITS.DAYS, Conversions.UNITS.MILLISECONDS);
        assertEquals(20*24*60*60*1000.0, ret, 0.01);
    }

    @Test
    public void testConversion4() {
        double ret = Conversions.convert(20, Conversions.UNITS.MILLISECONDS, Conversions.UNITS.DAYS);
        assertEquals(1.0/(20*24*60*60*1000.0), ret, 0.000001);
    }

    @Test
    public void testUnitGuess() {
        Conversions.UNITS got = Conversions.bestUnit(60.0, Conversions.UNITS.SECONDS);
        assertEquals(Conversions.UNITS.MINUTES, got);
    }

    @Test
    public void testUnitGuess2() {
        Conversions.UNITS got = Conversions.bestUnit(120.0, Conversions.UNITS.MINUTES);
        assertEquals(Conversions.UNITS.HOURS, got);
    }

    // No need to set the exact format in stone,
    // but at least toString shouldn't throw an exception.
    @Test
    public void testToStringDoesNotExplode() {
        for (Conversions.UNITS u : Conversions.UNITS.values()) {
            String s = Conversions.toString(12.5, u);
            assertNotEquals("", s);
        }
    }

    @Test
    public void testParseDate() {
        String candidate = "Tue, 3 Feb 2025 23:22:21 GMT";
        Date converted = Conversions.stringToDate(candidate);
        assertNotNull(converted);
        assertEquals(23, converted.toInstant().atOffset(ZoneOffset.UTC).get(ChronoField.HOUR_OF_DAY));
        assertEquals(22, converted.toInstant().atOffset(ZoneOffset.UTC).get(ChronoField.MINUTE_OF_HOUR));
        assertEquals(21, converted.toInstant().atOffset(ZoneOffset.UTC).get(ChronoField.SECOND_OF_MINUTE));
    }

    @Test
    public void testParseDate2() {
        String candidate = "2022-11-08T23:29:25Z";
        Date converted = Conversions.stringToDate(candidate);
        assertNotNull(converted);
        assertEquals(23, converted.toInstant().atOffset(ZoneOffset.UTC).get(ChronoField.HOUR_OF_DAY));
        assertEquals(29, converted.toInstant().atOffset(ZoneOffset.UTC).get(ChronoField.MINUTE_OF_HOUR));
        assertEquals(25, converted.toInstant().atOffset(ZoneOffset.UTC).get(ChronoField.SECOND_OF_MINUTE));
    }

}