package org.example;

import com.googlecode.lanterna.TerminalSize;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;


public class DrawTest {

    public static Drawer makeDrawer() {
        return new Drawer(Locale.US);
    }

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
        Drawer d = makeDrawer();
        String expected =
            "{•••••••••••••••••••\n"+
            "••\"one\":•\"hello\"••••\n"+
            "••\"two\":•\"world\"••••\n"+
            "}•••••••••••••••••••\n";
        JsonNode state = JsonNode.parseJson(
            "{"+
            "    \"one\": \"hello\",\n"+
            "    \"two\": \"world\"\n"+
            "}\n");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testNestedObject() throws Exception {
        Screen screen = setupScreen(25,7);
        Drawer d = makeDrawer();

        String expected =
            "{••••••••••••••••••••••••\n"+
            "••\"one\":•{•••••••••••••••\n"+
            "••••\"prime\":•\"nope\"••••••\n"+
            "••••\"positive\":•\"yup\"••••\n"+
            "••}••••••••••••••••••••••\n"+
            "••\"two\":•\"more\"••••••••••\n"+
            "}••••••••••••••••••••••••\n";
        JsonNode state = JsonNode.parseJson(
            "{ \n"+
            "    \"one\": { \n"+
            "        \"prime\": \"nope\", \n"+
            "        \"positive\": \"yup\" \n"+
            "    }, \n"+
            "    \"two\": \"more\" \n"+
            "}");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testTripleNestedObject() throws Exception {
        Screen screen = setupScreen(30,12);
        Drawer d = makeDrawer();

        String expected = 
            "{•••••••••••••••••••••••••••••\n"+
            "••\"one\":•{••••••••••••••••••••\n"+
            "••••\"prime\":•\"nope\"•••••••••••\n"+
            "••••\"more_facts\":•{•••••••••••\n"+
            "••••••\"negative\":•\"negative\"••\n"+
            "••••••\"positive\":•\"yes\"•••••••\n"+
            "••••••\"zero\":•\"nope\"••••••••••\n"+
            "••••}•••••••••••••••••••••••••\n"+
            "••}•••••••••••••••••••••••••••\n"+
            "••\"two\":•\"more\"•••••••••••••••\n"+
            "}•••••••••••••••••••••••••••••\n"+
            "••••••••••••••••••••••••••••••\n";
        JsonNode state = JsonNode.parseJson(
"            { \n"+
"                \"one\": { \n"+
"                    \"prime\": \"nope\", \n"+
"                    \"more_facts\": { \n"+
"                        \"negative\": \"negative\", \n"+
"                        \"positive\": \"yes\", \n"+
"                        \"zero\": \"nope\" \n"+
"                    } \n"+
"                }, \n"+
"                \"two\": \"more\" \n"+
"            } \n");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);
        assertEquals(expected, got);

    }

    @Test
    public void testPreviouslyBrokenObject() throws Exception {
        Screen screen = setupScreen(30,12);
        Drawer d = makeDrawer();

        String expected =
           "{•••••••••••••••••••••••••••••\n"+
           "••\"greeting\":•\"hello\"•••••••••\n"+
           "••\"who\":•\"world\"••••••••••••••\n"+
           "••\"nested\":•{•••••••••••••••••\n"+
           "••••\"again\":•{••••••••••••••••\n"+
           "••••••\"depth\":•\"two\"••••••••••\n"+
           "••••}•••••••••••••••••••••••••\n"+
           "••••\"depth\":•\"one\"••••••••••••\n"+
           "••••\"sub_2\":•\"two\"••••••••••••\n"+
           "••}•••••••••••••••••••••••••••\n"+
           "••\"conclusion\":•\"good•bye\"••••\n"+
           "}•••••••••••••••••••••••••••••\n";
        JsonNode state = JsonNode.parseJson(
           " { \n"+
           "    \"greeting\": \"hello\", \n"+
           "    \"who\": \"world\", \n"+
           "    \"nested\": { \n"+
           "      \"again\": { \n"+
           "        \"depth\": \"two\" \n"+
           "      }, \n"+
           "      \"depth\": \"one\", \n"+
           "      \"sub_2\": \"two\" \n"+
           "    }, \n"+
           "    \"conclusion\": \"good bye\" \n"+
           "  } \n");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);
        assertEquals(expected, got);
    }

    @Test
    public void testSimplePin() throws Exception {
        Screen screen = setupScreen(20,4);
        Drawer d = makeDrawer();

        String expected = 
            "{•...•••••••••••••••\n"+
            "P•\"two\":•\"world\"••••\n"+
            "}•••••••••••••••••••\n"+
            "••••••••••••••••••••\n";
        JsonNode state = JsonNode.parseJson(
            "{ \n"+
            "    \"one\": \"hello\", \n"+
            "    \"two\": \"world\" \n"+
            "} \n");
        // put pin at "two"
        state.cursorDown();
        state.cursorDown();
        state.setPinnedAtCursors(true);
        assertEquals(true, state.getPinnedAtCursor());
        // fold root
        state.cursorParent();
        state.setFoldedAtCursors(true);
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testSimplePin2() throws Exception {
        Screen screen = setupScreen(20,7);
        Drawer d = makeDrawer();

        String expected = 
            "{•...•••••••••••••••\n"+
            "••\"players\":•{•...••\n"+
            "••••\"Alex\":•{•...•••\n"+
            "P•••••\"score\":•10•••\n"+
            "••••}•••••••••••••••\n"+
            "••}•••••••••••••••••\n"+
            "}•••••••••••••••••••\n";
        JsonNode state = JsonNode.parseJson(
               " { \n"+
               "   \"players\": { \n"+
               "     \"Alex\": { \n"+
               "       \"score\": 10, \n"+
               "       \"category\": \"heavyweight\", \n"+
               "       \"age\": 32 \n"+
               "     }, \n"+
               "     \"Bob\": { \n"+
               "       \"score\": 35, \n"+
               "       \"category\": \"heavyweight\", \n"+
               "       \"age\": 36, \n"+
               "       \"requests\": \"pillow on chair\" \n"+
               "     } \n"+
               "   } \n"+
               " } \n");
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
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testNestedPin() throws Exception {
        Screen screen = setupScreen(20,5);
        Drawer d = makeDrawer();

        String expected =
            "{•...•••••••••••••••\n"+
            "••\"nested\":•{•...•••\n"+
            "P•••\"two\":•\"world\"••\n"+
            "••}•••••••••••••••••\n"+
            "}•••••••••••••••••••\n";
        JsonNode state = JsonNode.parseJson(
           " { \n"+
           "     \"nested\": { \n"+
           "         \"one\": \"hello\", \n"+
           "         \"two\": \"world\" \n"+
           "     } \n"+
           " } \n");
        // put pin at "two"
        state.cursorDown();
        state.cursorDown();
        state.cursorDown();
        state.setPinnedAtCursors(true);
        // fold root
        state.cursorParent();
        state.cursorParent();
        state.setFoldedAtCursors(true);
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testSimpleList() throws Exception {
        Screen screen = setupScreen(20,6);
        Drawer d = makeDrawer();

        String expected =
            "{•••••••••••••••••••\n"+
            "••\"numbers\":•[•//•2•\n"+
            "••••10••••••••••••••\n"+
            "••••11••••••••••••••\n"+
            "••]•••••••••••••••••\n"+
            "}•••••••••••••••••••\n";
        JsonNode state = JsonNode.parseJson(
           " { \n"+
           "     \"numbers\": [ \n"+
           "       10, \n"+
           "       11 \n"+
           "     ] \n"+
           " }");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testSimpleListFolded() throws Exception {
        Screen screen = setupScreen(30,3);
        Drawer d = makeDrawer();

        String expected =
            "{•••••••••••••••••••••••••••••\n"+
            ">>\"numbers\":•[•...•]•//•2•entr\n"+
            "}•••••••••••••••••••••••••••••\n";
        JsonNode state = JsonNode.parseJson(
            "{\n"+
            "    \"numbers\": [\n"+
            "      10,\n"+
            "      11\n"+
            "    ]\n"+
            "}\n");
        state.cursorDown();
        state.setFoldedAtCursors(true);
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testMatcher() {
        String foo = "#1234af";
        Matcher m = Drawer.colorPattern.matcher(foo);
        boolean found = m.find();
        assertTrue(found);
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
        Drawer d = makeDrawer();

        String expected =
            "{•...•••••••••••••••\n"+
            "P•\"numbers\":•[•//•2•\n"+
            "••••10••••••••••••••\n"+
            "••••11••••••••••••••\n"+
            "••]•••••••••••••••••\n"+
            "}•••••••••••••••••••\n";
        JsonNode node = JsonNode.parseJson(
           " { \n"+
           "     \"letters\": [ \n"+
           "       \"a\", \n"+
           "       \"b\" \n"+
           "     ], \n"+
           "     \"numbers\": [ \n"+
           "       10, \n"+
           "       11 \n"+
           "     ] \n"+
           " } \n");
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
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, node, null);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testPinnedMapWontFold() throws Exception {
        Screen screen = setupScreen(20,6);
        Drawer d = makeDrawer();

        String expected =
            "{•...•••••••••••••••\n"+
            "P•\"numbers\":•{••••••\n"+
            "••••\"ten\":•10•••••••\n"+
            "••••\"eleven\":•11••••\n"+
            "••}•••••••••••••••••\n"+
            "}•••••••••••••••••••\n";
        JsonNode node = JsonNode.parseJson(
           " { \n"+
           "     \"letters\": [ \n"+
           "       \"a\", \n"+
           "       \"b\" \n"+
           "     ], \n"+
           "     \"numbers\": { \n"+
           "       \"ten\": 10, \n"+
           "       \"eleven\": 11 \n"+
           "     } \n"+
           " } \n");
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
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, node, null);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }
}
