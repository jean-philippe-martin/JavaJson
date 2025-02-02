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

    private AggOpBasicStats.Sum summer = new AggOpBasicStats.Sum();

    public AggTotal(JsonNode aggNode) {
        int count = 0;
        double sum = 0.0;
        Traverse.values(aggNode, summer);
    }

    public @Nullable JsonNode write(JsonNode aggNode) {
        return aggNode;
        /*
        Double sum = summer.get();
        if (null==sum) return null;
        if (summer.getUnit()== AggOpBasicStats.Unit.LENGTH) {
            // special case for length: diff function name, and int instead of double
            JsonNodeValue<Integer> aggregate = new JsonNodeValue<>(sum.intValue(), aggNode, aggNode.asCursor().enterKey(summer.getName() + "()"), aggNode.rootInfo.root);
            aggNode.setAggregate(aggregate, summer.getName());
        } else {
            // normal case: sum, and double
            JsonNodeValue<Double> aggregate = new JsonNodeValue<>(sum, aggNode, aggNode.asCursor().enterKey(summer.getName() + "()"), aggNode.rootInfo.root);
            aggNode.setAggregate(aggregate, summer.getName());
        }
        return aggNode;
        */
    }
}
