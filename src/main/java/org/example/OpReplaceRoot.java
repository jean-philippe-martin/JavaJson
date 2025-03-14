package org.example;

import org.example.cursor.DescentKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;

public class OpReplaceRoot implements Operation {


    private final JsonNode oldRoot;
    private final JsonNode newRoot;

    public OpReplaceRoot(JsonNode root, JsonNode newRoot) {
        this.oldRoot = root;
        this.newRoot = newRoot;
    }

    @Override
    public String toString() {
        return "replace(...)";
    }

    @Override
    public JsonNode run() {
        return newRoot;
    }

    @Override
    public @NotNull JsonNode undo() {
        return oldRoot;
    }

}
