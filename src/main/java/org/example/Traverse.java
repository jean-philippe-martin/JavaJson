package org.example;

/** Move a visitor along */
public class Traverse {

    public static void values(JsonNode node, INodeVisitor visitor) {
        for (JsonNodeIterator it = node.iterateChildren(true); it!=null; it=it.next()) {
            JsonNode kid = it.get();
            if (kid.isSynthetic()) continue;
            visitor.visit(kid);
        }
    }

}
