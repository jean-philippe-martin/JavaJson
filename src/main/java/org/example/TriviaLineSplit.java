package org.example;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Splits verbatim Hjson trivia on the first line break so callers can tell same-line vs
 * continuation (after newline) regions. Newlines are preserved in {@link #fromFirstNewlineInclusive}.
 */
public final class TriviaLineSplit {
    public static final TriviaLineSplit EMPTY = new TriviaLineSplit("", "");

    /** Text before the first {@code \n}, {@code \r\n}, or bare {@code \r}. */
    public final @NotNull String sameLineBeforeFirstNewline;
    /** From the first line break through the end of the span (inclusive of that newline). */
    public final @NotNull String fromFirstNewlineInclusive;

    public TriviaLineSplit(@NotNull String sameLineBeforeFirstNewline, @NotNull String fromFirstNewlineInclusive) {
        this.sameLineBeforeFirstNewline = sameLineBeforeFirstNewline;
        this.fromFirstNewlineInclusive = fromFirstNewlineInclusive;
    }

    public static @NotNull TriviaLineSplit split(@Nullable String verbatim) {
        if (verbatim == null || verbatim.isEmpty()) {
            return EMPTY;
        }
        int idx = indexOfLineBreak(verbatim);
        if (idx < 0) {
            return new TriviaLineSplit(verbatim, "");
        }
        return new TriviaLineSplit(verbatim.substring(0, idx), verbatim.substring(idx));
    }

    private static int indexOfLineBreak(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') {
                return i;
            }
            if (c == '\r') {
                return i;
            }
        }
        return -1;
    }
}
