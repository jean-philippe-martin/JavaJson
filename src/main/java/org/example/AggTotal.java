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

    private INodeVisitor<Double> summer = new AggOpBasicStats.Sum();

    public AggTotal(JsonNode aggNode) {
        int count = 0;
        double sum = 0.0;
        for (JsonNodeIterator it = aggNode.iterateChildren(); it!=null; it=it.next()) {
            JsonNode kid = it.get();
            summer.visit(kid);
        }
    }

    public @Nullable JsonNode write(JsonNode aggNode) {
        Double sum = summer.get();
        if (null==sum) return null;
        JsonNodeValue<Double> aggregate = new JsonNodeValue<>(sum, aggNode, aggNode.asCursor().enterKey(OPNAME+"()"), aggNode.rootInfo.root);
        aggNode.setAggregate(aggregate, OPNAME);
        return aggNode;
    }
}
