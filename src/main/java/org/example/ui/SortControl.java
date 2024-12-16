package org.example.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.example.JsonNode;
import org.example.Sorter;

public class SortControl {

    boolean reverse;
    boolean ignoreCase;
    boolean numberify;
    int col, row;

    public SortControl(JsonNode nodeToSort) {
        init(nodeToSort);
    }

    public void init(JsonNode nodeToSort) {
        this.reverse = false;
        this.ignoreCase = true;
        this.numberify = false;
        this.col=0;
        this.row=0;
    }

    public void draw(TextGraphics g) {
    String menu = """
            ╭────────────[ SORT ]───────────────╮
            ├───────────────────────────────────┤
            │ R │ Aa │ num │                    │
            ├───────────────────────────────────┤
            │ r : reverse order                 │
            │ a : separate upper/lowercase      │
            │ n : sort strings as numbers       │
            ├───────────────────────────────────┤
            │ enter: sort                       │
            │ esc : cancel                      │
            │ x: return to original order       │
            ╰───────────────────────────────────╯
            """;
        TerminalSize s = g.getSize();
        g = g.newTextGraphics(new TerminalPosition(s.getColumns()-40,3), new TerminalSize(39,s.getRows()));
        String[] lines = menu.split("\n");
        TerminalPosition top = TerminalPosition.TOP_LEFT_CORNER;
        TerminalPosition pos = top;
        for (String l : lines) {
            g.putString(pos, l);
            pos = pos.withRelativeRow(1);
        }
        if (row==0) {
            int[] sep = new int[]{0, 4, 9, 15};
            TextColor fc = g.getForegroundColor();
            g.setForegroundColor(TextColor.ANSI.CYAN);
            Rectangle.drawSingle(g, top.withRelative(sep[col], 1), new TerminalSize(sep[col + 1] - sep[col] + 1, 3));
            g.putString(top.withRelative(sep[col], 2), ">");
            g.putString(top.withRelative(sep[col + 1], 2), "<");
            g.setForegroundColor(fc);
            if (reverse) {
                g.putString(top.withRelative(1+sep[0],2), " R ", SGR.REVERSE);
            }
            if (!ignoreCase) {
                g.putString(top.withRelative(1+sep[1],2), " Aa ", SGR.REVERSE);
            }
            if (numberify) {
                g.putString(top.withRelative(1+sep[2],2), " num ", SGR.REVERSE);
            }
        }
    }

    public Sorter update(KeyStroke key) {
            if (key.getKeyType()== KeyType.Enter) {
                return new Sorter(this.reverse, ignoreCase, numberify);
            }
            if (key.getKeyType()==KeyType.ArrowRight) {
                if (col<2) { this.col++; }
            }
            if (key.getKeyType()==KeyType.ArrowLeft) {
                if (this.col>0) { this.col--; }
            }
            if (key.getKeyType()==KeyType.Character && ((col==0 && key.getCharacter()==' ') || key.getCharacter()=='r')) {
                this.reverse = !this.reverse;
            }
            if (key.getKeyType()==KeyType.Character && ((col==1 && key.getCharacter()==' ') || key.getCharacter()=='a' || key.getCharacter()=='c')) {
                this.ignoreCase = !this.ignoreCase;
            }
            if (key.getKeyType()==KeyType.Character && ((col==2 && key.getCharacter()==' ') || key.getCharacter()=='n')) {
                this.numberify = !this.numberify;
            }
            return null;
    }

}
