package org.example.ui;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

public class Theme {

    // for auto-generated comments, like element counts.
    public TextColor synthetic = TextColor.ANSI.CYAN;
    // in maps, color of the key
    public TextColor key = TextColor.ANSI.BLUE_BRIGHT;
    // strings from the input
    public TextColor value_str = TextColor.ANSI.YELLOW;
    public TextColor value_num = // TextColor.ANSI.GREEN_BRIGHT;
      TextColor.Indexed.fromRGB(0, 128, 0);
    public TextColor value_null = TextColor.ANSI.WHITE;
    // row that's about to be deleted
    public TextColor deleting_row_fg = TextColor.ANSI.BLACK;
    public TextColor deleting_row_bg = TextColor.ANSI.BLACK_BRIGHT;
    // menu option that is disabled
    public TextColor disabled_option = TextColor.ANSI.BLACK_BRIGHT;

    public TextColor normal_fg = TextColor.ANSI.WHITE_BRIGHT;
    public TextColor normal_bg = TextColor.ANSI.BLACK;

    // The selected theme.
    public static Theme selected = make_bw();

    public static TextGraphics withColor(TextGraphics g, TextColor c) {
        TextGraphics ret = clone(g);
        ret.setForegroundColor(c);
        return ret;
    }

    public static TextGraphics clone(TextGraphics g) {
        return g.newTextGraphics(TerminalPosition.TOP_LEFT_CORNER, g.getSize());
    }

    private static Theme make_dark() {
        Theme dark = new Theme();
        // for auto-generated comments, like element counts.
        dark.synthetic = TextColor.ANSI.CYAN;
        // in maps, color of the key
        dark.key = TextColor.ANSI.BLUE_BRIGHT;
        // strings from the input
        dark.value_str = TextColor.ANSI.YELLOW;
        dark.value_num = TextColor.ANSI.GREEN_BRIGHT;
        dark.value_null = TextColor.ANSI.WHITE;
        // row that's about to be deleted
        dark.deleting_row_fg = TextColor.ANSI.BLACK;
        dark.deleting_row_bg = TextColor.ANSI.BLACK_BRIGHT;
        // menu option that is disabled
        dark.disabled_option = TextColor.ANSI.BLACK_BRIGHT;
        dark.normal_fg = TextColor.ANSI.WHITE_BRIGHT;
        dark.normal_bg = TextColor.ANSI.BLACK;
        return dark;
    }

    private static Theme make_light() {
        Theme light = new Theme();
        // for auto-generated comments, like element counts.
        light.synthetic = TextColor.ANSI.BLUE;
        // in maps, color of the key
        light.key = TextColor.ANSI.BLUE_BRIGHT;
        // strings from the input
        light.value_str = TextColor.Indexed.fromRGB(64, 64, 0);
        light.value_num = TextColor.Indexed.fromRGB(0, 128, 0);
        light.value_null = TextColor.ANSI.BLACK;
        // row that's about to be deleted
        light.deleting_row_fg = TextColor.ANSI.BLACK;
        light.deleting_row_bg = TextColor.ANSI.BLACK_BRIGHT;
        // menu option that is disabled
        light.disabled_option = TextColor.ANSI.BLACK_BRIGHT;
        light.normal_fg = TextColor.ANSI.BLACK;
        light.normal_bg = TextColor.ANSI.WHITE_BRIGHT;
        return light;
    }

    private static Theme make_bw() {
        Theme light = new Theme();
        light.normal_fg = TextColor.ANSI.BLACK;
        light.normal_bg = TextColor.ANSI.WHITE_BRIGHT;
        // for auto-generated comments, like element counts.
        light.synthetic = light.normal_fg;
        // in maps, color of the key
        light.key = light.normal_fg;
        // strings from the input
        light.value_str = light.normal_fg;
        light.value_num = light.normal_fg;
        light.value_null = light.normal_fg;
        // row that's about to be deleted
        light.deleting_row_fg = light.normal_bg;
        light.deleting_row_bg = light.normal_bg;
        // menu option that is disabled
        light.disabled_option = TextColor.ANSI.BLACK_BRIGHT;
        return light;
    }

    private static Theme make_wb() {
        Theme dark = new Theme();
        dark.normal_fg = TextColor.ANSI.WHITE_BRIGHT;
        dark.normal_bg = TextColor.ANSI.BLACK;
        // for auto-generated comments, like element counts.
        dark.synthetic = dark.normal_fg;
        // in maps, color of the key
        dark.key = dark.normal_fg;
        // strings from the input
        dark.value_str = dark.normal_fg;
        dark.value_num = dark.normal_fg;
        dark.value_null = dark.normal_fg;
        // row that's about to be deleted
        dark.deleting_row_fg = dark.normal_bg;
        dark.deleting_row_bg = dark.normal_bg;
        // menu option that is disabled
        dark.disabled_option = TextColor.ANSI.BLACK_BRIGHT;
        return dark;
    }

    public static Theme getTheme(String name) {
        switch (name.toUpperCase()) {
            case "DARK":
                return Theme.make_dark();
            case "LIGHT":
                return Theme.make_light();
            case "BW":
                // Black on white
                return Theme.make_bw();
            case "WB":
                // White on black
                return Theme.make_wb();
            default:
                throw new RuntimeException("Unknown theme: '" + name + "'. Valid values are: DARK,LIGHT,BW,WB");
        }
    }

}
