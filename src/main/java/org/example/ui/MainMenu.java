package org.example.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.example.Operation;
import org.example.OperationList;
import org.jetbrains.annotations.Nullable;

public class MainMenu {

    private int row = 0;

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
        Action.SHOW_DELETE_MENU,
        Action.UNDO,
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
            "Delete at the cursor, or more",
            "Undo the last edit action",
            "Press any key to exit the help screen",
            "Quit this program",
            "Close this menu",
            null,
    };
    // the max allowed row value
    private final int maxRow = help.length-2;
    private OperationList opsList;

    public MainMenu(OperationList opsList) {
        this.opsList = opsList;
    }

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
                    "│ b: group by selected key(s) │\n"+
                    "│ d: delete                   │\n"+
                    "│ Z: undo                     │\n"+
                    "│ h: help                     │\n"+
                    "│ q: quit                     │\n"+
                    "├─────────────────────────────┤\n"+
                    "│ esc : cancel                │\n"+
                    "╰─────────────────────────────╯\n";

        String[] lines = menu.split("\n");
        Operation op = opsList.peek();
        if (null!=op) {
            StringBuilder txt = new StringBuilder("│ Z: undo " + op.toString());
            while (txt.length()+1 < lines[8].length()) {
                txt.append(' ');
            }
            txt.append('│');
            lines[9] = txt.substring(0, lines[8].length());
        }

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

    public Action update(KeyStroke key) {
        if (key.getKeyType()==KeyType.ArrowUp && row>0) {
            row -= 1;
        }
        if (key.getKeyType()==KeyType.ArrowDown && row<maxRow) {
            row++;
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
            switch (key.getCharacter()) {
                case 'p': return Action.SHOW_ACTION_MENU;
                case 'v': return Action.SHOW_PASTE_MENU;
                case 'f': return Action.SHOW_FIND_MENU;
                case 's': return Action.SHOW_SORT_MENU;
                case 'a': return Action.SHOW_AGGREGATE_MENU;
                case '+': return Action.UNION;
                case 'b': return Action.GROUPBY;
                case 'd': return Action.SHOW_DELETE_MENU;
                case 'Z': return Action.UNDO;
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