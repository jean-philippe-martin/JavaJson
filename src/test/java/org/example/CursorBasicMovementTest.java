package org.example;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CursorBasicMovementTest {


    @Test
    public void testDown1() throws Exception {
        JsonNode state = JsonNode.parseJson(
               " {\n"+
               "     \"one\": \"hello\",\n"+
               "     \"two\": \"world\"\n"+
               " }\n");
                
        assertTrue(state.isAtCursor());
        state.cursorDown();
        assertTrue(state.isAtCursor("one"));
        state.cursorDown();
        assertTrue(state.isAtCursor("two"));
    }

    @Test
    public void testDownEntersObjects() throws Exception {
        JsonNode state = JsonNode.parseJson(
               "{ \n"+
               "    \"one\": { \n"+
               "        \"isa\": \"number\", \n"+
               "        \"properties\": { \n"+
               "            \"prime\": \"nope\", \n"+
               "            \"positive\": \"yup\" \n"+
               "        }, \n"+
               "        \"too_far\": \"oops\" \n"+
               "     } \n"+
               "} \n");
                
        assertTrue(state.isAtCursor());
        state.cursorDown();
        assertEquals(".one", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".one.isa", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".one.properties", state.rootInfo.userCursor.toString());
        ArrayList<String> keys = new ArrayList<String>(((JsonNodeMap)state.rootInfo.userCursor.getData()).getKeysInOrder());
        assertEquals("prime", keys.get(0));
        state.cursorDown();
        assertEquals(".one.properties.prime", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".one.properties.positive", state.rootInfo.userCursor.toString());
    }

    @Test
    public void testDownEntersObjects2() throws Exception {
        JsonNode state = JsonNode.parseJson(
               " { \n"+
               "    \"greeting\": \"hello\", \n"+
               "    \"who\": \"world\", \n"+
               "    \"nested\": { \n"+
               "      \"again\": { \n"+
               "        \"depth1\": \"two\" \n"+
               "      }, \n"+
               "      \"depth2\": \"one\", \n"+
               "      \"sub_2\": \"two\" \n"+
               "    }, \n"+
               "    \"conclusion\": \"good bye\" \n"+
               "  } \n");
                 
        assertTrue(state.isAtCursor());
        state.cursorDown();
        assertEquals(".greeting", state.rootInfo.userCursor.toString());
        state.cursorDown();
        state.cursorDown();
        assertEquals(".nested", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".nested.again", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".nested.again.depth1", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".nested.depth2", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".nested.sub_2", state.rootInfo.userCursor.toString());
    }

    @Test
    public void testDownLeavesObjects() throws Exception {
        JsonNode state = JsonNode.parseJson(
                "{\n"+
                "   \"down1\": {\n"+
                "        \"down2\": {\n"+
                "            \"down3\": {\n"+
                "                \"down4\": {\n"+
                "                },\n"+
                "                \"a_string\": \"hello\"\n"+
                "            }\n"+
                "        }\n"+
                "   },\n"+
                "   \"bookend\": \"ok\"\n"+
                " }\n");
        assertTrue(state.isAtCursor());
        state.cursorDown();
        assertEquals(".down1", state.rootInfo.userCursor.toString());
        state.cursorDown();
        state.cursorDown();
        state.cursorDown();
        assertEquals(".down1.down2.down3.down4", state.rootInfo.userCursor.toString());

        state.cursorDown();
        assertEquals(".down1.down2.down3.a_string", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".bookend", state.rootInfo.userCursor.toString());

    }

    @Test
    public void testDownEntersLists() throws Exception {
        JsonNode state = JsonNode.parseJson(
               " {\n"+
               "   \"list\": [\n"+
               "     \"one\",\n"+
               "     \"two\",\n"+
               "     \"three\"\n"+
               "   ]\n"+
               " }\n");
        assertTrue(state.isAtCursor());
        state.cursorDown();
        assertEquals(".list", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[0]", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1]", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[2]", state.rootInfo.userCursor.toString());
    }

    @Test
    public void testDownEntersEverything() throws Exception {
        JsonNode state = JsonNode.parseJson(
               " {\n"+
               "   \"list\": [\n"+
               "     \"one\",\n"+
               "     {\n"+
               "         \"subkey\": \"two\",\n"+
               "         \"subobject\": {\n"+
               "             \"hello\": \"there\",\n"+
               "             \"sublist\": [\n"+
               "                 10,\n"+
               "                 20\n"+
               "             ]\n"+
               "         }\n"+
               "     }\n"+
               "   ]\n"+
               " }\n");
        assertTrue(state.isAtCursor());
        state.cursorDown();
        assertEquals(".list", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[0]", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1]", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1].subkey", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1].subobject", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1].subobject.hello", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1].subobject.sublist", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1].subobject.sublist[0]", state.rootInfo.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1].subobject.sublist[1]", state.rootInfo.userCursor.toString());
    }

    @Test
    public void testDownLeavesLists() throws Exception {
        JsonNode state = JsonNode.parseJson(
               " {\n"+
               "   \"list\": [\n"+
               "     [\n"+
               "         [\n"+
               "             \"one\",\n"+
               "             [\n"+
               "                 []\n"+
               "             ]\n"+
               "         ],\n"+
               "         \"two\"\n"+
               "     ],\n"+
               "     \"three\"\n"+
               "   ]\n"+
               " }\n");
                
        assertTrue(state.isAtCursor());
        state.cursorDown();
        assertEquals(".list", state.rootInfo.userCursor.toString());
        // enter the empty list
        state.cursorDown();
        state.cursorDown();
        state.cursorDown();
        state.cursorDown();
        state.cursorDown();
        assertEquals(".list[0][0][1][0]", state.rootInfo.userCursor.toString());
        // leave the empty list. Now you're at "two"
        state.cursorDown();
        assertEquals(".list[0][1]", state.rootInfo.userCursor.toString());
        // Now at "three"
        state.cursorDown();
        assertEquals(".list[1]", state.rootInfo.userCursor.toString());
        // CursorDown at the end does nothing
        state.cursorDown();
        assertEquals(".list[1]", state.rootInfo.userCursor.toString());
    }

    @Test
    public void testUpLeavesObjects() throws Exception {
        JsonNode state = JsonNode.parseJson(
               " {\n"+
               "   \"numbers\": {\n"+
               "     \"one\": 1,\n"+
               "     \"two\": 2\n"+
               "   }\n"+
               " }\n");
        assertTrue(state.isAtCursor());
        state.cursorDown();
        state.cursorDown();
        state.cursorDown();
        assertEquals(".numbers.two", state.rootInfo.userCursor.toString());
        state.cursorUp();
        assertEquals(".numbers.one", state.rootInfo.userCursor.toString());
        state.cursorUp();
        assertEquals(".numbers", state.rootInfo.userCursor.toString());
        state.cursorUp();
        assertTrue(state.isAtCursor());
        assertEquals("", state.rootInfo.userCursor.toString());
    }

    @Test
    public void testMulti() throws Exception {
        JsonNode state = JsonNode.parseJson(
                "[\n"+
                        "{\"name\": \"Bob\",        \"score\": 35,        \"category\": \"heavy\"},\n"+
                        "{\"name\": \"Alice\",      \"score\": 10,        \"category\": \"light\"}\n"+
                        "]\n");
        state.cursorDownToAllChildren();
        state.cursorUp();

        List<JsonNode> atAnyCursor = state.atAnyCursor();

        assertEquals(1, atAnyCursor.size());

    }

}
