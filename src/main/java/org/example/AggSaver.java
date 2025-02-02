package org.example;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/** holds the cursors and previous aggregation states. **/
public class AggSaver {

    public final JsonNode beforeRoot;
    public final ArrayList<Cursor> cursors;
    public final ArrayList<AggInfo> aggBefore;


    /** save. */
    public AggSaver(JsonNode root) {
        this.beforeRoot = root.rootInfo.root;
        this.cursors = new ArrayList<>();
        this.aggBefore = new ArrayList<>();

        // Save the past
        for (JsonNode node : beforeRoot.atAnyCursor()) {
            cursors.add(node.asCursor());
            aggBefore.add(new AggInfo(node));
        }
    }

    /** restore. Returns the root. */
    public @NotNull JsonNode restore() {
        for (int i=0; i<cursors.size(); i++) {
            JsonNode node = cursors.get(i).getData();
            aggBefore.get(i).restore(node);
        }
        return beforeRoot;
    }

}
