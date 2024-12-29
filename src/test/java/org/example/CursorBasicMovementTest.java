package org.example;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class CursorBasicMovementTest {


    @Test
    public void testDown1() throws Exception {
        JsonNode state = JsonNode.parseJson("""
                {
                    "one": "hello",
                    "two": "world"
                }""");
        assertTrue(state.isAtCursor());
        state.cursorDown();
        assertTrue(state.isAtCursor("one"));
        state.cursorDown();
        assertTrue(state.isAtCursor("two"));
    }

    @Test
    public void testDownEntersObjects() throws Exception {
        JsonNode state = JsonNode.parseJson("""
                {
                    "one": {
                        "isa": "number",
                        "properties": {
                            "prime": "nope",
                            "positive": "yup"
                        },
                        "too_far": "oops"
                     }
                }""");
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
        JsonNode state = JsonNode.parseJson("""
                {
                   "greeting": "hello",
                   "who": "world",
                   "nested": {
                     "again": {
                       "depth1": "two"
                     },
                     "depth2": "one",
                     "sub_2": "two"
                   },
                   "conclusion": "good bye"
                 }""");
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
        JsonNode state = JsonNode.parseJson("""
                {
                   "down1": {
                        "down2": {
                            "down3": {
                                "down4": {
                                },
                                "a_string": "hello"
                            }
                        }
                   },
                   "bookend": "ok"   
                 }""");
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
        JsonNode state = JsonNode.parseJson("""
                {
                  "list": [
                    "one",
                    "two",
                    "three"
                  ]
                }""");
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
        JsonNode state = JsonNode.parseJson("""
                {
                  "list": [
                    "one",
                    {
                        "subkey": "two",
                        "subobject": {
                            "hello": "there",
                            "sublist": [
                                10,
                                20
                            ]
                        }
                    }
                  ]
                }""");
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
        JsonNode state = JsonNode.parseJson("""
                {
                  "list": [
                    [
                        [
                            "one",
                            [
                                []
                            ]
                        ],
                        "two"
                    ],
                    "three"
                  ]
                }""");
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
        JsonNode state = JsonNode.parseJson("""
                {
                  "numbers": {
                    "one": 1,
                    "two": 2
                  }
                }""");
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

}
