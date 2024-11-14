package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


// JsonNode represents the metadata we have about a specific subtree of
// the input JSON. And also it holds the actual data, because why not.
// So at any level in the tree you'll get a different JsonState object,
// and querying it gives info local to that subtree.
// *except* some operations are global, such as moving the cursor.
public abstract class JsonNode {
    // the actual data is held by a subclass.

    // Information that applies to the whole JSON tree rather than
    // just one node.
    public class RootInfo {
        protected @NotNull Cursor userCursor;
        protected @NotNull JsonNode root;
        // The fork is where we go to all children
        protected @Nullable Cursor fork;

        public RootInfo(@NotNull JsonNode root) {
            this.userCursor = new Cursor();
            this.root = root;
            this.userCursor.setData(root);
        }

        public void setCursor(@NotNull Cursor c) {
            this.userCursor = c;
        }

        // Null indicates no fork
        public void setFork(@Nullable Cursor fork) {
            this.fork = fork;
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
        String lines = String.join("\n", Files.readAllLines(path));
        return JsonNode.parseJson(lines);
    }

    public static JsonNode parseJson(String jsonLines) throws JsonProcessingException {
        // Parse it
        ObjectMapper parser = new ObjectMapper();
        // Todo: not assume the object's a map
        Object parsed = parser.readValue(jsonLines, LinkedHashMap.class);
        LinkedHashMap<String, Object> kv = (LinkedHashMap<String, Object>)parsed;

        return JsonNode.fromObject(kv, null, new Cursor(), null);
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
        return rootInfo.userCursor.selects(this, rootInfo.fork);
    }

    public boolean isAtPrimaryCursor() {
        return rootInfo.userCursor.selects(this, null);
    }


    /** True if the userCursor is pointing to this key of ours. **/
    public boolean isAtCursor(String key) {
        // JsonStateMap overrides this
        return false;
    }

    // An alternate way to represent the value, e.g. different unit
    public String getAnnotation() { return this.annotation; }
    public void setAnnotation(String a) {
        this.annotation = a;
    }

    /** Whether this JSON object is folded. **/
    public boolean getFolded() {
        return folded;
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
    public boolean setFoldedAtCursor(boolean folded) {
        Cursor where = rootInfo.userCursor;
        JsonNode place = where.getData();
        if (null==place) return false;
        boolean changed = place.folded != folded;
        place.folded = folded;
        boolean b = place instanceof JsonNodeValue<?>;
        return (changed && !b);
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

    public void cursorDown() {
        JsonNode current = rootInfo.userCursor.getData();
        // If there's a child, go there.
        JsonNode kid = current.firstChild();
        if (null!=kid) {
            rootInfo.setCursor(kid.whereIAm);
            return;
        }
        // No child? Go to next sibling.
        JsonNode sibling = current.nextSibling();
        if (null!=sibling) {
            rootInfo.setCursor(sibling.whereIAm);
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
            if (aunt!=null) {
                rootInfo.setCursor(aunt.whereIAm);
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
        if (null==sibling) {
            // no prev sibling, go to parent
            JsonNode parent = current.getParent();
            if (null!=parent) {
                rootInfo.setCursor(parent.whereIAm);
            }
            return;
        }
        // we moved to the prev sibling, but we must also now go deep.
        while (true) {
            JsonNode next = sibling.lastChild();
            if (next==null) break;
            sibling = next;
        }
        rootInfo.setCursor(sibling.whereIAm);
        return;
    };

    public boolean cursorDownToAllChildren() {
        Cursor cursor = rootInfo.userCursor;
        JsonNode current = cursor.getData();
        // If there's a child, go there.
        JsonNode kid = current.firstChild();
        if (null!=kid) {
            rootInfo.setCursor(kid.whereIAm);
            rootInfo.setFork(cursor);
            return true;
        }
        // no child, nothing to do
        return false;
    }

    /** Move the cursor to the parent. **/
    public void cursorParent() {
        JsonNode parent = rootInfo.userCursor.getData().getParent();
        if (null==parent) return;
        rootInfo.setCursor(parent.whereIAm);
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
        Cursor fork = rootInfo.fork;
        if (null==fork) return ret;
        // there may be secondary cursors
        List<Cursor.DescentStep> atFork = fork.asListOfSteps();
        List<Cursor.DescentStep> atCursor = rootInfo.userCursor.asListOfSteps();
        // ex: fork = ."foo" [1]
        //     cursor = ."foo" [1] [2] ."blah"
        //     so we visit all the children .foo[1][*].blah
        JsonNode forkJson = fork.getData();
        JsonNode child = forkJson.firstChild();
        while (child!=null) {
            JsonNode cur = child;
            for (int i = atFork.size() + 1; i < atCursor.size(); i++) {
                cur = atCursor.get(i).apply(cur);
            }
            if (cur!=primary) ret.add(cur);
            child = forkJson.nextChild(child.whereIAm);
        }
        return ret;
    }


}
