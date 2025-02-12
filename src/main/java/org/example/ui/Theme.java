package org.example.ui;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

public class Theme {

    // for auto-generated comments, like element counts.
    public static TextColor synthetic = TextColor.ANSI.CYAN;
    // in maps, color of the key
    public static TextColor key = TextColor.ANSI.BLUE_BRIGHT;
    // strings from the input
    public static TextColor value_str = TextColor.ANSI.YELLOW;
    public static TextColor value_num = TextColor.ANSI.GREEN_BRIGHT;
    public static TextColor value_null = TextColor.ANSI.WHITE;

    public static TextGraphics withColor(TextGraphics g, TextColor c) {
        TextGraphics ret = g.newTextGraphics(TerminalPosition.TOP_LEFT_CORNER, g.getSize());
        ret.setForegroundColor(c);
        return ret;
    }

}
