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

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.awt.Toolkit;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static class DebugMode {
        final List<KeyStroke> keys = new ArrayList<>();
    }

    // All about the search feature
    private boolean showFind = false;
    private FindControl findControl;
    private SortControl sortControl;
    private AggregateMenu aggregateMenu;
    private ActionMenu actionMenu;
    private MainMenu mainMenu;
    private PasteScreen pasteMenu=null;
    private DeleteMenu deleteMenu = null;
    private JsonNode.SavedCursors cursorsBeforeFind = null;
    private JsonNode myJson;
    private Terminal terminal;
    private Screen screen;
    private Drawer drawer;
    private int scroll;
    private int rowLimit;
    private boolean pleaseRefresh = false;
    private Deleter deleter = null;

    private OperationList operationList = new OperationList();
    private String notificationText = "";
    private String copied = "";

    // Non-null iff we're in debug mode.
    public @Nullable DebugMode debugMode;

    private static final String keys_help =
                "----------------[ Movement ]----------------\n"+
                "up / down       : navigate line             \n"+
                "pg up / pg dn   : navigate page             \n"+
                "home / g        : beginning of document      \n"+
                "end / G         : end of document           \n"+
                "shift + up/down : navigate sibling          \n"+
                "shift + left    : parent                    \n"+
                "                                            \n"+
                "----------------[ View ]--------------------\n"+
                "left / right    : fold / unfold             \n"+
                "p               : pin (show despite folds)  \n"+
                "0/1/2/3/...     : fold after this many levels\n" +
                "                                            \n"+
                "----------------[ Multicursor ]-------------\n"+
                "f / \"/\"         : find                      \n"+
                "e / *           : select all children       \n"+
                "n / N           : next/prev cursor          \n"+
                "ESC             : remove secondary cursors  \n"+
                "                                            \n"+
                "----------------[ Transform ]---------------\n"+
                "+               : union selected arrays     \n"+
                "a               : aggregate sel. array(s)   \n"+
                "b               : groupby sel. key(s)       \n"+
                "d               : delete parts of the doc   \n"+
                "s               : sort selected array(s)    \n"+
                "ENTER           : value menu: copy/parse/...\n"+
                "v               : paste a new document      \n"+
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

    protected static Main fromLines(@NotNull String[] lines, @Nullable Terminal terminalOverride) throws IOException {
        JsonNode myJson = JsonNode.parseLines(lines);
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
        var nullIfNoResize = screen.doResizeIfNecessary();
        if (nullIfNoResize!=null) pleaseRefresh = true;
        rowLimit = screen.getTerminalSize().getRows()-2;
        TextGraphics g = screen.newTextGraphics();
        screen.clear();

        if (deleteMenu!=null) {
            deleter = deleteMenu.getDeleter(myJson);
        } else {
            deleter = null;
        }

        drawer.printJsonTree(g, TerminalPosition.TOP_LEFT_CORNER.withRelativeRow(-scroll), 0, myJson, deleter);
        if (drawer.getCursorLineLastTime()>rowLimit) {
            scroll += (drawer.getCursorLineLastTime()-rowLimit);
            screen.clear();
            drawer.printJsonTree(g, TerminalPosition.TOP_LEFT_CORNER.withRelativeRow(-scroll), 0, myJson, deleter);
        } else if (drawer.getCursorLineLastTime()<0) {
            scroll += drawer.getCursorLineLastTime();
            screen.clear();
            drawer.printJsonTree(g, TerminalPosition.TOP_LEFT_CORNER.withRelativeRow(-scroll), 0, myJson, deleter);
        }
        if (null!=sortControl) {
            sortControl.draw(screen.newTextGraphics());
        } else if (showFind) {
            findControl.draw(screen.newTextGraphics());
        } else if (aggregateMenu!=null) {
            aggregateMenu.draw(screen.newTextGraphics());
        } else if (actionMenu!=null) {
            actionMenu.draw(screen.newTextGraphics());
        } else if (mainMenu!=null) {
            mainMenu.draw(screen.newTextGraphics());
        } else if (pasteMenu!=null) {
            pasteMenu.draw(screen.newTextGraphics());
        } else if (deleteMenu!=null) {
            deleteMenu.draw(screen.newTextGraphics());
        }

        // Bar at the bottom: notification if there's one, or path.
        String bottomText = notificationText;
        if (bottomText.isEmpty()) {
            bottomText = myJson.rootInfo.userCursor.toString();
        }
        int numCursors = myJson.atAnyCursor().size();
        bottomText += " ♦ " + numCursors + " cursor";
        if (numCursors!=1) bottomText += "s";

        if (numCursors>1 || !(myJson.rootInfo.secondaryCursors instanceof NoMultiCursor)) {
            bottomText += " ♦ ESC for one";
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

        if (pleaseRefresh) {
            screen.refresh(Screen.RefreshType.COMPLETE);
            pleaseRefresh = false;
        } else {
            screen.refresh(Screen.RefreshType.DELTA);
        }
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

    public String stringifyAllKeys() {
        String ret = "";
        for (JsonNode node : myJson.atAnyCursor()) {
            String foo = node.asCursor().toString();
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

    public void moveCursorNextCousin(boolean doScroll) {
        JsonNode atCursor = myJson.atCursor();
        var step = atCursor.asCursor().getStep();
        JsonNode parent = atCursor.getParent();
        if (null==parent || parent==atCursor) {
            // We are root.
            moveCursorDown(doScroll);
            return;
        }
        JsonNode grandParent = parent.getParent();
        if (null==grandParent || grandParent==parent) {
            // We don't have a grandpa.
            JsonNode goal = parent.nextChild(atCursor.asCursor());
            if (null!=goal) {
                myJson.rootInfo.setPrimaryCursor(goal.whereIAm);
            } else {
                moveCursorDown(doScroll);
            }
            return;
        }
        JsonNode parentsSibling = grandParent.nextChild(parent.asCursor());
        if (null==parentsSibling) {
            // This is the last sibling.
            moveCursorDown(doScroll);
            return;
        }
        JsonNode goal = null;
        try {
            goal = step.apply(parentsSibling);
        } catch (Exception x) {
            // "child not found", etc.
            goal = parentsSibling.firstChild();
            if (null==goal) goal = parentsSibling;
        }
        if (null!=goal) {
            myJson.rootInfo.setPrimaryCursor(goal.whereIAm);
        }
        if (doScroll && goal == atCursor) {
            // We are at the end of the document, didn't actually move down.
            // Let's scroll down a bit, maybe the user wants to see the closing
            // brackets.
            scroll++;
        }
    }

    public void moveCursorPrevCousin(boolean doScroll) {
        JsonNode atCursor = myJson.atCursor();
        var step = atCursor.asCursor().getStep();
        JsonNode parent = atCursor.getParent();
        if (null==parent || parent==atCursor) {
            // We are root.
            moveCursorUp();
            return;
        }
        JsonNode grandParent = parent.getParent();
        if (null==grandParent || grandParent==parent) {
            // We don't have a grandpa.
            JsonNode goal = parent.prevChild(atCursor.asCursor());
            if (null!=goal) {
                myJson.rootInfo.setPrimaryCursor(goal.whereIAm);
            } else {
                moveCursorUp();
            }
            return;
        }
        JsonNode parentsSibling = grandParent.prevChild(parent.asCursor());
        if (null==parentsSibling) {
            // This is the first sibling.
            moveCursorUp();
            return;
        }
        JsonNode goal = null;
        try {
            goal = step.apply(parentsSibling);
        } catch (Exception x) {
            // "child not found", etc.
            goal = parentsSibling.firstChild();
            if (null==goal) goal = parentsSibling;
        }
        if (null!=goal) {
            myJson.rootInfo.setPrimaryCursor(goal.whereIAm);
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
        myJson.rootInfo.fixCursors();
    }

    // Sort at the cursor in the specified way.
    public void sort(Sorter sorter) {
        Operation sort = new Operation.Sort(myJson, sorter);
        notificationText = sort.toString();
        myJson = operationList.run(sort);
        sortControl = null;
    }

    /**
     * Wait for at least one key press.
     * Call actOnKey(key) as long as there's a key in the buffer.
     * Return TRUE if you should continue, FALSE if quitting.
     */
    private boolean actOnAllKeys() throws IOException {
        KeyStroke key = waitForKey();
        if (!actOnKey(key)) return false;
        while (true) {
            key = terminal.pollInput();
            if (null == key) return true;
            if (!actOnKey(key)) return false;
        }
    }

    /**
     * Updates the state based on the key pressed.
     * Returns TRUE if you should continue, FALSE if quitting.
     **/
    public boolean actOnKey(KeyStroke key) throws IOException {

        if (null!=debugMode) {
            debugMode.keys.add(key);
        }

        // key -> Action
        Action choice = actionFromKey(key);

        // Action -> it is done.
        return act(choice);
    }

    /**
     * Updates the state based on the chosen action.
     * Returns TRUE if you should continue, FALSE if quitting.
     **/
    public boolean act(Action choice) throws IOException {
        switch (choice) {
            case QUIT:
                return false;
            case NOTHING:
                if (null!=mainMenu) {
                    String help = mainMenu.getHelpText();
                    if (null != help) notificationText = help;
                }
                return true;
            case NAV_PREV_LINE:
                moveCursorUp();
                return true;
            case NAV_NEXT_LINE:
                moveCursorDown(true);
                return true;
            case NAV_NEXT_COUSIN:
                moveCursorNextCousin(true);
                return true;
            case NAV_PREV_COUSIN:
                moveCursorPrevCousin(true);
                return true;
            case GROUPBY:
                mainMenu = null;
                groupby();
                return true;
            case UNION:
                mainMenu = null;
                Operation union = new Operation.UnionCursors(myJson);
                notificationText = union.toString();
                myJson = operationList.run(union);
                myJson.rootInfo.fixCursors();
                return true;
            case COPY_AT_CURSORS:
                copyToClipboard(stringifyAllCursors(), false);
                return true;
            case ADD_COPY_AT_CURSORS:
                copyToClipboard(stringifyAllCursors(), true);
                return true;
            case COPY_PATH_AT_CURSORS:
                copyToClipboard(stringifyAllKeys(), false);
                return true;
            case ADD_PATH_TO_COPY_AT_CURSORS:
                copyToClipboard(stringifyAllKeys(), true);
                return true;
            case SHOW_HELP_SCREEN:
                mainMenu = null;
                helpScreen(terminal, screen);
                return true;
            case SHOW_FIND_MENU:
                mainMenu = null;
                showFind = true;
                findControl.init();
                cursorsBeforeFind = myJson.rootInfo.save();
                maybeShowNotification(findControl.getHelpText());
                return true;
            case SHOW_ACTION_MENU:
                mainMenu = null;
                actionMenu = new ActionMenu();
                return true;
            case SHOW_PASTE_MENU:
                mainMenu = null;
                pasteMenu = new PasteScreen();
                notificationText = "Paste text, then press right arrow.";
                return true;
            case SHOW_SORT_MENU:
                boolean allValues = myJson.atAnyCursor().stream().allMatch(x->x instanceof JsonNodeValue);
                if (!allValues) {
                    mainMenu = null;
                    sortControl = new SortControl(myJson.atAnyCursor());
                    if (null != sortControl) {
                        String help = sortControl.getHelpText();
                        if (null != help) notificationText = help;
                    }
                } else {
                    notificationText = "Select a list/map for sort";
                }
                return true;
            case SHOW_AGGREGATE_MENU:
                mainMenu = null;
                aggregateMenu = new AggregateMenu();
                String help = aggregateMenu.getHelpText();
                if (null!=help) notificationText = help;
                return true;
            case SHOW_MAIN_MENU:
                mainMenu = new MainMenu(operationList);
                notificationText = "Hint: the shortcut keys here work even if the menu is not open";
                return true;
            case SHOW_DELETE_MENU:
                mainMenu = null;
                deleteMenu = new DeleteMenu(myJson);
                maybeShowNotification(deleteMenu.getHelpText());
                return true;
            case DELETE:
                mainMenu = null;
                deleter = deleteMenu.getDeleter(myJson);
                deleteMenu = null;
                delete(deleter);
                return true;
            case HIDE_MENU:
                mainMenu = null;
                actionMenu = null;
                pasteMenu = null;
                aggregateMenu = null;
                sortControl = null;
                deleteMenu = null;
                deleter = null;
                return true;
            case UNDO:
                Operation op = operationList.peek();
                if (null!=op) {
                    notificationText = "undo " + op.toString();
                    myJson = operationList.undo();
                    myJson.rootInfo.fixCursors();
                }
                mainMenu = null;
                return true;
        }
        return true;
    }

    // Apply this deletion operation.
    @VisibleForTesting
    public void delete(Deleter deleter) {
        Operation deleteOp = new OpDelete(myJson, deleter);
        notificationText = deleteOp.toString();
        myJson = operationList.run(deleteOp);
        myJson.rootInfo.fixCursors();
    }

    private void maybeShowNotification(@Nullable String help) {
        if (null!=help) notificationText = help;
    }

    // In principle this should return an Action that says what we want to do,
    // and the act method will do it. This allows the same action to reliably be
    // triggered e.g. from a menu and also a keyboard shortcut.
    //
    // However, not all have been converted yet so you'll see some acting in here still.
    public Action actionFromKey(KeyStroke key) {
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
            String help = findControl.getHelpText();
            if (null!=help) notificationText = help;
        }
        else if (null!=sortControl) {
            // manage the sort dialog
            Sorter s = sortControl.update(key);
            if (s!=null) {
                sort(s);
            }
            if (key.getKeyType()==KeyType.Escape) {
                return Action.HIDE_MENU;
            }
            if (key.getKeyType()==KeyType.Character && key.getCharacter()=='q') {
                return Action.HIDE_MENU;
            }
            if (key.getKeyType()==KeyType.Character && key.getCharacter()=='x') {
                Operation sort = new Operation.Sort(myJson, null);
                notificationText = sort.toString();
                myJson = operationList.run(sort);
                sortControl = null;
            }
            if  (null!=sortControl) {
                String help = sortControl.getHelpText();
                if (null != help) notificationText = help;
            }
        }
        else if (null!=aggregateMenu) {
            // manage the aggregate menu
            AggregateMenu.Choice choice = aggregateMenu.update(key);
            applyAggregation(choice);
            if (null!=aggregateMenu) {
                String help = aggregateMenu.getHelpText();
                if (null != help) notificationText = help;
            }
        }
        else if (null!=actionMenu) {
            ActionMenu.Choice choice = actionMenu.update(key);
            if (choice== ActionMenu.Choice.CANCEL) {
                return Action.HIDE_MENU;
            } else if (choice==ActionMenu.Choice.PARSE_AND_INTERPRET || choice==ActionMenu.Choice.PARSE_IGNORE_ESCAPES) {
                Operation.OpParse op = new Operation.OpParse(myJson, choice==ActionMenu.Choice.PARSE_AND_INTERPRET);
                JsonNode foo = operationList.run(op);
                if (null == foo) {
                    notificationText = "Could not parse as JSON";
                } else {
                    notificationText = "Parsed as JSON";
                }
                actionMenu = null;
                return Action.NOTHING;
            } else if (choice==ActionMenu.Choice.COPY) {
                actionMenu = null;
                return Action.COPY_AT_CURSORS;
            } else if (choice==ActionMenu.Choice.ADD_TO_COPY) {
                actionMenu = null;
                return Action.ADD_COPY_AT_CURSORS;
            } else if (choice==ActionMenu.Choice.COPY_PATH) {
                actionMenu = null;
                return Action.COPY_PATH_AT_CURSORS;
            } else if (choice==ActionMenu.Choice.ADD_PATH_TO_COPY) {
                actionMenu = null;
                return Action.ADD_PATH_TO_COPY_AT_CURSORS;
            } else if (choice==ActionMenu.Choice.NONE) {
                return Action.NOTHING;
            }
            notificationText = "Unknown action: " + choice.toString();
            return Action.NOTHING;
        }
        else if (mainMenu!=null) {
            return mainMenu.update(key);
        } else if (null!=pasteMenu) {
            var choice = pasteMenu.update(key);
            if (choice == PasteScreen.Choice.CANCEL) {
                return Action.HIDE_MENU;
            } else if (choice== PasteScreen.Choice.NONE) {
                // nothing to do
            } else if (choice==PasteScreen.Choice.PARSE_AND_INTERPRET) {
                try {
                    JsonNode newJson = JsonNode.parseLines(pasteMenu.getLines());
                    Operation op = new OpReplaceRoot(myJson, newJson);
                    notificationText = "Replaced with pasted text (interpreting the escapes)";
                    myJson = operationList.run(op);
                } catch (Exception x) {
                    notificationText = "Pasted text was not valid JSON nor JSONL";
                }
                pasteMenu = null;
            } else if (choice==PasteScreen.Choice.PARSE_IGNORE_ESCAPES) {
                try {
                    JsonNode newJson = JsonNode.parseJsonIgnoreEscapes(String.join("\n", pasteMenu.getLines()));
                    Operation op = new OpReplaceRoot(myJson, newJson);
                    notificationText = "Replaced with pasted text (interpreting the escapes)";
                    myJson = operationList.run(op);
                } catch (Exception x) {
                    notificationText = "Pasted text was not valid JSON nor JSONL";
                }
                pasteMenu = null;
            }
        } else if (null!=deleteMenu) {
            Action ret = deleteMenu.update(key);
            maybeShowNotification(deleteMenu.getHelpText());
            return ret;
        } else {
            // normal key handling
            if (key.getKeyType() == KeyType.ArrowDown && !key.isShiftDown()) {
                return Action.NAV_NEXT_LINE;
            }
            if (key.isCtrlDown() && pressed=='n') {
                // Ctrl-n: next line (Emacs shortcut)
                return Action.NAV_NEXT_LINE;
            }
            if (key.getKeyType() == KeyType.ArrowDown && key.isShiftDown()) {
                return Action.NAV_NEXT_COUSIN;
            }
            if (key.getKeyType() == KeyType.ArrowUp && !key.isShiftDown()) {
                return Action.NAV_PREV_LINE;
            }
            if (key.isCtrlDown() && pressed=='p') {
                // Ctrl-p: previous line (Emacs shortcut)
                return Action.NAV_PREV_LINE;
            }
            if (key.getKeyType() == KeyType.ArrowUp && key.isShiftDown()) {
                return Action.NAV_PREV_COUSIN;
            }
            if (key.getKeyType() == KeyType.ArrowLeft) {
                if (key.isShiftDown()) {
                    // shift-left: up
                    myJson.cursorParent();
                } else {
                    // left: fold/up
                    if (myJson.getFoldedAtCursor() || !myJson.setFoldedAtCursors(true)) {
                        myJson.cursorParent();
                    }
                }
            }
            if (key.getKeyType() == KeyType.ArrowRight
                    /*|| (key.getCharacter() != null && 'f' == pressed)*/) {
                myJson.setFoldedAtCursors(false);
            }
            if (key.getKeyType() == KeyType.Enter) {
                return Action.SHOW_ACTION_MENU;
            }
            if (key.getCharacter() != null && ('e' == pressed || '*' == key.getCharacter())) {
                myJson.cursorDownToAllChildren();
            }
            if ((key.getCharacter() != null && 'p' == pressed && !key.isCtrlDown())) {
                boolean pinned = myJson.getPinnedAtCursor();
                myJson.setPinnedAtCursors(!pinned);
            }
            if ((key.getCharacter() != null && 'n' == key.getCharacter()) && !key.isCtrlDown()) {
                // next cursor/match
                myJson.cursorNextCursor();
            }
            if ((key.getCharacter() != null && 'N' == key.getCharacter()) && !key.isCtrlDown()) {
                // prev cursor/match
                myJson.cursorPrevCursor();
            }
            if (pressed=='b' && !key.isCtrlDown()) {
                return Action.GROUPBY;
            }
            if ((key.getCharacter() != null && 'a' == pressed)) {
                // aggregate
                return Action.SHOW_AGGREGATE_MENU;
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
            if (key.getKeyType() == KeyType.Home || pressed=='g') {
                myJson.rootInfo.setPrimaryCursor(myJson.whereIAm);
            }
            if (key.getKeyType() == KeyType.End || pressed=='G') {
                JsonNode lastChild = myJson;
                for (int i=0; i<100; i++) {
                    JsonNode x = lastChild.lastChild();
                    if (x==null || x==lastChild) break;
                    lastChild = x;
                }
                myJson.rootInfo.setPrimaryCursor(lastChild.whereIAm);
            }
            if (key.getCharacter() != null && ('h' == key.getCharacter() || '?' == key.getCharacter())) {
                return Action.SHOW_HELP_SCREEN;
            }
            if (key.getCharacter() != null && ('f' == key.getCharacter() || '/' == key.getCharacter())) {
                return Action.SHOW_FIND_MENU;
            }
            if (key.getCharacter() != null && ('+' == key.getCharacter())) {
                return Action.UNION;
            }
            if (key.getCharacter() != null && ('Z' == key.getCharacter())) {
                return Action.UNDO;
            }
            if (key.getCharacter() != null && ('s' == key.getCharacter())) {
                return Action.SHOW_SORT_MENU;
            }
            if (pressed=='0') {
                for (JsonNode node : myJson.atAnyCursor()) {
                    node.folded = true;
                }
            }
            if (pressed=='1') {
                for (JsonNode node : myJson.atAnyCursor()) {
                    node.setFoldedLevels(1);
                }
            }
            if (pressed=='2') {
                for (JsonNode node : myJson.atAnyCursor()) {
                    node.setFoldedLevels(2);
                }
            }
            if (pressed=='3') {
                for (JsonNode node : myJson.atAnyCursor()) {
                    node.setFoldedLevels(3);
                }
            }
            if (pressed=='4') {
                for (JsonNode node : myJson.atAnyCursor()) {
                    node.setFoldedLevels(4);
                }
            }
            if (pressed=='5') {
                for (JsonNode node : myJson.atAnyCursor()) {
                    node.setFoldedLevels(5);
                }
            }
            if (pressed=='6') {
                for (JsonNode node : myJson.atAnyCursor()) {
                    node.setFoldedLevels(6);
                }
            }
            if (pressed=='7') {
                for (JsonNode node : myJson.atAnyCursor()) {
                    node.setFoldedLevels(7);
                }
            }
            if (pressed=='9') {
                // unfold everything
                for (JsonNode node : myJson.atAnyCursor()) {
                    node.setFoldedLevels(999);
                }
            }
            if (pressed=='m') {
                return Action.SHOW_MAIN_MENU;
            }
            if (pressed=='c') {
                return Action.COPY_AT_CURSORS;
            } else if (pressed=='C') {
                return Action.ADD_COPY_AT_CURSORS;
            }
            if (pressed=='k') {
                return Action.COPY_PATH_AT_CURSORS;
            } else if (pressed=='K') {
                return Action.ADD_PATH_TO_COPY_AT_CURSORS;
            }
            if (pressed=='v') {
                return Action.SHOW_PASTE_MENU;
            }
            if (key.getKeyType() == KeyType.Escape) {
                myJson.rootInfo.secondaryCursors = new NoMultiCursor();
            }
            if (key.isCtrlDown() && pressed=='l') {
                // Ctrl-L: refresh
                this.pleaseRefresh = true;
            }
            if (pressed=='d') {
                return Action.SHOW_DELETE_MENU;
            }
            if (pressed=='q')
                return Action.QUIT;
        }
        return Action.NOTHING;
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

        if (args.length==1 && args[0].equals("--help")) {
            System.out.println("(C) 2025 Jean-Philippe Martin");
            System.out.println();
            System.out.println("Usage:");
            System.out.println("java -jar JavaJson*.jar myfile.json [--theme LIGHT|DARK|BW|WB] [--goto <path>]");
            System.out.println("OR");
            System.out.println("java -jar JavaJson*.jar myfile.json --print <path>");
            System.out.println();
            System.out.println("Examples:");
            System.out.println("java -jar target/JavaJson-1.11-jar-with-dependencies.jar testdata/hello.json");
            System.out.println("java -jar target/JavaJson-1.11-jar-with-dependencies.jar testdata/hello.json --goto '.players[0].score'");
            System.out.println("java -jar target/JavaJson-1.11-jar-with-dependencies.jar testdata/hello.json --print '.players[*].name'");
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
        options.put("--theme", null);
        HashMap<String, Boolean> flags = new HashMap<>();
        flags.put("--debug", false);


        int i=-1;
        while (++i<args.length) {
            String s = args[i];
            if (!ignoreOpts && s.startsWith("--")) {
                if ("--".equals(s)) {
                    ignoreOpts = true;
                    continue;
                }
                if (options.containsKey(s)) {
                    if (options.get(s) != null) {
                        System.err.println("Cannot set " + s + " more than once.");
                        return;
                    }
                    if (i + 1 == args.length) {
                        System.err.println("Missing value for " + s);
                        return;
                    }
                    options.put(s, args[++i]);
                } else if (flags.containsKey(s)) {
                    flags.put(s, true);
                } else {
                    System.err.println("Unrecognized option: " + s);
                }
                continue;
            }
            if (null!=fileName) {
                System.err.println("Sorry, can only open one file.");
                return;
            }
            fileName = s;
        }

        Main main;
        if (null==fileName) {
            System.out.println("Missing: a file name to open. Will start from an empty document.");
            main = Main.fromLines(new String[] {"[]"}, null);
        } else {
            main = Main.fromPathStr(fileName);
        }
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
        boolean debugMode = flags.get("--debug");
        if (debugMode) main.debugMode = new Main.DebugMode();
        boolean startedDebugReport = false;
        try {
            String theme = options.get("--theme");
            if (null==theme) {
                Theme.selected = Theme.getTheme("DARK");
            } else {
                Theme.selected = Theme.getTheme(theme);
            }
            while(true) {
                main.display();
                if (debugMode) {
                    main.getRoot().checkInvariants();
                }
                if (!main.actOnAllKeys()) break;
            }
        } catch (Exception e) {
            main.closeScreen();
            if (debugMode) {
                startedDebugReport = true;
                System.out.println("[BEGIN DEBUG REPORT]------------------------------------------------");
            }
            e.printStackTrace();
        } finally {
            main.closeScreen();
            if (debugMode) {
                if (!startedDebugReport) {
                    System.out.println("[BEGIN DEBUG REPORT]------------------------------------------------");
                }
                System.out.println("Command line: " + (Arrays.stream(args).map(s->"\""+s+"\"").collect(Collectors.joining(" "))));
                System.out.println("keys: " + (main.debugMode.keys.stream().map(KeyStroke::toString).collect(Collectors.joining(", "))));
                System.out.println("--------------------------------------------------[END DEBUG REPORT]");
            }
        }

    }

    public void checkInvariants() throws InvariantException {
        myJson.rootInfo.checkInvariants(myJson);
        int openMenus = 0;
        if (mainMenu!=null) openMenus++;
        if (sortControl!=null) openMenus++;
        if (findControl!=null) openMenus++;
        if (actionMenu!=null) openMenus++;
        if (openMenus>1) throw new InvariantException("Should have at most 1 open menu, have " + openMenus);
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