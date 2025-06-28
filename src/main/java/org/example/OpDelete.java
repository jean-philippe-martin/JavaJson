package org.example;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class OpDelete implements Operation {


    private final JsonNode oldRoot;
    private final Deleter instructions;

    public OpDelete(JsonNode root, Deleter instructions) {
        this.oldRoot = root;
        this.instructions = instructions;
    }

    @Override
    public String toString() {
        return instructions.explain();
    }

    @Override
    public JsonNode run() {
        if (instructions.targets(oldRoot)) {
            ArrayList<JsonNodeBuilder> newRootBuilders = new ArrayList<>();
            List<JsonNode> newRoots = findNewRoots();
            for (JsonNode n : newRoots) {
                newRootBuilders.add(rebuild(n));
            }
            return new JsonNodeList.Builder(newRootBuilders.toArray(new JsonNodeBuilder[]{}))
                    .build(null, new Cursor());
        } else {
            return rebuild(oldRoot).build(null, new Cursor());
        }

    }

    @Override
    public @NotNull JsonNode undo() {
        return oldRoot;
    }

    private JsonNodeBuilder rebuild(JsonNode old) {
        if (old instanceof JsonNodeValue) {
            JsonNodeValue node = (JsonNodeValue)old;
            if (instructions.targets(old)) return null;
            return new JsonNodeValue.Builder(node.getValue()).pinned(node.pinned).folded(node.folded);
        } else if (old instanceof JsonNodeMap) {
            JsonNodeMap node = (JsonNodeMap)old;
            LinkedHashMap<String, JsonNodeBuilder> map = new LinkedHashMap<>();
            for (String k : node.getKeysInOrder()) {
                JsonNode kid = node.getChild(k);
                if (instructions.targets(kid)) continue;
                JsonNodeBuilder kidBuilder = rebuild(kid);
                if (null!=kidBuilder) map.put(k, kidBuilder);
            }
            return new JsonNodeMap.Builder(map).folded(node.folded).pinned(node.pinned);
        } else if (old instanceof JsonNodeList) {
            JsonNodeList node = (JsonNodeList)old;
            ArrayList<JsonNodeBuilder> children = new ArrayList<>();
            var it = node.iterateChildren(false);
            while (null!=it) {
                JsonNode kid = it.get();
                if (!instructions.targets(kid)) {
                    JsonNodeBuilder kidBuilder = rebuild(kid);
                    if (null!=kidBuilder) children.add(kidBuilder);
                }
                it = it.next();
            }
            return new JsonNodeList.Builder(children.toArray(new JsonNodeBuilder[]{}))
                    .pinned(node.pinned).folded(node.folded);
        } else {
            throw new RuntimeException("Unexpected node type: " + old.getClass());
        }
    }

    /** @return All the kept descendants that have no kept ancestor. */
    private List<JsonNode> findNewRoots() {
        Deque<JsonNode> visit = new ArrayDeque<>();
        visit.add(oldRoot);
        ArrayList<JsonNode> found = new ArrayList<>();

        while (!visit.isEmpty()) {
            JsonNode node = visit.pop();
            if (instructions.targets(node)) {
                // he gets deleted. But his children maybe?
                var it = node.iterateChildren(false);
                while (it!=null) {
                    JsonNode kid = it.get();
                    visit.add(kid);
                    it = it.next();
                }
            } else {
                // A survivor!
                found.add(node);
            }
        }
        return found;
    }

}
