package org.example.ui;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.jetbrains.annotations.NotNull;

public class InputField {
    // Where we'll put what the user types.
    // For now we only allow a single line, but we could expand.
    private TextGraphics textArea;

    // What the user typed
    private @NotNull String text = "";
    private final static String CURSOR = "█";

    public InputField() {}

    public void update(TextGraphics g, KeyStroke key) {
        if (key.getKeyType()== KeyType.Backspace) {
            if (!text.isEmpty()) {
                text = text.substring(0,text.length()-1);
            }
            return;
        }
        if (key.getCharacter() != null) {
            // normal key
            char k = key.getCharacter();
            if (k>=' ') { // skip control characters
                text += key.getCharacter();
            }
        }
    }
    public void draw(TextGraphics where) {
        where.putString(TerminalPosition.TOP_LEFT_CORNER, text + CURSOR);
    }

    public @NotNull String getText() {
        return this.text;
    }

}
