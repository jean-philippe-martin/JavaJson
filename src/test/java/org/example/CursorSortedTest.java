package org.example;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CursorSortedTest {

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
        jnl.sort(new Sorter(false, false, false, null));

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

    @Test
    public void testCursorInSortedMaps() throws Exception {
        JsonNode state = JsonNode.parseJson("""
                [
                 {
                   "name": "Zanzibar",
                   "score": 10
                 },
                 {
                   "name": "Aaron",
                   "score": 20
                 }
                ]""");
        assertTrue(state.isAtCursor());
        JsonNodeList jnl = (JsonNodeList) state;
        jnl.sort(new Sorter(false, false, false, "name"));

        state.cursorDown();
        Map<String, Object> firstObject = (Map<String, Object>) state.rootInfo.userCursor.getData().getValue();
        assertEquals("Aaron", firstObject.get("name"));

        state.cursorUp();
        jnl.sort(new Sorter(false, false, false, "score"));

        state.cursorDown();
        firstObject = (Map<String, Object>) state.rootInfo.userCursor.getData().getValue();
        assertEquals("Zanzibar", firstObject.get("name"));
    }

    }


