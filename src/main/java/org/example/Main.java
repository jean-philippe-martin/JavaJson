package org.example;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import org.example.cursor.FindCursor;
import org.example.cursor.MultiCursor;
import org.example.cursor.NoMultiCursor;
import org.example.ui.FindControl;
import org.example.ui.InputField;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class Main {

    // All about the search feature
    private static boolean showFind = false;
    private static FindControl findControl;
    private static JsonNode.SavedCursors cursorsBeforeFind = null;

    private static final String keys_help = """
                up/down         : navigate line
                pg up / pg dn   : navigate page
                shift + up/down : navigate sibling
                n / N           : navigate cursors
                ESC             : remove secondary cursors
                left / right    : fold / unfold
                e / *           : select all children
                f               : find
                p               : pin (show despite folds)
                h / ?           : show help page
                q               : quit
                """;


    public static void main(String[] args) throws Exception {

        if (args.length==1 && args[0].equals("--help")) {
            System.out.println("Key bindings:");
            System.out.println(keys_help);
            return;
        }

        // load the json
        Path path = FileSystems.getDefault().getPath(args[0]);
        JsonNode myJson = JsonNode.parse(path);
        findControl = new FindControl(myJson);


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
            Drawer d = new Drawer();

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

            int scroll = 0;
            int rowLimit = screen.getTerminalSize().getRows()-2;
            TextGraphics g2 = null;
            while (true) {
                screen.doResizeIfNecessary();
                TextGraphics g = screen.newTextGraphics();
                screen.clear();
                d.printJsonTree(g, TerminalPosition.TOP_LEFT_CORNER.withRelativeRow(-scroll), 0, myJson);
                if (d.getCursorLineLastTime()>rowLimit) {
                    scroll += (d.getCursorLineLastTime()-rowLimit);
                    screen.clear();
                    d.printJsonTree(g, TerminalPosition.TOP_LEFT_CORNER.withRelativeRow(-scroll), 0, myJson);
                } else if (d.getCursorLineLastTime()<0) {
                    scroll += d.getCursorLineLastTime();
                    screen.clear();
                    d.printJsonTree(g, TerminalPosition.TOP_LEFT_CORNER.withRelativeRow(-scroll), 0, myJson);
                }
                if (showFind) {
                    findControl.draw(screen.newTextGraphics());
                }
                screen.refresh(Screen.RefreshType.DELTA);
                KeyStroke key = terminal.readInput();
                if (!showFind) {
                    // normal key handling
                    if (key.getKeyType() == KeyType.ArrowDown && !key.isShiftDown()) myJson.cursorDown();
                    if (key.getKeyType() == KeyType.ArrowDown && key.isShiftDown()) myJson.cursorNextSibling();
                    if (key.getKeyType() == KeyType.ArrowUp && !key.isShiftDown()) myJson.cursorUp();
                    if (key.getKeyType() == KeyType.ArrowUp && key.isShiftDown()) myJson.cursorPrevSibling();
                    if (key.getKeyType() == KeyType.ArrowLeft) {
                        if (myJson.getFoldedAtCursor() || !myJson.setFoldedAtCursors(true)) {
                            myJson.cursorParent();
                        }
                    }
                    if (key.getKeyType() == KeyType.ArrowRight
                            || (key.getCharacter() != null && 'f' == key.getCharacter())) {
                        myJson.setFoldedAtCursors(false);
                    }
                    if (key.getCharacter() != null && ('e' == key.getCharacter() || '*' == key.getCharacter())) {
                        myJson.cursorDownToAllChildren();
                    }
                    if ((key.getCharacter() != null && 'p' == key.getCharacter())) {
                        boolean pinned = myJson.getPinnedAtCursor();
                        myJson.setPinnedAtCursors(!pinned);
                    }
                    if ((key.getCharacter() != null && 'n' == key.getCharacter())) {
                        // next cursor/match
                        myJson.cursorNextCursor();
                    }
                    if ((key.getCharacter() != null && 'N' == key.getCharacter())) {
                        // next cursor/match
                        myJson.cursorPrevCursor();
                    }
                    if ((key.getCharacter() != null && 'r' == key.getCharacter())) {
                        // restart (for testing)
                        myJson = JsonNode.parse(path);
                    }
                    if (key.getKeyType() == KeyType.PageDown) {
                        for (int i = 0; i < g.getSize().getRows() - 2; i++) {
                            myJson.cursorDown();
                        }
                    }
                    if (key.getKeyType() == KeyType.PageUp) {
                        for (int i = 0; i < g.getSize().getRows() - 2; i++) {
                            myJson.cursorUp();
                        }
                    }
                    if (key.getKeyType() == KeyType.Home) {
                        myJson.rootInfo.setPrimaryCursor(myJson.whereIAm);
                    }
                    if (key.getKeyType() == KeyType.End) {
                        JsonNode lastChild = myJson;
                        for (int i=0; i<100; i++) {
                            JsonNode x = lastChild.lastChild();
                            if (x==null || x==lastChild) break;
                            lastChild = x;
                        }
                        myJson.rootInfo.setPrimaryCursor(lastChild.whereIAm);
                    }
                    if (key.getCharacter() != null && ('h' == key.getCharacter() || '?' == key.getCharacter())) {
                        helpScreen(terminal, screen);
                    }
                    if (key.getCharacter() != null && ('f' == key.getCharacter())) {
                        showFind = true;
                        findControl.init();
                        cursorsBeforeFind = myJson.rootInfo.save();
                    }
                    if (key.getKeyType() == KeyType.Escape) {
                        myJson.rootInfo.secondaryCursors = new NoMultiCursor();
                    }
                    if (key.getCharacter() != null && 'q' == key.getCharacter())
                        break;
                } else {
                    // Incremental find mode...
                    FindControl.Choice choice = findControl.update(key);
                    // find screen mode
                    switch (choice) {
                        case FindControl.Choice.CANCEL -> {
                            showFind = false;
                            myJson.rootInfo.restore(cursorsBeforeFind);
                            cursorsBeforeFind = null;
                        }
                        case FindControl.Choice.FIND -> {
                            // enter: select all
                            FindCursor fc = new FindCursor(
                                    findControl.getText(), findControl.getAllowSubstring(),
                                    findControl.getIgnoreCase(),
                                    findControl.getSearchKeys(), findControl.getSearchValues(),
                                    findControl.getUseRegexp());
                            myJson.rootInfo.setSecondaryCursors(fc);
                            // TODO: set primary cursor to first match (or first match after cursor?)

                            JsonNode primary = myJson.rootInfo.userCursor.getData();
                            if (!primary.isAtSecondaryCursor()) {
                                myJson.cursorNextCursor();
                            }
                            if (!primary.isAtSecondaryCursor()) {
                                myJson.cursorPrevCursor();
                            }
                            showFind = false;
                        }
                        case FindControl.Choice.GOTO -> {
                            // shift-enter: go to current find
                            JsonNode primary = myJson.rootInfo.userCursor.getData();
                            if (!primary.isAtSecondaryCursor()) {
                                myJson.cursorNextCursor();
                            }
                            if (!primary.isAtSecondaryCursor()) {
                                myJson.cursorPrevCursor();
                            }
                            cursorsBeforeFind.primaryCursor = myJson.rootInfo.userCursor;
                            myJson.rootInfo.restore(cursorsBeforeFind);
                            cursorsBeforeFind = null;
                            showFind = false;
                        }
                        case FindControl.Choice.NONE -> {
                            // still in the "find" dialog, we preview the search results
                            FindCursor fc = new FindCursor(
                                    findControl.getText(), findControl.getAllowSubstring(),
                                    findControl.getIgnoreCase(),
                                    findControl.getSearchKeys(), findControl.getSearchValues(),
                                    findControl.getUseRegexp());
                            myJson.rootInfo.setSecondaryCursors(fc);
                        }
                    }
                }
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

    private static void helpScreen(Terminal terminal, Screen screen) throws IOException {
        TextGraphics g = screen.newTextGraphics();
        screen.clear();
        String[] lines = keys_help.split("\n");
        TerminalPosition pos = TerminalPosition.TOP_LEFT_CORNER.withRelativeColumn(4).withRelativeRow(1);
        for (String l : lines) {
            g.putString(pos, l);
            pos = pos.withRelativeRow(1);
        }
        screen.refresh(Screen.RefreshType.DELTA);
        KeyStroke key = terminal.readInput();
    }


    // returns the TextGraphic region for the input field
    private static TextGraphics findScreen(Terminal terminal, Screen screen) throws IOException {
        TextGraphics g = screen.newTextGraphics();
        String menu = """
                +----------[ FIND ]--------------+
                |                                |
                +--------------------------------+
                | Whole | Aa | K+V | .* |        |
                +--------------------------------+
                | alt-f: find in keys/values/both|
                | alt-c: case sensitivity        |
                | alt-s: find only within selection
                | alt-r: regular expression      |
                | up/down: prev/next result      |
                +--------------------------------+
                | enter: select one              |
                | shift-enter: select all        |
                | ... : add to selection         |
                | esc : cancel find              |
                +--------------------------------+
                """;
        String[] lines = menu.split("\n");
        TerminalPosition pos = TerminalPosition.TOP_LEFT_CORNER.withRelativeColumn(4).withRelativeRow(1);
        for (String l : lines) {
            g.putString(pos, l);
            pos = pos.withRelativeRow(1);
        }
        TextGraphics g2 = g.newTextGraphics(TerminalPosition.TOP_LEFT_CORNER.withRelative(5,2),
                new TerminalSize(32,1));
        g2.setForegroundColor(TextColor.ANSI.YELLOW_BRIGHT);
        g2.setBackgroundColor(TextColor.Indexed.fromRGB(0,0,128));
        g2.fill(' ');
        return g2;
    }
}