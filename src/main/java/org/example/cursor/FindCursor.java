package org.example.cursor;

import org.example.Cursor;
import org.example.JsonNode;
import org.example.JsonNodeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FindCursor implements MultiCursor {
    protected @NotNull String pattern;

    public FindCursor(@NotNull String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean selects(Cursor primary, @NotNull Cursor underTest) {
        if (null==underTest) return false;
        if (underTest.getStep() instanceof Cursor.DescentKey dk) {
            if (dk!=null && dk.get().equals(pattern)) return true;
        }
        JsonNode node = underTest.getData();
        if (null==node) return false;
        if (node instanceof JsonNodeValue<?> jns) {
            if (pattern.equals(jns.getValue())) return true;
            if (pattern.equals(jns.getValue().toString())) return true;
        }
        return false;
    }

    @Override
    public void addAllNodes(Cursor primaryCur, @NotNull List<JsonNode> list) {
        JsonNode primary = primaryCur.getData();
        // TODO!
    }

    @Override
    public @Nullable Cursor nextCursor(Cursor primaryCur) {
        throw new RuntimeException("unimplemented");
    }

    @Override
    public @Nullable Cursor prevCursor(Cursor primaryCur) {
        throw new RuntimeException("unimplemented");
    }


}