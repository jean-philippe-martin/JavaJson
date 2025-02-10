package org.example;

import com.googlecode.lanterna.input.KeyStroke;
import org.junit.Test;

import static org.junit.Assert.*;

public class GroupbyTest {


    // Yes I treat "groupby" as a single word.
    @Test
    public void testBasicGroupby() throws Exception {
        String[] lines = new String[] {
                "[",
                "  { ",
                "    \"c\": \"London\", ",
                "    \"s\": 12 ",
                "  }, ",
                "  { ",
                "    \"c\": \"Paris\", ",
                "    \"s\": 4 ",
                "  }, ",
                "  { ",
                "    \"c\": \"London\", ",
                "    \"s\": 24 ",
                "  }] ",
        };
        Main main = Main.fromLinesAndVirtual(lines, 40, 20);

        main.moveCursorDown();
        main.moveCursorDown();
        // we should now be at the "c"

        // Do the groupby
        main.actOnKey(KeyStroke.fromString("g"));

        main.display();
        String got = main.getTestViewOfScreen();

        String expected =
                "{•//•grouped•by•c•••••••••••••••••••••••\n"+
                "••\"London\":•[•//•2•entries••••••••••••••\n"+
                "••••{•••••••••••••••••••••••••••••••••••\n"+
                "••••••\"c\":•\"London\"•••••••••••••••••••••\n"+
                "••••••\"s\":•12•••••••••••••••••••••••••••\n"+
                "••••}•••••••••••••••••••••••••••••••••••\n"+
                "••••{•••••••••••••••••••••••••••••••••••\n"+
                "••••••\"c\":•\"London\"•••••••••••••••••••••\n"+
                "••••••\"s\":•24•••••••••••••••••••••••••••\n"+
                "••••}•••••••••••••••••••••••••••••••••••\n"+
                "••]•••••••••••••••••••••••••••••••••••••\n"+
                "••\"Paris\":•[•//•1•entry•••••••••••••••••\n"+
                "••••{•••••••••••••••••••••••••••••••••••\n"+
                "••••••\"c\":•\"Paris\"••••••••••••••••••••••\n"+
                "••••••\"s\":•4••••••••••••••••••••••••••••\n"+
                "••••}•••••••••••••••••••••••••••••••••••\n"+
                "••]•••••••••••••••••••••••••••••••••••••\n"+
                "}•••••••••••••••••••••••••••••••••••••••\n"+
                "••••••••••••••••••••••••••••••••••••••••\n"+
                "••••••••••••••••••••••••••••••••••••••••\n";

        assertEquals(expected, got);
    }

    @Test
    public void testGroupbyWithOtherStuff() throws Exception {
        String[] lines = new String[] {
                "[",
                "  { ",
                "    \"c\": \"London\", ",
                "    \"s\": 12 ",
                "  }, ",
                "  { ",
                "    \"c\": \"Paris\", ",
                "    \"s\": 4 ",
                "  }, ",
                "  { ",
                "    \"c\": \"London\", ",
                "    \"s\": 24 ",
                "  }, ",
                "  { ",
                "    \"s\": 0 ",
                "  }, ",
                "  true, ",
                "  12, ",
                "  \"rabbit\" ",
                "] ",
        };
        Main main = Main.fromLinesAndVirtual(lines, 40, 20);

        main.moveCursorDown();
        main.moveCursorDown();
        // we should now be at the "c"

        // Do the groupby
        main.actOnKey(KeyStroke.fromString("g"));

        // fold the two we know are good.
        main.actOnKey(KeyStroke.fromString("<Home>"));
        main.actOnKey(KeyStroke.fromString("<Down>"));
        main.actOnKey(KeyStroke.fromString("<Left>"));
        main.actOnKey(KeyStroke.fromString("<Down>"));
        main.actOnKey(KeyStroke.fromString("<Left>"));

        main.display();
        String got = main.getTestViewOfScreen();

        String expected =
                "{•//•grouped•by•c•••••••••••••••••••••••\n" +
                "••\"London\":•[•...•]•//•2•entries••••••••\n" +
                ">>\"Paris\":•[•...•]•//•1•entry•••••••••••\n" +
                "••\"(null)\":•[•//•4•entries••••••••••••••\n" +
                "••••{•••••••••••••••••••••••••••••••••••\n" +
                "••••••\"s\":•0••••••••••••••••••••••••••••\n" +
                "••••}•••••••••••••••••••••••••••••••••••\n" +
                "••••true••••••••••••••••••••••••••••••••\n" +
                "••••12••••••••••••••••••••••••••••••••••\n" +
                "••••\"rabbit\"••••••••••••••••••••••••••••\n" +
                "••]•••••••••••••••••••••••••••••••••••••\n" +
                "}•••••••••••••••••••••••••••••••••••••••\n" +
                "••••••••••••••••••••••••••••••••••••••••";

        String gotBeginning = got.substring(0, expected.length());
        assertEquals(expected, gotBeginning);
    }

    @Test
    public void testGroupbyInsideArray() throws Exception {
        String[] lines = new String[] {
                "[",
                "\"random string\",",
                "[",
                "  { ",
                "    \"c\": \"London\", ",
                "    \"s\": 12 ",
                "  }, ",
                "  { ",
                "    \"c\": \"Paris\", ",
                "    \"s\": 4 ",
                "  }, ",
                "  { ",
                "    \"c\": \"London\", ",
                "    \"s\": 24 ",
                "  }] ",
                "]"
        };
        Main main = Main.fromLinesAndVirtual(lines, 40, 20);

        main.moveCursorDown();
        main.moveCursorDown();
        main.moveCursorDown();
        main.moveCursorDown();
        // we should now be at the "c"

        // Do the groupby
        main.actOnKey(KeyStroke.fromString("g"));

        main.actOnKey(KeyStroke.fromString("<Home>"));
        main.moveCursorDown();
        main.moveCursorDown();
        main.moveCursorDown();
        main.actOnKey(KeyStroke.fromString("<Left>"));
        main.moveCursorDown();
        main.actOnKey(KeyStroke.fromString("<Left>"));
        main.display();
        String got = main.getTestViewOfScreen();

        String expected =
                "[•//•2•entries••••••••••••••••••••••••••\n" +
                "••\"random•string\"•••••••••••••••••••••••\n" +
                "••{•//•grouped•by•c•••••••••••••••••••••\n" +
                "••••\"London\":•[•...•]•//•2•entries••••••\n" +
                ">>••\"Paris\":•[•...•]•//•1•entry•••••••••\n" +
                "••}•••••••••••••••••••••••••••••••••••••\n" +
                "]•••••••••••••••••••••••••••••••••••••••\n";

        String gotBeginning = got.substring(0, expected.length());
        assertEquals(expected, gotBeginning);
    }

    @Test
    public void testGroupbyInsideMap() throws Exception {
        String[] lines = new String[] {
                "{",
                "\"group_this\": [",
                "  { ",
                "    \"c\": \"London\", ",
                "    \"s\": 12 ",
                "  }, ",
                "  { ",
                "    \"c\": \"Paris\", ",
                "    \"s\": 4 ",
                "  }, ",
                "  { ",
                "    \"c\": \"London\", ",
                "    \"s\": 24 ",
                "  }] ",
                "}"
        };
        Main main = Main.fromLinesAndVirtual(lines, 40, 40);

        main.moveCursorDown();
        main.moveCursorDown();
        main.moveCursorDown();
        // we should now be at the "c"

        // Do the groupby
        main.actOnKey(KeyStroke.fromString("g"));

        // Fold one section
        main.actOnKey(KeyStroke.fromString("<Home>"));
        main.moveCursorDown();
        main.moveCursorDown();
        main.actOnKey(KeyStroke.fromString("<Left>"));

        main.display();
        String got = main.getTestViewOfScreen();

        String expected =
                "{•••••••••••••••••••••••••••••••••••••••\n" +
                "••\"group_this\":•{•//•grouped•by•c•••••••\n" +
                ">>••\"London\":•[•...•]•//•2•entries••••••\n" +
                "••••\"Paris\":•[•//•1•entry•••••••••••••••\n" +
                "••••••{•••••••••••••••••••••••••••••••••\n" +
                "••••••••\"c\":•\"Paris\"••••••••••••••••••••\n" +
                "••••••••\"s\":•4••••••••••••••••••••••••••\n" +
                "••••••}•••••••••••••••••••••••••••••••••\n" +
                "••••]•••••••••••••••••••••••••••••••••••\n" +
                "••}•••••••••••••••••••••••••••••••••••••\n" +
                "}•••••••••••••••••••••••••••••••••••••••";

        String gotBeginning = got.substring(0, expected.length());
        assertEquals(expected, gotBeginning);
    }


    @Test
    public void testGroupbyTwoCursors() throws Exception {
        String[] lines = new String[] {
                "[",
                "[",
                "  { ",
                "    \"c\": \"London\", ",
                "    \"s\": 12 ",
                "  }, ",
                "  { ",
                "    \"c\": \"Paris\", ",
                "    \"s\": 4 ",
                "  }, ",
                "  { ",
                "    \"c\": \"London\", ",
                "    \"s\": 24 ",
                "  }], ",
                "[",
                "  { ",
                "    \"c\": \"Londonne\", ",
                "    \"s\": 12 ",
                "  }, ",
                "  { ",
                "    \"c\": \"Parize\", ",
                "    \"s\": 4 ",
                "  }, ",
                "  { ",
                "    \"c\": \"Londonne\", ",
                "    \"s\": 24 ",
                "  }] ",
                "]"
        };
        Main main = Main.fromLinesAndVirtual(lines, 40, 80);

        main.actOnKey(KeyStroke.fromString("*"));
        main.moveCursorDown();
        main.moveCursorDown();
        // we should now be at the "c" of *both* lists

        // Do the groupby
        main.actOnKey(KeyStroke.fromString("g"));

        main.actOnKey(KeyStroke.fromString("<Home>"));
        main.moveCursorDown();
        main.moveCursorDown();
        main.moveCursorDown();
        main.actOnKey(KeyStroke.fromString("<Left>"));
        main.moveCursorDown();
        main.actOnKey(KeyStroke.fromString("<Left>"));
        main.display();
        String got = main.getTestViewOfScreen();

        String expected =
                "[•//•2•entries••••••••••••••••••••••••••\n" +
                "••{•//•grouped•by•c•••••••••••••••••••••\n" +
                "••••\"London\":•[•//•2•entries••••••••••••\n" +
                "••••••{•...•}•••••••••••••••••••••••••••\n" +
                ">>••••{•...•}•••••••••••••••••••••••••••\n" +
                "••••]•••••••••••••••••••••••••••••••••••\n" +
                "••••\"Paris\":•[•//•1•entry•••••••••••••••\n" +
                "••••••{•••••••••••••••••••••••••••••••••\n" +
                "••••••••\"c\":•\"Paris\"••••••••••••••••••••\n" +
                "••••••••\"s\":•4••••••••••••••••••••••••••\n" +
                "••••••}•••••••••••••••••••••••••••••••••\n" +
                "••••]•••••••••••••••••••••••••••••••••••\n" +
                "••}•••••••••••••••••••••••••••••••••••••\n" +
                "••{•//•grouped•by•c•••••••••••••••••••••\n" +
                "••••\"Londonne\":•[•//•2•entries••••••••••\n" +
                "••••••{•••••••••••••••••••••••••••••••••\n" +
                "••••••••\"c\":•\"Londonne\"•••••••••••••••••\n" +
                "••••••••\"s\":•12•••••••••••••••••••••••••\n" +
                "••••••}•••••••••••••••••••••••••••••••••\n" +
                "••••••{•••••••••••••••••••••••••••••••••\n" +
                "••••••••\"c\":•\"Londonne\"•••••••••••••••••\n" +
                "••••••••\"s\":•24•••••••••••••••••••••••••\n" +
                "••••••}•••••••••••••••••••••••••••••••••\n" +
                "••••]•••••••••••••••••••••••••••••••••••\n" +
                "••••\"Parize\":•[•//•1•entry••••••••••••••\n" +
                "••••••{•••••••••••••••••••••••••••••••••\n" +
                "••••••••\"c\":•\"Parize\"•••••••••••••••••••\n" +
                "••••••••\"s\":•4••••••••••••••••••••••••••\n" +
                "••••••}•••••••••••••••••••••••••••••••••\n" +
                "••••]•••••••••••••••••••••••••••••••••••\n" +
                "••}•••••••••••••••••••••••••••••••••••••\n" +
                "]•••••••••••••••••••••••••••••••••••••••";

        String gotBeginning = got.substring(0, expected.length());
        assertEquals(expected, gotBeginning);
    }

}
