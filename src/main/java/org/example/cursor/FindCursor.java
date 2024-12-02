package org.example.cursor;

import org.example.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A multicursor that highlights all the places that match a search string.
 */
public class FindCursor implements MultiCursor {
    protected @NotNull String pattern;
    // false: only match if the whole string equals the pattern
    // (otherwise, do substring)
    protected boolean substring;

    /**
     * Search for exact matches of "text".
     */
    public FindCursor(@NotNull String text) {
        this.pattern = text;
        this.substring = false;
    }

    /**
     * Search for substring matches of "substring" (that is, anything that contains "substring")
     */
    public FindCursor(@NotNull String substring, boolean allowSubstring) {
        this.pattern = substring;
        this.substring = allowSubstring;
    }

    private boolean matches(@Nullable JsonNode node) {
        if (null==node) return false;
        // check the key that holds this guy
        if (node.asCursor().getStep() instanceof Cursor.DescentKey dk) {
            if (dk!=null && stringMatches(dk.get())) return true;
        }
        // check the value in this guy
        if (node instanceof JsonNodeValue<?> jns) {
            if (stringMatches(jns.getValue().toString())) return true;
        }
        return false;
    }

    private boolean stringMatches(String s) {
        if (substring) return (s!=null && s.contains(pattern));
        return pattern.equals(s);
    }

    @Override
    public boolean selects(Cursor primary, @NotNull Cursor underTest) {
        if (null==underTest) return false;
        JsonNode node = underTest.getData();
        return matches(node);
    }

    @Override
    public void addAllNodes(Cursor primaryCur, @NotNull List<JsonNode> list) {
        JsonNode primary = primaryCur.getData();
        // TODO!
    }

    @Override
    public @Nullable Cursor nextCursor(Cursor primaryCur) {
        List<Cursor.DescentStep> atCursorList = primaryCur.asListOfSteps();
        Cursor.DescentStep[] atCursor = atCursorList.toArray(new Cursor.DescentStep[0]);
        JsonNode root = primaryCur.getData().getRoot();
        JsonNode nextGuy = findFirstAfter(root, atCursor, 0);
        if (null==nextGuy) return null;
        return nextGuy.asCursor();
    }

    @Override
    public @Nullable Cursor prevCursor(Cursor primaryCur) {
        List<Cursor.DescentStep> atCursorList = primaryCur.asListOfSteps();
        Cursor.DescentStep[] atCursor = atCursorList.toArray(new Cursor.DescentStep[0]);
        JsonNode root = primaryCur.getData().getRoot();
        JsonNode prevGuy = findLastBefore(root, atCursor, 0);
        if (null==prevGuy) return null;
        return prevGuy.asCursor();

    }

    private @Nullable JsonNode findFirstAfter(JsonNode node, Cursor.DescentStep[] atCursor, int index) {
        if (index == atCursor.length) {
            // leaf case: that's where the cursor already is, don't accept this. Children are OK though.
            if (node instanceof JsonNodeValue<?> v) return null;
            return findFirstUnder(node, true);
        } else {
            Cursor.DescentStep nextStep = atCursor[index];
            JsonNode nextNode = nextStep.apply(node);
            JsonNode result = findFirstAfter(nextNode, atCursor, index+1);
            if (null!=result) return result;
            // now try all children past that one.
            switch (nextStep) {
                case Cursor.DescentIndex di -> {
                    JsonNodeList nodeList = (JsonNodeList) node;
                    for (int i=di.get()+1; i<nodeList.childCount(); i++) {
                        nextNode = nodeList.get(i);
                        result = findFirstUnder(nextNode, false);
                        if (null!=result) return result;
                    }
                }
                case Cursor.DescentKey dk -> {
                    JsonNodeMap nm = (JsonNodeMap) node;
                    boolean searching = true;
                    for (String key : nm.getKeysInOrder()) {
                        if (searching) {
                            if (key.equals(dk.get())) {
                                searching = false;
                            }
                            continue;
                        }
                        result = findFirstUnder(nm.getChild(key), false);
                        if (null!=result) { return result; }
                    }
                }
                default -> { return null; }
            }
        }
        return null;
    }

    private @Nullable JsonNode findLastBefore(JsonNode node, Cursor.DescentStep[] atCursor, int index) {
        if (index == atCursor.length) {
            // leaf case: that's where the cursor already is, don't accept this. Children are OK though.
            if (node instanceof JsonNodeValue<?> v) return null;
            return findLastUnder(node, true);
        } else {
            Cursor.DescentStep nextStep = atCursor[index];
            JsonNode nextNode = nextStep.apply(node);
            JsonNode result = findLastBefore(nextNode, atCursor, index+1);
            if (null!=result) return result;
            // now try all children past that one.
            switch (nextStep) {
                case Cursor.DescentIndex di -> {
                    JsonNodeList nodeList = (JsonNodeList) node;
                    for (int i=di.get()-1; i>=0; i--) {
                        nextNode = nodeList.get(i);
                        result = findLastUnder(nextNode, false);
                        if (null!=result) return result;
                    }
                }
                case Cursor.DescentKey dk -> {
                    JsonNodeMap nm = (JsonNodeMap) node;
                    boolean searching = true;
                    for (String key : nm.getKeysInOrder().reversed()) {
                        if (searching) {
                            if (key.equals(dk.get())) {
                                searching = false;
                            }
                            continue;
                        }
                        result = findLastUnder(nm.getChild(key), false);
                        if (null!=result) { return result; }
                    }
                }
                default -> { return null; }
            }
        }
        return null;
    }

    /**
     * @return the first child of "node" (or "node" itself) that matches.
     */
    private @Nullable JsonNode findFirstUnder(JsonNode node, boolean excludeSelf) {
        if (!excludeSelf && matches(node)) return node;
        if (node instanceof JsonNodeValue<?> v) {
            return null;
        }
        JsonNode child = node.firstChild();
        while (null!=child) {
            JsonNode ret = findFirstUnder(child, false);
            if (null!=ret) return ret;
            child = child.nextSibling();
        }
        return null;
    }

    /**
     * @return the last child of "node" (or "node" itself) that matches.
     */
    private @Nullable JsonNode findLastUnder(JsonNode node, boolean excludeSelf) {
        if (!excludeSelf && matches(node)) return node;
        if (node instanceof JsonNodeValue<?> v) {
            return null;
        }
        JsonNode child = node.lastChild();
        while (null!=child) {
            JsonNode ret = findLastUnder(child, false);
            if (null!=ret) return ret;
            child = child.prevSibling();
        }
        return null;
    }


}