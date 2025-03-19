package org.example.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.jetbrains.annotations.Nullable;

public class MainMenu {

    private int row = 0;
    // the max allowed row value
    private final int maxRow = Choice.values().length-2;

    public enum Choice {
        // leave the menu visible
        NONE,
        // close the menu, do nothing
        CANCEL,
        // Action choices:
        ACTION,
        PASTE,
        FIND,
        SORT,
        AGGREGATE,
        UNION,
        GROUPBY,
        HELP,
        QUIT
    }

    private final String[] help = new String[]{
            "Menu to act on the current value",
            "Select this, then paste the new document in",
            "Menu to find keys/values",
            "Menu to sort values in this list or object",
            "Menu to show information about this list/object",
            "Your cursors must point to arrays",
            "Your cursor(s) must be in an object in a list",
            "Press any key to exit the help screen",
            "Quits this program",
            "Close this menu",
            null,
    };

    public void init() {
        row = 0;
    }

    public void draw(TextGraphics g) {

        var s = g.getSize();
        g = g.newTextGraphics(new TerminalPosition(s.getColumns()-40,3), new TerminalSize(32,s.getRows()));

        String menu =
                    "╭───────────[ MENU ]──────────╮\n"+
                    "│ (ENTER): change value       │\n"+
                    "│ v: paste new document       │\n"+
                    "│ f: find                     │\n"+
                    "│ s: sort                     │\n"+
                    "│ a: aggregate                │\n"+
                    "│ +: union selected arrays    │\n"+
                    "│ g: group by selected key(s) │\n"+
                    "│ h: help                     │\n"+
                    "│ q: quit                     │\n"+
                    "├─────────────────────────────┤\n"+
                    "│ esc : cancel                │\n"+
                    "╰─────────────────────────────╯\n";

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
            // +2 to skip NONE and CANCEL
            return Choice.values()[row+2];
        }
        if (key.getKeyType()==KeyType.Character) {
            switch (Character.toLowerCase(key.getCharacter())) {
                case 'p': return Choice.ACTION;
                case 'v': return Choice.PASTE;
                case 'f': return Choice.FIND;
                case 's': return Choice.SORT;
                case 'a': return Choice.AGGREGATE;
                case '+': return Choice.UNION;
                case 'g': return Choice.GROUPBY;
                case 'h': return Choice.HELP;
                case 'q': return Choice.QUIT;
            }
        }
        return Choice.NONE;
    }

    /** The help text for what the user has selected. */
    public @Nullable String getHelpText() {
        return help[row];
    }
}