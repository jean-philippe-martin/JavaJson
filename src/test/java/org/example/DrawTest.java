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
    public void testListWithCommentsAfterLine() throws Exception {
        Screen screen = setupScreen(20,6);
        Drawer d = makeDrawer();

        String expected =
            "{•••••••••••••••••••\n"+
            "••\"numbers\":•[•//•2•\n"+
            "••••10,•//•ten••••••\n"+
            "••••11/*•eleven•*/••\n"+
            "••]•••••••••••••••••\n"+
            "}•••••••••••••••••••\n";
        JsonNode state = JsonNode.parseJson(
           " { \n"+
           "     \"numbers\": [ \n"+
           "       10, // ten \n"+
           "       11 /* eleven */\n"+
           "     ] \n"+
           " }");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testListWithCommentsAboveLine() throws Exception {
        Screen screen = setupScreen(20,6);
        Drawer d = makeDrawer();

        String expected =
            "{•••••••••••••••••••\n" + 
            "••\"numbers\":•[•//•2•\n" + 
            "••••//•ten••••••••••\n" + 
            "••••10••••••••••••••\n" + 
            "••••/*•eleven•*/••••\n" + 
            "••••\"11\"••••••••••••\n";
        JsonNode state = JsonNode.parseJson(
           " { \n"+
           "     \"numbers\": [ \n"+
           "       // ten\n"+
           "       10,\n"+
           "       /* eleven */\n"+
           "       \"11\"\n"+
           "     ] \n"+
           " }");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    @Test
    public void testMapWithCommentsAboveLine() throws Exception {
        Screen screen = setupScreen(20,6);
        Drawer d = makeDrawer();

        String expected =
           "{•••••••••••••••••••\n"+
           "••//•ten••••••••••••\n"+
           "••\"number\":•10••••••\n"+
           "••/*•eleven•*/••••••\n"+
           "••\"number2\":•11•••••\n"+
           "}•••••••••••••••••••\n";
        JsonNode state = JsonNode.parseJson(
           " { \n"+
           "     // ten\n"+
           "     \"number\": 10\n"+
           "     /* eleven */\n"+
           "     \"number2\": 11\n"+
           " }");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);

        assertEquals(expected, got);
    }

    /**
     * Parser-fed multiline block comments before keys: lines that trim to {@code * ...} continuations must not get a
     * synthetic {@code // } prefix in the drawer (that would show as {@code // * ...} and misrepresent the source).
     */
    @Test
    public void testMapWithMultilineCommentAboveLine() throws Exception {
        int w = 20;
        int h = 12;
        Screen screen = setupScreen(w, h);
        Drawer d = makeDrawer();

        String blank = "•".repeat(w) + "\n";
        String expected =
           "{•••••••••••••••••••\n" +
           "••/*•this•••••••••••\n" +
           "••*•comment•••••••••\n" +
           "••*•spans•3•lines•••\n" +
           "••*/••••••••••••••••\n" +
           "••\"number\":•10••••••\n" +
           "••/*•this•one•••••••\n"+
           "••too!•*/•••••••••••\n"+
           "••\"number2\":•11•••••\n"+
           "}•••••••••••••••••••\n" +
           blank.repeat(2);
        JsonNode state = JsonNode.parseJson(
           " { \n"+
           "     /* this \n"+
           "      * comment\n"+
           "      * spans 3 lines\n"+
           "      */\n"+
           "     \"number\": 10\n"+
           "     /* this one\n"+
           "        too! */\n"+
           "     \"number2\": 11\n"+
           " }");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);

        assertEquals(expected, got);
        String norm = got.replace('•', ' ');
        assertFalse(norm.contains("// *"), "block continuation lines must not be prefixed with //");
    }

    /**
     * Trailing trivia on same row as values: null, string, and folded inner map (cursor returned to root).
     */
    @Test
    public void testTrailingTriviaNullStringAndFoldedMap() throws Exception {
        int w = 45;
        int h = 14;
        Screen screen = setupScreen(w, h);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "{\n"
                        + "  \"n\": null, // nullT\n"
                        + "  \"s\": \"x\", // strT\n"
                        + "  \"m\": { \"q\": 1 } // foldT\n"
                        + "}");
        state.cursorDown();
        state.cursorDown();
        state.cursorDown();
        state.setFoldedAtCursors(true);
        state.cursorUp();
        state.cursorUp();
        state.cursorUp();
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);
        String blank = "•".repeat(w) + "\n";
        String expected =
                "{••••••••••••••••••••••••••••••••••••••••••••\n"
                + "••\"n\":•null,•//•nullT••••••••••••••••••••••••\n"
                + "••\"s\":•\"x\",•//•strT••••••••••••••••••••••••••\n"
                + "••\"m\":•{•...•}•••••••••••••••••••••••••••••••\n"
                + "•••••••//•foldT••••••••••••••••••••••••••••••\n"
                + "}••••••••••••••••••••••••••••••••••••••••••••\n"
                + blank.repeat(8);
        assertEquals(expected, got);
    }

    /** Preserved trailing comment after an inner map's closing brace; drawn on the row below that brace. */
    @Test
    public void testTrailingTriviaAfterMapClose() throws Exception {
        int w = 45;
        int h = 8;
        Screen screen = setupScreen(w, h);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "{\n"
                        + "  \"o\": {\n"
                        + "    \"a\": 1\n"
                        + "  } // mapTrail\n"
                        + "}");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);
        String blank = "•".repeat(w) + "\n";
        String expected =
                "{••••••••••••••••••••••••••••••••••••••••••••\n"
                + "••\"o\":•{•••••••••••••••••••••••••••••••••••••\n"
                + "••••\"a\":•1•••••••••••••••••••••••••••••••••••\n"
                + "••}••••••••••••••••••••••••••••••••••••••••••\n"
                + "••//•mapTrail••••••••••••••••••••••••••••••••\n"
                + "}••••••••••••••••••••••••••••••••••••••••••••\n"
                        + blank.repeat(2);
        assertEquals(expected, got);
    }

    /** Preserved trailing comment after a list's closing bracket; drawn on the row below that bracket. */
    @Test
    public void testTrailingTriviaAfterListClose() throws Exception {
        int w = 45;
        int h = 8;
        Screen screen = setupScreen(w, h);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "{\n"
                        + "  \"arr\": [ 1, 2 ] // listTrail\n"
                        + "}");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);
        String blank = "•".repeat(w) + "\n";
        String expected =
                "{••••••••••••••••••••••••••••••••••••••••••••\n"
                + "••\"arr\":•[•//•2•entries••••••••••••••••••••••\n"
                + "••••1••••••••••••••••••••••••••••••••••••••••\n"
                + "••••2••••••••••••••••••••••••••••••••••••••••\n"
                + "••]••••••••••••••••••••••••••••••••••••••••••\n"
                + "••//•listTrail•••••••••••••••••••••••••••••••\n"
                + "}••••••••••••••••••••••••••••••••••••••••••••\n"
                        + blank.repeat(1);
        assertEquals(expected, got);
    }

    /** Folded source comment truncates with Unicode ellipsis when wider than the terminal. */
    @Test
    public void testFoldedSourceCommentEllipsis() throws Exception {
        Screen screen = setupScreen(22, 5);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "{\n" +
                "  \"v\":\n" +
                "  // this_comment_is_far_too_wide_for_the_screen\n" +
                "  \"x\"\n" +
                "}");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);
        assertTrue(got.contains("\u2026"), "expected Unicode ellipsis in: " + got);
    }

    /** Unfolded preserved line comments: each non-empty trivia line is drawn (may share the key row). */
    @Test
    public void testUnfoldedSourceCommentsRendered() throws Exception {
        int w = 45;
        int h = 10;
        Screen screen = setupScreen(w, h);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "{\n" +
                "  \"k\":\n" +
                "  // line one\n" +
                "  // line two\n" +
                "  42\n" +
                "}");
        ((JsonNodeMap) state).getChild("k").setCommentFolded(false);
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);
        String blank = "•".repeat(w) + "\n";
        String expected =
                "{••••••••••••••••••••••••••••••••••••••••••••\n" +
                "••\"k\":•//•line•one•••••••••••••••••••••••••••\n" +
                "•••••••//•line•two•••••••••••••••••••••••••••\n" +
                "•••••••42••••••••••••••••••••••••••••••••••••\n" +
                "}••••••••••••••••••••••••••••••••••••••••••••\n" +
                blank.repeat(5);
        assertEquals(expected, got);
    }

    /** Default folded leading comment: trivia already starting with // is not prefixed again. */
    @Test
    public void testFoldedSourceCommentSingleLineRendering() throws Exception {
        int w = 45;
        int h = 10;
        Screen screen = setupScreen(w, h);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "{\n"
                + "  \"k\":\n"
                + "  // short\n"
                + "  7\n"
                + "}");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);
        String blank = "•".repeat(w) + "\n";
        String expected =
                "{••••••••••••••••••••••••••••••••••••••••••••\n" +
                "••\"k\":•//•short••••••••••••••••••••••••••••••\n" +
                "•••••••7•••••••••••••••••••••••••••••••••••••\n" +
                "}••••••••••••••••••••••••••••••••••••••••••••\n" +
                blank.repeat(6);
        assertEquals(expected, got);
    }

    /** Inline {@code , // ...} after a scalar is drawn after the value (Hjson trivia after comma). */
    @Test
    public void testDrawInlineTrailingCommentAfterBoolean() throws Exception {
        Screen screen = setupScreen(56, 12);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "{\n"
                + "  \"debugMode\": true, // Another inline comment\n"
                + "  \"logLevel\": \"DEBUG\"\n"
                + "}");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String norm = extractAsString(screen).replace('•', ' ');
        assertTrue(norm.contains("Another inline comment"), norm);
        assertTrue(norm.contains("true"));
    }

    /** Comment line before a key suppresses synthetic epoch annotation on the value. */
    @Test
    public void testDrawSuppressesEpochAnnotationWhenCommentBeforeTimestampKey() throws Exception {
        Screen screen = setupScreen(48, 10);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "{\n" + "  # hash comment\n" + "  \"timestamp\": 1678886400000\n" + "}");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String norm = extractAsString(screen).replace('•', ' ');
        assertFalse(norm.contains("2023"), "synthetic date should not duplicate source comments: " + norm);
    }

    /** Block-style trivia is drawn as stored (no extra "// " prefix). */
    @Test
    public void testUnfoldedBlockSourceCommentRendered() throws Exception {
        Screen screen = setupScreen(40, 8);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "{\n"
                + "  \"k\":\n"
                + "  /* block note */\n"
                + "  0\n"
                + "}");
        ((JsonNodeMap) state).getChild("k").setCommentFolded(false);
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String got = extractAsString(screen);
        String norm = got.replace('•', ' ');
        assertTrue(norm.contains("/* block note */"), got);
        assertTrue(norm.contains("0"), got);
    }

    /** Leading trivia on a list element is drawn above that element. */
    @Test
    public void testUnfoldedSourceCommentOnListElement() throws Exception {
        Screen screen = setupScreen(36, 8);
        Drawer d = makeDrawer();
        JsonNode root = JsonNode.parseJson("[\n" + "  // above elem\n" + "  99\n" + "]");
        ((JsonNodeList) root).get(0).setCommentFolded(false);
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, root, null);
        String got = extractAsString(screen);
        String norm = got.replace('•', ' ');
        assertTrue(norm.contains("// above elem"), got);
        assertTrue(norm.contains("99"), got);
    }

    /** Comment before the root object (document leading trivia). */
    @Test
    public void testDrawDocumentLeadingCommentBeforeRootBrace() throws Exception {
        Screen screen = setupScreen(48, 10);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "// file begins here\n"
                        + "{\n"
                        + "  \"a\": 1\n"
                        + "}\n");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String norm = extractAsString(screen).replace('•', ' ');
        assertTrue(norm.contains("file begins here"), norm);
        assertTrue(norm.contains("\"a\""), norm);
    }

    /** Comment after the root object's closing brace (document trailing trivia). */
    @Test
    public void testDrawDocumentTrailingCommentAfterRootBrace() throws Exception {
        Screen screen = setupScreen(48, 10);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "{\n"
                        + "  \"a\": 1\n"
                        + "}\n"
                        + "// file ends here\n");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String norm = extractAsString(screen).replace('•', ' ');
        assertTrue(norm.contains("file ends here"), norm);
    }

    /**
     * Comment on a new line after {@code ,} belongs to the next member's key-leading trivia (not the previous
     * value's trailing).
     */
    @Test
    public void testDrawKeyLeadingCommentAfterCommaBeforeNextMember() throws Exception {
        Screen screen = setupScreen(52, 10);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "{\n"
                        + "  \"first\": 1,\n"
                        + "  // belongs to second\n"
                        + "  \"second\": 2\n"
                        + "}\n");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String norm = extractAsString(screen).replace('•', ' ');
        assertTrue(norm.contains("belongs to second"), norm);
        assertTrue(norm.contains("\"second\""), norm);
    }

    /**
     * Same-line {@code //} after {@code :} (before the newline) is stored as key-trailing trivia
     * (see {@link JsonNodeMap#getKeyTrailingTrivia(String)}); the drawer does not render it yet, so the screen
     * must still show the value without losing the tree.
     */
    @Test
    public void testDrawKeyTrailingColonLineCommentNotRenderedYetTreeStillValid() throws Exception {
        Screen screen = setupScreen(48, 8);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson("{\n  \"x\": // on colon line\n  1\n}");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String norm = extractAsString(screen).replace('•', ' ');
        assertTrue(norm.contains("1"), norm);
        assertTrue(norm.contains("\"x\""), norm);
    }

    /**
     * Block comment between the key and {@code :} is key-trailing trivia; not drawn yet, but the value must appear.
     */
    @Test
    public void testDrawKeyTrailingBlockBetweenKeyAndColonNotRenderedYetValueShown() throws Exception {
        Screen screen = setupScreen(52, 8);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "{\n"
                        + "  \"k\" /* by key */ : 0\n"
                        + "}\n");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String norm = extractAsString(screen).replace('•', ' ');
        assertTrue(norm.contains("0"), norm);
        assertTrue(norm.contains("\"k\""), norm);
    }

    /** Key-leading trivia inside a nested object (not only top-level keys). */
    @Test
    public void testDrawNestedMapKeyLeadingComment() throws Exception {
        Screen screen = setupScreen(52, 12);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "{\n"
                        + "  \"outer\": {\n"
                        + "    // inner key note\n"
                        + "    \"inner\": 3\n"
                        + "  }\n"
                        + "}\n");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String norm = extractAsString(screen).replace('•', ' ');
        assertTrue(norm.contains("inner key note"), norm);
        assertTrue(norm.contains("\"inner\""), norm);
    }

    /** Top-level array: leading comment before {@code [} and trailing after {@code ]}. */
    @Test
    public void testDrawTopLevelArrayDocumentLeadingAndTrailingComments() throws Exception {
        Screen screen = setupScreen(48, 12);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "// before array\n"
                        + "[ 7, 8 ]\n"
                        + "// after array\n");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String norm = extractAsString(screen).replace('•', ' ');
        assertTrue(norm.contains("before array"), norm);
        assertTrue(norm.contains("after array"), norm);
        assertTrue(norm.contains("7"), norm);
    }

    /** Trailing line comment after the last array element (same line, before {@code ]}). */
    @Test
    public void testDrawTrailingCommentAfterLastArrayElementBeforeClose() throws Exception {
        Screen screen = setupScreen(52, 10);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "{\n"
                        + "  \"items\": [ 1, 2 ] // tail on line with ]\n"
                        + "}\n");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String norm = extractAsString(screen).replace('•', ' ');
        assertTrue(norm.contains("tail on line"), norm);
    }

    /** Root is a scalar: leading comment only (no wrapping object). */
    @Test
    public void testDrawRootScalarWithLeadingComment() throws Exception {
        Screen screen = setupScreen(40, 6);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson("// root is just a number\n42\n");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String norm = extractAsString(screen).replace('•', ' ');
        assertTrue(norm.contains("root is just"), norm);
        assertTrue(norm.contains("42"), norm);
    }

    /** String map value with trailing {@code //} on the same line as the closing quote. */
    @Test
    public void testDrawTrailingCommentAfterStringValueInMap() throws Exception {
        Screen screen = setupScreen(56, 8);
        Drawer d = makeDrawer();
        JsonNode state = JsonNode.parseJson(
                "{\n"
                        + "  \"msg\": \"hi\" // string trail\n"
                        + "}\n");
        d.printJsonTree(screen.newTextGraphics(), TerminalPosition.TOP_LEFT_CORNER, 0, state, null);
        String norm = extractAsString(screen).replace('•', ' ');
        assertTrue(norm.contains("string trail"), norm);
        assertTrue(norm.contains("hi"), norm);
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
