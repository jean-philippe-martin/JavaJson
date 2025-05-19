package org.example;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;

public class OpCountEachDistinct implements Operation {

    /** Holds information needed to undo a groupby */
    public static class CountEachUndo {
        private JsonNodeList holderList;
        private JsonNode listParent;
        private Cursor toList;
        public CountEachUndo(JsonNodeList listToGroup) {
            this.holderList = listToGroup;
            this.listParent = listToGroup.getParent();
            this.toList = listToGroup.whereIAm;
        }
        public void undo() {
            JsonNode.Builder builder = JsonNode.Builder.fromNode(holderList);
            if (listParent!=null) {
                listParent.replaceChild(toList, builder);
            }
        }
    }

    private JsonNode oldRoot;
    private JsonNode innerMap;
    private String keyToGroupBy;
    private ArrayList<CountEachUndo> undoers;

    public OpCountEachDistinct(JsonNode root) {
        this.oldRoot = root;
        this.undoers = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "count_dups()";
    }

    @Override
    public JsonNode run() {
        JsonNode newRoot = null;
        for (JsonNode selectedValue : oldRoot.atAnyCursor()) {
            // must point to a string value inside a list
            if (null==selectedValue.getParent()) continue;
            if (!(selectedValue.getParent() instanceof JsonNodeList)) continue;
            JsonNodeList listToGroup = (JsonNodeList)(selectedValue.parent);
            undoers.add(new CountEachUndo(listToGroup));
            newRoot = groupThenLength(listToGroup);
        }
        return newRoot;
    }

    @Override
    public @NotNull JsonNode undo() {
        int l = undoers.size();
        for (var i=0; i<l; i++) {
            undoers.get(l-i-1).undo();
        }
        return oldRoot;
    }

    // returns the new root
    private JsonNode groupThenLength(JsonNodeList listToGroup) {
        JsonNode newRoot = listToGroup.getRoot();
        JsonNode oldParent = listToGroup.getParent();
        Cursor cursorToOld = listToGroup.whereIAm;
        // find the groups
        // (value of the key being grouped by -> list of objects that have that value for that key)
        LinkedHashMap<String, Integer> groups = new LinkedHashMap<>();
        JsonNodeIterator it = listToGroup.iterateChildren(false);
        while (null!=it) {
            if (it.isAggregate()) {it = it.next();continue;}
            JsonNode kid = it.get();
            it = it.next();
            String groupName;
            if (kid==null) {
                groupName = "(null)";
            } else if (kid instanceof JsonNodeList) {
                groupName = "(list)";
            } else if (kid instanceof JsonNodeMap) {
                groupName = "(map)";
            } else {
                groupName = kid.getValue().toString();
            }
            if (!(groups.containsKey(groupName))) groups.put(groupName, 0);
            groups.put(groupName, groups.get(groupName) + 1);
        }

        // Don't group if there's nothing there.
        if (groups.isEmpty()) return null;

        // for each of the groups (= distinct values), create a JsonNode.
        // It will contain the count.
        LinkedHashMap<String, JsonNodeBuilder> groupsAsNodes = new LinkedHashMap<>();
        for (String key : groups.keySet()) {
            groupsAsNodes.put(key, JsonNode.Builder.fromObject(groups.get(key)));
        }

        JsonNodeMap.Builder rootMap = new JsonNodeMap.Builder(groupsAsNodes);

        JsonNode newChild;
        if (listToGroup.isRoot()) {
            newRoot = rootMap.build(null, new Cursor());
            newChild = newRoot;
        } else {
            newChild = oldParent.replaceChild(cursorToOld, rootMap);
        }
        newChild.setAnnotation("duplicate counts");
        return newRoot;
    }
}
