package org.example.ui;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.example.Deleter;
import org.example.Deleter.MOD;
import org.example.Deleter.SUBJECT;
import org.example.Deleter.TARGET;
import org.example.JsonNode;
import org.jetbrains.annotations.Nullable;

public class DeleteMenu {



    int row = 0;
    int col = 0;
    JsonNode root;

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

    // row,col -> screen pos of left side
    final int[][] xOffset = {
            {0, 27, 29, 29 },
            {0, 15, 7 },
            {0, 29, 29, 29 },
            {1, 32},
            {1, 32},
            {1, 32},
            {1, 32},
            {1, 32},
            {4,13}
    };
    // row -> screen Y coord
    final int[] yOffset = {
            3,5,7,10,11,12, 15,16, 18
    };
    // row -> multiple-choice options
    final String[][] choices = new String[][]{
            new String[]{
                    "Everything             ↕",
                    "Every child of >>>     ↕"
            },
            new String[]{
                    "that       ↕",
                    "Unless it  ↕"
            },
            new String[]{
                    "is                       ↕",
                    "has a child that is      ↕",
                    "has a descendant that is ↕"
            },
    };
    // row -> selected column
    int[] choice = {
            // text options
            0,
            0,
            0,
            // checkbox group 1
            0,
            1,
            0,
            // checkbox group 2
            0,
            1
    };
    final int ROW_TARGET=0;
    final int ROW_SUBJECT=1;
    final int ROW_MOD=2;
    final int ROW_FILTER_PINNED=3;
    final int ROW_FILTER_SELECTED=4;
    final int ROW_FILTER_VISIBLE=5;
    final int ROW_OPT_KEEP_PARENTS=6;
    final int ROW_OPT_KEEP_CHILDREN =7;


    public DeleteMenu(JsonNode root) {
        init(root);
    }

    public void init(JsonNode root) {
        row = 0; col=0;
        this.root = root;
    }

    public boolean keepParentsEnabled() {
        return (choice[1]==1 && choice[2]!=2);
    }

    public boolean keepChildrenEnabled() {
        return (choice[1]==1);
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
                    "│ (still under development)       │\n"+
                    "├──────────────────────────┬──────┤\n"+
                    "│ Every child below >>>  | │      │\n"+
                    "├──────────────┬───────────┴──────┤\n"+
                    "│ unless it    │                  │\n"+
                    "├──────────────┴─────────────┬────┤\n"+
                    "│ has a descendant that is   │    │\n"+
                    "├────────────────────────────┴────┤\n"+
                    "│ either:                         │\n"+
                    "│ [x] pinned                      │\n"+
                    "│ [x] selected                    │\n"+
                    "│ [x] visible                     │\n"+
                    "│                                 │\n"+
                    "│ options:                        │\n"+
                    "│ [x] keep parents                │\n"+
                    "│ [x] keep orphans                │\n"+
                    "│   ╭────────╮                    │\n"+
                    "│   │   GO   │                    │\n"+
                    "│   ╰────────╯                    │\n"+
                    "├─────────────────────────────────┤\n"+
                    "│ esc : cancel                    │\n"+
                    "│ enter :                         │\n"+
                    "│                                 │\n"+
                    "│                                 │\n"+
                    "╰─────────────────────────────────╯\n";


        String[] lines = menu.split("\n");

        TerminalPosition top = TerminalPosition.TOP_LEFT_CORNER;
        TerminalPosition pos = top;
        int i=0;
        for (String l : lines) {
            g.putString(pos, l);
            i++;
            pos = pos.withRelativeRow(1);
        }

        // Draw choices text
        pos = top;
        for (int r=0; r<3; r++) {
            int x = xOffset[r][0]+1;
            int y = yOffset[r];
            int c = choice[r];
            g.putString(pos.withRelative(x,y), " " + choices[r][c]);
        }

        // Draw choices checkboxes
        for (int r=3; r<8; r++) {
            int x = xOffset[r][0]+2;
            int y = yOffset[r];
            int c = choice[r];
            g.putString(pos.withRelative(x,y), choice[r]==0?" ":"x");
        }

        // Gray/ungray options
        {
            TextGraphics g2;
            if (keepParentsEnabled()) g2 = g;
            else g2 = Theme.withColor(g, Theme.disabled_option);
            g2.putString(top.withRelative(xOffset[ROW_OPT_KEEP_PARENTS][0]+5, yOffset[ROW_OPT_KEEP_PARENTS]), "keep parents");
            if (!keepParentsEnabled()) g2.putString(top.withRelative(xOffset[ROW_OPT_KEEP_PARENTS][0]+1, yOffset[ROW_OPT_KEEP_PARENTS]), "[x]");
        }
        {
            TextGraphics g2;
            if (keepChildrenEnabled()) g2 = g;
            else g2 = Theme.withColor(g, Theme.disabled_option);
            g2.putString(top.withRelative(xOffset[ROW_OPT_KEEP_CHILDREN][0]+5, yOffset[ROW_OPT_KEEP_CHILDREN]), "keep children");
            if (!keepChildrenEnabled()) g2.putString(top.withRelative(xOffset[ROW_OPT_KEEP_CHILDREN][0]+1, yOffset[ROW_OPT_KEEP_CHILDREN]), "[ ]");

        }

        // draw selection rectangle
        pos = top.withRelativeRow(yOffset[row]);
        int xo = xOffset[row][0];
        int xo2 = xOffset[row][0+1];
        TextGraphics g2 = Theme.withColor(g, TextColor.ANSI.CYAN);
        Rectangle.drawSingle(g2, pos.withRelative(xo,-1), new TerminalSize(xo2-xo+1, 3));
        g2.putString(pos.withRelativeColumn(xo), ">");
        g2.putString(pos.withRelativeColumn(xo2), "<");


        // Draw deleter explanation
        String desc = getDeleter(root).explain();
        pos = top.withRelative(10,yOffset[yOffset.length-1]+4);
        int width = 31;
        int margin = 8;
        g.putString(pos, atMost(desc, width-margin));
        desc = trim(desc, width-margin);
        pos = pos.withRelative(-margin, 1 );
        while (desc.length()>0) {
            g.putString(pos, atMost(desc, width));
            desc = trim(desc,width);
            pos = pos.withRelativeRow(1 );
        }
    }

    private String atMost(String str, int maxLen) {
        if (str.length()<=maxLen) return str;
        return str.substring(0, maxLen);
    }

    private String trim(String str, int leftTrim) {
        if (str.length()<=leftTrim) return "";
        return str.substring(leftTrim);
    }

    public Deleter getDeleter(JsonNode root) {
        int filter = 0;
        if (choice[ROW_FILTER_PINNED]==1) filter |= Deleter.FILTER.PINNED;
        if (choice[ROW_FILTER_SELECTED]==1) filter |= Deleter.FILTER.SELECTED;
        if (choice[ROW_FILTER_VISIBLE]==1) filter |= Deleter.FILTER.VISIBLE;
        int options = 0;
        if (choice[ROW_OPT_KEEP_PARENTS]==1) options |= Deleter.OPTIONS.KEEP_PARENTS;
        if (choice[ROW_OPT_KEEP_CHILDREN]==1) options |= Deleter.OPTIONS.KEEP_CHILDREN;
        return new Deleter(
                root.getRoot(),
                choice[ROW_TARGET]==0? TARGET.EVERYTHING: TARGET.CHILDREN,
                choice[ROW_SUBJECT]==0? SUBJECT.THAT : SUBJECT.UNLESS,
                choice[ROW_MOD]==0? MOD.IS : (choice[ROW_MOD]==1? MOD.CONTAINS_DIRECT : MOD.CONTAINS_INDIRECT),
                filter,
                options
        );
    }

    public Action update(KeyStroke key) {
        if (key.getKeyType()==KeyType.ArrowUp && row>0) {
            row -= 1;
            col = 0;
        }
        if (key.getKeyType()==KeyType.ArrowDown && row<xOffset.length-1) {
            row++;
            col = 0;
        }
        if (key.getKeyType()==KeyType.ArrowRight && col<xOffset[row].length-2) {
            col++;
        }
        if (key.getKeyType()==KeyType.ArrowLeft && col>0) {
            col--;
        }
        if (key.getKeyType()==KeyType.Escape || (null!=key.getCharacter() && key.getCharacter()=='q')) {
            return Action.HIDE_MENU;
        }
        if (key.getKeyType()==KeyType.Character && key.getCharacter()==' ') {
            if (row==xOffset.length-1) {
                // "GO" button
                return Action.DELETE;
            }
            if (row<choice.length) {
                if (row>=3) {
                    choice[row] = (choice[row]==0?1:0);
                } else {
                    choice[row] = (choice[row]+1) % choices[row].length;
                }
            }
        }
        if (key.getKeyType()==KeyType.Enter) {
            return Action.DELETE;
        }

        return Action.NOTHING;
    }

    /** The help text for what the user has selected. */
    public @Nullable String getHelpText() {
        return help[row];
    }
}