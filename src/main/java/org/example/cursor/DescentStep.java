package org.example.cursor;

import org.example.Cursor;
import org.example.JsonNode;

public abstract class DescentStep {
    // Apply this step to the given node (must have the correct type).
    public abstract JsonNode apply(JsonNode node);

    public abstract Cursor.DescentStyle getDescentStyle();
}
