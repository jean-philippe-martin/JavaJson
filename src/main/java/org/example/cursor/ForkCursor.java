package org.example.cursor;

import org.example.Cursor;
import org.example.JsonNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;

public class ForkCursor implements MultiCursor {
    // The fork is where we go to all children. This is one way to get a multicursor.
    protected @NotNull Cursor fork;

    public ForkCursor(@NotNull Cursor fork) {
        this.fork = fork;
    }

    @Override
    public boolean selects(Cursor primary, @NotNull Cursor underTest) {
        return primary.selects(underTest.getData(), fork);
    }

    @Override
    public void addAllNodes(Cursor primaryCur, @NotNull List<JsonNode> list) {
        JsonNode primary = primaryCur.getData();
        // there may be secondary cursors
        List<Cursor.DescentStep> atFork = fork.asListOfSteps();
        List<Cursor.DescentStep> atCursor = primaryCur.asListOfSteps();
        // ex: fork = ."foo" [1]
        //     cursor = ."foo" [1] [2] ."blah"
        //     so we visit all the children .foo[1][*].blah
        JsonNode forkJson = fork.getData();
        JsonNode child = forkJson.firstChild();
        while (child!=null) {
            JsonNode cur = child;
            try {
                for (int i = atFork.size() + 1; cur != null && i < atCursor.size(); i++) {
                    cur = atCursor.get(i).apply(cur);
                }
            } catch (NoSuchElementException nope) {
                cur = null;
            } catch (ArrayIndexOutOfBoundsException nope2) {
                cur = null;
            }
            // Some children may not have the whole path apply to them; skip those.
            if (null!=cur && cur!=primary) list.add(cur);
            child = forkJson.nextChild(child.asCursor());
        }
    }

    @Override
    public @Nullable Cursor nextCursor(Cursor primaryCur) {
        // Run a variant of "addAllNodes" that tries to speedrun to the primary.
        JsonNode primary = primaryCur.getData();
        // there may be secondary cursors
        List<Cursor.DescentStep> atFork = fork.asListOfSteps();
        List<Cursor.DescentStep> atCursor = primaryCur.asListOfSteps();
        // ex: fork = ."foo" [1]
        //     cursor = ."foo" [1] [2] ."blah"
        //     so we visit all the children .foo[1][*].blah
        JsonNode forkJson = fork.getData();
        // Primary cursor is above the fork point, so there's no secondary cursor visible.
        if (atCursor.size() <= atFork.size()) return null;
        // The child of fork in the direction of the primary.
        JsonNode forkChild = atCursor.get(atFork.size()).apply(forkJson);
        JsonNode cur;
        while (null!=forkChild) {
            forkChild = forkChild.nextSibling();
            if (null==forkChild) {
                // last child, we're done.
                return null;
            }
            cur = forkChild;
            try {
                for (int i = atFork.size() + 1; cur != null && i < atCursor.size(); i++) {
                    cur = atCursor.get(i).apply(cur);
                }
                if (null != cur) {
                    return cur.asCursor();
                }
            } catch (NoSuchElementException | ArrayIndexOutOfBoundsException nope) {
                // go to next sibling
                continue;
            }
        }
        return null;
    }

    @Override
    public @Nullable Cursor prevCursor(Cursor primaryCur) {
        // Run a variant of "addAllNodes" that tries to speedrun to the primary.
        JsonNode primary = primaryCur.getData();
        // there may be secondary cursors
        List<Cursor.DescentStep> atFork = fork.asListOfSteps();
        List<Cursor.DescentStep> atCursor = primaryCur.asListOfSteps();
        // ex: fork = ."foo" [1]
        //     cursor = ."foo" [1] [2] ."blah"
        //     so we visit all the children .foo[1][*].blah
        JsonNode forkJson = fork.getData();
        // Primary cursor is above the fork point, so there's no secondary cursor visible.
        if (atCursor.size() <= atFork.size()) return null;
        // The child of fork in the direction of the primary.
        JsonNode forkChild = atCursor.get(atFork.size()).apply(forkJson);
        JsonNode cur;
        while (null!=forkChild) {
            forkChild = forkChild.prevSibling();
            if (null==forkChild) {
                // last child, we're done.
                return null;
            }
            cur = forkChild;
            try {
                for (int i = atFork.size() + 1; cur != null && i < atCursor.size(); i++) {
                    cur = atCursor.get(i).apply(cur);
                }
                if (null != cur) {
                    return cur.asCursor();
                }
            } catch (NoSuchElementException | ArrayIndexOutOfBoundsException nope) {
                // go to another sibling
                continue;
            }
        }
        return null;
    }

    public @NotNull Cursor getFork() {
        return fork;
    }
}