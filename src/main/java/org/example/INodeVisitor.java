package org.example;

import org.jetbrains.annotations.NotNull;

/**
 * An "INodeVisitor" is something you can show nodes to.
 * Then afterwards you get some sort of value out. Maybe it's a max, say. */
public interface INodeVisitor<T> {
    @NotNull String getName();
    void visit(JsonNode node);
    /** Don't call `visit` anymore after this one. */
    T get();
}
