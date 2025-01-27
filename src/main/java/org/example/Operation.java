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

    /** Undo this operation (must be most recept op).
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
                    // (sadly this doesn't work)
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

    public class AggTotalOp implements Operation {

        JsonNode beforeRoot;
        private final ArrayList<Cursor> cursors;
        private final ArrayList<AggInfo> aggBefore;
        private AggOpBasicStats.Unit unit = AggOpBasicStats.Unit.NOTHING;


        public AggTotalOp(JsonNode root) {
            this.beforeRoot = root.rootInfo.root;
            this.cursors = new ArrayList<>();
            this.aggBefore = new ArrayList<>();
        }

        @Override
        public JsonNode run() {

            // Save the past
            for (JsonNode node : beforeRoot.atAnyCursor()) {
                cursors.add(node.asCursor());
                aggBefore.add(new AggInfo(node));
            }

            // Do the thing
            boolean happened = false;
            for (JsonNode node : beforeRoot.atAnyCursor()) {
                AggTotal agg = new org.example.AggTotal(node);
                JsonNode foo = agg.write(node);
                if (null!=foo) happened=true;
            }
            if (!happened) return null;
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
            return AggTotal.OPNAME + "()";
        }

    }

    public class AggMinMaxOp implements Operation {

        JsonNode beforeRoot;
        private final ArrayList<Cursor> cursors;
        private final ArrayList<AggInfo> aggBefore;


        public AggMinMaxOp(JsonNode root) {
            this.beforeRoot = root.rootInfo.root;
            this.cursors = new ArrayList<>();
            this.aggBefore = new ArrayList<>();
        }

        @Override
        public JsonNode run() {

            // Save the past
            for (JsonNode node : beforeRoot.atAnyCursor()) {
                cursors.add(node.asCursor());
                aggBefore.add(new AggInfo(node));
            }

            // Do the thing
            boolean happened = false;
            for (JsonNode node : beforeRoot.atAnyCursor()) {
                INodeVisitor<String> vis = new AggOpBasicStats.MinMax();
                Traverse.values(node, vis);
                String result = vis.get();
                if (null==result) continue;
                happened = true;
                JsonNodeValue<String> aggregate = new JsonNodeValue<>(result, node, node.asCursor().enterKey(vis.getName()+"()"), node.rootInfo.root);
                node.setAggregate(aggregate, vis.getName());
            }
            if (!happened) return null;
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
            return new AggOpBasicStats.MinMax().getName() + "()";
        }

    }
}
