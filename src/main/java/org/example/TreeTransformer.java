package org.example;

import java.util.ArrayList;

public class TreeTransformer {
    /**
     * Given a tree with secondary cursors pointing to arrays,
     * this generates a new tree that just has one big array
     * with the union of all that was in the original arrays.
     *
     * The copy will include the primary cursor.
     *
     * @param rootNode
     * @return
     */
    public static JsonNode UnionCursors(JsonNode rootNode) {
        ArrayList<Object> myList = new ArrayList<>();
        for (JsonNode node : rootNode.atAnyCursor()) {
            // we flat out ignore non-lists
           if (!(node instanceof JsonNodeList)) continue;
           JsonNodeList jnl = (JsonNodeList) node;
           myList.addAll(jnl.values);
        }
        JsonNode newRoot = JsonNode.fromObject(myList, null, new Cursor(), null);
        return newRoot;
    }
}
