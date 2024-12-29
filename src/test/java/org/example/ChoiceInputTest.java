package org.example;

import com.googlecode.lanterna.input.KeyStroke;
import org.example.ui.ChoiceInputField;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

}
