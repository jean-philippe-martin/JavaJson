package org.example;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs the aggregation "unique_fields":
 * Given a list of maps, for each key compute which fraction of the maps have that key.
 **/
public class AggUniqueFields {

    // how many maps are contained inside us.
    private int total;
    // how many times we see each given field name
    final private Map<String, Long> counts;
    // fields in our kids may have an array with unique fields too.
    final private Map<String, AggUniqueFields> grandKids;

    public AggUniqueFields(JsonNodeList aggNode) {
        total = 0;
        counts = new HashMap<>();
        grandKids = new HashMap<>();

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

                JsonNode grandKid = map.getChild(k);
                if (grandKid instanceof JsonNodeList) {
                    // maybe it holds structs and we should aggregate them?
                    AggUniqueFields itsCounts = new AggUniqueFields((JsonNodeList)grandKid);
                    // first time around we get null, adding null does nothing.
                    itsCounts = itsCounts.add(grandKids.get(k));
                    grandKids.put(k, itsCounts);
                }
            }
        }

        // at this point, total and counts are updated.
    }

    private AggUniqueFields(int total, Map<String, Long> counts, Map<String, AggUniqueFields> grandKids) {
        this.total = total;
        this.counts = counts;
        this.grandKids = grandKids;
    }


    public AggUniqueFields add(@Nullable AggUniqueFields rhs) {
        if (null==rhs) return this;
        int newTotal = this.total + rhs.total;
        HashMap<String, Long> newCounts = new HashMap<>();
        Map<String, AggUniqueFields> newGrandKids = new HashMap<>();

        HashSet<String> keys = new HashSet<>();
        keys.addAll(counts.keySet());
        keys.addAll(rhs.counts.keySet());
        for (String k : keys) {
            newCounts.put(k, this.counts.getOrDefault(k, Long.valueOf(0)) + rhs.counts.getOrDefault(k, Long.valueOf(0)));
        }

        keys = new HashSet<>();
        keys.addAll(grandKids.keySet());
        keys.addAll(rhs.grandKids.keySet());
        for (String k : keys) {
            if (!this.grandKids.containsKey(k)) {
                newGrandKids.put(k, rhs.grandKids.get(k));
                continue;
            }
            if (!rhs.grandKids.containsKey(k)) {
                newGrandKids.put(k, this.grandKids.get(k));
                continue;
            }
            // They both have a value, let's combine them.
            newGrandKids.put(k, this.grandKids.get(k).add(rhs.grandKids.get(k)));
        }

        return new AggUniqueFields(newTotal, newCounts, newGrandKids);
    }


    private LinkedHashMap<String, Object> asHashMap() {
        LinkedHashMap<String, Object> ret = new LinkedHashMap<>();
        for (String k : counts.keySet()) {
            if (grandKids.containsKey(k) && grandKids.get(k).total>0 ) {
                ret.put(k, grandKids.get(k).asHashMap());
            } else {
                ret.put(k, "");
            }
        }
        return ret;
    }

    private void setChildrenComments(JsonNodeMap aggregate) {
        Long longTotal = Long.valueOf(total);
        for (String k : counts.keySet()) {
            Long fieldCount = counts.get(k);
            if (fieldCount.equals(longTotal)) {
                aggregate.setChildAggregateComment(k, "=100%");
            } else {
                int pct = (int)(100.0 * fieldCount / (1.0*total) + 0.5);
                StringBuilder pctStr = new StringBuilder(pct + "%");
                //String pctStr = "" + pct + "%";
                while (pctStr.length()<5) {
                    pctStr.insert(0, ' ');
                    //pctStr = " " + pctStr;
                }
                aggregate.setChildAggregateComment(k, pctStr.toString());
            }
            if (grandKids.containsKey(k) && grandKids.get(k).total>0) {
                JsonNode next = aggregate.getChild(k);
                JsonNodeMap nextMap = (JsonNodeMap) next;
                grandKids.get(k).setChildrenComments(nextMap);
            }
        }
    }

    public void write(JsonNodeList aggNode) {

        LinkedHashMap<String, Object> kv = asHashMap();

        JsonNodeMap aggregate = new JsonNodeMap(kv, aggNode, aggNode.asCursor().enterKey("unique_keys()"), aggNode.root);
        aggNode.setAggregate(aggregate, "unique_keys");
        setChildrenComments(aggregate);
    }



}
