package org.example;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import org.example.cursor.FindCursor;
import org.example.cursor.NoMultiCursor;
import org.example.cursor.PathCursor;
import org.example.ui.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class Main {

    // All about the search feature
    private boolean showFind = false;
    private FindControl findControl;
    private SortControl sortControl;
    private AggregateMenu aggregateMenu;
    private ActionMenu actionMenu;
    private MainMenu mainMenu;
    private JsonNode.SavedCursors cursorsBeforeFind = null;
    private JsonNode myJson;
    private Terminal terminal;
    private Screen screen;
    private Drawer drawer;
    private int scroll;
    private int rowLimit;

    private OperationList operationList = new OperationList();
    private String notificationText = "";
    private String copied = "";

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
                "1/2/3           : fold after this many levels\n" +
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
                "g               : groupby sel. key(s)       \n"+
                "s               : sort selected array(s)    \n"+
                "ENTER           : parse as JSON             \n"+
                "shift-Z         : undo last transform       \n"+
                "                                            \n"+
                "----------------[ Program ]-----------------\n"+
                "h / ?           : show help page            \n"+
                "q               : quit                      \n";


    public static Main fromPathStr(@NotNull String pathStr) throws IOException {
        return Main.fromPathStr(pathStr, null);
    }

    public static Main fromPathStrAndVirtual(@NotNull String pathStr, int width, int height) throws IOException {
        Terminal term = new DefaultVirtualTerminal(new TerminalSize(width, height));
        return Main.fromPathStr(pathStr, term);
    }

    public static Main fromLinesAndVirtual(@NotNull String[] lines, int width, int height) throws IOException {
        Terminal term = new DefaultVirtualTerminal(new TerminalSize(width, height));
        JsonNode myJson = JsonNode.parseLines(lines);
        return new Main(myJson, term, Locale.US);
    }

    protected static Main fromPathStr(@NotNull String pathStr, @Nullable Terminal terminalOverride) throws IOException {
        // load the JSON, using NIO libraries if they were added to the classpath.
        Path path = Paths.get(pathStr);
        JsonNode myJson = JsonNode.parse(path);
        return new Main(myJson, terminalOverride);
    }

    private Main(@NotNull JsonNode root, @Nullable Terminal terminalOverride) throws IOException {
        this(root, terminalOverride, null);
    }

    private Main(@NotNull JsonNode root, @Nullable Terminal terminalOverride, Locale defaultLocale) throws IOException {
        myJson = root;
        findControl = new FindControl(myJson);

        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
        screen = null;
        if (null==terminalOverride) {
            terminal = defaultTerminalFactory.createTerminal();
        } else {
            terminal = terminalOverride;
        }
        screen = new TerminalScreen(terminal);
        drawer = new Drawer(defaultLocale);
        // TUI mode
        screen.startScreen();
        // hide cursor
        screen.setCursorPosition(null);
        scroll = 0;
        rowLimit = screen.getTerminalSize().getRows()-2;
    }


    /**
     * Display the UI, including any menu open at the time.
     **/
    public void display() throws IOException {
        screen.doResizeIfNecessary();
        rowLimit = screen.getTerminalSize().getRows()-2;
        TextGraphics g = screen.newTextGraphics();
        screen.clear();

        drawer.printJsonTree(g, TerminalPosition.TOP_LEFT_CORNER.withRelativeRow(-scroll), 0, myJson);
        if (drawer.getCursorLineLastTime()>rowLimit) {
            scroll += (drawer.getCursorLineLastTime()-rowLimit);
            screen.clear();
            drawer.printJsonTree(g, TerminalPosition.TOP_LEFT_CORNER.withRelativeRow(-scroll), 0, myJson);
        } else if (drawer.getCursorLineLastTime()<0) {
            scroll += drawer.getCursorLineLastTime();
            screen.clear();
            drawer.printJsonTree(g, TerminalPosition.TOP_LEFT_CORNER.withRelativeRow(-scroll), 0, myJson);
        }
        if (null!=sortControl) {
            sortControl.draw(screen.newTextGraphics());
        }
        else if (showFind) {
            findControl.draw(screen.newTextGraphics());
        } else if (aggregateMenu!=null) {
            aggregateMenu.draw(screen.newTextGraphics());
        } else if (actionMenu!=null) {
            actionMenu.draw(screen.newTextGraphics());
        } else if (mainMenu!=null) {
            mainMenu.draw(screen.newTextGraphics());
        }

        // Bar at the bottom: notification if there's one, or path.
        String bottomText = notificationText;
        if (bottomText.isEmpty()) {
            int numCursors = myJson.atAnyCursor().size();
            bottomText = myJson.rootInfo.userCursor.toString() + " ♦ " + numCursors + " cursor";
            if (numCursors!=1) bottomText += "s";

            if (numCursors>1 || !(myJson.rootInfo.secondaryCursors instanceof NoMultiCursor)) {
                bottomText += " ♦ ESC for one";
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(bottomText);
        String info = "m: menu ";
        int targetLength = screen.getTerminalSize().getColumns()-info.length();
        if (sb.length() >= targetLength) {
            sb = new StringBuilder(sb.substring(0, targetLength-1));
        }
        while (sb.length() < targetLength) {
            sb.append(" ");
        }
        sb.append(info);
        g.putString(TerminalPosition.TOP_LEFT_CORNER.withRelativeRow(g.getSize().getRows()-1), sb.toString(), SGR.REVERSE);
        // Notifications last only one frame.
        notificationText = "";

        screen.refresh(Screen.RefreshType.DELTA);
    }

    public KeyStroke waitForKey() throws IOException {
        return terminal.readInput();
    }

    public String stringifyAllCursors() {
        String ret = "";
        for (JsonNode node : myJson.atAnyCursor()) {
            Object value = node.getValue();
            String foo = (null==value?"null":value.toString());
            if (ret.isEmpty()) {
                ret = foo;
            } else {
                ret = ret + "\n" + foo;
            }
        }
        return ret;
    }

    public void copyToClipboard(String theString, boolean add) {
        if (add) {
            if (copied.length()>0) copied += "\n";
            copied += theString;
        } else {
            copied = theString;
        }
        StringSelection selection = new StringSelection(copied);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
        if (add) {
            notificationText = "Added to copy";
        } else {
            notificationText = "Copied";
        }
    }

    /**
     * Behave as if the user had selected that aggregation via menu or key.
     * @return true if we did something.
     **/
    public boolean applyAggregation(AggregateMenu.Choice choice) {
        if (choice== AggregateMenu.Choice.CANCEL) {
            aggregateMenu = null;
            return true;
        } else if (choice== AggregateMenu.Choice.UNIQUE_FIELDS) {
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
            return true;
        } else
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
            return true;
        } else
        if (choice==AggregateMenu.Choice.AGG_TOTAL) {
            if (myJson!=null) {
                Operation sumOp = new Operation.OpAggTotal(myJson.root);
                JsonNode newRoot = operationList.run(sumOp);
                if (null!=newRoot) {
                    notificationText = "sum()";
                    myJson = newRoot;
                } else {
                    notificationText = "Nothing to sum; move cursor to a list of numbers";
                }
            }
            aggregateMenu = null;
            return true;
        } else
        if (choice==AggregateMenu.Choice.AGG_MIN_MAX) {
            if (myJson != null) {
                Operation op = new Operation.OpAggMinMax(myJson.root);
                JsonNode newRoot = operationList.run(op);
                if (null != newRoot) {
                    notificationText = op.toString();
                    myJson = newRoot;
                } else {
                    notificationText = "Nothing to min-max; move cursor to a list of numbers";
                }
            }
            aggregateMenu = null;
            return true;
        } else if (choice==AggregateMenu.Choice.AGG_AVG) {
            if (myJson != null) {
                Operation op = new Operation.OpAggAvg(myJson.root);
                JsonNode newRoot = operationList.run(op);
                if (null != newRoot) {
                    notificationText = op.toString();
                    myJson = newRoot;
                } else {
                    notificationText = "Nothing to average; move cursor to a list of numbers (or lists)";
                }
            }
            aggregateMenu = null;
            return true;
        } else if (choice== AggregateMenu.Choice.NONE) {
            return false;
        }
        return false;
    }

    /**
     * Behave as if the user had selected that action via menu or key.
     * @return true if we did something.
     **/
    public boolean applyAction(ActionMenu.Choice choice) {
        if (choice== ActionMenu.Choice.CANCEL) {
            actionMenu = null;
            return true;
        } else if (choice== ActionMenu.Choice.PARSE) {

            Operation.OpParse op = new Operation.OpParse(myJson);
            JsonNode foo = operationList.run(op);
            if (null == foo) {
                notificationText = "Could not parse as JSON";
            } else {
                notificationText = "Parsed as JSON";
            }
            actionMenu = null;
            return true;
        } else if (choice==ActionMenu.Choice.COPY) {
            copyToClipboard(stringifyAllCursors(), false);
            actionMenu = null;
            return true;
        } else if (choice==ActionMenu.Choice.ADD_TO_COPY) {
            copyToClipboard(stringifyAllCursors(), true);
            actionMenu = null;
            return true;
        } else if (choice==ActionMenu.Choice.NONE) {
            return false;
        }
        notificationText = "Unknown action: " + choice.toString();
        return false;
    }

    @VisibleForTesting
    public String getTestViewOfScreen() {
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

    // do this when we press the "up" arrow
    public void moveCursorUp() {
        Cursor current = myJson.rootInfo.userCursor;
        if (!drawer.tryCursorUp(myJson.rootInfo.userCursor)) {
            myJson.cursorUp();
        }
    }

    // For testing
    public JsonNode getRoot() {
        return myJson;
    }

    // do this when we press the "down" arrow
    public void moveCursorDown(boolean doScroll) {
        Cursor current = myJson.rootInfo.userCursor;
        if (!drawer.tryCursorDown(myJson.rootInfo.userCursor)) {
            myJson.cursorDown();
            if (doScroll && myJson.rootInfo.userCursor == current) {
                // We are at the end of the document, didn't actually move down.
                // Let's scroll down a bit, maybe the user wants to see the closing
                // brackets.
                scroll++;
            }
        }
    }

    public void groupby() {
        // Should we do groupby or countdups?
        int parentIsList = 0;
        for (JsonNode cur : myJson.atAnyCursor()) {
            if (cur.getParent() instanceof JsonNodeList) parentIsList++;
        }
        if (parentIsList > myJson.atAnyCursor().size()/2) {
            // most parents are lists, do a countdups
            OpCountEachDistinct op = new OpCountEachDistinct(myJson);
            JsonNode result = operationList.run(op);
            if (null == result) {
                notificationText = "Unable to run countdups here. Try on a value in a list.";
            } else {
                notificationText = op.toString();
                myJson = result;
            }
        } else {
            // most parents are maps, do a groupby
            OpGroupby op = new OpGroupby(myJson);
            JsonNode result = operationList.run(op);
            if (null == result) {
                notificationText = "Unable to run groupby here. Try the key in a map in a list.";
            } else {
                notificationText = op.toString();
                myJson = result;
            }
        }
    }

    // Sort at the cursor in the specified way.
    public void sort(Sorter sorter) {
        Operation sort = new Operation.Sort(myJson, sorter);
        notificationText = sort.toString();
        myJson = operationList.run(sort);
        sortControl = null;
    }

    /**
     * Updates the state based on the key pressed.
     * Returns TRUE if you should continue, FALSE if quitting.
     **/
    public boolean actOnKey(KeyStroke key) throws IOException {
        char pressed = '\0';
        if (key.getKeyType()==KeyType.Character) pressed = key.getCharacter();



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
                sort(s);
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
            applyAggregation(choice);
        }
        else if (null!=actionMenu) {
            ActionMenu.Choice choice = actionMenu.update(key);
            applyAction(choice);
        }
        else if (mainMenu!=null) {
            // The "main menu" is a bit special, because it doesn't block the normal keys.
            MainMenu.Choice choice = mainMenu.update(key);
            if (choice==MainMenu.Choice.CANCEL) {
                mainMenu = null;
            }
            if (choice==MainMenu.Choice.ACTION) {
                mainMenu = null;
                actionMenu = new ActionMenu();
            }
            if (choice==MainMenu.Choice.AGGREGATE) {
                mainMenu = null;
                aggregateMenu = new AggregateMenu();
            }
            if (choice== MainMenu.Choice.FIND) {
                mainMenu = null;
                showFind = true;
                findControl.init();
                cursorsBeforeFind = myJson.rootInfo.save();
            }
            if (choice== MainMenu.Choice.SORT) {
                mainMenu = null;
                sortControl = new SortControl(myJson.atAnyCursor());
            }
            if (choice==MainMenu.Choice.UNION) {
                mainMenu = null;
                Operation union = new Operation.UnionCursors(myJson);
                notificationText = union.toString();
                myJson = operationList.run(union);
            }
            if (choice==MainMenu.Choice.GROUPBY) {
                mainMenu = null;
                groupby();
            }
            if (choice==MainMenu.Choice.HELP) {
                mainMenu = null;
                helpScreen(terminal, screen);
            }
            if (choice== MainMenu.Choice.QUIT) {
                return false;
            }
        } else {
            // normal key handling
            if (key.getKeyType() == KeyType.ArrowDown && !key.isShiftDown()) {
                moveCursorDown(true);
            }
            if (key.getKeyType() == KeyType.ArrowDown && key.isShiftDown()) myJson.cursorNextSibling();
            if (key.getKeyType() == KeyType.ArrowUp && !key.isShiftDown()) {
                moveCursorUp();
            }
            if (key.getKeyType() == KeyType.ArrowUp && key.isShiftDown()) myJson.cursorPrevSibling();
            if (key.getKeyType() == KeyType.ArrowLeft) {
                if (myJson.getFoldedAtCursor() || !myJson.setFoldedAtCursors(true)) {
                    myJson.cursorParent();
                }
            }
            if (key.getKeyType() == KeyType.ArrowRight
                    || (key.getCharacter() != null && 'f' == pressed)) {
                myJson.setFoldedAtCursors(false);
            }
            if (key.getKeyType() == KeyType.Enter) {
                actionMenu = new ActionMenu();
            }
            if (key.getCharacter() != null && ('e' == pressed || '*' == key.getCharacter())) {
                myJson.cursorDownToAllChildren();
            }
            if ((key.getCharacter() != null && 'p' == pressed)) {
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
            if (pressed=='g') {
                groupby();
            }
            if ((key.getCharacter() != null && 'a' == pressed)) {
                // aggregate
                aggregateMenu = new AggregateMenu();
            }
//                    if ((key.getCharacter() != null && 'r' == key.getCharacter())) {
//                        // restart (for testing)
//                        myJson = JsonNode.parse(path);
//                    }
            if (key.getKeyType() == KeyType.PageDown) {
                for (int i = 0; i < rowLimit-1; i++) {
                    moveCursorDown(false);
                }
                moveCursorDown(true);
            }
            if (key.getKeyType() == KeyType.PageUp) {
                for (int i = 0; i < rowLimit; i++) {
                    moveCursorUp();
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
            if (pressed=='1') {
                myJson.atCursor().setFoldedLevels(1);
            }
            if (pressed=='2') {
                myJson.atCursor().setFoldedLevels(2);
            }
            if (pressed=='3') {
                myJson.atCursor().setFoldedLevels(3);
            }
            if (pressed=='4') {
                myJson.atCursor().setFoldedLevels(4);
            }
            if (pressed=='5') {
                myJson.atCursor().setFoldedLevels(5);
            }
            if (pressed=='6') {
                myJson.atCursor().setFoldedLevels(6);
            }
            if (pressed=='7') {
                myJson.atCursor().setFoldedLevels(7);
            }
            if (pressed=='9') {
                // unfold everything
                myJson.atCursor().setFoldedLevels(999);
            }
            if (pressed=='m') {
                mainMenu = new MainMenu();
                notificationText = "Hint: the shortcut keys here work even if the menu is not open";
            }
            if (pressed=='c') {
                copyToClipboard(stringifyAllCursors(), false);
            } else if (pressed=='C') {
                Object val = myJson.atCursor().getValue();
                copyToClipboard(stringifyAllCursors(), true);
            }
            if (key.getKeyType() == KeyType.Escape) {
                myJson.rootInfo.secondaryCursors = new NoMultiCursor();
            }
            if (pressed=='q')
                return false;
        }
        return true;
    }

    public void closeScreen() {
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

    public void go_to(String cursorLike) {
        myJson.setCursors(cursorLike);
    }

    public static void main(String[] args) throws Exception {

        if (args.length==0 || (args.length==1 && args[0].equals("--help"))) {
            System.out.println("(C) 2025 Jean-Philippe Martin");
            System.out.println();
            System.out.println("Usage:");
            System.out.println("java -jar JavaJson*.jar myfile.json [--goto <path>]");
            System.out.println("OR");
            System.out.println("java -jar JavaJson*.jar myfile.json --print <path>");
            System.out.println();
            System.out.println("Examples:");
            System.out.println("java -jar target/JavaJson-1.6-SNAPSHOT-jar-with-dependencies.jar testdata/hello.json");
            System.out.println("java -jar target/JavaJson-1.6-SNAPSHOT-jar-with-dependencies.jar testdata/hello.json --goto '.players[0].score'");
            System.out.println("java -jar target/JavaJson-1.6-SNAPSHOT-jar-with-dependencies.jar testdata/hello.json --print '.players[*].name'");
            System.out.println();
            System.out.println("Key bindings:");
            System.out.println(keys_help);
            return;
        }


        String fileName = null;
        HashMap<String, String> options = new HashMap<>();
        boolean ignoreOpts = false;
        options.put("--goto", null);
        options.put("--print", null);


        int i=-1;
        while (++i<args.length) {
            String s = args[i];
            if (!ignoreOpts && s.startsWith("--")) {
                if ("--".equals(s)) {
                    ignoreOpts = true;
                    continue;
                }
                if (!options.containsKey(s)) {
                    System.err.println("Unrecognized option: " + s);
                    return;
                }
                if (options.get(s)!=null) {
                    System.err.println("Cannot set " + s + " more than once.");
                    return;
                }
                if (i+1==args.length) {
                    System.err.println("Missing value for " + s);
                    return;
                }
                options.put(s, args[++i]);
                continue;
            }
            if (null!=fileName) {
                System.err.println("Sorry, can only open one file.");
                return;
            }
            fileName = s;
        }

        if (null==fileName) {
            System.out.println("Missing: a file name to open");
        }
        Main main = Main.fromPathStr(fileName);
        String p = options.get("--print");
        if (null!=p) {
            JsonNode myJson = main.myJson;
            main.closeScreen();
            // print those values, then quit
            PathCursor selected = new PathCursor(p);
            java.util.List<JsonNode> nodes = new ArrayList<>();
            selected.addAllNodes(myJson.asCursor(), nodes);
            for (JsonNode n : nodes) {
                Object value = n.getValue();
                System.out.println((null==value?"null":value.toString()));
            }
            return;
        }
        String g = options.get("--goto");
        if (null!=g) try {
            main.go_to(g);
            main.notificationText = "Gone to: " + g;
        } catch (RuntimeException oops) {
            main.notificationText = oops.getMessage();
        }

        try {
            while(true) {
                main.display();
                KeyStroke key = main.waitForKey();
                if (!(main.actOnKey(key))) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            main.closeScreen();
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