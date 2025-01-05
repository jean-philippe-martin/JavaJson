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

}
