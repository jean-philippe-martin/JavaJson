package org.example;

import com.googlecode.lanterna.TerminalSize;
import org.junit.Test;
import static org.junit.Assert.*;


import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.screen.VirtualScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;


public class DrawTest {

    public static String extractAsString(Screen screen) {
        StringBuilder ret = new StringBuilder();
        for (int row=0; row<screen.getTerminalSize().getRows(); row++) {
            for (int col=0; col<screen.getTerminalSize().getColumns(); col++) {
                String c = screen.getBackCharacter(col, row).getCharacterString();
                if (" ".equals(c)) c="•";
                ret.append(c);
            }
            ret.append("\n");
        }
        return ret.toString();
    }

    public Screen setupScreen(int width, int height) throws IOException {
        Terminal term = new DefaultVirtualTerminal(new TerminalSize(width, height));
        Screen screen = new TerminalScreen(term);
        screen.startScreen();
        screen.clear();
        return screen;
    }

    @Test
    public void testSimpleObject() throws Exception {
        Screen screen = setupScreen(20,4);

        String expected = """
            {•••••••••••••••••••
            ••"one":•"hello"••••
            ••"two":•"world"••••
            }•••••••••••••••••••
            """;
        JsonState state = JsonState.parseJson("""
            {
                "one": "hello",
                "two": "world"
            }""");
        Drawer.printJsonObject(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testNestedObject() throws Exception {
        Screen screen = setupScreen(25,7);

        String expected = """
            {••••••••••••••••••••••••
            ••"one":•{•••••••••••••••
            ••••"prime":•"nope"••••••
            ••••"positive":•"yup"••••
            ••}••••••••••••••••••••••
            ••"two":•"more"••••••••••
            }••••••••••••••••••••••••
            """;
        JsonState state = JsonState.parseJson("""
            {
                "one": {
                    "prime": "nope",
                    "positive": "yup"
                },
                "two": "more"
            }""");
        Drawer.printJsonObject(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testTripleNestedObject() throws Exception {
        Screen screen = setupScreen(30,12);

        String expected = """
            {•••••••••••••••••••••••••••••
            ••"one":•{••••••••••••••••••••
            ••••"prime":•"nope"•••••••••••
            ••••"more_facts":•{•••••••••••
            ••••••"negative":•"negative"••
            ••••••"positive":•"yes"•••••••
            ••••••"zero":•"nope"••••••••••
            ••••}•••••••••••••••••••••••••
            ••}•••••••••••••••••••••••••••
            ••"two":•"more"•••••••••••••••
            }•••••••••••••••••••••••••••••
            ••••••••••••••••••••••••••••••
            """;
        JsonState state = JsonState.parseJson("""
            {
                "one": {
                    "prime": "nope",
                    "more_facts": {
                        "negative": "negative",
                        "positive": "yes",
                        "zero": "nope"
                    }
                },
                "two": "more"
            }""");
        Drawer.printJsonObject(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);

        assertEquals(expected, got);

        state.cursorDown();
        Drawer.printJsonObject(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        got = extractAsString(screen);
        assertEquals(expected, got);
    }

    @Test
    public void testPreviouslyBrokenObject() throws Exception {
        Screen screen = setupScreen(30,12);

        String expected = """
           {•••••••••••••••••••••••••••••
           ••"greeting":•"hello"•••••••••
           ••"who":•"world"••••••••••••••
           ••"nested":•{•••••••••••••••••
           ••••"again":•{••••••••••••••••
           ••••••"depth":•"two"••••••••••
           ••••}•••••••••••••••••••••••••
           ••••"depth":•"one"••••••••••••
           ••••"sub_2":•"two"••••••••••••
           ••}•••••••••••••••••••••••••••
           ••"conclusion":•"good•bye"••••
           }•••••••••••••••••••••••••••••
           """;
        JsonState state = JsonState.parseJson("""
            {
               "greeting": "hello",
               "who": "world",
               "nested": {
                 "again": {
                   "depth": "two"
                 },
                 "depth": "one",
                 "sub_2": "two"
               },
               "conclusion": "good bye"
             }""");
        Drawer.printJsonObject(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);

        assertEquals(expected, got);

        for (int i=0; i<5; i++) {
            state.cursorDown();
            Drawer.printJsonObject(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
            got = extractAsString(screen);
            assertEquals(expected, got);
        }
    }

    @Test
    public void testSimplePin() throws Exception {
        Screen screen = setupScreen(20,4);

        String expected = """
            {•...•••••••••••••••
            P•"two":•"world"••••
            }•••••••••••••••••••
            ••••••••••••••••••••
            """;
        JsonState state = JsonState.parseJson("""
            {
                "one": "hello",
                "two": "world"
            }""");
        // put pin at "two"
        state.cursorDown();
        state.cursorDown();
        state.togglePinnedAtCursor();
        // fold root
        state.cursorParent();
        state.setFoldedAtCursor(true);
        Drawer.printJsonObject(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testNestedPin() throws Exception {
        Screen screen = setupScreen(20,5);

        String expected = """
            {•...•••••••••••••••
            ••"nested":•{•...•••
            P•••"two":•"world"••
            ••}•••••••••••••••••
            }•••••••••••••••••••
            """;
        JsonState state = JsonState.parseJson("""
            {
                "nested": {
                    "one": "hello",
                    "two": "world"
                }
            }""");
        // put pin at "two"
        state.cursorDown();
        state.cursorDown();
        state.cursorDown();
        state.togglePinnedAtCursor();
        // fold root
        state.cursorParent();
        state.cursorParent();
        state.setFoldedAtCursor(true);
        Drawer.printJsonObject(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testSimpleList() throws Exception {
        Screen screen = setupScreen(20,6);

        String expected = """
            {•••••••••••••••••••
            ••"numbers":•[••••••
            ••••10••••••••••••••
            ••••11••••••••••••••
            ••]•••••••••••••••••
            }•••••••••••••••••••
            """;
        JsonState state = JsonState.parseJson("""
            {
                "numbers": [
                  10,
                  11
                ]
            }""");
        Drawer.printJsonObject(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }
}
