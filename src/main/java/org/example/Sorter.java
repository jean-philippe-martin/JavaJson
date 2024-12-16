package org.example;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class Sorter implements Comparator<Object> {

    private boolean reverse;
    private boolean ignoreCase;
    private boolean parseNumbers;
    private Map<String, ArrayList<Object>> numberified;

    public Sorter(boolean reverse, boolean ignoreCase, boolean parseNumbers) {
        this.reverse = reverse;
        this.ignoreCase = ignoreCase;
        this.parseNumbers = parseNumbers;
        if (parseNumbers) numberified = new HashMap<>();
    }

    // Removes intermediate sorting data
    public void pack() {}

    @Override
    public int compare(Object o1, Object o2) {
        if (!parseNumbers) return compareObjects(o1, o2);
        ArrayList<Object> l1 = translate(o1);
        ArrayList<Object> l2 = translate(o2);
        if (l1.isEmpty()) {
            if (l2.isEmpty()) return 0;
            // empty list comes first
            return -1;
        }
        for (int i=0; i<l1.size(); i++) {
            // equal up to this point, shorter wins.
            if (l2.size()<=i) return -1;
            int c = compareObjects(l1.get(i), l2.get(i));
            if (c!=0) return c;
        }
        if (l1.size()==l2.size()) return 0;
        // l2 is longer, shorter wins.
        return -1;
    }

    public int compareObjects(Object o1, Object o2) {
        if (o1 instanceof String s1 && o2 instanceof String s2) {
            if (ignoreCase) {
                s1 = s1.toUpperCase();
                s2 = s2.toUpperCase();
            }
            int ret = s1.compareTo(s2);
            if (reverse) ret = -ret;
            return ret;
        }
        if (o1 instanceof String && (o2 instanceof Integer || o2 instanceof Double)) {
            return reverse?-1:1;
        }
        if ((o1 instanceof Integer || o1 instanceof Double) && o2 instanceof String) {
            return reverse?1:-1;
        }
        if (o1 instanceof Integer i1 && o2 instanceof Integer i2) {
            int ret = i1.compareTo(i2);
            if (reverse) ret = -ret;
            return ret;
        }
        if (o1 instanceof Double d1 && o2 instanceof Double d2) {
            int ret = d1.compareTo(d2);
            if (reverse) ret = -ret;
            return ret;
        }
        Double d1 = null;
        if (o1 instanceof Double) {
            d1 = (Double)o1;
        } else if (o1 instanceof Integer i1) {
            d1 = Double.valueOf(i1);
        } else {
            // Don't know how to handle this type
            return 0;
        }
        Double d2 = null;
        if (o2 instanceof Double) {
            d2 = (Double)o2;
        } else if (o2 instanceof Integer i2) {
            d2 = Double.valueOf(i2);
        } else {
            // Don't know how to handle this type
            return 0;
        }
        int ret = d1.compareTo(d2);
        if (reverse) ret=-ret;
        return ret;
    }

    // Accessible for tests.
    /** "Untitled (2)" -> ["Untitled (", 2.0, ")"] **/
    ArrayList<Object> translate(Object o) {
        ArrayList<Object> ret = new ArrayList<>();
        if (o instanceof Double d) {
            ret.add(d);
            return ret;
        }
        if (o instanceof Integer i) {
            ret.add(Double.valueOf(i));
            return ret;
        }
        if (o instanceof String s) {
            if (numberified.containsKey(s)) return numberified.get(s);
            char[] chars = new char[s.length()];
            s.getChars(0, s.length(), chars, 0);
            if (chars.length==0) {
                return ret;
            }
            StringBuilder stringBuilder = new StringBuilder();
            double numberBuilder = 0;
            boolean buildingANumber = chars[0] >= '0' && chars[0] <= '9';
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c >= '0' && c <= '9') {
                    if (buildingANumber) {
                        numberBuilder = numberBuilder * 10.0 + (c - '0');
                    } else {
                        ret.add(stringBuilder.toString());
                        buildingANumber = true;
                        numberBuilder = (c - '0');
                    }
                } else {
                    if (buildingANumber) {
                        ret.add(numberBuilder);
                        buildingANumber = false;
                        stringBuilder = new StringBuilder();
                    }
                    stringBuilder.append(c);
                }
            }
            if (buildingANumber) {
                ret.add(numberBuilder);
            } else {
                ret.add(stringBuilder.toString());
            }
            return ret;
        }
        if (o instanceof Map) {
            // we're not set up to compare maps... return an empty list.
            ret.add(o.toString());
            return ret;
        }
        // If we get here it means we didn't recognize the type
        throw new RuntimeException("Unrecognized type for " + o.toString());
    }
}
