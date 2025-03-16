package org.example;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import org.example.ui.ChoiceInputField;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;


public class ChoiceInputTest {

    @Test
    public void testSwitchOnFirstLetter() {
        ChoiceInputField field = new ChoiceInputField(new String[] { "Bob", "Alice" });
        assertEquals("Alice", field.getChoice());

        field.update(KeyStroke.fromString("B"));
        assertEquals("Bob", field.getChoice());
    }

    @Test
    public void testNoSwitchOnFirstLetters() {
        ChoiceInputField field = new ChoiceInputField(new String[] { "Bob", "Alice" });

        assertEquals("", field.getTyped());
        field.update(KeyStroke.fromString("A"));
        assertEquals("Alice", field.getChoice());
        assertEquals("A", field.getTyped());

        field.update(KeyStroke.fromString("l"));
        assertEquals("Alice", field.getChoice());
        assertEquals("Al", field.getTyped());
    }

    @Test
    public void testNoSwitchOnFirstLetters2() {
        ChoiceInputField field = new ChoiceInputField(new String[] { "Bob", "Alice" });

        assertEquals("", field.getTyped());
        field.update(KeyStroke.fromString("B"));
        assertEquals("Bob", field.getChoice());
        assertEquals("B", field.getTyped());

        field.update(KeyStroke.fromString("o"));
        assertEquals("Bob", field.getChoice());
        assertEquals("Bo", field.getTyped());
    }

    @Test
    public void testBadChoice() {
        ChoiceInputField field = new ChoiceInputField(new String[] { "Bob", "Alice" });

        assertEquals("", field.getTyped());
        field.update(KeyStroke.fromString("C"));
        assertEquals("Alice", field.getChoice());
        assertEquals("", field.getTyped());

        field.update(KeyStroke.fromString("B"));
        assertEquals("Bob", field.getChoice());
        assertEquals("B", field.getTyped());

        field.update(KeyStroke.fromString("A"));
        assertEquals("Bob", field.getChoice());
        assertEquals("B", field.getTyped());
    }

    @Test
    public void testSwitchToNext() {
        ChoiceInputField field = new ChoiceInputField(new String[] { "Aloe", "Alix", "Ale", "Alice" });

        assertEquals("", field.getTyped());
        field.update(KeyStroke.fromString("A"));
        assertEquals("Ale", field.getChoice());

        field.update(KeyStroke.fromString("l"));
        assertEquals("Ale", field.getChoice());

        field.update(KeyStroke.fromString("i"));
        assertEquals("Alice", field.getChoice());

        field.update(KeyStroke.fromString("x"));
        assertEquals("Alix", field.getChoice());

    }

    @Test
    public void testDefaultsToLexicographicalFirst() {
        ChoiceInputField field = new ChoiceInputField(new String[] { "(keys)", "(values)" });
        assertEquals("(keys)", field.getChoice());
    }

    @Test
    public void testDefaultsToLexicographicalFirstEvenAfterDraw() throws IOException {
        ChoiceInputField field = new ChoiceInputField(new String[] { "(keys)", "(values)" });
        Terminal term = new DefaultVirtualTerminal(new TerminalSize(20, 20));
        Screen screen = new TerminalScreen(term);
        field.draw(screen.newTextGraphics());

        assertEquals("(keys)", field.getChoice());

        field.update(KeyStroke.fromString("x"));

        assertEquals("(keys)", field.getChoice());
    }

    @Test
    public void testTabWorks() {
        ChoiceInputField field = new ChoiceInputField(new String[] { "(keys)", "(values)" });
        field.update(new KeyStroke(KeyType.Tab));
        field.update(new KeyStroke(KeyType.ArrowDown));
        field.update(new KeyStroke(KeyType.Enter));
        assertEquals("(values)", field.getChoice());
    }

}
