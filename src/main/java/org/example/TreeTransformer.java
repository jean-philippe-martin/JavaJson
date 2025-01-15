package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

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

    // TBD
    public static void AggregateUniqueFields(JsonNodeList aggNode) {

        HashMap<String, Long> counts = new HashMap<String, Long>();
        long total = 0;

        for (int i=0; i<aggNode.childCount(); i++) {
            JsonNode kid = aggNode.get(i);
            if (!(kid instanceof JsonNodeMap)) continue;
            total += 1;
            JsonNodeMap map = (JsonNodeMap) kid;
            for (String k : map.getKeysInOrder()) {
                if (!counts.containsKey(k)) {
                    counts.put(k, Long.valueOf(0));
                }
                Long oldCount = counts.get(k);
                oldCount += 1;
                counts.put(k, oldCount);
            }
        }


        LinkedHashMap<String, Object> kv = new LinkedHashMap<>();
        for (String k : counts.keySet()) {
            kv.put(k, "");
        }
        JsonNodeMap aggregate = new JsonNodeMap(kv, aggNode, aggNode.asCursor().enterKey("unique_fields()"), aggNode.root);
        aggNode.aggregate = aggregate;
        aggNode.aggregateComment = "unique_fields";
        for (String k : counts.keySet()) {
            Long fieldCount = counts.get(k);
            if (fieldCount.equals(total)) {
                aggregate.setChildAggregateComment(k, "=100%");
            } else {
                int pct = (int)(100.0 * fieldCount / (1.0*total) + 0.5);
                String pctStr = "" + pct + "%";
                while (pctStr.length()<5) {
                    pctStr = " " + pctStr;
                }
                aggregate.setChildAggregateComment(k, pctStr);
            }
        }
    }
}
