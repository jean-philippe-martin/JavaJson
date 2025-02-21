package org.example;

import org.junit.Test;

import java.util.ArrayList;
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
        jnl.sort(new Sorter(false, false, false, new ArrayList<String>(), false));

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
        var fields = new ArrayList<String>();
        fields.add("name");
        jnl.sort(new Sorter(false, false, false, fields, false));

        state.cursorDown();
        Map<String, Object> firstObject = (Map<String, Object>) state.rootInfo.userCursor.getData().getValue();
        assertEquals("Aaron", firstObject.get("name"));

        state.cursorUp();
        fields = new ArrayList<String>();
        fields.add("score");
        jnl.sort(new Sorter(false, false, false, fields, false));

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
        var fields = new ArrayList<String>();
        fields.add("description");

        state.sort(new Sorter(false, true, true, fields, false));
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
        jnm.sort(new Sorter(false, false, false, new ArrayList<String>(), true));

        state.cursorDown();
        assertEquals(".Aaron", state.rootInfo.userCursor.toString());

        state.cursorUp();
        jnm.sort(new Sorter(true, false, false, new ArrayList<String>(), false));
        state.cursorDown();
        assertEquals(".Charlize", state.rootInfo.userCursor.toString());
    }

    @Test
    public void testSortByField() throws Exception {
        JsonNode state = JsonNode.parseJson(
                "[\n"+
                "{\"name\": \"Bob\",        \"score\": 35,        \"category\": \"heavy\"},\n"+
                "{\"name\": \"Alice\",      \"score\": 10,        \"category\": \"light\"}\n"+
                "]\n");
        state.cursorDownToAllChildren();
        state.cursorUp();

        // Sort in the same way that the main program does it
        OperationList undoLog = new OperationList();
        var fields = new ArrayList<String>();
        fields.add("name");
        Sorter s = new Sorter(false, false, true, fields, false);
        Operation sort = new Operation.Sort(state, s);
        state = undoLog.run(sort);

        // Key order should remain unchanged
        state.cursorDown();
        // [1] because we keep the original numbers: Alice is [1], Bob is [0]
        assertEquals("[1]", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals("[1].name", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals("[1].score", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals("[1].category", state.rootInfo.userCursor.toString());


    }

}


