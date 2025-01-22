package org.example;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs the aggregation "sum":
 * Given a list of values, compute their sum.
 **/
public class AggTotal {

    public static final String OPNAME="sum";

    private Double total;

    public AggTotal(JsonNode aggNode) {
        int count = 0;
        double sum = 0.0;
        for (JsonNodeIterator it = aggNode.iterateChildren(); it!=null; it=it.next()) {
            JsonNode kid = it.get();
            if (!(kid instanceof JsonNodeValue)) continue;
            count+=1;
            sum += ((JsonNodeValue<?>) kid).asDouble();
        }
        if (count>0) {
            this.total = sum;
        }
    }

    public @Nullable JsonNode write(JsonNode aggNode) {
        if (null==total) return null;
        JsonNodeValue<Double> aggregate = new JsonNodeValue<>(total, aggNode, aggNode.asCursor().enterKey(OPNAME+"()"), aggNode.rootInfo.root);
        aggNode.setAggregate(aggregate, OPNAME);
        return aggNode;
    }
}
