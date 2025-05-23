package org.example;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An "AggOp" is a sort of visitor: you show it a bunch of JsonNodes, and then you
 * get some basic statistics out.
 *
 */
public class AggOpBasicStats {
    Double minNum;
    Double maxNum;
    String minStr;
    String maxStr;
    Integer minLength;
    Integer maxLength;
    Double total = (double) 0;
    long totalLength = 0;
    int numCount = 0;
    int strCount = 0;
    int lenCount = 0;
    int otherCount = 0;

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
        public void init() {
            stats.init();
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
        public void init() {
            stats.init();
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
        Unit unit;

        public MinMax() {
            stats = new AggOpBasicStats();
        }

        @Override
        public void init() {
            stats.init();
        }

        @Override
        public @NotNull String getName() {
            if (unit==Unit.LENGTH) {
                return "min_max_length";
            }
            return "min_max";
        }

        @Override
        public void visit(JsonNode node) {
            stats.visit(node);
        }

        @Override
        public String get() {
            unit = stats.getUnit();
            if (unit == Unit.NOTHING) return null;
            if (unit==Unit.LENGTH) {
                // special case: remove decimal point
                return stats.stringify(unit, (Integer)stats.getMin(unit)) + " - " + stats.stringify(unit, (Integer)stats.getMax(unit));
            } else {
                return stats.stringify(unit, stats.getMin(unit)) + " - " + stats.stringify(unit, stats.getMax(unit));
            }
        }
    }

    public static class Sum implements INodeVisitor<Object> {
        AggOpBasicStats stats;
        Unit unit = Unit.NOTHING;

        public Sum() {
            stats = new AggOpBasicStats();
        }

        @Override
        public void init() {
            stats.init();
        }

        @Override
        public @NotNull String getName() {
            if (unit==Unit.LENGTH) return "sum_length";
            return "sum";
        }

        @Override
        public void visit(JsonNode node) {
            stats.visit(node);
        }

        @Override
        public Object get() {
            unit = stats.getUnit();
            if (unit==Unit.LENGTH) {
                return (Integer) stats.getSum(unit).intValue();
            } else {
                return stats.getSum(unit);
            }
        }

        /** only valid after calling "get" */
        public Unit getUnit() {
            return unit;
        }
    }

    public static class Avg implements INodeVisitor<Double> {
        AggOpBasicStats stats;
        Unit unit = Unit.NOTHING;

        public Avg() {
            stats = new AggOpBasicStats();
        }

        @Override
        public void init() {
            stats.init();
        }

        @Override
        public @NotNull String getName() {
            if (unit==Unit.LENGTH) return "avg_length";
            return "avg";
        }

        @Override
        public void visit(JsonNode node) {
            stats.visit(node);
        }

        @Override
        public Double get() {
            unit = stats.getUnit();
            if (unit==Unit.LENGTH) {
                return stats.getSum(unit) / stats.lenCount;
            }
            if (unit==Unit.NUMBER) {
                return stats.getSum(unit) / stats.numCount;
            }
            return null;
        }

        /** only valid after calling "get" */
        public Unit getUnit() {
            return unit;
        }
    }


    public AggOpBasicStats() {
        init();
    }

    public void init() {
        minNum = null;
        maxNum = null;
        minStr = null;
        maxStr = null;
        minLength = null;
        maxLength = null;
        totalLength = 0;
        numCount = 0;
        strCount = 0;
        lenCount = 0;
        otherCount = 0;
        total = (double) 0;
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
