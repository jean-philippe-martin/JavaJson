package org.example;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {

        // load the json
        Path path = FileSystems.getDefault().getPath("testdata/list.json");

        JsonNode myJson = JsonNode.parse(path);


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

            int selected = 0;
            while (true) {
                TextGraphics g = screen.newTextGraphics();
                screen.clear();
                Drawer.printJsonObject(g, TerminalPosition.TOP_LEFT_CORNER, 0, myJson);
                screen.refresh(Screen.RefreshType.DELTA);
                KeyStroke key = terminal.readInput();
                if (key.getKeyType() == KeyType.ArrowDown) myJson.cursorDown();
                if (key.getKeyType() == KeyType.ArrowUp) myJson.cursorUp();
                if (key.getKeyType() == KeyType.ArrowLeft) {
                    if (myJson.getFoldedAtCursor() || !myJson.setFoldedAtCursor(true)) {
                        myJson.cursorParent();
                    }
                }
                if (key.getKeyType() == KeyType.ArrowRight
                || (key.getCharacter() != null && 'f' == key.getCharacter())) {
                    myJson.setFoldedAtCursor(false);
                }
                if (key.getCharacter()!=null && 'e' == key.getCharacter()) {
                    myJson.cursorDownToAllChildren();
                }
                if ((key.getCharacter() != null && 'p' == key.getCharacter())) {
                    boolean pinned = myJson.getPinnedAtCursor();
                    myJson.setPinnedAtCursors(!pinned);
                }
                if ((key.getCharacter() != null && 'r' == key.getCharacter())) {
                    // restart (for testing)
                    myJson = JsonNode.parse(path);
                }
                if (key.getKeyType() == KeyType.Escape || (key.getCharacter() != null && 'q' == key.getCharacter()))
                    break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (screen != null) {
                try {
                    /*
                    The close() call here will restore the terminal by exiting from private mode which was done in
                    the call to startScreen(), and also restore things like echo mode and intr
                     */
                    screen.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}