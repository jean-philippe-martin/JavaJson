package org.example;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import org.example.ui.AggregateMenu;
import org.junit.Test;

import java.io.IOException;
import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Make sure that the aggregations are displayed reasonably.
 * In particular, the "//" symbols line up and are indented in a way that makes sense.
 **/
public class DrawAggTest {

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
    public void testSumOfList() throws Exception {

        String[] input = new String[]{
                "[ 5,7,10,12 ]"
        };

        Main main = Main.fromLinesAndVirtual(input, 20, 4);
        main.applyAggregation(AggregateMenu.Choice.AGG_TOTAL);
        main.display();

        String expected =
            "[•//•4•entries••••••\n"+
            "••//•sum()•34.0•••••\n"+
            "••5•••••••••••••••••\n";

        String got = main.getTestViewOfScreen();

        // Check only the start of the string to ignore the status bar
        assert(got.startsWith(expected));
    }

    @Test
    public void testSumLenths() throws Exception {

        String[] input = new String[]{
                "[ [5], [7,10] ]"
        };

        Main main = Main.fromLinesAndVirtual(input, 25, 4);
        main.applyAggregation(AggregateMenu.Choice.AGG_TOTAL);
        main.display();

        String expected =
                "[•//•2•entries•••••••••••\n"+
                "••//•sum_length()•3••••••\n"+
                "••[•//•1•entry•••••••••••\n";

        String got = main.getTestViewOfScreen();

        // Check only the start of the string to ignore the status bar
        // (but do it in a way that'll give us the "click to see diff" button.
        String beginning = got.substring(0, expected.length());
        assertEquals(expected, beginning);
    }

    @Test
    public void testMinMaxOfList() throws Exception {

        String[] input = new String[]{
                "[ 5,7,10,12 ]"
        };

        Main main = Main.fromLinesAndVirtual(input, 30, 4);
        main.applyAggregation(AggregateMenu.Choice.AGG_MIN_MAX);
        main.display();

        String expected =
                "[•//•4•entries••••••••••••••••\n"+
                "••//•min_max()•\"5.0•-•12.0\"•••\n"+
                "••5•••••••••••••••••••";

        String got = main.getTestViewOfScreen();

        // Check only the start of the string to ignore the status bar
        // (but do it in a way that'll give us the "click to see diff" button.
        String beginning = got.substring(0, expected.length());
        assertEquals(expected, beginning);
    }


    @Test
    public void testUniqueKeys() throws Exception {

        String[] input = new String[]{
                "[\n" +
                "    {\"a\": 1, \"b\": 2},\n" +
                "    {\"b\": 1, \"c\": 2}\n" +
                "]"
        };

        Main main = Main.fromLinesAndVirtual(input, 30, 7);
        main.applyAggregation(AggregateMenu.Choice.UNIQUE_FIELDS);
        main.display();

        String expected =
                "[•//•2•entries••••••••••••••••\n" +
                "••//•unique_keys()•{••••••••••\n" +
                "••//•••••50%•\"a\"••••••••••••••\n" +
                "••//•••=100%•\"b\"••••••••••••••\n" +
                "••//•••••50%•\"c\"••••••••••••••\n" +
                "••//•}••••••••••••••••••••••••";

        String got = main.getTestViewOfScreen();

        // Check only the start of the string to ignore the status bar
        // (but do it in a way that'll give us the "click to see diff" button.
        String beginning = got.substring(0, expected.length());
        assertEquals(expected, beginning);
    }



}
