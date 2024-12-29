package org.example.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.example.JsonNode;
import org.example.JsonNodeList;
import org.example.JsonNodeMap;
import org.example.Sorter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SortControl {

    List<JsonNode> selectedNodes;
    // Whether we need to select a key to sort by,
    // i.e. if one of the selected nodes is an array of maps.
    boolean selectField;
    boolean reverse;
    boolean ignoreCase;
    boolean numberify;
    boolean showHelp = true;
    int col, row;
    HashSet<String> fieldChoices = null;
    ChoiceInputField input;

    public SortControl(@Nullable List<JsonNode> selectedNodes) {
        init(selectedNodes);
    }

    public void init(@Nullable List<JsonNode> selectedNodes) {
        this.reverse = false;
        this.ignoreCase = true;
        this.numberify = true;
        this.col=0;
        this.row=0;
        if (null==selectedNodes) {
            selectedNodes = new ArrayList<>();
        }
        this.selectedNodes = selectedNodes;
        this.selectField = false;
        this.fieldChoices = new HashSet<>();
        for (JsonNode node : this.selectedNodes) {
            if (node instanceof JsonNodeList jnl) {
                for (int i=0; i<jnl.childCount(); i++) {
                    if (jnl.get(i) instanceof JsonNodeMap jnm) {
                        this.selectField = true;
                        fieldChoices.addAll(jnm.getKeysInOrder());
                    }
                }
            }
        }
        input = null;
        if (!this.selectField) row=1;
        if (this.selectField) input = new ChoiceInputField(fieldChoices.toArray(new String[0]));
    }

    public void draw(TextGraphics g) {
    String menu = """
            ╭────────────[ SORT ]───────────────╮
            │                                   │
            ├───┬────┬─────┬────────────────────┤
            │ R │ Aa │ num │                    │
            """;
    if (showHelp) menu += """
            ├───┴────┴─────┴────────────────────┤
            │ r : reverse order                 │
            │ a : separate upper/lowercase      │
            │ n : sort strings as numbers       │
            ├───────────────────────────────────┤
            │ TAB: show/hide choices            │
            │ enter: sort                       │
            │ esc : cancel                      │
            │ x: return to original order       │
            │ ? : toggle help text              │
            ╰───────────────────────────────────╯
            """;
        else menu += """
            ╰───┴────┴─────┴────────────────[?]─╯
            """;
        TerminalSize s = g.getSize();
        g = g.newTextGraphics(new TerminalPosition(s.getColumns()-40,3), new TerminalSize(39,s.getRows()));
        String[] lines = menu.split("\n");
        TerminalPosition top = TerminalPosition.TOP_LEFT_CORNER;
        TerminalPosition pos = top;
        TerminalPosition optsPos = TerminalPosition.TOP_LEFT_CORNER.withRelativeRow(2);
        if (!selectField) optsPos = TerminalPosition.TOP_LEFT_CORNER.withRelativeRow(1);
        for (int i=0; i<lines.length; i++) {
            String l = lines[i];
            if ((!selectField) && i==1) continue;
            if ((!selectField) && l.contains("TAB")) continue;
            g.putString(pos, l);
            pos = pos.withRelativeRow(1);
        }
        int[] sep = new int[]{0, 4, 9, 15};
        TextColor fc = g.getForegroundColor();
        if (row==0) {
            g.setForegroundColor(TextColor.ANSI.CYAN);
            Rectangle.drawBold(g, top.withRelative(0,0), new TerminalSize(37, 3));
            g.putString(top.withRelative(13, 0), "[ SORT ]");
            g.setForegroundColor(fc);
        }
        else if (row==1) {
            g.setForegroundColor(TextColor.ANSI.CYAN);
            Rectangle.drawBold(g, optsPos.withRelative(sep[col], 0), new TerminalSize(sep[col + 1] - sep[col] + 1, 3));
            g.putString(optsPos.withRelative(sep[col], 1), ">");
            g.putString(optsPos.withRelative(sep[col + 1], 1), "<");
            if (col==0 && !showHelp){
                g.putString(optsPos.withRelative(sep[col], 2), "╰");
            }
        }
        g.setForegroundColor(fc);
        if (reverse) {
            g.putString(optsPos.withRelative(1+sep[0],1), " R ", SGR.REVERSE);
        }
        if (!ignoreCase) {
            g.putString(optsPos.withRelative(1+sep[1],1), " Aa ", SGR.REVERSE);
        }
        if (numberify) {
            g.putString(optsPos.withRelative(1+sep[2],1), " num ", SGR.REVERSE);
        }
        if (selectField) {
            TextGraphics g2 = g.newTextGraphics(top.withRelative(0,1), new TerminalSize(37,12));
            input.setFocused(row==0);
            input.draw(g2);
        }
    }

    /**
     * @param key : the key the user pressed
     * @return null in normal case, or a Sorter if the user pressed Enter.
     *         This sorter will sort records in the order specified by the user.
     */
    public Sorter update(KeyStroke key) {
        if (row==0) {
            boolean handled = input.update(key);
            if (handled) return null;
        }
        if (key.getKeyType()== KeyType.Enter) {
            String field;
            if (input!=null) {
                field = input.getChoice();
            } else {
                field = null;
            }
            return new Sorter(this.reverse, ignoreCase, numberify, field);
        }
        if (key.getKeyType()==KeyType.ArrowDown) {
            if (row<1) { this.row++; }
        }
        if (key.getKeyType()==KeyType.ArrowRight) {
            if (col<2) { this.col++; }
        }
        if (key.getKeyType()==KeyType.ArrowLeft) {
            if (this.col>0) { this.col--; }
        }
        if (row>0) {
            if (key.getKeyType()==KeyType.ArrowUp) {
                if (row>0 && selectField) { this.row--; }
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
            if (key.getKeyType()==KeyType.Character && (key.getCharacter()=='?')) {
                this.showHelp = !this.showHelp;
            }
        }
        return null;
    }

}
