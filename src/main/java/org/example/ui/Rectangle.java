package org.example.ui;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.terminal.Terminal;

public class Rectangle {
    // bottom-left
    static int BL=0;
    // bottom-right
    static int BR=1;
    // horizontal
    static int H=3;
    // top-left
    static int TL=5;
    // top-right
    static int TR=6;
    // vertical
    static int V=7;
    // horizontal and center down
    static int HD=8;
    // horizontal + vertical
    static int HV=9;
    // horizontal and center up
    static int HU=10;
    static char[] BOLD  ="┗┛┣━┫┏┓┃┳╋┻".toCharArray();
    static char[] SINGLE="└┘├─┤┌┐│┬┼┴".toCharArray();
    static char[] DOUBLE="╚╝╠═╣╔╗║╦╬╩".toCharArray();
    static char[] ROUNDED="╰╯├─┤╭╮│┬┼┴".toCharArray();

    public static void drawBold(TextGraphics g, TerminalPosition  pos, TerminalSize size) {
        draw(g,pos,size, BOLD);
    }

    public static void drawSingle(TextGraphics g, TerminalPosition  pos, TerminalSize size) {
        draw(g,pos,size, SINGLE);
    }

    public static void drawDouble(TextGraphics g, TerminalPosition  pos, TerminalSize size) {
        draw(g,pos,size, DOUBLE);
    }

    public static void drawRounded(TextGraphics g, TerminalPosition  pos, TerminalSize size) {
        draw(g,pos,size, ROUNDED);
    }

    private static void draw(TextGraphics g, TerminalPosition pos, TerminalSize size, char[] chars) {
        TerminalPosition right = pos.withRelativeColumn(size.getColumns()-1);
        g.drawLine(pos, pos.withRelativeColumn(size.getColumns()-1), BOLD[H]);
        g.drawLine(pos.withRelativeRow(size.getRows()-1), pos.withRelativeRow(size.getRows()-1).withRelativeColumn(size.getColumns()-1), BOLD[H]);
        g.drawLine(pos.withRelativeRow(1), pos.withRelativeRow(size.getRows()-1), BOLD[V]);
        g.drawLine(right.withRelativeRow(1), right.withRelativeRow(size.getRows()-1), BOLD[V]);
        g.putString(pos, ""+BOLD[TL]);
        g.putString(right, ""+BOLD[TR]);
        g.putString(pos.withRelativeRow(size.getRows()-1), ""+BOLD[BL]);
        g.putString(pos.withRelativeRow(size.getRows()-1).withRelativeColumn(size.getColumns()-1), ""+BOLD[BR]);
    }
}
