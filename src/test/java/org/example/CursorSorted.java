package org.example;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CursorSorted {

    @Test
    public void testCursorInSortedStrings() throws Exception {
        JsonNode state = JsonNode.parseJson("""
                [
                 "LOUD", "quiet", "a", "SENTENCE",
                 "sentence", "A"
                ]""");
        assertTrue(state.isAtCursor());
        JsonNodeList jnl = (JsonNodeList)state;
        // Case-sensitive sort
        jnl.sort(new Sorter(false, false, false));

        state.cursorDown();
        assertEquals("A", state.rootInfo.userCursor.getData().getValue());

        state.cursorDown();
        assertEquals("LOUD", state.rootInfo.userCursor.getData().getValue());

        state.cursorDown();
        assertEquals("SENTENCE", state.rootInfo.userCursor.getData().getValue());

        state.cursorDown();
        assertEquals("a", state.rootInfo.userCursor.getData().getValue());

        state.cursorDown();
        assertEquals("quiet", state.rootInfo.userCursor.getData().getValue());

        state.cursorDown();
        assertEquals("sentence", state.rootInfo.userCursor.getData().getValue());
    }

}


