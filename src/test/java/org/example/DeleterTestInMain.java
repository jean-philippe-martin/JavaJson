package org.example;

import com.googlecode.lanterna.input.KeyStroke;
import org.example.Deleter.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class DeleterTestInMain {

    public String[] oneFishPond=new String[]{"{\n" +
            "    \"one\": {\n" +
            "        \"fish\": {\n" +
            "            \"pond\": 1\n" +
            "        }\n" +
            "    },\n" +
            "    \"two\": {\n" +
            "        \"world\": {\n" +
            "           \"records\": 1\n" +
            "        }\n" +
            "    }\n" +
            "}"};

    @Test
    public void deleteUnlessVisible() throws Exception {
        Main main = Main.fromLinesAndVirtual(oneFishPond, 10, 10);
        main.display();
        // fold everything
        main.actOnKey(KeyStroke.fromString("1"));
        // Delete everything not visible
        main.delete(
                new Deleter(main.getRoot(), TARGET.EVERYTHING, SUBJECT.UNLESS, MOD.IS, FILTER.VISIBLE,
                        OPTIONS.KEEP_PARENTS));
        String screen = main.getTestViewOfScreen();
        assertEquals("{•••••••••\n}•••••••••", screen);
    }

}