package org.example.ui;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Text;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TextWithTest {

    @Test
    public void testEnglishString() {
        assertEquals(5, TextWidth.length("hello"));
    }

    @Test
    public void testKoreanString() {
        assertEquals(8, TextWidth.length("달아나지"));
        assertEquals(6, TextWidth.length("도토리"));
    }

    @Test
    public void testChineseString() {
        assertEquals(4, TextWidth.length("历史"));
    }

    @Test
    public void testFunkyQuotes() {
        assertEquals(6, TextWidth.length("《hi》"));
    }
}
