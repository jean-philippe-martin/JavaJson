package org.example.ui;

import java.util.regex.Pattern;

public class TextWidth {

    private static final Pattern FULLWIDTH_PATTERN = Pattern.compile("[\\p{InCJKUnifiedIdeographs}\\p{InCJKSymbolsAndPunctuation}\\p{InHiragana}\\p{InKatakana}\\p{InHangulSyllables}]");

    public static int length(String text) {
        int len = 0;
        for (char c : text.toCharArray()) {
          len += spacesTakenBy(c);
        }
        return len;
    }

    private static int spacesTakenBy(char c) {
        if (FULLWIDTH_PATTERN.matcher(String.valueOf(c)).matches() || isKoreanCharacter(c) || isChineseCharacter(c)) return 2;
        return 1;
    }

    private static boolean isKoreanCharacter(char ch) {
        return (ch >= 0xAC00 && ch <= 0xD7AF) || // Hangul Syllables
                (ch >= 0x1100 && ch <= 0x11FF) || // Hangul Jamo
                (ch >= 0x3130 && ch <= 0x318F) || // Hangul Compatibility Jamo
                (ch >= 0xA960 && ch <= 0xA97F) || // Hangul Jamo Extended-A
                (ch >= 0xD7B0 && ch <= 0xD7FF);   // Hangul Jamo Extended-B
    }

    private static boolean isChineseCharacter(char ch) {
        return (ch >= '\u4E00' && ch <= '\u9FFF') || // CJK Unified Ideographs
                (ch >= '\u3400' && ch <= '\u4DBF') || // CJK Unified Ideographs Extension A
                (ch >= '\uF900' && ch <= '\uFAFF') ||   // CJK Compatibility Ideographs
                (ch >= '\u3000' && ch <= '\u303F');   // CJK Symbols and Punctuation
    }
}
