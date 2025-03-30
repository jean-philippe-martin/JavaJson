package org.example.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.example.Drawer;
import org.jetbrains.annotations.Nullable;

public class DeleteMenu {

    int row = 0;
    int col = 0;

    public Action[] choice = new Action[]{
        // leave the menu visible
        Action.NOTHING,
        // close the menu, do nothing
        Action.HIDE_MENU,
        // actions:
        Action.SHOW_ACTION_MENU,
        Action.SHOW_PASTE_MENU,
        Action.SHOW_FIND_MENU,
        Action.SHOW_SORT_MENU,
        Action.SHOW_AGGREGATE_MENU,
        Action.UNION,
        Action.GROUPBY,
        Action.SHOW_HELP_SCREEN,
        Action.QUIT
    };

    private final String[] help = new String[]{
            "Menu to act on the current value",
            "Select this, then paste the new document in",
            "Menu to find keys/values",
            "Menu to sort values in this list or map",
            "Menu to show information about this list/object",
            "Your cursors must point to arrays",
            "Your cursor(s) must be in a map/string in a list",
            "Press any key to exit the help screen",
            "Quit this program",
            "Close this menu",
            null,
    };
    // the max allowed row value
    private final int maxRow = help.length-2;


    public void init() {
        row = 0; col=0;
    }

    public void draw(TextGraphics g) {

        var s = g.getSize();
        g = g.newTextGraphics(new TerminalPosition(s.getColumns()-42,3), new TerminalSize(35,s.getRows()));

        /*
        All children (of primary cursor) except those containing a pin
        All children  (of primary cursor) except those with a secondary cursor
        Everything except pinned (removing all structure)
        Everything except visible (keeps the structure)
        Everything except cursors
        */

        String menu =
                    "╭────────────[ DELETE ]───────────╮\n"+
                    "├────────────┬─────────────┬──────┤\n"+
                    "│ Everything │ Every child │      │\n"+
                    "├──────┬─────┴──────┬──────┴──────┤\n"+
                    "│ that │ unless it  │             │\n"+
                    "├─────┬┴─────────┬──┴────────┬────┤\n"+
                    "│ is  │ contains │           │    │\n"+
                    "├─────┴──────────┴───────────┴────┤\n"+
                    "│ either:                         │\n"+
                    "│ [x] pinned                      │\n"+
                    "│ [x] selected                    │\n"+
                    "│ [x] visible                     │\n"+
                    "│   ╭────────╮                    │\n"+
                    "│   │   GO   │                    │\n"+
                    "│   ╰────────╯                    │\n"+
                    "├─────────────────────────────────┤\n"+
                    "│ esc : cancel                    │\n"+
                    "│ GO : Delete all children of the │\n"+
                    "│      main cursor except those   │\n"+
                    "│      that contain a pin.        │\n"+
                    "╰─────────────────────────────────╯\n";


        String[] lines = menu.split("\n");
        TerminalPosition top = TerminalPosition.TOP_LEFT_CORNER;
        TerminalPosition pos = top;
        int[][] xOffset = {
                {0, 13, 27 },
                {0, 7, 20 },
                {0, 6, 17 },
        };
        int[] yOffset = {
            2,4,6
        };
        int i=0;
        for (String l : lines) {
            if (i==yOffset[row]) {
                // selected row
                int xo = xOffset[row][col];
                if (xo>0) {
                    String before = l.substring(0, xo);
                    g.putString(pos, before);
                }
                int xo2 = xOffset[row][col+1];
                String selected = l.substring(xo+1, xo2);
                g.putString(pos.withRelativeColumn(xo+1), selected, SGR.REVERSE);
                String rest = l.substring(xo2+1);
                g.putString(pos.withRelativeColumn(xo2+1), rest);
            } else {
                // rest of the menu
                g.putString(pos, l);
            }
            i++;
            pos = pos.withRelativeRow(1);
        }

        // draw selection rectangle
        pos = top.withRelativeRow(yOffset[row]);
        int xo = xOffset[row][col];
        int xo2 = xOffset[row][col+1];
        TextGraphics g2 = Theme.withColor(g, TextColor.ANSI.CYAN);
        Rectangle.drawSingle(g2, pos.withRelative(xo,-1), new TerminalSize(xo2-xo+1, 3));
        g2.putString(pos.withRelativeColumn(xo), ">");
        g2.putString(pos.withRelativeColumn(xo2), "<");


    }

    public Action update(KeyStroke key) {
        if (key.getKeyType()==KeyType.ArrowUp && row>0) {
            row -= 1;
            col = 0;
        }
        if (key.getKeyType()==KeyType.ArrowDown && row<maxRow) {
            row++;
            col = 0;
        }
        if (key.getKeyType()==KeyType.ArrowRight) {
            col++;
        }
        if (key.getKeyType()==KeyType.ArrowLeft && col>0) {
            col--;
        }
        if (key.getKeyType()==KeyType.Escape) {
            return Action.HIDE_MENU;
        }
        if (key.getKeyType()==KeyType.Enter) {
            if (row==maxRow) return Action.HIDE_MENU;
            // +2 to skip NONE and CANCEL
            return choice[row+2];
        }
        if (key.getKeyType()==KeyType.Character) {
            switch (Character.toLowerCase(key.getCharacter())) {
                case 'p': return Action.SHOW_ACTION_MENU;
                case 'v': return Action.SHOW_PASTE_MENU;
                case 'f': return Action.SHOW_FIND_MENU;
                case 's': return Action.SHOW_SORT_MENU;
                case 'a': return Action.SHOW_AGGREGATE_MENU;
                case '+': return Action.UNION;
                case 'b': return Action.GROUPBY;
                case 'h': return Action.SHOW_HELP_SCREEN;
                case 'q': return Action.QUIT;
            }
        }
        return Action.NOTHING;
    }

    /** The help text for what the user has selected. */
    public @Nullable String getHelpText() {
        return help[row];
    }
}