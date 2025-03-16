package org.example;

import org.example.cursor.DescentKey;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class OpGroupby implements Operation {

    /** Holds information needed to undo a groupby */
    public static class GroupbyUndo {
        // The list that contains the objects we're going to group
        private JsonNodeList holderList;
        // Parent of the list
        private JsonNode listParent;
        // cursor to the list
        private Cursor toList;
        public GroupbyUndo(JsonNodeMap mapToGroup) {
            JsonNodeList holder = (JsonNodeList) mapToGroup.getParent();
            this.holderList = holder;
            this.listParent = holder.getParent();
            this.toList = holder.whereIAm;
        }
        public void undo() {
            JsonNode.Builder builder = JsonNode.Builder.fromNode(holderList);
            if (null!=listParent) {
                listParent.replaceChild(toList, builder);
            } else if (null!=holderList) {
                // we're root, but we still need to fix all the maps to have the correct parent.
                List<JsonNode> children = new ArrayList<>();
                var it = holderList.iterateChildren();
                while (it!=null) {
                    children.add(it.get());
                    it = it.next();
                }
                int index=0;
                for (var kid: children) {

                    kid.reparent(holderList, holderList.asCursor().enterIndex(index));
                    //holderList.replaceChild(kid.asCursor(), JsonNode.Builder.fromNode(kid));
                    index++;
                }
            }
        }
    }

    private JsonNode oldRoot;
    private JsonNode innerMap;
    private String keyToGroupBy;
    private ArrayList<GroupbyUndo> undoers;

    public OpGroupby(JsonNode root) {
        this.oldRoot = root;
        this.undoers = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "groupby(."+keyToGroupBy+")";
    }

    @Override
    public JsonNode run() {
        JsonNode newRoot = null;
        for (JsonNode selectedValue : oldRoot.atAnyCursor()) {
            // must point to a key inside a list of maps.
            if (null==selectedValue.getParent()) continue;
            if (!(selectedValue.getParent() instanceof JsonNodeMap)) continue;
            if (null==selectedValue.getParent().getParent()) continue;
            if (!(selectedValue.getParent().getParent() instanceof JsonNodeList)) continue;
            JsonNodeMap mapToGroup = (JsonNodeMap)(selectedValue.parent);
            keyToGroupBy = ((DescentKey)(selectedValue.asCursor().getStep())).get();
            undoers.add(new GroupbyUndo(mapToGroup));
            newRoot = groupby(mapToGroup, keyToGroupBy);
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
    private JsonNode groupby(JsonNodeMap mapToGroup, String keyToGroupBy) {
        JsonNodeList list = (JsonNodeList)(mapToGroup.parent);
        JsonNode newRoot = mapToGroup.getRoot();
        JsonNode oldParent = list.getParent();
        Cursor cursorToOld = list.whereIAm;
        // find the groups
        // (value of the key being grouped by -> list of objects that have that value for that key)
        LinkedHashMap<String, List<JsonNode>> groups = new LinkedHashMap<>();
        JsonNodeIterator it = list.iterateChildren();
        while (null!=it) {
            JsonNode kid = it.get();
            it = it.next();
            String groupName;
            if (!(kid instanceof JsonNodeMap)) {
                groupName = "(null)";
            } else {
                JsonNodeMap kidMap = (JsonNodeMap) kid;
                try {
                    JsonNode value = kidMap.getChild(keyToGroupBy);
                    if ((value instanceof JsonNodeValue)) {
                        groupName = value.getValue().toString();
                    } else {
                        // "other" is the group for maps that have a map or list under that key.
                        groupName = "(other)";
                    }
                } catch (NoSuchElementException nsel) {
                    // "null" is the group for maps that don't have that key
                    groupName = "(null)";
                }
            }
            if (!(groups.containsKey(groupName))) groups.put(groupName, new ArrayList<>());
            groups.get(groupName).add(kid);
        }

        // Don't group if there's nothing there.
        if (groups.isEmpty()) return null;

        // for each of the groups, create a JsonNodeMap.
        // for each of the values of the map, create a JsonNodeList.
        LinkedHashMap<String, JsonNodeBuilder> groupsAsNodes = new LinkedHashMap<>();
        for (String key : groups.keySet()) {
            JsonNodeBuilder[] kidsArray = groups.get(key).stream().map(JsonNode.Builder::new).toArray(JsonNodeBuilder[]::new);
            groupsAsNodes.put(key, new JsonNodeList.Builder(kidsArray));
        }

        JsonNodeMap.Builder rootMap = new JsonNodeMap.Builder(groupsAsNodes);

        JsonNode newNode;
        if (list.isRoot()) {
            newRoot = rootMap.build(null, new Cursor());
            newNode = newRoot;
        } else {
            newNode = oldParent.replaceChild(cursorToOld, rootMap);
        }
        newNode.setAnnotation("grouped by " + keyToGroupBy);
        return newRoot;
    }
}
