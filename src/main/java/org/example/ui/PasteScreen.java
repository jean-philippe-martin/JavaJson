package org.example.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PasteScreen {

    public enum Choice {
        // leave the menu visible
        NONE,
        // just do the thing.
        PARSE,
        // close the menu, do nothing
        CANCEL

    }
    private List<String> lines;
    private StringBuilder nextLine = new StringBuilder();

    public PasteScreen() {
        lines = new ArrayList<>();
    }

    public String[] getLines() {
        if (nextLine.length()>0) lines.add(consumeLine());
        return lines.toArray(new String[] {});
    }

    public void draw(TextGraphics g) {
        var s = g.getSize();
        var big_g = g;

        TerminalPosition pos = TerminalPosition.TOP_LEFT_CORNER.withRelative(1,1);
        big_g.fill(' ');
        for (String l : this.lines) {
            big_g.putString(pos, l);
            pos = pos.withRelativeRow(1);
        }
        big_g.putString(pos, nextLine.toString());

        int ypos = 3;
        if (this.lines.size()>1 && this.lines.size()<10) {
            ypos = 11;
        }

        g = g.newTextGraphics(new TerminalPosition(s.getColumns()-50,ypos), new TerminalSize(45,s.getRows()));

        String menu =
                "╭──────────────[ PASTE ]─────────────╮\n"+
                "│ Juste paste (or type) stuff.       │\n"+
                "├────────────────────────────────────┤\n"+
                "│ right : accept                     │\n"+
                "│ left : cancel                      │\n"+
                "╰────────────────────────────────────╯\n";

        String[] lines = menu.split("\n");
        TerminalPosition top = TerminalPosition.TOP_LEFT_CORNER;
         pos = top;
        int i=0;
        //int highlight = row+1;
        // the final choice is after the separator
        //if (row==maxRow) highlight++;
        for (String l : lines) {
            g.putString(pos, l);
            i++;
            pos = pos.withRelativeRow(1);
        }

    }

    public PasteScreen.Choice update(KeyStroke key) {
        if (key.getKeyType()== KeyType.ArrowRight) {
            lines.add(consumeLine());
            return Choice.PARSE;
        }
        if (key.getKeyType()== KeyType.ArrowLeft || key.getKeyType()==KeyType.Escape) {
            return Choice.CANCEL;
        }
        if (key.getKeyType() == KeyType.Character) {
            char foo = key.getCharacter();
            if (foo=='\n') {
                lines.add(consumeLine());
            } else {
                nextLine.append(foo);
            }
        }
        if (key.getKeyType()==KeyType.Enter) {
            lines.add(consumeLine());
        }

        return Choice.NONE;
    }

    private String consumeLine() {
        String ret = nextLine.toString();
        nextLine = new StringBuilder();
        return ret;
    }


}
