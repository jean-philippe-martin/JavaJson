package org.example;

import com.googlecode.lanterna.input.KeyStroke;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Random;

public class FuzzTest {

    static final String[] MIX_OF_TYPES = new String[]{
            "{ \"map1\": {",
            "  \"map2\": {",
            "      \"alist\": [ ",
            "          { \"number\": 12, \"string\": \"hello\", \"boolean\": true },",
            "          { \"number\": 12, \"string\": \"hello\", \"boolean\": true }",
            "       ],",
            "       \"another_bool\": false",
            "} } }"
    };

    @Ignore
    @Test
    public void testNavigation() throws Exception {
        int steps = 1000;
        int seed = 42;

        Main main = Main.fromLinesAndVirtual(MIX_OF_TYPES, 80, 40);
        main.display();
        Random rnd = new Random(seed);

        KeyStroke[] choices = new KeyStroke[]{
                KeyStroke.fromString("<left>"),
                KeyStroke.fromString("<right>"),
                KeyStroke.fromString("<up>"),
                KeyStroke.fromString("<down>"),
                KeyStroke.fromString("p"),
        };

        for (int i=0; i<steps; i++) {
            KeyStroke key = choices[rnd.nextInt(choices.length)];
            main.actOnKey(key);
            main.display();
            if (steps%10==0) main.checkInvariants();
        }
        main.checkInvariants();
        String state = main.getTestViewOfScreen();

    }

}
