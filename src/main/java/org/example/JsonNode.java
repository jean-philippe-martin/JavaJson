package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.cursor.ForkCursor;
import org.example.cursor.MultiCursor;
import org.example.cursor.NoMultiCursor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


// JsonNode represents the metadata we have about a specific subtree of
// the input JSON. And also it holds the actual data, because why not.
// So at any level in the tree you'll get a different JsonState object,
// and querying it gives info local to that subtree.
// *except* some operations are global, such as moving the cursor.
public abstract class JsonNode {
    // the actual data is held by a subclass.

    // This holds saved cursors so we can stash them.
    public class SavedCursors {
        public Cursor primaryCursor;
        public MultiCursor secondaryCursors;

        public SavedCursors(Cursor primary, MultiCursor secondaries) {
            primaryCursor = primary;
            secondaryCursors = secondaries;
        }
    }

    // Information that applies to the whole JSON tree rather than
    // just one node.
    public class RootInfo {
        protected @NotNull Cursor userCursor;
        protected @NotNull JsonNode root;
        protected @NotNull MultiCursor secondaryCursors;

        public RootInfo(@NotNull JsonNode root) {
            this.root = root;
            this.userCursor = root.whereIAm;
            this.secondaryCursors = new NoMultiCursor();
        }

        public void setPrimaryCursor(@NotNull Cursor c) {
            this.userCursor = c;
        }

        // Null indicates no fork
        public void setFork(@Nullable Cursor fork) {
            if (null==fork) {
                this.secondaryCursors = new NoMultiCursor();
            } else {
                this.secondaryCursors = new ForkCursor(fork);
            }
        }

        public void setSecondaryCursors(@NotNull MultiCursor mc) {
            this.secondaryCursors = mc;
        }

        public SavedCursors save() {
            return new SavedCursors(userCursor, secondaryCursors);
        }

        public void restore(SavedCursors save) {
            this.userCursor = save.primaryCursor;
            this.secondaryCursors = save.secondaryCursors;
        }
    }

    protected boolean folded = false;
    protected boolean pinned = false;
    protected @NotNull String annotation = "";
    // A Cursor representing our ("this") position in the JSON tree.
    protected final Cursor whereIAm;
    protected final @NotNull RootInfo rootInfo;
    // A pointer to the root JSON node.
    protected final JsonNode root;
    // A pointer to the parent JSON node. Or null if we're the root.
    protected final JsonNode parent;
    // The number of pinned entries in my children.
    protected int pinnedUnderMe;

    public static JsonNode parse(Path path) throws IOException {
        // Read the file
        List<String> allLines = Files.readAllLines(path);
        return JsonNode.parseLines(allLines.toArray(String[]::new));
    }

    // Try to read as either JSON or JSONL.
    public static JsonNode parseLines(String[] lines) throws JsonProcessingException {

        // Is each line individually valid?
        List<Object> all = new ArrayList<>();
        int i=0;
        for (String l : lines) {
            i++;
            try {
                if (l.isEmpty()) continue;
                ObjectMapper parser = new ObjectMapper();
                Object parsed = parser.readValue(l, Object.class);
                all.add(parsed);
            } catch (JsonProcessingException jpx) {
                // Try the thing as a whole
                String linesTogether = String.join("\n", lines);
                return JsonNode.parseJson(linesTogether);
            }
        }
        JsonNode ret = JsonNode.fromObject(all, null, new Cursor(), null);
        ret.setAnnotation("JSONL");
        return ret;
    }

    public static JsonNode parseJson(String jsonLines) throws JsonProcessingException {
        // Parse it
        ObjectMapper parser = new ObjectMapper();
        Object parsed = parser.readValue(jsonLines, Object.class);
        return JsonNode.fromObject(parsed, null, new Cursor(), null);
    }

    /** Create a JsonState object to wrap the given JSON object. **/
    protected static JsonNode fromObject(Object json, JsonNode parent, Cursor toMe, JsonNode root) {
        if (json instanceof LinkedHashMap) {
            LinkedHashMap<String, Object> kv = (LinkedHashMap<String, Object>) json;
            return new JsonNodeMap(kv, parent, toMe, root);
        }
        return switch (json) {
            case String s -> new JsonNodeValue<String>(s, parent, toMe, root);
            case Integer s -> new JsonNodeValue<Integer>(s, parent, toMe, root);
            case Double s -> new JsonNodeValue<Double>(s, parent, toMe, root);
            case List l -> new JsonNodeList(l, parent, toMe, root);
            default -> throw new RuntimeException("Unknown type for json object " + json.getClass());
        };
    }

    protected JsonNode(JsonNode parent, Cursor curToMe, JsonNode root) {
        curToMe.setData(this);
        this.whereIAm = curToMe;
        if (null==root) root=this;
        this.root = root;
        if (this.root==this) {
            // we're the root, so we need a user cursor
            this.rootInfo = new RootInfo(this);
        } else {
            // use root.userCursor instead.
            this.rootInfo = root.rootInfo;
        }
        this.parent = parent;
    }

    // Any of the cursors
    public boolean isAtCursor() {
        return (rootInfo.userCursor.selects(this, null))
            || (rootInfo.secondaryCursors.selects(rootInfo.userCursor, whereIAm));
    }

    public boolean isAtPrimaryCursor() {
        return rootInfo.userCursor.selects(this, null);
    }

    // True if a secondary cursor is on this node
    public boolean isAtSecondaryCursor() {
        return rootInfo.secondaryCursors.selects(rootInfo.userCursor, whereIAm);
    }


    /** True if the userCursor is pointing to this key of ours. **/
    public boolean isAtCursor(String key) {
        // JsonNodeMap overrides this
        return false;
    }

    // true if this node is where the fork is.
    public boolean isAtFork() {
        if (rootInfo.secondaryCursors instanceof ForkCursor fc) {
            return fc.getFork().getData() == this;
        }
        return false;
    }

    public Cursor asCursor() {
        return whereIAm;
    }

    // An alternate way to represent the value, e.g. different unit
    public String getAnnotation() { return this.annotation; }
    public void setAnnotation(String a) {
        this.annotation = a;
    }

    /**
     * Whether the user explicitly folded this JSON object.
     * Note that it may still be shown folded because a parent is folded.
     **/
    public boolean getFolded() {
        return folded;
    }

    /**
     * Whether this node should be shown in the UI.
     */
    public boolean isVisible() {
        if (hasPins()) return true;
        if (rootInfo.root == this) return true;
        // if any of our ancestors is folded, then we're hidden since we have no pins.
        // If any of our ancestors is pinned, then we're visible because folds higher up
        // wouldn't have an effect.
        JsonNode cur = this.getParent();
        while (cur!=null) {
            if (cur.getPinned()) return true;
            if (cur.getFolded()) return false;
            cur = cur.getParent();
        }
        return true;
    }

    public boolean getFoldedAtCursor() {
        Cursor where = rootInfo.userCursor;
        JsonNode place = where.getData();
        if (null==place) return false;
        return place.getFolded();
    }

    /**
     * Sets the "folded" state at the cursor position.
     * @return whether "folded" was changed.
     */
    public boolean setFoldedAtCursors(boolean folded) {
        Cursor where = rootInfo.userCursor;
        JsonNode place = where.getData();
        boolean changed = false;
        if (null!=place) {
            if (place.folded != folded && !(place instanceof JsonNodeValue)) {
                changed=true;
            }
            place.folded = folded;
        }
        for (JsonNode sibling : atAnyCursor()) {
            sibling.folded = folded;
        }
        return changed;
    }

    public boolean getPinned() {
        return this.pinned;
    }

    public void setPinned(boolean newPinned) {
        if (this.pinned==newPinned) return;
        // set the new state
        this.pinned = newPinned;
        // update the counters at & above
        int delta = -1;
        if (this.pinned) delta = 1;
        JsonNode cur = this;
        while (cur != null) {
            cur.pinnedUnderMe += delta;
            JsonNode dad = cur.getParent();
            if (dad==cur) break;
            cur = dad;
        }
    }

    /**
     * @return true if any of my descendants are pinned.
     */
    public boolean hasPins() {
        return this.pinnedUnderMe>0;
    }

    public boolean getPinnedAtCursor() {
        JsonNode place = rootInfo.userCursor.getData();
        if (null==place) return false;
        return place.getPinned();
    }

    /**
     */
    public boolean setPinnedAtCursors(boolean newPinned) {
        JsonNode place = rootInfo.userCursor.getData();
        if (null==place) return false;
        for (JsonNode sibling : atAnyCursor()) {
            sibling.setPinned(newPinned);
        }
        return true;
    }

    // Override this.
    /** Returns the next child after this one. If there's none, return null. **/
    public abstract JsonNode nextChild(Cursor pointingToAChild);

    public abstract JsonNode prevChild(Cursor pointingToAChild);

    public abstract JsonNode firstChild();

    public abstract JsonNode lastChild();

    public JsonNode nextSibling() {
        JsonNode dad = getParent();
        if (null==dad) return null;
        return dad.nextChild(this.whereIAm);
    }

    public JsonNode prevSibling() {
        JsonNode dad = getParent();
        if (null==dad) return null;
        return dad.prevChild(this.whereIAm);
    }

    public JsonNode getParent() {
        return this.parent;
    }

    /**
     * @return the value stored in that node.
     */
    public abstract Object getValue();

    /**
     * Move the cursor more or less one line down on the screen.
     *
     * That means enter the object we're at, or go to the next sibling.
     */
    public void cursorDown() {
        JsonNode current = rootInfo.userCursor.getData();
        // If there's a child, go there.
        // unless we're folded with no pins.
        if (!current.getFolded() || current.hasPins()) {
            JsonNode kid = current.firstChild();
            // go down until we find a visible child
            while (kid!=null && !kid.isVisible()) {
                kid = current.nextChild(kid.whereIAm);
            }
            if (null != kid) {
                rootInfo.setPrimaryCursor(kid.whereIAm);
                return;
            }
        }
        // No child? Go to next sibling.
        JsonNode sibling = current.nextSibling();
        while (null!=sibling && !sibling.isVisible()) {
            sibling = sibling.nextSibling();
        }
        if (null!=sibling) {
            rootInfo.setPrimaryCursor(sibling.whereIAm);
            return;
        }
        // No sibling? Go to dad's next sibling.
        JsonNode dad = current.getParent();
        while (true) {
            if (dad==null) {
                // root. Don't move the cursor.
                return;
            }

            JsonNode aunt = dad.nextSibling();
            while (null!=aunt && !aunt.isVisible()) {
                aunt = aunt.nextSibling();
            }
            if (aunt!=null) {
                rootInfo.setPrimaryCursor(aunt.whereIAm);
                return;
            }
            JsonNode newDad = dad.getParent();
            // If parent()==this that means we're at the root
            if (dad==newDad) {
                return;
            }
            dad = newDad;
        }
    };

    public void cursorUp() {
        JsonNode current = rootInfo.userCursor.getData();
        // Go to prev sibling.
        JsonNode sibling = current.prevSibling();
        while (null!=sibling && !sibling.isVisible()) {
            sibling = sibling.prevSibling();
        }
        if (null==sibling) {
            // no prev sibling, go to parent
            JsonNode parent = current.getParent();
            if (null!=parent) {
                rootInfo.setPrimaryCursor(parent.whereIAm);
            }
            return;
        }
        // we moved to the prev sibling, but we must also now go deep.
        while (true) {
            JsonNode next = sibling.lastChild();
            while (next!=null && !next.isVisible()) {
                next = next.prevSibling();
            }
            if (next==null) break;
            sibling = next;
        }
        rootInfo.setPrimaryCursor(sibling.whereIAm);
        return;
    };

    public boolean cursorDownToAllChildren() {
        Cursor cursor = rootInfo.userCursor;
        JsonNode current = cursor.getData();
        // If there's a child, go there.
        JsonNode kid = current.firstChild();
        if (null!=kid) {
            rootInfo.setFork(cursor);
            rootInfo.setPrimaryCursor(kid.whereIAm);
            return true;
        }
        // no child, nothing to do
        return false;
    }

    /** Move the cursor to the parent. **/
    public void cursorParent() {
        JsonNode parent = rootInfo.userCursor.getData().getParent();
        if (null==parent) return;
        rootInfo.setPrimaryCursor(parent.whereIAm);
    }

    public void cursorNextSibling() {
        JsonNode start = rootInfo.userCursor.getData();
        JsonNode sibling = start.nextSibling();
        while (sibling!=null && !sibling.isVisible()) {
            sibling = sibling.nextSibling();
        }
        if (null!=sibling && sibling.isVisible()) {
            rootInfo.setPrimaryCursor(sibling.whereIAm);
            return;
        }
        JsonNode dad = start.getParent();
        while (null!=dad) {
            sibling = dad.nextChild(start.whereIAm);
            while (sibling!=null && !sibling.isVisible()) {
                sibling = sibling.nextSibling();
            }
            if (null != sibling && sibling.isVisible()) {
                rootInfo.setPrimaryCursor(sibling.whereIAm);
                return;
            }
            dad = dad.getParent();
        }
    }

    public void cursorPrevSibling() {
        JsonNode start = rootInfo.userCursor.getData();
        JsonNode sibling = start.prevSibling();
        while (sibling!=null && !sibling.isVisible()) {
            sibling = sibling.prevSibling();
        }
        if (null!=sibling && sibling.isVisible()) {
            rootInfo.setPrimaryCursor(sibling.whereIAm);
            return;
        }
        JsonNode dad = start.getParent();
        while (null!=dad) {
            sibling = dad.prevChild(start.whereIAm);
            while (sibling!=null && !sibling.isVisible()) {
                sibling = sibling.prevSibling();
            }
            if (null != sibling && sibling.isVisible()) {
                rootInfo.setPrimaryCursor(sibling.whereIAm);
                return;
            }
            dad = dad.getParent();
        }
    }

    /** Move the primary cursor to the next secondary cursor. **/
    public void cursorNextCursor() {
        Cursor next = this.rootInfo.secondaryCursors.nextCursor(this.rootInfo.userCursor);
        if (null!=next) {
            this.rootInfo.setPrimaryCursor(next);
        }
    }

    /** Move the primary cursor to the previous secondary cursor. **/
    public void cursorPrevCursor() {
        Cursor next = this.rootInfo.secondaryCursors.prevCursor(this.rootInfo.userCursor);
        if (null!=next) {
            this.rootInfo.setPrimaryCursor(next);
        }
    }

    /**
     * @return the Json node that the user primary cursor is pointing to.
     */
    public JsonNode atCursor() {
        return this.rootInfo.userCursor.getData();
    }

    /**
     * @return the Json nodes that the user primary or secondary cursor is pointing to
     *         (primary is returned first).
     */
    public List<JsonNode> atAnyCursor() {
        ArrayList<JsonNode> ret = new ArrayList<>();
        JsonNode primary = rootInfo.userCursor.getData();
        ret.add(primary);
        rootInfo.secondaryCursors.addAllNodes(rootInfo.userCursor, ret);
        return ret;
    }

    /**
     * @return The root node.
     */
    public @NotNull JsonNode getRoot() {
        return rootInfo.root;
    }

    /** Sort. But also save the sort order. **/
    public abstract void sort(Sorter sorter);

    /** Return to the original order. **/
    public abstract void unsort();

    /** Return the current sort rules. Null = original sort order. */
    public abstract @Nullable Sorter getSort();

}
