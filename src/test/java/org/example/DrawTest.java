package org.example;

import com.googlecode.lanterna.TerminalSize;
import org.junit.Test;
import static org.junit.Assert.*;


import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.util.regex.Matcher;


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

    public static Screen setupScreen(int width, int height) throws IOException {
        Terminal term = new DefaultVirtualTerminal(new TerminalSize(width, height));
        Screen screen = new TerminalScreen(term);
        screen.startScreen();
        screen.clear();
        return screen;
    }

    @Test
    public void testSimpleObject() throws Exception {
        Screen screen = setupScreen(20,4);
        Drawer d = new Drawer();
        String expected = """
            {•••••••••••••••••••
            ••"one":•"hello"••••
            ••"two":•"world"••••
            }•••••••••••••••••••
            """;
        JsonNode state = JsonNode.parseJson("""
            {
                "one": "hello",
                "two": "world"
            }""");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testNestedObject() throws Exception {
        Screen screen = setupScreen(25,7);
        Drawer d = new Drawer();

        String expected = """
            {••••••••••••••••••••••••
            ••"one":•{•••••••••••••••
            ••••"prime":•"nope"••••••
            ••••"positive":•"yup"••••
            ••}••••••••••••••••••••••
            ••"two":•"more"••••••••••
            }••••••••••••••••••••••••
            """;
        JsonNode state = JsonNode.parseJson("""
            {
                "one": {
                    "prime": "nope",
                    "positive": "yup"
                },
                "two": "more"
            }""");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testTripleNestedObject() throws Exception {
        Screen screen = setupScreen(30,12);
        Drawer d = new Drawer();

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
        JsonNode state = JsonNode.parseJson("""
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
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);
        assertEquals(expected, got);

    }

    @Test
    public void testPreviouslyBrokenObject() throws Exception {
        Screen screen = setupScreen(30,12);
        Drawer d = new Drawer();

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
        JsonNode state = JsonNode.parseJson("""
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
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);
        assertEquals(expected, got);
    }

    @Test
    public void testSimplePin() throws Exception {
        Screen screen = setupScreen(20,4);
        Drawer d = new Drawer();

        String expected = """
            {•...•••••••••••••••
            P•"two":•"world"••••
            }•••••••••••••••••••
            ••••••••••••••••••••
            """;
        JsonNode state = JsonNode.parseJson("""
            {
                "one": "hello",
                "two": "world"
            }""");
        // put pin at "two"
        state.cursorDown();
        state.cursorDown();
        state.setPinnedAtCursors(true);
        assertEquals(true, state.getPinnedAtCursor());
        // fold root
        state.cursorParent();
        state.setFoldedAtCursors(true);
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testSimplePin2() throws Exception {
        Screen screen = setupScreen(20,7);
        Drawer d = new Drawer();

        String expected = """
            {•...•••••••••••••••
            ••"players":•{•...••
            ••••"Alex":•{•...•••
            P•••••"score":•10•••
            ••••}•••••••••••••••
            ••}•••••••••••••••••
            }•••••••••••••••••••
            """;
        JsonNode state = JsonNode.parseJson("""
                {
                  "players": {
                    "Alex": {
                      "score": 10,
                      "category": "heavyweight",
                      "age": 32
                    },
                    "Bob": {
                      "score": 35,
                      "category": "heavyweight",
                      "age": 36,
                      "requests": "pillow on chair"
                    }
                  }
                }""");
        // put pin at "Alex.score"
        state.cursorDown();
        state.cursorDown();
        state.cursorDown();
        state.setPinnedAtCursors(true);
        assertEquals(true, state.getPinnedAtCursor());
        // fold root
        state.cursorParent();
        state.cursorParent();
        state.cursorParent();
        state.setFoldedAtCursors(true);
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testNestedPin() throws Exception {
        Screen screen = setupScreen(20,5);
        Drawer d = new Drawer();

        String expected = """
            {•...•••••••••••••••
            ••"nested":•{•...•••
            P•••"two":•"world"••
            ••}•••••••••••••••••
            }•••••••••••••••••••
            """;
        JsonNode state = JsonNode.parseJson("""
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
        state.setPinnedAtCursors(true);
        // fold root
        state.cursorParent();
        state.cursorParent();
        state.setFoldedAtCursors(true);
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testSimpleList() throws Exception {
        Screen screen = setupScreen(20,6);
        Drawer d = new Drawer();

        String expected = """
            {•••••••••••••••••••
            ••"numbers":•[•//•2•
            ••••10••••••••••••••
            ••••11••••••••••••••
            ••]•••••••••••••••••
            }•••••••••••••••••••
            """;
        JsonNode state = JsonNode.parseJson("""
            {
                "numbers": [
                  10,
                  11
                ]
            }""");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testSimpleListFolded() throws Exception {
        Screen screen = setupScreen(30,3);
        Drawer d = new Drawer();

        String expected = """
            {•••••••••••••••••••••••••••••
            >>"numbers":•[•...•]•//•2•entr
            }•••••••••••••••••••••••••••••
            """;
        JsonNode state = JsonNode.parseJson("""
            {
                "numbers": [
                  10,
                  11
                ]
            }""");
        state.cursorDown();
        state.setFoldedAtCursors(true);
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testMatcher() {
        String foo = "#1234af";
        Matcher m = Drawer.colorPattern.matcher(foo);
        m.find();
        assertTrue(m.hasMatch());
        assertEquals(3, m.groupCount());
        assertEquals("12", m.group(1));
        assertEquals("34", m.group(2));
        assertEquals("af", m.group(3));
        int value = Integer.parseInt( m.group(3), 16);
        assertEquals(0xaf, value);
    }

    @Test
    public void testPinnedListWontFold() throws Exception {
        Screen screen = setupScreen(20,6);
        Drawer d = new Drawer();

        String expected = """
            {•...•••••••••••••••
            P•"numbers":•[•//•2•
            ••••10••••••••••••••
            ••••11••••••••••••••
            ••]•••••••••••••••••
            }•••••••••••••••••••
            """;
        JsonNode node = JsonNode.parseJson("""
            {
                "letters": [
                  "a",
                  "b"
                ],
                "numbers": [
                  10,
                  11
                ]
            }""");
        // pin 'numbers'
        node.cursorDown();
        node.cursorDown();
        node.cursorDown();
        node.cursorDown();
        assertEquals(".numbers", node.rootInfo.userCursor.toString());
        node.setPinnedAtCursors(true);
        // fold at root
        node.cursorUp();
        node.cursorUp();
        node.cursorUp();
        node.cursorUp();
        node.setFoldedAtCursors(true);
        // render
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, node);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testPinnedMapWontFold() throws Exception {
        Screen screen = setupScreen(20,6);
        Drawer d = new Drawer();

        String expected = """
            {•...•••••••••••••••
            P•"numbers":•{••••••
            ••••"ten":•10•••••••
            ••••"eleven":•11••••
            ••}•••••••••••••••••
            }•••••••••••••••••••
            """;
        JsonNode node = JsonNode.parseJson("""
            {
                "letters": [
                  "a",
                  "b"
                ],
                "numbers": {
                  "ten": 10,
                  "eleven": 11
                }
            }""");
        // pin 'numbers'
        node.cursorDown();
        node.cursorDown();
        node.cursorDown();
        node.cursorDown();
        assertEquals(".numbers", node.rootInfo.userCursor.toString());
        node.setPinnedAtCursors(true);
        // fold at root
        node.cursorUp();
        node.cursorUp();
        node.cursorUp();
        node.cursorUp();
        node.setFoldedAtCursors(true);
        // render
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, node);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }
}
