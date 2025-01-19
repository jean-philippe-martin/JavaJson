package org.example.ui;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

public class Theme {

    public static TextColor synthetic = TextColor.ANSI.GREEN_BRIGHT;

    public static TextGraphics withColor(TextGraphics g, TextColor c) {
        TextGraphics ret = g.newTextGraphics(TerminalPosition.TOP_LEFT_CORNER, g.getSize());
        ret.setForegroundColor(c);
        return ret;
    }

}
