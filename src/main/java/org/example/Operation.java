package org.example;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/** An operation is a thing you can do and undo too.
 *
 * It holds whatever information is necessary for the undo.
 * */
public interface Operation {

    /** Run this operation; returns the new root. **/
    public JsonNode run();

    /** Undo this operation (must be most recent op).
     *
     * @return the new root.
     */
    public @NotNull JsonNode undo();

    public class UnionCursors implements Operation {
        JsonNode rootBefore;
        public UnionCursors(JsonNode rootBefore) {
            this.rootBefore = rootBefore;
        }
        // You should only call "run" if the root you gave to the ctor is current.
        // Returns the new root.
        @Override
        public JsonNode run() {
            return TreeTransformer.UnionCursors(rootBefore);
        }
        // You should only call "undo" if we're the latest operation.
        // Returns the new root.
        @Override
        public @NotNull JsonNode undo() {
            return rootBefore;
        }

        @Override
        public String toString() {
            return "union " + rootBefore.atAnyCursor().size() + " cursor(s)";
        }
    }

    public class Sort implements Operation {

        private final ArrayList<Cursor> cursors;
        private final ArrayList<Sorter> sortersBefore;
        private final Sorter newSorter;
        // and after, we don't change what the root is.
        private final JsonNode rootBefore;

        public Sort(@NotNull JsonNode rootBefore, @Nullable Sorter s) {
            this.cursors = new ArrayList<>();
            this.sortersBefore = new ArrayList<>();
            this.newSorter = s;
            this.rootBefore = rootBefore;

            for (JsonNode node : rootBefore.atAnyCursor()) {
                cursors.add(node.asCursor());
                sortersBefore.add(node.getSort());
            }

        }

        @Override
        public JsonNode run() {
            for (JsonNode node : rootBefore.atAnyCursor()) {
                node.sort(this.newSorter);
            }
            if (null!=newSorter) {
                newSorter.pack();
            }
            return rootBefore;
        }

        @Override
        public @NotNull JsonNode undo() {
            for (int i=0; i<cursors.size(); i++) {
                JsonNode node = cursors.get(i).getData();
                node.sort(sortersBefore.get(i));
            }
            return rootBefore;
        }

        @Override
        public String toString() {
            if (null==newSorter) return "unsort";
            return newSorter.toString();
        }
    }

    public class AggUniqueFields implements Operation {

        private JsonNode beforeRoot;
        private final ArrayList<Cursor> cursors;
        private final ArrayList<AggInfo> aggBefore;
        // true = add aggregation, false = remove
        private final boolean addAgg;


        public AggUniqueFields(JsonNode beforeRoot, boolean addAgg) {
            this.beforeRoot = beforeRoot;
            this.cursors = new ArrayList<Cursor>();
            this.aggBefore = new ArrayList<>();
            this.addAgg = addAgg;

        }

        @Override
        public JsonNode run() {
            int cursorShouldGoUp = 0;
            // save "before" state
            int i=0;
            for (JsonNode node : beforeRoot.atAnyCursor()) {
                if (!addAgg) {
                    // we can press "remove aggregate" from within the aggregate itself.
                    while (node.getParent()!=null && node.isAggregate) {
                        node = node.getParent();
                        // the first cursor is main; so if we're removing the thing the cursor is in,
                        // we need to move the cursor so it's on something that's still there.
                        if (i==0) cursorShouldGoUp++;
                    }
                }
                cursors.add(node.asCursor());
                aggBefore.add(new AggInfo(node));
                i++;
            }

            // make the change
            int didSomething = 0;
            for (Cursor cur : cursors) {
                JsonNode node = cur.getData();
                if (node instanceof JsonNodeList) {
                    if (addAgg) {
                        didSomething++;
                        TreeTransformer.AggregateUniqueFields((JsonNodeList) node);
                    } else {
                        // remove aggregation
                        if (node.aggregate != null) {
                            didSomething++;
                            node.aggregate = null;
                            node.aggregateComment = null;
                        }
                    }
                } else if (!addAgg) {
                    // remove aggregate there
                    if (node.aggregate != null) {
                        didSomething++;
                        node.aggregate = null;
                        node.aggregateComment = null;
                    }
                }
            }
            // move the cursor out of the thing we removed.
            // technically there's a bug: we're not saving that cursor for undo.
            for (i=0; i<cursorShouldGoUp; i++) {
                beforeRoot.cursorParent();
            }
            if (didSomething==0) return null;
            return beforeRoot;
        }

        @Override
        public @NotNull JsonNode undo() {
            for (int i=0; i<cursors.size(); i++) {
                JsonNode node = cursors.get(i).getData();
                aggBefore.get(i).restore(node);
            }
            return beforeRoot;
        }

        @Override
        public String toString() {
            if (!addAgg) return "remove_aggregates()";
            return "unique_keys()";
        }
    }

    // For transformation that want to add aggregation info
    // to the nodes at the cursor(s).
    public class AggGeneric<T> implements Operation {

        private final INodeVisitor<T> visitor;
        private final JsonNode beforeRoot;
        private AggSaver before;

        public AggGeneric(JsonNode root, INodeVisitor<T> visitor) {
            this.visitor = visitor;
            this.beforeRoot = root.rootInfo.root;
        }

        @Override
        public JsonNode run() {
            // Save the past
            this.before = new AggSaver(beforeRoot);
            // Do the thing
            boolean happened = false;
            for (JsonNode node : beforeRoot.atAnyCursor()) {
                visitor.init();
                INodeVisitor<T> vis = visitor;
                Traverse.values(node, vis);
                T result = vis.get();
                if (null==result) continue;
                happened = true;
                JsonNodeValue<T> aggregate = new JsonNodeValue<>(result, node, node.asCursor().enterKey(vis.getName()+"()"), node.rootInfo.root);
                node.setAggregate(aggregate, vis.getName());
            }
            if (!happened) return null;
            return beforeRoot;
        }

        @Override
        public @NotNull JsonNode undo() {
            return before.restore();
        }

        @Override
        public String toString() {
            return visitor.getName() + "()";
        }
    }

    public class OpAggMinMax extends AggGeneric<String> {

        public OpAggMinMax(JsonNode root) {
            super(root, new AggOpBasicStats.MinMax());
        }

    }

    public class OpAggTotal extends AggGeneric<Object> {

        public OpAggTotal(JsonNode root) {
            super(root, new AggOpBasicStats.Sum());
        }

    }

    public class OpAggAvg extends AggGeneric<Double> {

        public OpAggAvg(JsonNode root) {
            super(root, new AggOpBasicStats.Avg());
        }
    }

    // For transforms that want to replace the node(s) at the cursor
    // with new ones.
    public abstract class TransformGeneric implements Operation {

        // The root, before we mess with it.
        private final @NotNull JsonNode beforeRoot;
        private final @NotNull ArrayList<JsonNode> oldNodes;

        public TransformGeneric(@NotNull JsonNode oldRoot) {
            this.beforeRoot = oldRoot;
            oldNodes = new ArrayList<>();
            oldNodes.addAll(oldRoot.atAnyCursor());
        }

        // Given a node, return a Builder for the transformed version
        public abstract JsonNodeBuilder transform(JsonNode oldNode);

        // Called on the result of builder.build()
        public abstract void onTransform(JsonNode newNode);

        @Override
        public JsonNode run() {
            boolean oneWorked = false;
            JsonNode newRoot = beforeRoot;
            for (JsonNode node : oldNodes) {
                JsonNode dad = node.getParent();
                Cursor toChild = node.whereIAm;
                JsonNodeBuilder builder = transform(node);
                if (null==builder) continue;
                if (null==dad) {
                    newRoot = builder.build(null, new Cursor());
                    onTransform(newRoot);
                } else {
                    JsonNode newGuy = dad.replaceChild(toChild, builder);
                    onTransform(newGuy);
                }
                oneWorked = true;
            }
            // return null to indicate (complete) failure
            if (!oneWorked) return null;
            // otherwise return the root node
            return newRoot;
        }

        @Override
        public @NotNull JsonNode undo() {
            JsonNode newRoot = beforeRoot;
            int i=0;
            for (i=oldNodes.size()-1; i>=0; i--) {
                JsonNode node = oldNodes.get(i);
                JsonNode dad = node.getParent();
                Cursor toChild = node.whereIAm;
                JsonNode.Builder builder = JsonNode.Builder.fromNode(node);
                if (null==dad) {
                    // case that we're root
                    newRoot = node;
                } else {
                    // we're not root
                    dad.replaceChild(toChild, builder);
                }
            }
            return beforeRoot;
        }
    }

    // Parse string to JSON
    public class OpParse extends TransformGeneric {

        public OpParse(JsonNode beforeRoot) {
            super(beforeRoot);
        }

        @Override
        public JsonNodeBuilder transform(JsonNode oldNode) {
            if (!((oldNode instanceof JsonNodeValue) && (oldNode.getValue() instanceof String))) {
                return null;
            }
            String s = (String) oldNode.getValue();
            if (!s.contains("{") && !s.contains("[")) {
                return null;
            }

            // ok, let's parse this.
            JsonNode parsedNoBuilder = null;
            try {
                parsedNoBuilder = JsonNode.parseJson(s);
            } catch (Exception x) {
                // nope
            }
            if (parsedNoBuilder == null) {
                //notificationText = "Could not parse as JSON";
                return null;
            }

            return JsonNode.Builder.fromNode(parsedNoBuilder);
        }

        @Override
        public void onTransform(JsonNode newNode) {
            newNode.annotation = "Parsed from a string";
        }

        public String toString() {
            return "parse_json()";
        }
    }

}
