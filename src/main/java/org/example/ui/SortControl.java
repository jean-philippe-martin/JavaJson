package org.example.ui;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import org.example.JsonNode;

public class SortControl {

    private JsonNode node;

        public SortControl(JsonNode nodeToSort) {
            init(nodeToSort);
        }

        public void init(JsonNode nodeToSort) {
            this.node = nodeToSort;
        }

        public void draw(TextGraphics g) {
        String menu = """
                ╭────────────[ SORT ]──────────────╮
                ├──────────────────────────────────┤
                │ R │ Aa │ num │                   │
                ├──────────────────────────────────┤
                │ r : reverse order                │
                │ a : separate upper/lowercase     │
                │ n : sort strings as numbers      │
                ├──────────────────────────────────┤
                │ enter: sort                      │
                │ esc : cancel                     │
                ╰──────────────────────────────────╯
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
        }

    public void update(KeyStroke key) {
            // TODO: return sort instructions
            return;
    }

}
