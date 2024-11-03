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
    // This is the JSON object we want to display
    protected boolean folded = false;
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
    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    // Override this.
    /** Returns the next child after this one. If there's none, return null. **/
    public abstract JsonState nextChild(Cursor pointingToAChild);

    public abstract JsonState firstChild();

    public JsonState nextSibling() {
        return getParent().nextChild(this.whereIAm);
    }

    public JsonState getParent() {
        return this.parent;
    }

    public void cursorDown() {
        //root.userCursor = root.userCursor.down();
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
            JsonState aunt = dad.nextSibling();
            if (aunt!=null) {
                root.userCursor = aunt.whereIAm;
                return;
            }
            JsonState newDad = dad.getParent();
            // If parent()==this that means we're at the root
            if (null==newDad || dad==newDad) {
                root.userCursor = dad.whereIAm;
                return;
            }
            dad = newDad;
        }
    };

    public void cursorUp() {};

}
