package org.example.cursor;

import org.example.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

/**
 * A multicursor that highlights all the places that match a search string.
 */
public class FindCursor implements MultiCursor {
    protected @NotNull String pattern;
    protected @Nullable java.util.regex.Pattern regexp = null;
    // false: only match if the whole string equals the pattern
    // (otherwise, do substring)
    protected boolean substring;
    protected boolean ignoreCase;
    // Whether we search the keys (e.g. "key": "value")
    protected boolean inKey;
    // Whether we search the values.
    // Note: we have to always search in at least one of them.
    protected boolean inValue;
    protected boolean ignoreComments;


    /**
     * Search for exact matches of "text".
     */
    public FindCursor(@NotNull String text) {
        this.pattern = text;
        this.substring = false;
        this.ignoreCase = false;
        this.inKey = true;
        this.inValue = true;
        this.ignoreComments = false;
    }

    /**
     * Search for substring matches of "substring" (that is, anything that contains "substring")
     */
    public FindCursor(@NotNull String substring, boolean allowSubstring, boolean ignoreCase, boolean inKey, boolean inValue, boolean ignoreComments, boolean isRegExp) {
        this.pattern = substring;
        this.substring = allowSubstring;
        this.ignoreCase = ignoreCase;
        this.inKey = inKey;
        this.inValue = inValue;
        this.ignoreComments = ignoreComments;
        if (!inKey && !inValue) throw new RuntimeException("cannot search by neither key nor value.");
        if (isRegExp) {
            int flags = 0;
            if (ignoreCase) {
                flags += Pattern.CASE_INSENSITIVE + Pattern.UNICODE_CASE;
            }
            this.regexp = Pattern.compile(this.pattern, flags);
        } else {
            if (ignoreCase) {
                this.pattern = this.pattern.toUpperCase();
            }
        }
    }

    private boolean matches(@Nullable JsonNode node) {
        if (null==node) return false;
        if (ignoreComments && node.isSynthetic()) return false;
        // check the key that holds this guy
        if (inKey && node.asCursor().getStep() instanceof Cursor.DescentKey) {
            Cursor.DescentKey dk = (Cursor.DescentKey)(node.asCursor().getStep());
            if (dk!=null && stringMatches(dk.get())) return true;
        }
        // check the value in this guy
        if (inValue && node instanceof JsonNodeValue<?>) {
            JsonNodeValue jns = (JsonNodeValue)node;
            if (stringMatches(jns.getValue().toString())) return true;
        }
        return false;
    }

    private boolean stringMatches(String s) {
        if (null==s) return false;
        if (regexp!=null) {
            if (this.substring) {
                return regexp.matcher(s).find();
            } else {
                return regexp.matcher(s).matches();
            }
        }
        if (ignoreCase) {
            s = s.toUpperCase();
        }
        if (substring) return (s.contains(pattern));
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
        JsonNode root = primaryCur.getData().getRoot();
        innerAddAllNodes(primary, root, list);
    }

    private void innerAddAllNodes(JsonNode primary, JsonNode cur, @NotNull List<JsonNode> list) {
        if (matches(cur) && cur!=primary) list.add(cur);

        if (cur instanceof JsonNodeList) {
            JsonNodeList nl = (JsonNodeList)cur;
            for (int i=0; i<nl.size(); i++) {
                innerAddAllNodes(primary, nl.get(i), list);
            }
        } else if (cur instanceof JsonNodeMap) {
            JsonNodeMap nm = (JsonNodeMap)cur;
            for (String k : nm.getKeysInOrder()) {
                innerAddAllNodes(primary, nm.getChild(k), list);
            }
        }
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
            if (node instanceof JsonNodeValue) return null;
            return findFirstUnder(node, true);
        } else {
            Cursor.DescentStep nextStep = atCursor[index];
            JsonNode nextNode = nextStep.apply(node);
            JsonNode result = findFirstAfter(nextNode, atCursor, index + 1);
            if (null != result) return result;
            // now try all children past that one.
            if (nextStep instanceof Cursor.DescentIndex) {
                Cursor.DescentIndex di = (Cursor.DescentIndex) nextStep;
                JsonNodeList nodeList = (JsonNodeList) node;
                for (int i = di.get() + 1; i < nodeList.childCount(); i++) {
                    nextNode = nodeList.get(i);
                    result = findFirstUnder(nextNode, false);
                    if (null != result) return result;
                }
            } else if (nextStep instanceof Cursor.DescentKey) {
                Cursor.DescentKey dk = (Cursor.DescentKey) nextStep;
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
                    if (null != result) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    private @Nullable JsonNode findLastBefore(JsonNode node, Cursor.DescentStep[] atCursor, int index) {
        if (index == atCursor.length) {
            // leaf case: that's where the cursor already is, don't accept this. Children are OK though.
            if (node instanceof JsonNodeValue<?>) return null;
            return findLastUnder(node, true);
        } else {
            Cursor.DescentStep nextStep = atCursor[index];
            JsonNode nextNode = nextStep.apply(node);
            JsonNode result = findLastBefore(nextNode, atCursor, index+1);
            if (null!=result) return result;
            // now try all children past that one.
            if (nextStep instanceof Cursor.DescentIndex) {
                Cursor.DescentIndex di = (Cursor.DescentIndex) nextStep;
                JsonNodeList nodeList = (JsonNodeList) node;
                for (int i=di.get()-1; i>=0; i--) {
                    nextNode = nodeList.get(i);
                    result = findLastUnder(nextNode, false);
                    if (null!=result) return result;
                }
            } else if (nextStep instanceof Cursor.DescentKey) {
                Cursor.DescentKey dk = (Cursor.DescentKey) nextStep;
                JsonNodeMap nm = (JsonNodeMap) node;
                boolean searching = true;
                List<String> keys = new ArrayList<>(nm.getKeysInOrder());
                Collections.reverse(keys);
                for (String key : keys) {
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
        }
        return null;
    }

    /**
     * @return the first child of "node" (or "node" itself) that matches.
     */
    private @Nullable JsonNode findFirstUnder(JsonNode node, boolean excludeSelf) {
        if (!excludeSelf && matches(node)) return node;
        if (node instanceof JsonNodeValue<?>) {
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
        if (node instanceof JsonNodeValue) {
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