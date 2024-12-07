package org.example.ui;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import org.example.JsonNode;
import org.example.cursor.FindCursor;
import org.jetbrains.annotations.NotNull;

public class FindControl {

    public enum Choice {
        // leave the find dialog visible
        NONE,
        // select the found text
        FIND,
        // go to the first found text
        GOTO,
        // cancel search
        CANCEL
    }

    JsonNode myJson;
    InputField findInput;
    boolean allowSubstring = true;
    boolean careAboutCase = false;
    boolean searchKey=true;
    boolean searchValue=true;
    boolean regExp = false;
    // Which row is selected (has input focus)
    int row = 0;
    int col=0;

    public FindControl(JsonNode rootNode) {
        this.findInput = new InputField();
        this.myJson = rootNode;
    }

    public void init() {
        col = 0;
        row = 0;
        clearText();
        this.findInput.setFocused(false);
    }

    public void draw(TextGraphics g) {
        TerminalSize s = g.getSize();
        g = g.newTextGraphics(new TerminalPosition(s.getColumns()-40,3), new TerminalSize(39,s.getRows()));
        String menu = """
                ╭────────────[ FIND ]────────────╮
                │                                │
                ├────────────────────────────────┤
                │ Whole │ Aa │ K+V │ .* │        │
                ├────────────────────────────────┤
                │ w: match whole words only      │
                │ c/a: case-sensitive match      │
                │ k/v: find in keys/values/both  │
                │ r/.: regular expression        │
                │ n/N: next/prev result          │
                ├────────────────────────────────┤
                │ enter: select all              │
                │ g: go to current find only     │
                │ esc : cancel find              │
                ╰────────────────────────────────╯
                """;
        String[] lines = menu.split("\n");
        TerminalPosition top = TerminalPosition.TOP_LEFT_CORNER.withRelativeColumn(4).withRelativeRow(1);
        TerminalPosition pos = top;
        for (String l : lines) {
            g.putString(pos, l);
            pos = pos.withRelativeRow(1);
        }
        if (!this.allowSubstring) {
            g.putString(top.withRelativeColumn(1).withRelativeRow(3), " Whole ", SGR.REVERSE);
        }
        if (this.careAboutCase) {
            g.putString(top.withRelativeColumn(9).withRelativeRow(3), " Aa ", SGR.REVERSE);
        }
        String[][] kvRepr = new String[][] {
                {"  ?  ", " Val "},
                {" Key ", " K+V "}
        };
        String kvChoice = kvRepr[searchKey?1:0][searchValue?1:0];
        if (searchKey && searchValue) {
            g.putString(top.withRelativeColumn(14).withRelativeRow(3), kvChoice); }
            else {
                    g.putString(top.withRelativeColumn(14).withRelativeRow(3), kvChoice, SGR.REVERSE);
        }
        if (this.regExp) {
            g.putString(top.withRelativeColumn(20).withRelativeRow(3), " .* ", SGR.REVERSE);
        }
        findInput.setFocused(row==0);
        if (row==0) {
            TextColor fc = g.getForegroundColor();
            g.setForegroundColor(TextColor.ANSI.CYAN);
            Rectangle.drawSingle(g, top.withRelative(0,0), new TerminalSize(34, 3));
            g.putString(top.withRelativeColumn(0).withRelativeRow(row * 2 + 1), ">");
            g.putString(top.withRelativeColumn(33).withRelativeRow(row * 2 + 1), "<");
            g.putString(top.withRelativeColumn(13), "[ FIND ]");
            g.setForegroundColor(fc);
        } else {
            int[] sep = new int[] {0, 8, 13, 19, 24};
            TextColor fc = g.getForegroundColor();
            g.setForegroundColor(TextColor.ANSI.CYAN);
            Rectangle.drawSingle(g, top.withRelative(sep[col], 2), new TerminalSize(sep[col+1]-sep[col]+1, 3));
            g.putString(top.withRelative(sep[col], 3), ">");
            g.putString(top.withRelative(sep[col+1], 3), "<");
            g.setForegroundColor(fc);
        }

        TextGraphics g2 = g.newTextGraphics(TerminalPosition.TOP_LEFT_CORNER.withRelative(6,2),
                new TerminalSize(31,1));
        g2.setForegroundColor(TextColor.ANSI.YELLOW_BRIGHT);
        g2.setBackgroundColor(TextColor.Indexed.fromRGB(0,0,128));
        g2.fill(' ');
        findInput.draw(g2);
    }

    public Choice update(KeyStroke key) {
        if (key.getKeyType()==KeyType.ArrowDown) {
            row += 1;
        }
        if (key.getKeyType()==KeyType.ArrowUp) {
            row -= 1;
        }
        if (row<0) row=0;
        if (row>1) row=1;
        if (row==0) {
            findInput.update(key);
        } else if (row==1) {
            if (key.getKeyType()==KeyType.ArrowRight) col += 1;
            if (key.getKeyType()==KeyType.ArrowLeft) col -= 1;
            if (col<0) col=0;
            if (col>3) col=3;
            if (key.getKeyType()== KeyType.Character && ((key.getCharacter()==' ' && col==0)|| key.getCharacter()=='w')) {
                this.allowSubstring = !this.allowSubstring;
            }
            if (key.getKeyType()== KeyType.Character && ((key.getCharacter()==' ' && col==1) || key.getCharacter()=='c' || key.getCharacter()=='a')) {
                this.careAboutCase = !this.careAboutCase;
            }
            if (key.getKeyType()== KeyType.Character && (key.getCharacter()==' ' && col==2)) {
                if (searchKey)
                    if (searchValue) {
                        searchValue = false;
                    } else {
                        searchKey = false;
                        searchValue = true;
                    }
                else {
                    searchKey = true;
                    searchValue = true;
                }

            }
            if (key.getKeyType()== KeyType.Character &&  key.getCharacter()=='k' && (searchKey || !searchValue)) {
                this.searchValue = !this.searchValue;
            }
            if (key.getKeyType()== KeyType.Character &&  key.getCharacter()=='v' && (!searchKey || searchValue)) {
                this.searchKey = !this.searchKey;
            }
            if (key.getKeyType()== KeyType.Character && ((key.getCharacter()==' ' && col==3) || key.getCharacter()=='r' || key.getCharacter()=='.')) {
                this.regExp = !this.regExp;
            }
            if (key.getKeyType()== KeyType.Character && key.getCharacter()=='n') {
                myJson.cursorNextCursor();
            }
            if (key.getKeyType()== KeyType.Character && key.getCharacter()=='N') {
                myJson.cursorPrevCursor();
            }
            if (key.getKeyType()== KeyType.Character && key.getCharacter()=='g') {
                return Choice.GOTO;
            }
        }
        if (key.getKeyType() == KeyType.Escape) {
            return Choice.CANCEL;
        }
        if (key.getKeyType()==KeyType.Enter) {
            return Choice.FIND;
        }
        return Choice.NONE;
    }

    public @NotNull String getText() {
        return findInput.getText();
    }

    public void clearText() {
        this.findInput = new InputField();
    }

    public boolean getAllowSubstring() {
        return this.allowSubstring;
    }

    public boolean getIgnoreCase() {
        return !careAboutCase;
    }

    public boolean getSearchKeys() {
        return searchKey;
    }

    public boolean getSearchValues() {
        return searchValue;
    }

    public boolean getUseRegexp() {
        return regExp;
    }

}
