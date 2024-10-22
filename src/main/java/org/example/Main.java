package org.example;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.input.KeyType.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.swing.SwingTerminal;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.LinkedHashMap;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

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
        for(int column = 0; column < terminalSize.getColumns(); column++) {
            for(int row = 0; row < terminalSize.getRows(); row++) {
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
        while(System.currentTimeMillis() - startTime < 2000) {
            // The call to pollInput() is not blocking, unlike readInput()
            if(screen.pollInput() != null) {
                break;
            }
            try {
                Thread.sleep(1);
            }
            catch(InterruptedException ignore) {
                break;
            }
        }

        /*
            Ok, now we loop and keep modifying the screen until the user exits by pressing escape on the keyboard or the
            input stream is closed. When using the Swing/AWT bundled emulator, if the user closes the window this will
            result in an EOF KeyStroke.
             */
        while(true) {
            KeyStroke keyStroke = screen.pollInput();
            if(keyStroke != null && (keyStroke.getKeyType() == KeyType.Escape || keyStroke.getKeyType() == KeyType.EOF)) {
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
            if(newSize != null) {
                terminalSize = newSize;
            }

            // Increase this to increase speed
            final int charactersToModifyPerLoop = 10;
            for(int i = 0; i < charactersToModifyPerLoop; i++) {
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

    public static void printMaybeBolded(TextGraphics g, TerminalPosition pos, String s, boolean bolded) {
        if (bolded) {
            g.putString(pos, s, SGR.REVERSE);
        } else {
            g.putString(pos, s);
        }
    }

    // Returns how many lines it went down, beyond the initial one.
    // jsonObj can be String, List, LinkedHashMap<String, Object>, ...
    public static int printJsonObject(TextGraphics g, TerminalPosition start, int initialOffset, Object jsonObj, int selectedLine) {
        int line = 0;
        if (jsonObj instanceof String) {
            printMaybeBolded(g, start.withRelativeColumn(initialOffset), "\"" + (String)jsonObj + "\"", 0==selectedLine);
            return 1;
        }
        else if (jsonObj instanceof Integer) {
            printMaybeBolded(g, start.withRelativeColumn(initialOffset), jsonObj.toString(), 0==selectedLine);
            return 1;
        }
        if (jsonObj instanceof List) {
            TerminalPosition pos = start;
            TerminalPosition pos2 = pos.withRelativeColumn(initialOffset);
            g.putString(pos2, "[");
            TerminalPosition pos3 = pos.withRelativeColumn(INDENT);
            line += 1;
            pos3 = pos3.withRelativeRow(1);
            for (Object row : (List)jsonObj) {
                int height = printJsonObject(g, pos3, 0, row, selectedLine-line);
                line += height;
                pos3 = pos3.withRelativeRow(height);
            }
            pos = pos3.withRelativeColumn(-INDENT);
            g.putString(pos, "]");
            return line+1;
        }
        if (jsonObj instanceof LinkedHashMap) {
            LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) jsonObj;
            int indent = start.getColumn();
            TerminalPosition pos = start;
            //TextGraphics g = screen.newTextGraphics();
            printMaybeBolded(g, pos.withRelativeColumn(initialOffset), "{", line==selectedLine);
            pos = pos.withRelativeColumn(INDENT).withRelativeRow(1);
            line += 1;
            for (Map.Entry<String, Object> e : map.sequencedEntrySet()) {
                printMaybeBolded(g, pos, "\"" + e.getKey() + "\"", line==selectedLine);
                TerminalPosition pos2 = pos.withRelativeColumn(2+e.getKey().length());
                Object val = e.getValue();
                if (val instanceof String) {
                    g.putString(pos2, ": ");
                    // passing -1 because we never want the constant value highlighted (just the key, possibly)
                    int height = printJsonObject(g, pos, pos2.getColumn()-pos.getColumn()+2, val, -1);
                    line += height;
                    pos = pos.withRelativeRow(height);
                } else if (val instanceof List) {
                    g.putString(pos2, ": ");
                    // passing -1 because we never want the constant value highlighted (just the key, possibly)
                    int height = printJsonObject(g, pos, pos2.getColumn()-pos.getColumn()+2, val, selectedLine-line);
                    line += height;
                    pos = pos.withRelativeRow(height);
                    // for tall elements, we don't want the cursor to stop
                    // at the closing element.
                    if (height>1) line -=1;
                } else if (val instanceof LinkedHashMap) {
                    //g.putString(pos2, ": (LinkedHashMap)" + val.toString());
                    g.putString(pos2, ": ");
                    int childOffset = e.getKey().length() + 4;
                    int childSelected = selectedLine-line;
                    if (childSelected==0) childSelected=-1; // do not highlight opening bracket
                    int childHeight = printJsonObject(g, pos, childOffset, (LinkedHashMap<String, Object>) val, childSelected);
                    line += childHeight - 1;
                    pos = pos.withRelativeRow(childHeight);
                } else {
                    g.putString(pos2, ": " + val.toString());
                    line += 1;
                    pos = pos.withRelativeRow(1);
                }
            }
            line += 1;
            pos = pos.withRelativeColumn(-INDENT);
            g.putString(pos, "}");
            return line;
        }
        throw new RuntimeException("Unrecognized type: " + jsonObj.getClass());
    }

    public static void main(String[] args) throws Exception {

        // load the json
        Path path = FileSystems.getDefault().getPath("../testdata/hello.json");
        String lines = String.join("\n",Files.readAllLines(path));

        // Parse it
        ObjectMapper parser = new ObjectMapper();
        LinkedHashMap<String, Object> kv = parser.readValue(lines, LinkedHashMap.class);


        /*
        A Screen works similar to double-buffered video memory, it has two surfaces than can be directly addressed and
        modified and by calling a special method that content of the back-buffer is move to the front. Instead of pixels
        though, a Screen holds two text character surfaces (front and back) which corresponds to each "cell" in the
        terminal. You can freely modify the back "buffer" and you can read from the front "buffer", calling the
        refreshScreen() method to copy content from the back buffer to the front buffer, which will make Lanterna also
        apply the changes so that the user can see them in the terminal.
         */
        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
        Screen screen = null;
        try {
            Terminal terminal = defaultTerminalFactory.createTerminal();
            screen = new TerminalScreen(terminal);

            /*
            Screens will only work in private mode and while you can call methods to mutate its state, before you can
            make any of these changes visible, you'll need to call startScreen() which will prepare and setup the
            terminal.
             */
            screen.startScreen();

            // show a changing jumble of colors
            //demo(screen);

            // hide cursor
            screen.setCursorPosition(null);

//
//            TextGraphics textGraphics = screen.newTextGraphics();
//            TerminalPosition pos = TerminalPosition.TOP_LEFT_CORNER;
//            textGraphics.putString(pos, "{");
//            pos = pos.withRelativeRow(1);
//            String key = "  \"name\"";
//            textGraphics.putString(pos, key, SGR.REVERSE);
//            textGraphics.putString(pos.withRelativeColumn(key.length()), ": \"Bob\"");
//            pos = pos.withRelativeRow(1);
//            textGraphics.putString(pos, "}");

            int selected = 0;
            while (true) {
                TextGraphics g = screen.newTextGraphics();
                printJsonObject(g, TerminalPosition.TOP_LEFT_CORNER, 0, kv, selected);
                screen.refresh();
                KeyStroke key = terminal.readInput();
                if (key.getKeyType() == KeyType.ArrowDown) selected += 1;
                if (key.getKeyType() == KeyType.ArrowUp) selected -= 1;
                if (key.getKeyType() == KeyType.Escape || (key.getCharacter()!=null && 'q'==key.getCharacter())) break;
            }

        }
        catch(IOException e) {
            e.printStackTrace();
        }
        finally {
            if(screen != null) {
                try {
                    /*
                    The close() call here will restore the terminal by exiting from private mode which was done in
                    the call to startScreen(), and also restore things like echo mode and intr
                     */
                    screen.close();
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}