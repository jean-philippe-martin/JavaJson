package org.example.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

public class ActionMenu {

    private int row = 0;
    // the max allowed row value
    private final int maxRow = 2;

    public enum Choice {
        // leave the menu visible
        NONE,
        // close the menu, do nothing
        CANCEL,
        // Action choices:
        PARSE,
        COPY,
    }

    public void init() {
        row = 0;
    }

    public void draw(TextGraphics g) {

        String menu =
                        "╭─────────[ ACTION ]─────────╮\n"+
                        "│ p: parse JSON              │\n"+
                        "│ c: copy                    │\n"+
                        "├────────────────────────────┤\n"+
                        "│ esc : cancel               │\n"+
                        "╰────────────────────────────╯\n";

        String[] lines = menu.split("\n");
        TerminalPosition top = TerminalPosition.TOP_LEFT_CORNER.withRelativeColumn(4).withRelativeRow(1);
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
            if (row==0) return Choice.PARSE;
            if (row==1) return Choice.COPY;
        }
        if (key.getKeyType()==KeyType.Character) {
            switch (Character.toLowerCase(key.getCharacter())) {
                case 'p': return Choice.PARSE;
                case 'c': return Choice.COPY;
                case 'q': return Choice.CANCEL;
            }
        }
        return Choice.NONE;
    }
}