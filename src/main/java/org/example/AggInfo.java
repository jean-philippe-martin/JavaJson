package org.example;

/** Stores aggregate-related information from a node, for "undo" purposes. **/
public class AggInfo {
    final JsonNode aggregate;
    final String aggregateComment;

    public AggInfo(JsonNode node) {
        this.aggregate = node.aggregate;
        this.aggregateComment = node.aggregateComment;
    }

    public void restore(JsonNode recipient) {
        recipient.aggregate = this.aggregate;
        recipient.aggregateComment = this.aggregateComment;
    }

}
