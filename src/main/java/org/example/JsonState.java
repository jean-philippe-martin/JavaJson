package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


// JsonState represents the metadata we have about a specific subtree of
// the input JSON. And also it holds the actual data, because why not.
// So at any level in the tree you'll get a different JsonState object,
// and querying it gives info local to that subtree.
// *except* some operations are global, such as moving the cursor.
public abstract class JsonState {
    // the actual data is held by a subclass.

    protected boolean folded = false;
    protected boolean pinned = false;
    protected String annotation = "";
    // A Cursor representing our ("this") position in the JSON tree.
    protected final Cursor whereIAm;
    // Where we should highlight, the user-controlled cursor.
    // This one is only set in the root.
    protected Cursor userCursor;
    // A pointer to the root JSON node.
    protected final JsonState root;
    // A pointer to the parent JSON node. Or null if we're the root.
    protected final JsonState parent;
    // The number of pinned entries in my children.
    protected int pinnedUnderMe;

    public static JsonState parse(Path path) throws IOException {
        // Read the file
        String lines = String.join("\n", Files.readAllLines(path));
        return JsonState.parseJson(lines);
    }

    public static JsonState parseJson(String jsonLines) throws JsonProcessingException {
        // Parse it
        ObjectMapper parser = new ObjectMapper();
        // Todo: not assume the object's a map
        Object parsed = parser.readValue(jsonLines, LinkedHashMap.class);
        LinkedHashMap<String, Object> kv = (LinkedHashMap<String, Object>)parsed;

        return JsonState.fromObject(kv, null, new Cursor(), null);
    }

    /** Create a JsonState object to wrap the given JSON object. **/
    protected static JsonState fromObject(Object json, JsonState parent, Cursor toMe, JsonState root) {
        if (json instanceof LinkedHashMap) {
            LinkedHashMap<String, Object> kv = (LinkedHashMap<String, Object>) json;
            return new JsonStateMap(kv, parent, toMe, root);
        }
        return switch (json) {
            case String s -> new JsonStateValue<String>(s, parent, toMe, root);
            case Integer s -> new JsonStateValue<Integer>(s, parent, toMe, root);
            case List l -> new JsonStateList(l, parent, toMe, root);
            default -> throw new RuntimeException("Unknown type for json object " + json.getClass());
        };
    }

    protected JsonState(JsonState parent, Cursor curToMe, JsonState root) {
        curToMe.setData(this);
        this.whereIAm = curToMe;
        if (null==root) root=this;
        this.root = root;
        if (this.root==this) {
            // we're the root, so we need a user cursor
            this.userCursor = new Cursor();
            this.userCursor.setData(this);
        } else {
            // use root.userCursor instead.
            this.userCursor = null;
        }
        this.parent = parent;
    }

    public boolean isAtCursor() {
        return whereIAm.toString().equals(root.userCursor.toString());
    }


    /** True if the userCursor is pointing to this key of ours. **/
    public boolean isAtCursor(String key) {
        Cursor hypothetical = whereIAm.enterKey(key);
        return hypothetical.toString().equals(root.userCursor.toString());
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
        Cursor where = root.userCursor;
        JsonState place = where.getData();
        if (null==place) return false;
        return place.getFolded();
    }

    /**
     * Sets the "folded" state at the cursor position.
     * @return whether "folded" was changed.
     */
    public boolean setFoldedAtCursor(boolean folded) {
        Cursor where = root.userCursor;
        JsonState place = where.getData();
        if (null==place) return false;
        boolean changed = place.folded != folded;
        place.folded = folded;
        boolean b = place instanceof JsonStateValue<?>;
        return (changed && !b);
    }

    public boolean getPinned() {
        return this.pinned;
    }

    /**
     * @return true if any of my descendants are pinned.
     */
    public boolean hasPins() {
        return this.pinnedUnderMe>0;
    }

    /**
     */
    public boolean togglePinnedAtCursor() {
        Cursor where = root.userCursor;
        JsonState place = where.getData();
        if (null==place) return false;
        place.pinned = !place.pinned;
        // update the pin count all the way to the root.
        int delta = -1;
        if (place.pinned) delta = 1;
        JsonState cur = place;
        while (cur != null) {
            cur.pinnedUnderMe += delta;
            JsonState dad = cur.getParent();
            if (dad==cur) break;
            cur = dad;
        }
        return true;
    }

    // Override this.
    /** Returns the next child after this one. If there's none, return null. **/
    public abstract JsonState nextChild(Cursor pointingToAChild);

    public abstract JsonState prevChild(Cursor pointingToAChild);

    public abstract JsonState firstChild();

    public abstract JsonState lastChild();

    public JsonState nextSibling() {
        JsonState dad = getParent();
        if (null==dad) return null;
        return dad.nextChild(this.whereIAm);
    }

    public JsonState prevSibling() {
        JsonState dad = getParent();
        if (null==dad) return null;
        return dad.prevChild(this.whereIAm);
    }

    public JsonState getParent() {
        return this.parent;
    }

    public void cursorDown() {
        JsonState current = root.userCursor.getData();
        // If there's a child, go there.
        JsonState kid = current.firstChild();
        if (null!=kid) {
            root.userCursor = kid.whereIAm;
            return;
        }
        // No child? Go to next sibling.
        JsonState sibling = current.nextSibling();
        if (null!=sibling) {
            root.userCursor = sibling.whereIAm;
            return;
        }
        // No sibling? Go to dad's next sibling.
        JsonState dad = current.getParent();
        while (true) {
            if (dad==null) {
                // root. Don't move the cursor.
                return;
            }

            JsonState aunt = dad.nextSibling();
            if (aunt!=null) {
                root.userCursor = aunt.whereIAm;
                return;
            }
            JsonState newDad = dad.getParent();
            // If parent()==this that means we're at the root
            if (dad==newDad) {
                return;
            }
            dad = newDad;
        }
    };



    public void cursorUp() {
        JsonState current = root.userCursor.getData();
        // Go to prev sibling.
        JsonState sibling = current.prevSibling();
        if (null==sibling) {
            // no prev sibling, go to parent
            JsonState parent = current.getParent();
            if (null!=parent) {
                root.userCursor = parent.whereIAm;
            }
            return;
        }
        // we moved to the prev sibling, but we must also now go deep.
        while (true) {
            JsonState next = sibling.lastChild();
            if (next==null) break;
            sibling = next;
        }
        root.userCursor = sibling.whereIAm;
        return;
    };

    /** Move the cursor to the parent. **/
    public void cursorParent() {
        JsonState current = root.userCursor.getData();
        if (null==current) return;
        JsonState parent = current.getParent();
        if (null==parent) return;
        root.userCursor = parent.whereIAm;
    }

}
