package org.example.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

public class ActionMenu {

    private int row = 0;
    // the max allowed row value
    private final int maxRow = Choice.values().length-2;

    public enum Choice {
        // leave the menu visible
        NONE,
        // close the menu, do nothing
        CANCEL,
        // Action choices:
        PARSE_AND_INTERPRET,
        PARSE_IGNORE_ESCAPES,
        COPY,
        ADD_TO_COPY,
        COPY_PATH,
        ADD_PATH_TO_COPY
    }

    public void init() {
        row = 0;
    }

    public void draw(TextGraphics g) {

        var s = g.getSize();
        g = g.newTextGraphics(new TerminalPosition(s.getColumns()-40,3), new TerminalSize(32,s.getRows()));

        String menu =
                        "╭─────────[ ACTION ]─────────╮\n"+
                        "│ p: parse JSON              │\n"+
                        "│ P: parse JSON and escapes  │\n"+
                        "│ c: copy                    │\n"+
                        "│ shift-C: add to copy       │\n"+
                        "│ k: copy key (path)         │\n"+
                        "│ shift-K: add key to copy   │\n"+
                        "├────────────────────────────┤\n"+
                        "│ esc : cancel               │\n"+
                        "╰────────────────────────────╯\n";

        String[] lines = menu.split("\n");
        TerminalPosition top = TerminalPosition.TOP_LEFT_CORNER;
        TerminalPosition pos = top;
        int i=0;
        int highlight = row+1;
        // the final choice is after the separator
        if (row==maxRow) highlight++;
        for (String l : lines) {
            if (i==highlight) {
                // selected row
                g.putString(pos, ">");
                g.putString(pos.withRelativeColumn(1), l.substring(1, l.length()-1), SGR.REVERSE);
                g.putString(pos.withRelativeColumn(l.length()-1), "<");
            } else {
                // rest of the menu
                g.putString(pos, l);
            }
            i++;
            pos = pos.withRelativeRow(1);
        }

    }

    public Choice update(KeyStroke key) {
        if (key.getKeyType()==KeyType.ArrowUp && row>0) {
            row -= 1;
        }
        if (key.getKeyType()==KeyType.ArrowDown && row<maxRow) {
            row++;
        }
        if (key.getKeyType()==KeyType.Escape) {
            return Choice.CANCEL;
        }
        if (key.getKeyType()==KeyType.Enter) {
            if (row==maxRow) return Choice.CANCEL;
            if (row==0) return Choice.PARSE_IGNORE_ESCAPES;
            if (row==1) return Choice.PARSE_AND_INTERPRET;
            if (row==2) return Choice.COPY;
            if (row==3) return Choice.COPY_PATH;
            if (row==4) return Choice.ADD_PATH_TO_COPY;
        }
        if (key.getKeyType()==KeyType.Character) {
            switch (Character.toLowerCase(key.getCharacter())) {
                case 'p': return Choice.PARSE_IGNORE_ESCAPES;
                case 'P': return Choice.PARSE_AND_INTERPRET;
                case 'q': return Choice.CANCEL;
            }
            switch (key.getCharacter()) {
                case 'c': return Choice.COPY;
                case 'C': return Choice.ADD_TO_COPY;
                case 'k': return Choice.COPY_PATH;
                case 'K': return Choice.ADD_PATH_TO_COPY;
            }
        }
        return Choice.NONE;
    }
}