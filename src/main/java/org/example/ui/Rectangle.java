package org.example.ui;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;

public class Rectangle {
    static int BL=0;
    static int BR=1;
    static int H=3;
    static int TL=5;
    static int TR=6;
    static int V=7;
    static int HD=8;
    static int HV=9;
    static int HU=10;
    static char[] BOLD  ="┗┛┣━┫┏┓┃┳╋┻".toCharArray();
    static char[] SINGLE="└┘├─┤┌┐│┬┼┴".toCharArray();
    static char[] DOUBLE="╚╝╠═╣╔╗║╦╬╩".toCharArray();
    static char[] circle="╰╯├─┤╭╮│┬┼┴".toCharArray();

    public static void drawSingle(TextGraphics g, TerminalPosition  pos, TerminalSize size) {
        g.drawLine(pos, pos.withRelativeColumn(size.getColumns()-1), BOLD[H]);
        g.drawLine(pos.withRelativeRow(size.getRows()-1), pos.withRelativeRow(size.getRows()-1).withRelativeColumn(size.getColumns()-1), BOLD[H]);
        g.putString(pos.withRelativeRow(1), ""+BOLD[V]);
        g.putString(pos.withRelativeColumn(size.getColumns()-1).withRelativeRow(1), ""+BOLD[V]);
        g.putString(pos, ""+BOLD[TL]);
        g.putString(pos.withRelativeColumn(size.getColumns()-1), ""+BOLD[TR]);
        g.putString(pos.withRelativeRow(size.getRows()-1), ""+BOLD[BL]);
        g.putString(pos.withRelativeRow(size.getRows()-1).withRelativeColumn(size.getColumns()-1), ""+BOLD[BR]);

    }
}
