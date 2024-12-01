package org.example.cursor;

import org.example.Cursor;
import org.example.JsonNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface MultiCursor {
    /**
     * @param primary the primary user cursor
     * @param underTest cursor to the node we'd like to know whether to highlight
     * @return true if "underTest" should be highlighted. Note that we're allowed
     *         to return false if underTest.equals(primary).
     */
    public boolean selects(Cursor primary, @NotNull Cursor underTest);

    /**
     * @param primary the primary user cursor
     * @param list list to which to add all the nodes that are currently selected, excluding the primary.
     */
    public void addAllNodes(Cursor primary, @NotNull List<JsonNode> list);

    /**
     * @return the highest secondary cursor that is lower than the given primary cursor.
     *         Note that "previous"
     *         does not necessarily need to be lined up with one the secondary cursors.
     *         Returns null if there is no such cursor.
     */
    public @Nullable Cursor nextCursor(Cursor primary);
    /** Opposite of nextCursor. **/
    public @Nullable Cursor prevCursor(Cursor primary);
}
