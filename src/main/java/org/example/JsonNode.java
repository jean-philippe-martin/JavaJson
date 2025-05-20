package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.cursor.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


// JsonNode represents the metadata we have about a specific subtree of
// the input JSON. And also it holds the actual data, because why not.
// So at any level in the tree you'll get a different JsonState object,
// and querying it gives info local to that subtree.
// *except* some operations are global, such as moving the cursor.
public abstract class JsonNode {
    // the actual data is held by a subclass.

    public static class Builder implements JsonNodeBuilder {
        private final JsonNode node;
        private final Object value;

        public Builder(@NotNull JsonNode node) {
            this.node = node;
            this.value = null;
        }

        private Builder(@NotNull Object obj, boolean _ignored) {
            this.node = null;
            this.value = obj;
        }

        public static Builder fromNode(@NotNull JsonNode node) {
            return new Builder(node);
        }

        public static Builder fromObject(Object value) {
            return new Builder(value, true);
        }

        @Override
        public JsonNode build(JsonNode parent, Cursor curToMe) {
            if (null!=node) {
                node.reparent(parent, curToMe);
                return node;
            }
            JsonNode root = null;
            if (parent != null) {
                root = parent.getRoot();
            }
            return JsonNode.fromObject(value, parent, curToMe, root);
        }
    }

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

        // Makes sure the userCursor and its ancestors all point to nodes
        // that are actually part of the current tree.
        public void fixCursors() {
            JsonNode node = this.root;
            List<DescentStep> steps = userCursor.asListOfSteps();
            try {
                for (DescentStep step: steps) {
                    node = step.apply(node);
                }
            } catch (Exception x) {
            }
            userCursor = node.asCursor();
        }

        public void checkInvariants(JsonNode expectedRoot) throws InvariantException {
            if (root!=expectedRoot) throw new InvariantException("root.rootInfo.root != root.");
            HashSet<JsonNode> nodesInTree = new HashSet<>();
            addAndAllChildren(root, nodesInTree);
            checkAllCursors(root, null, root, 0);
        }

        private void addAndAllChildren(@NotNull JsonNode me, @NotNull Set<JsonNode> addHere) throws InvariantException {
            addHere.add(me);
            int pinnedCount = 0;
            var it = me.iterateChildren(true);
            while (it!=null) {
                JsonNode kid = it.get();
                addAndAllChildren(kid, addHere);
                pinnedCount += kid.pinnedUnderMe;
                it = it.next();
            }
            if (me.pinned) pinnedCount += 1;
            if (me.pinnedUnderMe != pinnedCount) {
                throw new InvariantException("Pin count mismatch for " + me.asCursor().toString() + ": " + pinnedUnderMe + " but sum of kids is " + pinnedCount);
            }
        }

        private void checkAllCursors(@NotNull JsonNode root, @Nullable JsonNode parent, @NotNull JsonNode me, int depth) throws InvariantException {
            // Everyone agrees on root & rootInfo
            List<String> brokenInvariants = new ArrayList<>();
            if (me.asCursor().asListOfSteps().size() != depth) {
                brokenInvariants.add("Cursor depth is " + me.asCursor().asListOfSteps().size() + ", should be " + depth);
            }
            if (me.root != root) {
                brokenInvariants.add("Root is incorrect for node " + me.asCursor().toString());
            }
            if (root.rootInfo != me.rootInfo) {
                brokenInvariants.add("Two distinct rootInfos: " + me.asCursor().toString() + "'s ("
                        + me.rootInfo.root.getClass()
                        + ") does not match root's (" + root.getClass()+")");
            }
            // Cursor data and parent are correct.
            try {
                Cursor cur = me.asCursor();
                if (parent!=null && parent!=me.parent) {
                    brokenInvariants.add("Node " + cur.toString() + " has wrong parent.");
                }
                me.checkInvariants();
            } catch (InvariantException ie) {
                if (!brokenInvariants.isEmpty()) {
                    throw new InvariantException(brokenInvariants.stream().collect(Collectors.joining("\nAND ")), ie);
                }
                throw ie;
            }
            if (!brokenInvariants.isEmpty()) {
                throw new InvariantException(brokenInvariants.stream().collect(Collectors.joining("\nAND ")));
            }
            // Recurse.
            var it = me.iterateChildren(true);
            while (it!=null) {
                checkAllCursors(root, me, it.get(), depth+1);
                it = it.next();
            }
        }
    }

    protected boolean folded = false;
    protected boolean pinned = false;
    protected @NotNull String annotation = "";
    // A Cursor representing our ("this") position in the JSON tree.
    protected Cursor whereIAm;
    protected @NotNull RootInfo rootInfo;
    // A pointer to the root JSON node.
    protected JsonNode root;
    // A pointer to the parent JSON node. Or null if we're the root.
    protected JsonNode parent;
    // The number of pinned entries in my children.
    protected int pinnedUnderMe;
    // This is meant for a list to be able to hold a JSON-like header
    // that has, say, the number of distinct values for each field.
    // Or the fraction of entries that have a given field.
    protected @Nullable String aggregateComment;
    // synthetic field. Should be true if it's someone's "aggregate" member
    // or its descendant.
    protected boolean isAggregate;
    protected @Nullable JsonNode aggregate;

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
        if (all.size()==1) {
            // special case: a single line. Let's not say this is JSONL.
            return JsonNode.fromObject(all.get(0), null, new Cursor(), null);
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

    public static JsonNode parseJsonIgnoreEscapes(String jsonLines) throws JsonProcessingException {
        // Remove escapes
        jsonLines = Pattern.compile("\\\\").matcher(jsonLines).replaceAll("\\\\\\\\");
        // Parse
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
        if (json instanceof List) {
            return new JsonNodeList((List)json, parent, toMe, root);
        }
        if (json instanceof String) {
            JsonNodeValue ret = new JsonNodeValue<String>((String)json, parent, toMe, root);
            ret.folded = true;
            return ret;
        }
        return new JsonNodeValue(json, parent, toMe, root);
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
        if (rootInfo.secondaryCursors instanceof ForkCursor) {
            return ((ForkCursor)rootInfo.secondaryCursors).getFork().getData() == this;
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
        JsonNode cur = getParent();
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
            if (place.folded != folded &&
            // cannot unfold ints, but we *can* unfold strings.
                    ((!(place instanceof JsonNodeValue))
                            || (place instanceof JsonNodeValue) && (place.getValue() instanceof String))) {
                changed=true;
            }
            place.folded = folded;
        }
        for (JsonNode sibling : atAnyCursor()) {
            sibling.folded = folded;
        }
        return changed;
    }

    /**
     * 1 = I'm unfolded, my children are folded.
     * 2 = Me and my children are unfolded. Their children are folded.
     **/
    public void setFoldedLevels(int levelCount) {
        if (levelCount<1) {
            this.folded = true;
            return;
        }
        this.folded = false;
        var it = iterateChildren(true);
        while (it!=null) {
            JsonNode kid = it.get();
            kid.setFoldedLevels(levelCount-1);
            it = it.next();
        }
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

    public boolean isSynthetic() {
        return isAggregate;
    }

    /** Assign an aggregate to this node. */
    public void setAggregate(@Nullable JsonNode aggregate, @Nullable String aggregateComment) {
        if (null!=aggregate) {
            // Mark the aggregate and all its children
            markAsSynthetic(aggregate);
        }
        this.aggregate = aggregate;
        this.aggregateComment = aggregateComment;
    }

    // Mark this node and all descendants as "synthetic" (marked as a comment when drawn)
    private void markAsSynthetic(JsonNode aggregate) {
        aggregate.isAggregate = true;
        for (JsonNodeIterator it = aggregate.iterateChildren(true); it!=null; it = it.next()) {
            JsonNode kid = it.get();
            markAsSynthetic(kid);
        }
    }

    // Override this.
    /** Returns the next child after this one. If there's none, return null. **/
    public abstract JsonNode nextChild(Cursor pointingToAChild);

    public abstract JsonNode prevChild(Cursor pointingToAChild);

    public abstract JsonNode firstChild();

    public abstract JsonNode lastChild();

    public abstract @Nullable JsonNodeIterator iterateChildren(boolean includeAggregates);

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

    /**
     * @return true if this node is root.
     */
    public boolean isRoot() {
        if (null==parent) return true;
        if (parent==this) return true;
        return false;
    }

    /**
     * change the parent node.
     * To make it root, pass null for the parent and a new cursor.
     */
    public void reparent(JsonNode newParent, Cursor cursorToMe) {
        if (null==newParent) {
            this.rootInfo = new RootInfo(this);
            this.root = this.rootInfo.root;
            this.parent = null;
        } else {
            // we have a parent.
            this.rootInfo = newParent.rootInfo;
            this.root = this.rootInfo.root;
            this.parent = newParent;
        }
        this.whereIAm = cursorToMe;
        this.whereIAm.setData(this);
        if (null!=aggregate) {
            DescentStep lastStep = aggregate.whereIAm.getStep();
            if (lastStep instanceof DescentKey) {
                String key = ((DescentKey) lastStep).get();
                aggregate.reparent(this, cursorToMe.enterKey(key));
            } else {
                int index = ((DescentIndex) lastStep).get();
                aggregate.reparent(this, cursorToMe.enterIndex(index));
            }
        }
    }

    /** Sort. But also save the sort order. **/
    public abstract void sort(Sorter sorter);

    /** Return to the original order. **/
    public abstract void unsort();

    /** Return the current sort rules. Null = original sort order. */
    public abstract @Nullable Sorter getSort();


    /**
     * Replace this child with another.
     * The Cursor must point to a direct child of ours.
     * Returns the constructed child.
     **/
    public abstract @NotNull JsonNode replaceChild(Cursor toChild, JsonNodeBuilder newChildBuilder);

    /**
     *  examples: ".foo[1].bar", ".baz[*].bar", "[1][2].city"
     * Will throw if it can't find what you're asking for.
     *
     * Multi-cursor not yet supported.
     * */
    public void setCursors(@NotNull String whereToGo) {
        JsonNode node = rootInfo.root;
        while (!whereToGo.isEmpty()) {
            if (whereToGo.startsWith(".")) {
                int dot = whereToGo.indexOf('.', 1);
                int bracket = whereToGo.indexOf('[');
                int end = whereToGo.length();
                int choice = dot;
                if (choice < 0 || (bracket >= 0 && bracket < choice)) choice = bracket;
                if (choice < 0 || (end >= 0 && end < choice)) choice = end;
                String token = whereToGo.substring(1, choice);
                node = ((JsonNodeMap)node).getChild(token);
                whereToGo = whereToGo.substring(choice);
            } else if (whereToGo.startsWith("[")) {
                int end = whereToGo.length();
                int bracket = whereToGo.indexOf(']');
                int choice = end;
                if (bracket>=0 && bracket<end) {
                    choice = bracket;
                }
                String token = whereToGo.substring(1, choice);
                Integer index = Integer.parseInt(token);
                node = ((JsonNodeList)node).get(index);
                whereToGo = whereToGo.substring(choice+1);
            } else {
                throw new RuntimeException("Don't know how to go to " + whereToGo);
            }
        }
        rootInfo.setPrimaryCursor(node.whereIAm);
    }

    public void checkInvariants() throws InvariantException {
        Cursor cur = this.asCursor();
        if (cur.getData() != this) throw new InvariantException("Cursor doesn't point back to node");
        Cursor curParent = cur.getParent();
        if (curParent != null && curParent.getData() != this.parent)
            throw new InvariantException("Cursor parent is wrong.");
    }
}
