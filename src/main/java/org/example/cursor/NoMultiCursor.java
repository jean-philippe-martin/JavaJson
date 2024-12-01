package org.example.cursor;

import org.example.Cursor;
import org.example.JsonNode;
import org.example.cursor.MultiCursor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class NoMultiCursor implements MultiCursor {

    @Override
    public boolean selects(Cursor primary, @NotNull Cursor underTest) {
        return false;
    }

    @Override
    public void addAllNodes(Cursor primaryCur, @NotNull List<JsonNode> list) {
        // Nothing to do
    }

    @Override
    public @Nullable Cursor nextCursor(Cursor primaryCur) {
        // There is no secondary cursor, so nothing to find.
        return null;
    }

    @Override
    public @Nullable Cursor prevCursor(Cursor primaryCur) {
        // There is no secondary cursor, so nothing to find.
        return null;
    }
}