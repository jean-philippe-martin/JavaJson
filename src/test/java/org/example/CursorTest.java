package org.example;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;
public class CursorTest {

    @Test
    public void testCursorToString1() {
        Cursor root = new Cursor();
        assertEquals("", root.toString());
        Cursor foobar = root.enterKey("foo").enterKey("bar");
        assertEquals(".foo.bar", foobar.toString());
    }

    @Test
    public void testCursorToString2() {
        Cursor root = new Cursor();
        assertEquals("", root.toString());
        Cursor bar = root.enterIndex(0).enterKey("bar").enterIndex(2);
        assertEquals("[0].bar[2]", bar.toString());
    }

    @Test
    public void testDown1() throws Exception {
        JsonState state = JsonState.parseJson("""
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
        JsonState state = JsonState.parseJson("""
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
        assertEquals(".one", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".one.isa", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".one.properties", state.userCursor.toString());
        ArrayList<String> keys = new ArrayList<String>(((JsonStateMap)state.userCursor.getData()).getKeysInOrder());
        assertEquals("prime", keys.get(0));
        state.cursorDown();
        assertEquals(".one.properties.prime", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".one.properties.positive", state.userCursor.toString());
    }

    @Test
    public void testDownEntersObjects2() throws Exception {
        JsonState state = JsonState.parseJson("""
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
        assertEquals(".greeting", state.userCursor.toString());
        state.cursorDown();
        state.cursorDown();
        assertEquals(".nested", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".nested.again", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".nested.again.depth1", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".nested.depth2", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".nested.sub_2", state.userCursor.toString());
    }

    @Test
    public void testDownLeavesObjects() throws Exception {
        JsonState state = JsonState.parseJson("""
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
        assertEquals(".down1", state.userCursor.toString());
        state.cursorDown();
        state.cursorDown();
        state.cursorDown();
        assertEquals(".down1.down2.down3.down4", state.userCursor.toString());

        state.cursorDown();
        assertEquals(".down1.down2.down3.a_string", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".bookend", state.userCursor.toString());

    }

    @Test
    public void testDownEntersLists() throws Exception {
        JsonState state = JsonState.parseJson("""
                {
                  "list": [
                    "one",
                    "two",
                    "three"
                  ]
                }""");
        assertTrue(state.isAtCursor());
        state.cursorDown();
        assertEquals(".list", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[0]", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1]", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[2]", state.userCursor.toString());
    }

    @Test
    public void testDownEntersEverything() throws Exception {
        JsonState state = JsonState.parseJson("""
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
        assertEquals(".list", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[0]", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1]", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1].subkey", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1].subobject", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1].subobject.hello", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1].subobject.sublist", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1].subobject.sublist[0]", state.userCursor.toString());
        state.cursorDown();
        assertEquals(".list[1].subobject.sublist[1]", state.userCursor.toString());
    }

    @Test
    public void testDownLeavesLists() throws Exception {
        JsonState state = JsonState.parseJson("""
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
        assertEquals(".list", state.userCursor.toString());
        // enter the empty list
        state.cursorDown();
        state.cursorDown();
        state.cursorDown();
        state.cursorDown();
        state.cursorDown();
        assertEquals(".list[0][0][1][0]", state.userCursor.toString());
        // leave the empty list. Now you're at "two"
        state.cursorDown();
        assertEquals(".list[0][1]", state.userCursor.toString());
        // Now at "three"
        state.cursorDown();
        assertEquals(".list[1]", state.userCursor.toString());
        // CursorDown at the end does nothing
        state.cursorDown();
        assertEquals(".list[1]", state.userCursor.toString());
    }

    @Test
    public void testUpLeavesObjects() throws Exception {
        JsonState state = JsonState.parseJson("""
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
        assertEquals(".numbers.two", state.userCursor.toString());
        state.cursorUp();
        assertEquals(".numbers.one", state.userCursor.toString());
        state.cursorUp();
        assertEquals(".numbers", state.userCursor.toString());
        state.cursorUp();
        assertTrue(state.isAtCursor());
        assertEquals("", state.userCursor.toString());
    }
}