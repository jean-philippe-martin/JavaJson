package org.example;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An "AggOp" is a sort of visitor: you show it a bunch of JsonNodes, and then you
 * get some basic statistics out.
 *
 */
public class AggOpBasicStats {
    Double minNum = null;
    Double maxNum = null;
    String minStr = null;
    String maxStr = null;
    Integer minLength = null;
    Integer maxLength = null;
    long totalLength = 0;
    int numCount = 0;
    int strCount = 0;
    int lenCount = 0;
    int otherCount = 0;
    Double total = (double) 0;

    public enum Unit {
        NOTHING,
        NUMBER,
        STRING,
        LENGTH
    }


    public static class Min implements INodeVisitor<Object> {
        AggOpBasicStats stats;

        public Min() {
            stats = new AggOpBasicStats();
        }

        @Override
        public @NotNull String getName() {
            return "min";
        }

        @Override
        public void visit(JsonNode node) {
            stats.visit(node);
        }

        @Override
        public Object get() {
            Unit unit = stats.getUnit();
            return stats.getMin(unit);
        }
    }

    public static class Max implements INodeVisitor<Object> {
        AggOpBasicStats stats;

        public Max() {
            stats = new AggOpBasicStats();
        }

        @Override
        public @NotNull String getName() {
            return "max";
        }

        @Override
        public void visit(JsonNode node) {
            stats.visit(node);
        }

        @Override
        public Object get() {
            Unit unit = stats.getUnit();
            return stats.getMin(unit);
        }
    }

    public static class MinMax implements INodeVisitor<String> {
        AggOpBasicStats stats;

        public MinMax() {
            stats = new AggOpBasicStats();
        }

        @Override
        public @NotNull String getName() {
            return "min_max";
        }

        @Override
        public void visit(JsonNode node) {
            stats.visit(node);
        }

        @Override
        public String get() {
            Unit unit = stats.getUnit();
            if (unit == Unit.NOTHING) return null;
            return stats.stringify(unit, stats.getMin(unit)) + " - " + stats.stringify(unit, stats.getMax(unit));
        }
    }

    public static class Sum implements INodeVisitor<Double> {
        AggOpBasicStats stats;

        public Sum() {
            stats = new AggOpBasicStats();
        }

        @Override
        public @NotNull String getName() {
            return "sum";
        }

        @Override
        public void visit(JsonNode node) {
            stats.visit(node);
        }

        @Override
        public Double get() {
            Unit unit = stats.getUnit();
            return stats.getSum(unit);
        }
    }


    public AggOpBasicStats() {
    }

    public void visit(JsonNode node) {
        if (node instanceof JsonNodeValue) {
            JsonNodeValue val = (JsonNodeValue) node;
            Double number = val.asDouble();
            if (number!=null) {
                total += number;
                numCount++;
                if (null == minNum || number < minNum) minNum = number;
                if (null == maxNum || number > maxNum) maxNum = number;
                return;
            } else {
                strCount++;
                // treat as a string
                String str;
                if (val.getValue() instanceof Boolean) {
                    str = ((Boolean)val.getValue()? "true" : "false");
                } else {
                    str = (String) val.getValue();
                }
                if (null==minStr || minStr.compareTo(str)>0) minStr = str;
                if (null==maxStr || maxStr.compareTo(str)<0) maxStr = str;
                return;
            }
        } else if (node instanceof JsonNodeList) {
            JsonNodeList list = (JsonNodeList)node;
            int len = list.childCount();
            totalLength += len;
            lenCount++;
            if (minLength==null || len<minLength) minLength = len;
            if (maxLength==null || len>maxLength) maxLength = len;
            return;
        }
        otherCount++;
    }

    /** Return the "min" value.
     * @return null if we didn't visit anything of that type. **/
    public @Nullable Object getMin(@NotNull Unit unit) {
        switch (unit) {
            case NUMBER:
                return minNum;
            case STRING:
                return minStr;
            case LENGTH:
                return minLength;
            case NOTHING:
                return null;
            default:
                throw new RuntimeException("Bug, missing code for unit " + unit);
        }
    }

    /** Return the "max" value.
     * @return null if we didn't visit anything of that type. **/
    public @Nullable Object getMax(@NotNull Unit unit) {
        switch (unit) {
            case NUMBER:
                return maxNum;
            case STRING:
                return maxStr;
            case LENGTH:
                return maxLength;
            case NOTHING:
                return null;
            default:
                throw new RuntimeException("Bug, missing code for unit " + unit);
        }
    }

    public Unit getUnit() {
        if (maxNum!=null) return Unit.NUMBER;
        if (maxStr!=null) return Unit.STRING;
        if (maxLength!=null) return Unit.LENGTH;
        return Unit.NOTHING;
    }

    public @Nullable String stringify(Unit unit, Object val) {
        if (unit==Unit.NUMBER) return val.toString();
        if (unit==Unit.STRING) return "\"" + val + "\"";
        if (unit==Unit.LENGTH) return val.toString();
        return null;
    }

    /** How many nodes of that type we visited. */
    public int getCount(@NotNull Unit unit) {
        switch (unit) {
            case NUMBER:
                return numCount;
            case STRING:
                return strCount;
            case LENGTH:
                return lenCount;
            case NOTHING: return 0;
            default:
                throw new RuntimeException("Bug, missing code for unit " + unit);
        }
    }

    /** How many nodes we visited. */
    public int getTotalCount() {
        return numCount + strCount + lenCount + otherCount;
    }

    /** Sum of all the numbers we saw (ignores lengths) */
    public Double getSum(@NotNull Unit unit) {
        switch (unit) {
            case NUMBER:
                return total;
            case LENGTH:
                return (double) totalLength;
        }
        return null;
    }

    /** Average value of the numbers we saw. Unaffected by non-number nodes. **/
    public Double getAvg(@NotNull Unit unit) {
        switch (unit) {
            case NUMBER:
                return total/numCount;
            case LENGTH:
                return (double) totalLength/lenCount;
        }
        return null;
    }

    public long getTotalLength() {
        return totalLength;
    }

}
