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
import org.example.cursor.NoMultiCursor;
import org.example.ui.AggregateMenu;
import org.example.ui.FindControl;
import org.example.ui.SortControl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class Main {

    // All about the search feature
    private static boolean showFind = false;
    private static FindControl findControl;
    private static SortControl sortControl;
    private static AggregateMenu aggregateMenu;
    private static JsonNode.SavedCursors cursorsBeforeFind = null;

    private static OperationList operationList = new OperationList();
    private static String notificationText = "";

    private static final String keys_help =
                "----------------[ Movement ]----------------\n"+
                "up/down         : navigate line             \n"+
                "pg up / pg dn   : navigate page             \n"+
                "home / end      : beginning/end of document \n"+
                "shift + up/down : navigate sibling          \n"+
                "                                            \n"+
                "----------------[ View ]--------------------\n"+
                "left / right    : fold / unfold             \n"+
                "p               : pin (show despite folds)  \n"+
                "                                            \n"+
                "----------------[ Multicursor ]-------------\n"+
                "f               : find                      \n"+
                "e / *           : select all children       \n"+
                "n / N           : navigate cursors          \n"+
                "ESC             : remove secondary cursors  \n"+
                "                                            \n"+
                "----------------[ Transform ]---------------\n"+
                "+               : union selected arrays     \n"+
                "a               : aggregate sel. array(s)   \n"+
                "s               : sort selected array(s)    \n"+
                "shift-Z         : undo last transform       \n"+
                "                                            \n"+
                "----------------[ Program ]-----------------\n"+
                "h / ?           : show help page            \n"+
                "q               : quit                      \n";


    public static void main(String[] args) throws Exception {

        if (args.length==0 || (args.length==1 && args[0].equals("--help"))) {
            System.out.println("Usage:");
            System.out.println("java -jar JavaJson*.jar myfile.json");
            System.out.println();
            System.out.println("Example:");
            System.out.println("java -jar target/JavaJson-1.0-jar-with-dependencies.jar testdata/hello.json");
            System.out.println();
            System.out.println("Key bindings:");
            System.out.println(keys_help);
            return;
        }

        JsonNode myJson;

//        This doesn't work, it breaks the UI.. not sure why.
//        if (args.length==1 && args[0].equals("-")) {
//            // Read the JSON from stdin
//            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//            List<String> lines = reader.lines().collect().toList();
//            myJson = JsonNode.parseJson(String.join("\n", lines));
//        }

        // load the JSON, using NIO libraries if they were added to the classpath.
        Path path = Paths.get(args[0]);
        myJson = JsonNode.parse(path);
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
                if (null!=sortControl) {
                    sortControl.draw(screen.newTextGraphics());
                }
                else if (showFind) {
                    findControl.draw(screen.newTextGraphics());
                } else if (aggregateMenu!=null) {
                    aggregateMenu.draw(screen.newTextGraphics());
                }

                // Bar at the bottom: notification if there's one, or path.
                String bottomText = notificationText;
                if (bottomText.isEmpty()) {
                    bottomText = myJson.rootInfo.userCursor.toString();
                }
                StringBuilder sb = new StringBuilder();
                sb.append(bottomText);
                while (sb.length() < g.getSize().getColumns()-1) {
                    sb.append(" ");
                }
                g.putString(TerminalPosition.TOP_LEFT_CORNER.withRelativeRow(g.getSize().getRows()-1), sb.toString(), SGR.REVERSE);
                // Notifications last only one frame.
                notificationText = "";

                screen.refresh(Screen.RefreshType.DELTA);
                KeyStroke key = terminal.readInput();
                if (showFind) {
                    // Incremental find mode...
                    FindControl.Choice choice = findControl.update(key);
                    // find screen mode
                    switch (choice) {
                        case CANCEL:
                        {
                            showFind = false;
                            myJson.rootInfo.restore(cursorsBeforeFind);
                            cursorsBeforeFind = null;
                            break;
                        }
                        case FIND:
                        {
                            // enter: select all
                            FindCursor fc = new FindCursor(
                                    findControl.getText(), findControl.getAllowSubstring(),
                                    findControl.getIgnoreCase(),
                                    findControl.getSearchKeys(), findControl.getSearchValues(),
                                    findControl.getIgnoreComments(),
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
                            break;
                        }
                        case GOTO:
                        {
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
                            break;
                        }
                        case NONE:
                        {
                            // still in the "find" dialog, we preview the search results
                            if (!findControl.getText().isEmpty()) {
                                FindCursor fc = new FindCursor(
                                        findControl.getText(), findControl.getAllowSubstring(),
                                        findControl.getIgnoreCase(),
                                        findControl.getSearchKeys(), findControl.getSearchValues(),
                                        findControl.getIgnoreComments(),
                                        findControl.getUseRegexp());
                                myJson.rootInfo.setSecondaryCursors(fc);
                            } else {
                                myJson.rootInfo.setSecondaryCursors(cursorsBeforeFind.secondaryCursors);
                            }
                            break;
                        }
                    }
                }
                else if (null!=sortControl) {
                    // manage the sort dialog
                    Sorter s = sortControl.update(key);
                    if (s!=null) {
                        Operation sort = new Operation.Sort(myJson, s);
                        notificationText = sort.toString();
                        myJson = operationList.run(sort);
                        sortControl = null;
                    }
                    if (key.getKeyType()==KeyType.Escape) {
                        sortControl = null;
                    }
                    if (key.getKeyType()==KeyType.Character && key.getCharacter()=='x') {
                        Operation sort = new Operation.Sort(myJson, null);
                        notificationText = sort.toString();
                        myJson = operationList.run(sort);
                        sortControl = null;
                    }
                }
                else if (null!=aggregateMenu) {
                    // manage the aggregate menu
                    AggregateMenu.Choice choice = aggregateMenu.update(key);
                    if (choice== AggregateMenu.Choice.CANCEL) {
                        aggregateMenu = null;
                    }
                    if (choice== AggregateMenu.Choice.UNIQUE_FIELDS) {
                        Operation aggOp = new Operation.AggUniqueFields(myJson.root, true);
                        JsonNode newRoot = operationList.run(aggOp);
                        if (null==newRoot) {
                            // We didn't do anything
                            notificationText = "unique_fields() requires a list of maps";
                        } else {
                            myJson = newRoot;
                            notificationText = "unique_fields()";
                        }
                        aggregateMenu = null;
                    }
                    if (choice== AggregateMenu.Choice.REMOVE_AGGREGATE) {
                        if (myJson!=null) {
                            Operation aggOp = new Operation.AggUniqueFields(myJson.root, false);
                            JsonNode newRoot = operationList.run(aggOp);
                            if (null == newRoot) {
                                // We didn't do anything
                                notificationText = "No aggregation found; move cursor to parent of aggregate";
                            } else {
                                myJson = newRoot;
                                notificationText = "remove_aggregates()";
                            }
                        }
                        aggregateMenu = null;
                    }
                }
                else {
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
                        // prev cursor/match
                        myJson.cursorPrevCursor();
                    }
                    if ((key.getCharacter() != null && 'a' == key.getCharacter())) {
                        // aggregate
                        aggregateMenu = new AggregateMenu();
                    }
//                    if ((key.getCharacter() != null && 'r' == key.getCharacter())) {
//                        // restart (for testing)
//                        myJson = JsonNode.parse(path);
//                    }
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
                    if (key.getCharacter() != null && ('+' == key.getCharacter())) {
                        Operation union = new Operation.UnionCursors(myJson);
                        notificationText = union.toString();
                        myJson = operationList.run(union);
                    }
                    if (key.getCharacter() != null && ('Z' == key.getCharacter())) {
                        Operation op = operationList.peek();
                        if (null!=op) {
                            notificationText = "undo " + op.toString();
                            myJson = operationList.undo();
                        }
                    }
                    if (key.getCharacter() != null && ('s' == key.getCharacter())) {
                        boolean allValues = myJson.atAnyCursor().stream().allMatch(x->x instanceof JsonNodeValue);
                        if (!allValues) {
                            sortControl = new SortControl(myJson.atAnyCursor());
                        }
                    }
                    if (key.getKeyType() == KeyType.Escape) {
                        myJson.rootInfo.secondaryCursors = new NoMultiCursor();
                    }
                    if (key.getCharacter() != null && 'q' == key.getCharacter())
                        break;
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


}