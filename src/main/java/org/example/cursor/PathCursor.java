package org.example.cursor;

import org.example.Cursor;
import org.example.JsonNode;
import org.example.JsonNodeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PathCursor implements MultiCursor {

    private List<DescentStep> steps;

    public PathCursor(String path) {
        List<DescentStep> newSteps = new ArrayList<>();
        String whereToGo = path;
        while (!whereToGo.isEmpty()) {
            if (whereToGo.startsWith(".")) {
                int dot = whereToGo.indexOf('.', 1);
                int bracket = whereToGo.indexOf('[');
                int end = whereToGo.length();
                int choice = dot;
                if (choice < 0 || (bracket >= 0 && bracket < choice)) choice = bracket;
                if (choice < 0 || (end >= 0 && end < choice)) choice = end;
                String token = whereToGo.substring(1, choice);
                if ("*".equals(token)) {
                    newSteps.add(new DescentAll(Cursor.DescentStyle.ALL_KEYS));
                } else {
                    newSteps.add(new DescentKey(token));
                }
                whereToGo = whereToGo.substring(choice);
            } else if (whereToGo.startsWith("[")) {
                int end = whereToGo.length();
                int bracket = whereToGo.indexOf(']');
                int choice = end;
                if (bracket>=0 && bracket<end) {
                    choice = bracket;
                }
                String token = whereToGo.substring(1, choice);
                if (token.equals("*")) {
                    // special case: all indices
                    newSteps.add(new DescentAll(Cursor.DescentStyle.ALL_INDICES));
                } else {
                    Integer index = Integer.parseInt(token);
                    newSteps.add(new DescentIndex(index));
                }
                whereToGo = whereToGo.substring(choice+1);
            } else {
                throw new RuntimeException("Don't know how to go to " + whereToGo);
            }
        }
        steps = newSteps;
    }

    @Override
    public boolean selects(Cursor primary, @NotNull Cursor underTest) {
        List<DescentStep> listOfSteps = underTest.asListOfSteps();
        if (this.steps.size() != listOfSteps.size()) return false;
        int i=-1;
        for (DescentStep step: listOfSteps) {
            i++;
            DescentStep myStep = this.steps.get(i);
            if (myStep instanceof DescentAll) {
                DescentAll mine = (DescentAll) myStep;
                if (mine.getDescentStyle()== Cursor.DescentStyle.ALL_INDICES && step instanceof DescentIndex) continue;
                if (mine.getDescentStyle()== Cursor.DescentStyle.ALL_KEYS && step instanceof DescentKey) continue;
                return false;
            }
            if (!step.equals(myStep)) return false;
        }
        return true;
    }

    @Override
    public void addAllNodes(Cursor primary, @NotNull List<JsonNode> list) {
        if (null==primary) throw new RuntimeException("Sorry, PathCursor needs a primary even if it's just root");
        JsonNode node = primary.getData().getRoot();
        addAllNodesInternal(-1, node, primary, list);
    }

    // Add all the selected children, recursively.
    // parentDepth must be the depth of "node", the node we're going to apply descent step parentDepth+1 to.
    private void addAllNodesInternal(int parentDepth, JsonNode node, Cursor primary, List<JsonNode> list) {
        int depth = parentDepth;
        while (++depth<this.steps.size()) {
            DescentStep step = this.steps.get(depth);
            if (step instanceof DescentKey || step instanceof DescentKey) {
                node = step.apply(node);
            } else if (step instanceof DescentAll) {
                var it = node.iterateChildren(true);
                while (it!=null) {
                    if (!it.isAggregate()) {
                        addAllNodesInternal(depth, it.get(), primary, list);
                    }
                    it = it.next();
                }
                return;
            } else {
                throw new RuntimeException("Unknown descent step: " + step);
            }
        }
        if (!node.asCursor().equals(primary)) {
            list.add(node);
        }
    }

    @Override
    public @Nullable Cursor nextCursor(Cursor primary) {
        if (null==primary) {
            if (null==primary) throw new RuntimeException("Sorry, PathCursor needs a primary even if it's just root");
        }
        List<DescentStep> listOfSteps = primary.asListOfSteps();
        JsonNode node = primary.getData().getRoot();
        // Find where we differ
        int i=0;
        while (i<steps.size() && i<listOfSteps.size()) {
            if (!steps.get(i).equals(listOfSteps.get(i))) {
                break;
            }
            node = steps.get(i).apply(node);
            i++;
        }
        if (i>=steps.size()) {
            // all our steps match, we're done.
            return null;
        }
        DescentStep lastEqualStep = steps.get(i-1);
        if (lastEqualStep.getDescentStyle()== Cursor.DescentStyle.A_KEY || lastEqualStep.getDescentStyle()== Cursor.DescentStyle.ALL_KEYS) {
            // Two keys. Find the first one after the primary's.
            JsonNodeMap jnm = (JsonNodeMap) node;
            boolean found = false;
            String theirKey = ((DescentKey)listOfSteps.get(i)).get();
            String myKey = null; // null here indicates ALL_KEYS
            if (steps.get(i) instanceof DescentKey) {
                myKey = ((DescentKey)steps.get(i)).get();
            }
            for (String key : jnm.getKeysInOrder()) {
                if (key.equals(theirKey)) {
                    // we can start attempting to go down from this very key.
                    found = true;
                }
                if (found && (myKey==null || myKey.equals(key)  )) {
                    // we found the next guy.
                    JsonNode deepNode = jnm.getChild(key);
                    JsonNode descendant = firstDescendantFromPrefix(deepNode, i);
                    if (null==descendant) {
                        // maybe path is foo.*.bar,
                        // we tried foo.a but it doesn't have foo.a.bar.
                        // So now we're going to try foo.b, etc.
                        continue;
                    }
                    Cursor ret = descendant.asCursor();
                    // in case what we found was *exactly* the cursor we started from, keep looking.
                    if (ret.equals(primary)) continue;
                    return ret;
                }
            }
            // no match. That should mean we were past the last cursor.
            return null;
        }
        // TODO: the rest
        return primary;
    }

    // so if deepNodeDepth=0 that means we applied steps[0] to get to deepNode.
    private JsonNode firstDescendantFromPrefix(JsonNode deepNode, int deepNodeDepth) {
        JsonNode node = deepNode;
        for (int i=deepNodeDepth+1; i<steps.size(); i++) {
            if (null==node) return null;
            DescentStep step = steps.get(i);
            if (step instanceof DescentAll) {
                node = ((DescentAll) step).applyFirst(node);
            } else {
                node = step.apply(node);
            }
        }
        return node;
    }

    @Override
    public @Nullable Cursor prevCursor(Cursor primary) {
        return null;
    }
}
