package org.example;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

// TODO: only store a limited number of undos, to save memory.
public class OperationList {

    private final ArrayList<Operation> ops = new ArrayList<>();

    public OperationList() {}

    /** run this operation and add it to the list if successful.
     *  Return the new root if sucessful.
     *  Lack of success is shown by returning null. */
    public @Nullable JsonNode run(Operation op) {
        JsonNode newRoot = op.run();
        if (newRoot != null) {
            this.ops.add(op);
        }
        return newRoot;
    }

    // returns null if the list is empty.
    public @Nullable Operation peek() {
        if (ops.isEmpty()) return null;
        return ops.get(ops.size()-1);
    }

    /** undo the last operation and remove it from the list. */
    public @Nullable JsonNode undo() {
        if (ops.isEmpty()) return null;
        Operation op = ops.remove(ops.size()-1);
        JsonNode newRoot = op.undo();
        return newRoot;
    }

}
