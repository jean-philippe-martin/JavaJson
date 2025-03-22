package org.example.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.example.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class SortControl {

    public static class SortEntry {
        public final ArrayList<String> fields;

        public SortEntry(List<String> strings) {
            fields = new ArrayList<>(strings);
        }

        public static SortEntry fromString(String s) {
            ArrayList<String> list = new ArrayList<>();
            list.add(s);
            return new SortEntry(list);
        }

        public static SortEntry empty() {
            return new SortEntry(new ArrayList<>());
        }

        public SortEntry withAlso(String s) {
            ArrayList<String> extended = new ArrayList<>(this.fields);
            extended.add(s);
            return new SortEntry(extended);
        }

        public String toString() {
            return String.join(".", fields);
        }

        @Override
        public boolean equals(Object rhs) {
            if (rhs instanceof SortEntry) {
                return toString().equals(((SortEntry)rhs).toString());
            }
            return rhs.equals(this);
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }

    List<JsonNode> selectedNodes;
    // Whether we need to select a key to sort by,
    // i.e. if one of the selected nodes is an array of maps.
    boolean selectField;
    boolean reverse;
    boolean ignoreCase;
    boolean numberify;
    boolean showHelp = true;
    // true if one of the things we're sorting is a map,
    // so we can choose between sorting by keys or values.
    boolean sortingAMap = false;
    int col, row;
    HashSet<SortEntry> fieldChoices = null;
    ChoiceInputField input;

    public final static String KEYS_CHOICE="(keys)";
    public final static String VALUES_CHOICE="(values)";

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
        this.sortingAMap = false;
        for (JsonNode node : this.selectedNodes) {
            if (node instanceof JsonNodeMap) {
                JsonNodeMap jnm = (JsonNodeMap)node;
                sortingAMap = true;
                for (String key : jnm.getKeysInOrder()) {
                    if (jnm.getChild(key) instanceof JsonNodeMap) {
                        JsonNodeMap kid = (JsonNodeMap)(jnm.getChild(key));
                        addAll(SortEntry.empty(), kid);
                    }
                }
            }
            if (node instanceof JsonNodeList) {
                JsonNodeList jnl = (JsonNodeList)node;
                for (int i=0; i<jnl.childCount(); i++) {
                    if (jnl.get(i) instanceof JsonNodeMap) {
                        JsonNodeMap jnm = (JsonNodeMap)jnl.get(i);
                        addAll(SortEntry.empty(), jnm);
                    }
                }
            }
        }
        if (sortingAMap) {
            if (fieldChoices.isEmpty()) {
                // Only values: we can choose to sort by key or value
                fieldChoices.add(SortEntry.fromString(KEYS_CHOICE));
                fieldChoices.add(SortEntry.fromString(VALUES_CHOICE));
            } else {
                // maps in there: we can choose a field from the map.
                fieldChoices.add(SortEntry.fromString(KEYS_CHOICE));
            }
        }
        this.selectField = !fieldChoices.isEmpty();
        input = null;
        if (!this.selectField) row=1;
        if (this.selectField) input = new ChoiceInputField(fieldChoices.stream().map(SortEntry::toString).toArray(String[]::new));
    }

    private void addAll(SortEntry prefix, JsonNode node) {
        JsonNodeIterator it = node.iterateChildren();
        while (it!=null) {
            JsonNode kid = it.get();
            fieldChoices.add(prefix.withAlso(it.key().toString()));
            if (kid instanceof JsonNodeMap) {
                addAll(prefix.withAlso(it.key().toString()), kid);
            }
            it = it.next();
        }
    }

    public void draw(TextGraphics g) {
        boolean skippedField = false;
        String menu =
                "╭────────────[ SORT ]───────────────╮\n"+
                "│                                   │\n"+
                "├───┬────┬─────┬────────────────────┤\n"+
                "│ R │ Aa │ num │                    │\n";
        if (showHelp) menu +=
                "├───┴────┴─────┴────────────────────┤\n"+
                "│ r : reverse order                 │\n"+
                "│ a : separate upper/lowercase      │\n"+
                "│ n : sort strings as numbers       │\n"+
                "├───────────────────────────────────┤\n"+
                "│ TAB: show/hide choices            │\n"+
                "│ enter: sort                       │\n"+
                "│ esc : cancel                      │\n"+
                "│ x: return to original order       │\n"+
                "│ ? : toggle help text              │\n"+
                "╰───────────────────────────────────╯\n";
        else menu += "╰───┴────┴─────┴────────────────[?]─╯\n";
        TerminalSize s = g.getSize();
        g = g.newTextGraphics(new TerminalPosition(s.getColumns()-40,3), new TerminalSize(39,s.getRows()));
        String[] lines = menu.split("\n");
        TerminalPosition top = TerminalPosition.TOP_LEFT_CORNER;
        TerminalPosition pos = top;
        for (int i=0; i<lines.length; i++) {
            String l = lines[i];
            // skip the line for the field names
            if (i==1) {
                if ((!selectField && !sortingAMap)) {
                    skippedField=true;
                    continue;
                }
            }
            if ((!selectField) && l.contains("TAB")) continue;
            g.putString(pos, l);
            pos = pos.withRelativeRow(1);
        }
        TerminalPosition optsPos = TerminalPosition.TOP_LEFT_CORNER.withRelativeRow(2);
        if (skippedField) optsPos = TerminalPosition.TOP_LEFT_CORNER.withRelativeRow(1);
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
            g.putString(optsPos.withRelative(1+sep[1],1), " aA ");
        }
        if (!ignoreCase) {
            // the "Aa" icon shows the order in which they'll be sorted.
            if (reverse) {
                g.putString(optsPos.withRelative(1 + sep[1], 1), " aA ", SGR.REVERSE);
            } else {
                g.putString(optsPos.withRelative(1 + sep[1], 1), " Aa ", SGR.REVERSE);
            }
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
            ArrayList<String> fields = new ArrayList<>();
            fields.add(field);
            // We figure out which keys were meant from the string.
            // This is imperfect: if some keys have a dot in them
            // then this may be ambiguous.
            for (SortEntry e : fieldChoices) {
                if (e.toString().equals(field)) {
                    // found our choice!
                    fields = e.fields;
                }
            }
            return new Sorter(this.reverse, ignoreCase, numberify, fields, KEYS_CHOICE.equals(field));
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
            char pressed = '\0';
            if (key.getKeyType()==KeyType.Character) pressed = Character.toLowerCase(key.getCharacter());
            if (key.getKeyType()==KeyType.Character && ((col==0 && pressed==' ') || pressed=='r')) {
                this.reverse = !this.reverse;
            }
            if (key.getKeyType()==KeyType.Character && ((col==1 && pressed==' ') || pressed=='a' || pressed=='c')) {
                this.ignoreCase = !this.ignoreCase;
            }
            if (key.getKeyType()==KeyType.Character && ((col==2 && pressed==' ') || pressed=='n')) {
                this.numberify = !this.numberify;
            }
            if (key.getKeyType()==KeyType.Character && (pressed=='?')) {
                this.showHelp = !this.showHelp;
            }
        }
        return null;
    }

    /** The help text for what the user has selected. */
    public @Nullable String getHelpText() {
        if (row==0) {
            return "For lists of maps, which key of the map to sort by";
        }
        if (row==1) {
            if (col==0) return (reverse ? "Reversed order (large to small)" : "Sorted small to large");
            if (col==1) return (ignoreCase ? "Ignoring case" : "Uppercase counts as 'smaller'");
            if (col==2) return (numberify ? "Numbers in strings are sorted numerically": "Purely lexicographical sort");
        }
        return null;
    }

}
