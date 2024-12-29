package org.example;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

// Tests for the Sorter class directly.
public class SorterTest {

    @Test
    public void testAlphaSort() {
        String[] words = new String[] { "Bob", "Alice" };
        Sorter sort = new Sorter(false, false, false, null);
        List<String> sorted = Arrays.stream(words).sorted(sort).toList();

        assertEquals(2, sorted.size());
        assertEquals("Alice", sorted.get(0));
        assertEquals("Bob", sorted.get(1));
    }

    @Test
    public void testObjSortWithStrings() {
        Object[] words = new Object[] { "Bob", "Alice" };
        Sorter sort = new Sorter(false, false, false, null);
        List<Object> sorted = Arrays.stream(words).sorted(sort).toList();

        assertEquals(2, sorted.size());
        assertEquals("Alice", sorted.get(0));
        assertEquals("Bob", sorted.get(1));
    }

    @Test
    public void testObjSortWithStringsAndNumbers() {
        Object[] words = new Object[] { "Bob", "Alice", 15, 12 };
        Sorter sort = new Sorter(false, false, false, null);
        List<Object> sorted = Arrays.stream(words).sorted(sort).toList();

        assertEquals(4, sorted.size());
        assertEquals(12, sorted.get(0));
        assertEquals(15, sorted.get(1));
        assertEquals("Alice", sorted.get(2));
        assertEquals("Bob", sorted.get(3));
    }

    @Test
    public void testObjSortWithStringsAndNumbersReversed() {
        Object[] words = new Object[] { "Alice", "Bob", 15.1, 12 };
        Sorter sort = new Sorter(true, false, false, null);
        List<Object> sorted = Arrays.stream(words).sorted(sort).toList();

        assertEquals(4, sorted.size());
        assertEquals("Bob", sorted.get(0));
        assertEquals("Alice", sorted.get(1));
        assertEquals(15.1, sorted.get(2));
        assertEquals(12, sorted.get(3));
    }

    @Test
    public void testObjSortWithNumbers() {
        Object[] words = new Object[] { 1, 10, "hi", 2.1, 15.6, -5.1, 11 };
        Sorter sort = new Sorter(false, false, false, null);
        Object[] got = Arrays.stream(words).sorted(sort).toArray();
        Object[] expected = new Object[] { -5.1, 1, 2.1, 10, 11, 15.6, "hi" };

        assertArrayEquals(expected, got);
    }

    @Test
    public void testObjSortWithNumbersReversed() {
        Object[] words = new Object[] { 1, 10, "hi", 2.1, 15.6, -5.1, 11 };
        Sorter sort = new Sorter(true, false, false, null);
        Object[] got = Arrays.stream(words).sorted(sort).toArray();
        Object[] expected = new Object[] { "hi", 15.6, 11, 10, 2.1, 1, -5.1 };

        assertArrayEquals(expected, got);
    }

    @Test
    public void testNaturalSort() {
        Object[] words = new Object[] { "file 1", "file 10", "file 2b", "file 2" };
        Sorter sort = new Sorter(false, false, true, null);
        Object[] got = Arrays.stream(words).sorted(sort).toArray();
        Object[] expected = new Object[] { "file 1", "file 2", "file 2b", "file 10" };

        assertArrayEquals(expected, got);
    }

    @Test
    public void testConversion() {
        String input = "Hello 12 bottles 2b";
        Object[] expected = new Object[] {"Hello ", 12.0, " bottles ", 2.0, "b"};
        Sorter sort = new Sorter(false, false, true, null);

        List<Object> got = sort.translate(input);
        assertArrayEquals(expected, got.toArray(new Object[0]));

        got = sort.translate(input);
        assertArrayEquals(expected, got.toArray(new Object[0]));
    }

    @Test
    public void testSortNumberified() {
        Sorter sort = new Sorter(false, false, true, null);
        String input1 = "Hello 12";
        String input2 = "Hello 12";

        assertEquals(0, sort.compare(input1, input2));
        assertEquals(0, sort.compare(input2, input1));
    }

    @Test
    public void testSortNumberified_empty() {
        Sorter sort = new Sorter(false, false, true, null);
        String input1 = "";
        String input2 = "";

        assertEquals(0, sort.compare(input1, input2));
        assertEquals(0, sort.compare(input2, input1));
    }

    @Test
    public void testSortNumberified_numbers() {
        Sorter sort = new Sorter(false, false, true, null);
        String input1 = "12";
        Double input2 = 12.0;

        assertEquals(0, sort.compare(input1, input2));
        assertEquals(0, sort.compare(input2, input1));
    }

    @Test
    public void testSortNumberified_2() {
        Sorter sort = new Sorter(false, false, true, null);
        String input1 = "hello 1";
        String input2 = "hello 2";

        assertEquals(-1, sort.compare(input1, input2));
        assertEquals(1, sort.compare(input2, input1));
    }

    @Test
    public void testSortNumberified_3() {
        Sorter sort = new Sorter(false, false, true, null);
        String input1 = "1 fish";
        String input2 = "2 fish";

        assertEquals(-1, sort.compare(input1, input2));
        assertEquals(1, sort.compare(input2, input1));
    }

    @Test
    public void testSortNumberified_4() {
        Sorter sort = new Sorter(false, false, true, null);
        String input1 = "fish 2";
        String input2 = "fish 2b";

        assertEquals(-1, sort.compare(input1, input2));
        assertEquals(1, sort.compare(input2, input1));

        // Packing shouldn't change the outcome
        sort.pack();

        assertEquals(-1, sort.compare(input1, input2));
        assertEquals(1, sort.compare(input2, input1));
    }

    @Test
    public void testSortNumberified_5() {
        Sorter sort1 = new Sorter(false, false, true, null);
        Sorter sort2 = new Sorter(false, false, true, "foobar");
        String input1 = "";
        String input2 = "fish";

        assertEquals(-1, sort1.compare(input1, input2));
        assertEquals(1, sort1.compare(input2, input1));

        assertEquals(-1, sort2.compare(input1, input2));
        assertEquals(1, sort2.compare(input2, input1));
    }

    @Test
    public void testSortWithNulls() {
        Sorter sort1 = new Sorter(false, false, false, null);
        Sorter sort2 = new Sorter(false, false, true, null);
        Sorter sort3 = new Sorter(false, false, true, "fancy");
        String input1 = null;
        String input2 = "2 fish";

        assertEquals(1, sort1.compare(input1, input2));
        assertEquals(-1, sort1.compare(input2, input1));

        assertEquals(1, sort2.compare(input1, input2));
        assertEquals(-1, sort2.compare(input2, input1));

        assertEquals(1, sort3.compare(input1, input2));
        assertEquals(-1, sort3.compare(input2, input1));
    }

    @Test
    public void testSortWithTwoNulls() {
        Sorter sort1 = new Sorter(false, false, false, null);
        Sorter sort2 = new Sorter(false, false, true, null);
        Sorter sort3 = new Sorter(false, false, true, "fancy");

        assertEquals(0, sort1.compare(null, null));
        assertEquals(0, sort2.compare(null, null));
        assertEquals(0, sort3.compare(null, null));
    }

    @Test
    public void testSortWithMaps1() {
        Sorter sort = new Sorter(false, false, true, "score");
        Map<String, Object> input1 = new HashMap<>();
        input1.put("name", "Zanzibar");
        input1.put("score", 10);
        Map<String, Object> input2 = new HashMap<>();
        input2.put("name", "Aaron");
        input2.put("score", 20);

        assertTrue(sort.compare(input1, input2) < 0);
        assertTrue(sort.compare(input2, input1) > 0);
    }

    @Test
    public void testSortWithMaps2() {
        Sorter sort = new Sorter(false, false, true, "name");
        Map<String, Object> input1 = new HashMap<>();
        input1.put("name", "Zanzibar");
        input1.put("score", 10);
        Map<String, Object> input2 = new HashMap<>();
        input2.put("name", "Aaron");
        input2.put("score", 20);

        assertTrue(sort.compare(input1, input2) > 0);
        assertTrue(sort.compare(input2, input1) < 0);
    }

    @Test
    public void testSortMapVsString() {
        Sorter sort = new Sorter(false, false, true, "name");
        Map<String, Object> input1 = new HashMap<>();
        input1.put("name", "Zanzibar");
        input1.put("score", 10);
        String input2 = "input2";

        assertTrue(sort.compare(input1, input2) < 0);
        assertTrue(sort.compare(input2, input1) > 0);
    }

    @Test
    public void testSortMapVsInteger() {
        Sorter sort = new Sorter(false, false, true, "name");
        Map<String, Object> input1 = new HashMap<>();
        input1.put("name", "Zanzibar");
        input1.put("score", 10);
        Integer input2 = 222;

        assertTrue(sort.compare(input1, input2) < 0);
        assertTrue(sort.compare(input2, input1) > 0);
    }


}
