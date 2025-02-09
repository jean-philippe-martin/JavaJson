package org.example;

/**
 * Builds a JsonNode. We use this to create trees from the bottom
 * when we have JsonNodes that we want to relink in some new way.
 */
public interface JsonNodeBuilder {
    /**
     * Create this JsonNode and all its children,
     * recursively.
     * To make it root, pass null for the parent and a new cursor.
     */
    JsonNode build(JsonNode parent, Cursor curToMe);
}
