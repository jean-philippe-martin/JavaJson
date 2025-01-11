package org.example;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CursorSortedTest {

    @Test
    public void testCursorInSortedStrings() throws Exception {
        JsonNode state = JsonNode.parseJson(
                "[ "+
                " \"LOUD\", \"quiet\", \"a\", \"SENTENCE\", "+
                " \"sentence\", \"A\" "+
                "] ");
        assertTrue(state.isAtCursor());
        JsonNodeList jnl = (JsonNodeList)state;
        // Case-sensitive sort
        jnl.sort(new Sorter(false, false, false, null, false));

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
        JsonNode state = JsonNode.parseJson(
               " [ \n"+
               "  { \n"+
               "    \"name\": \"Zanzibar\", \n"+
               "    \"score\": 10 \n"+
               "  }, \n"+
               "  { \n"+
               "    \"name\": \"Aaron\", \n"+
               "    \"score\": 20 \n"+
               "  } \n"+
               " ] \n");
        assertTrue(state.isAtCursor());
        JsonNodeList jnl = (JsonNodeList) state;
        jnl.sort(new Sorter(false, false, false, "name", false));

        state.cursorDown();
        Map<String, Object> firstObject = (Map<String, Object>) state.rootInfo.userCursor.getData().getValue();
        assertEquals("Aaron", firstObject.get("name"));

        state.cursorUp();
        jnl.sort(new Sorter(false, false, false, "score", false));

        state.cursorDown();
        firstObject = (Map<String, Object>) state.rootInfo.userCursor.getData().getValue();
        assertEquals("Zanzibar", firstObject.get("name"));
    }

    @Test
    public void testSortingArrays() throws Exception {
        JsonNode state = JsonNode.parseJson(
               " { \n"+
               "     \"maintenance\": [ \n"+
               "       { \n"+
               "         \"date\": \"01/01/2021\", \n"+
               "         \"description\": [\"oil change\", \"oil filter replacement\"] \n"+
               "       }, \n"+
               "       { \n"+
               "         \"date\": \"01/08/2021\", \n"+
               "         \"description\": [\"tire rotation\"] \n"+
               "       } \n"+
               "     ] \n"+
               " } \n");
                
        assertTrue(state.isAtCursor());
        state.cursorDown();
        state.sort(new Sorter(false, true, true, "description", false));
    }

    @Test
    public void testSortingMaps() throws Exception {
        JsonNode state = JsonNode.parseJson(
               "{ \n"+
               "   \"Zanzibar\": 10, \n"+
               "   \"Aaron\": 20, \n"+
               "   \"Charlize\": 123456 \n"+
               "} \n");

        JsonNodeMap jnm = (JsonNodeMap) state;
        jnm.sort(new Sorter(false, false, false, null, true));

        state.cursorDown();
        assertEquals(".Aaron", state.rootInfo.userCursor.toString());

        state.cursorUp();
        jnm.sort(new Sorter(true, false, false, null, false));
        state.cursorDown();
        assertEquals(".Charlize", state.rootInfo.userCursor.toString());
    }

}


