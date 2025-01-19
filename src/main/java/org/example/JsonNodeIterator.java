package org.example;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface JsonNodeIterator<T> {
    @NotNull JsonNode get();
    @NotNull T key();
    boolean isAggregate();

    @Nullable JsonNodeIterator next();
}
