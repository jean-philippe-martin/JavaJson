package org.example;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;

import java.io.IOException;
import java.util.Random;
import java.util.SequencedCollection;

public class Drawer {

    static final int INDENT = 2;

    // blocks
    public static void demo(Screen screen) throws IOException {

                    /*
            Let's turn off the cursor for this tutorial
             */
        screen.setCursorPosition(null);

            /*
            Now let's draw some random content in the screen buffer
             */
        Random random = new Random();
        TerminalSize terminalSize = screen.getTerminalSize();
        for (int column = 0; column < terminalSize.getColumns(); column++) {
            for (int row = 0; row < terminalSize.getRows(); row++) {
                screen.setCharacter(column, row, new TextCharacter(
                        ' ',
                        TextColor.ANSI.DEFAULT,
                        // This will pick a random background color
                        TextColor.ANSI.values()[random.nextInt(TextColor.ANSI.values().length)]));
            }
        }

            /*
            So at this point, we've only modified the back buffer in the screen, nothing is visible yet. In order to
            move the content from the back buffer to the front buffer and refresh the screen, we need to call refresh()
             */
        screen.refresh();

            /*
            Now there should be completely random colored cells in the terminal (assuming your terminal (emulator)
            supports colors). Let's look at it for two seconds or until the user press a key.
             */
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 2000) {
            // The call to pollInput() is not blocking, unlike readInput()
            if (screen.pollInput() != null) {
                break;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignore) {
                break;
            }
        }

        /*
            Ok, now we loop and keep modifying the screen until the user exits by pressing escape on the keyboard or the
            input stream is closed. When using the Swing/AWT bundled emulator, if the user closes the window this will
            result in an EOF KeyStroke.
             */
        while (true) {
            KeyStroke keyStroke = screen.pollInput();
            if (keyStroke != null && (keyStroke.getKeyType() == KeyType.Escape || keyStroke.getKeyType() == KeyType.EOF)) {
                break;
            }

                /*
                Screens will automatically listen and record size changes, but you have to let the Screen know when is
                a good time to update its internal buffers. Usually you should do this at the start of your "drawing"
                loop, if you have one. This ensures that the dimensions of the buffers stays constant and doesn't change
                while you are drawing content. The method doReizeIfNecessary() will check if the terminal has been
                resized since last time it was called (or since the screen was created if this is the first time
                calling) and update the buffer dimensions accordingly. It returns null if the terminal has not changed
                size since last time.
                 */
            TerminalSize newSize = screen.doResizeIfNecessary();
            if (newSize != null) {
                terminalSize = newSize;
            }

            // Increase this to increase speed
            final int charactersToModifyPerLoop = 10;
            for (int i = 0; i < charactersToModifyPerLoop; i++) {
                    /*
                    We pick a random location
                     */
                TerminalPosition cellToModify = new TerminalPosition(
                        random.nextInt(terminalSize.getColumns()),
                        random.nextInt(terminalSize.getRows()));

                    /*
                    Pick a random background color again
                     */
                TextColor.ANSI color = TextColor.ANSI.values()[random.nextInt(TextColor.ANSI.values().length)];

                    /*
                    Update it in the back buffer, notice that just like TerminalPosition and TerminalSize, TextCharacter
                    objects are immutable so the withBackgroundColor(..) call below returns a copy with the background color
                    modified.
                     */
                TextCharacter characterInBackBuffer = screen.getBackCharacter(cellToModify);
                characterInBackBuffer = characterInBackBuffer.withBackgroundColor(color);
                characterInBackBuffer = characterInBackBuffer.withCharacter(' ');   // Because of the label box further down, if it shrinks
                screen.setCharacter(cellToModify, characterInBackBuffer);
            }

                /*
                Just like with Terminal, it's probably easier to draw using TextGraphics. Let's do that to put a little
                box with information on the size of the terminal window
                 */
            String sizeLabel = "Terminal Size: " + terminalSize;
            TerminalPosition labelBoxTopLeft = new TerminalPosition(1, 1);
            TerminalSize labelBoxSize = new TerminalSize(sizeLabel.length() + 2, 3);
            TerminalPosition labelBoxTopRightCorner = labelBoxTopLeft.withRelativeColumn(labelBoxSize.getColumns() - 1);
            TextGraphics textGraphics = screen.newTextGraphics();
            //This isn't really needed as we are overwriting everything below anyway, but just for demonstrative purpose
            textGraphics.fillRectangle(labelBoxTopLeft, labelBoxSize, ' ');

                /*
                Draw horizontal lines, first upper then lower
                 */
            textGraphics.drawLine(
                    labelBoxTopLeft.withRelativeColumn(1),
                    labelBoxTopLeft.withRelativeColumn(labelBoxSize.getColumns() - 2),
                    Symbols.DOUBLE_LINE_HORIZONTAL);
            textGraphics.drawLine(
                    labelBoxTopLeft.withRelativeRow(2).withRelativeColumn(1),
                    labelBoxTopLeft.withRelativeRow(2).withRelativeColumn(labelBoxSize.getColumns() - 2),
                    Symbols.DOUBLE_LINE_HORIZONTAL);

                /*
                Manually do the edges and (since it's only one) the vertical lines, first on the left then on the right
                 */
            textGraphics.setCharacter(labelBoxTopLeft, Symbols.DOUBLE_LINE_TOP_LEFT_CORNER);
            textGraphics.setCharacter(labelBoxTopLeft.withRelativeRow(1), Symbols.DOUBLE_LINE_VERTICAL);
            textGraphics.setCharacter(labelBoxTopLeft.withRelativeRow(2), Symbols.DOUBLE_LINE_BOTTOM_LEFT_CORNER);
            textGraphics.setCharacter(labelBoxTopRightCorner, Symbols.DOUBLE_LINE_TOP_RIGHT_CORNER);
            textGraphics.setCharacter(labelBoxTopRightCorner.withRelativeRow(1), Symbols.DOUBLE_LINE_VERTICAL);
            textGraphics.setCharacter(labelBoxTopRightCorner.withRelativeRow(2), Symbols.DOUBLE_LINE_BOTTOM_RIGHT_CORNER);

                /*
                Finally put the text inside the box
                 */
            textGraphics.putString(labelBoxTopLeft.withRelative(1, 1), sizeLabel);

                /*
                Ok, we are done and can display the change. Let's also be nice and allow the OS to schedule other
                threads so we don't clog up the core completely.
                 */
            screen.refresh();
            Thread.yield();

                /*
                Every time we call refresh, the whole terminal is NOT re-drawn. Instead, the Screen will compare the
                back and front buffers and figure out only the parts that have changed and only update those. This is
                why in the code drawing the size information box above, we write it out every time we loop but it's
                actually not sent to the terminal except for the first time because the Screen knows the content is
                already there and has not changed. Because of this, you should never use the underlying Terminal object
                when working with a Screen because that will cause modifications that the Screen won't know about.
                 */
        }
    }

    public static void printMaybeReversed(TextGraphics g, TerminalPosition pos, String s, boolean bolded) {
        if (bolded) {
            g.putString(pos, s, SGR.REVERSE);
        } else {
            g.putString(pos, s);
        }
    }

    // inFoldedContext = we're folded, only print pinned rows.
    public static int printJsonMap(TextGraphics g, JsonNodeMap jsonMap, TerminalPosition start, int initialOffset, boolean inFoldedContext) {
        int line = 0;
        SequencedCollection<String> keys = jsonMap.getKeysInOrder();
        int indent = start.getColumn();
        TerminalPosition pos = start;

        // In a folded context, we only show pinned things.
        if (jsonMap.getFolded()) inFoldedContext = true;
        // Pinning an object means we show the whole object
        if (jsonMap.getPinned()) inFoldedContext = false;

        if (inFoldedContext) {
            if (!jsonMap.hasPins()) {
                printMaybeReversed(g, pos.withRelativeColumn(initialOffset), "{ ... }", jsonMap.isAtCursor());
                return 1;
            }
            // we contain at least one thing that'll be shown, so open up.
            printMaybeReversed(g, pos.withRelativeColumn(initialOffset), "{ ...", jsonMap.isAtCursor());
        } else {
            printMaybeReversed(g, pos.withRelativeColumn(initialOffset), "{", jsonMap.isAtCursor());
        }

        pos = pos.withRelativeColumn(INDENT).withRelativeRow(1);
        line += 1;
        for (String key : keys) {
            JsonNode child = jsonMap.getChild(key);
            if (inFoldedContext && !child.hasPins()) {
                // skip this child
                continue;
            }
            printMaybeReversed(g, pos, "\"" + key + "\"", jsonMap.isAtCursor(key));
            TerminalPosition pos2 = pos.withRelativeColumn(2 + key.length());
            switch (child) {
                case JsonNodeValue v -> {
                    Object val = v.getValue();
                    g.putString(pos2, ": ");
                    int height = printJsonObject(g, pos, pos2.getColumn() - pos.getColumn() + 2, child, inFoldedContext);
                    line += height;
                    pos = pos.withRelativeRow(height);
                }
                case JsonNode m -> {
                    g.putString(pos2, ": ");
                    int childOffset = key.length() + 4;
                    int childHeight = printJsonObject(g, pos, childOffset, child, inFoldedContext);
                    line += childHeight;
                    pos = pos.withRelativeRow(childHeight);
                }
            }
        }
        line += 1;
        pos = pos.withRelativeColumn(-INDENT);
        g.putString(pos, "}");
        return line;
    }

    // Returns how many lines it went down, beyond the initial one.
    // jsonObj can be String, List, LinkedHashMap<String, Object>, ...
    public static int printJsonObject(TextGraphics g, TerminalPosition start, int initialOffset, JsonNode json) {
        return printJsonObject(g, start, initialOffset, json, false);
    }

    // Returns how many lines it went down, beyond the initial one.
    // jsonObj can be String, List, LinkedHashMap<String, Object>, ...
    public static int printJsonObject(TextGraphics g, TerminalPosition start, int initialOffset, JsonNode json, boolean inFoldedContext) {
        int line = 0;
        if (json.isAtPrimaryCursor() && json.parent!=null) {
            g.putString(start.withColumn(0), ">>");
        }
        if (json.getPinned()) {
            // draw the pin
            g.putString(start.withColumn(0), "P");
        }
        if (json instanceof JsonNodeValue jsonValue) {
            int lines = 0;
            if (inFoldedContext && !json.hasPins()) {
                // skip
                return 0;
            }
            String annotation = jsonValue.getAnnotation();
            if (!annotation.isEmpty()) {
                printMaybeReversed(g, start.withRelativeColumn(initialOffset), "// " + annotation, false);
                start = start.withRelativeRow(1);
                lines++;
            }
            Object value = jsonValue.getValue();
            if (value instanceof String) {
                printMaybeReversed(g, start.withRelativeColumn(initialOffset), "\"" + (String) value + "\"", json.isAtCursor());
                return lines+1;
            } else {
                printMaybeReversed(g, start.withRelativeColumn(initialOffset), value.toString(), json.isAtCursor());
                return lines+1;
            }
        }
        if (json instanceof JsonNodeList) {
            JsonNodeList jsonList = (JsonNodeList) json;
            inFoldedContext = jsonList.folded || inFoldedContext;
            TerminalPosition pos = start;
            TerminalPosition pos2 = pos.withRelativeColumn(initialOffset);
            if (inFoldedContext) {
                if (jsonList.hasPins()) {
                    g.putString(pos2, "[ ...");
                } else {
                    g.putString(pos2, "[ ... ]");
                    return 1;
                }
            } else {
                g.putString(pos2, "[");
            }
            TerminalPosition pos3 = pos.withRelativeColumn(INDENT);
            line += 1;
            pos3 = pos3.withRelativeRow(1);
            for (int index=0; index<jsonList.size(); index++) {
                JsonNode child = jsonList.get(index);
                if (inFoldedContext && !child.hasPins()) {
                    // skip that one, we're folded and it's not pinned.
                    continue;
                }
                int height = printJsonObject(g, pos3, 0, child, inFoldedContext);
                line += height;
                pos3 = pos3.withRelativeRow(height);
            }
            pos = pos3.withRelativeColumn(-INDENT);
            g.putString(pos, "]");
            return line + 1;
        }
        else if (json instanceof JsonNodeMap) {
            JsonNodeMap jsonMap = (JsonNodeMap) json;
            if (inFoldedContext && !(jsonMap.getPinned() || jsonMap.hasPins())) {
                return 0; // hidden in the fold
            }
            return printJsonMap(g, jsonMap, start, initialOffset, inFoldedContext);
        }

        throw new RuntimeException("Unrecognized type: " + json.getClass());
    }

}
