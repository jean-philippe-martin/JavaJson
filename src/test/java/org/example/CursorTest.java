package org.example;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void testMultiCursorIntoMap() throws Exception {
        JsonNode state = JsonNode.parseJson("""
                  {
                  "players": {
                    "Alex": {
                      "score": 10
                    },
                    "Bob": {
                      "score": 35
                    }
                  }
                }""");
        assertTrue(state.isAtCursor());
        state.cursorDown();
        JsonNodeMap players = (JsonNodeMap) state.rootInfo.userCursor.getData();
        assertTrue(players.isAtCursor());
        assertTrue(state.isAtCursor("players"));
        assertEquals(".players", state.rootInfo.userCursor.toString());
        state.cursorDownToAllChildren();
        // Now we have a primary cursor at "Alex" and a secondary cursor at "Bob".
        assertEquals(".players.Alex", state.rootInfo.userCursor.toString());
        JsonNode alex = players.getChild("Alex");
        JsonNode bob = players.getChild("Bob");
        assertTrue(alex.isAtCursor());
        assertTrue(bob.isAtCursor());
        List<JsonNode> selected = state.atAnyCursor();
        assertEquals(2, selected.size());
        assertEquals(".players.Alex", selected.get(0).whereIAm.toString());
        assertEquals(".players.Bob", selected.get(1).whereIAm.toString());
        // Now the cursors are at "score"
        state.cursorDown();
        assertEquals(".players.Alex.score", state.rootInfo.userCursor.toString());
        assertTrue(alex.isAtCursor("score"));
        assertTrue(bob.isAtCursor("score"));
    }

    @Test
    public void testMultiCursorIntoArray() throws Exception {
        JsonNode state = JsonNode.parseJson("""
                {
                  "players": [
                    { "name": "Alex",
                      "score": 10
                    },
                    { "name": "Bob",
                      "score": 35
                    }
                  ]
                }""");
        assertTrue(state.isAtCursor());
        state.cursorDown();
        JsonNodeList players = (JsonNodeList) state.rootInfo.userCursor.getData();
        assertTrue(players.isAtCursor());
        assertTrue(state.isAtCursor("players"));
        assertEquals(".players", state.rootInfo.userCursor.toString());
        state.cursorDownToAllChildren();
        // Now we have a primary cursor at "Alex" and a secondary cursor at "Bob".
        assertEquals(".players[0]", state.rootInfo.userCursor.toString());
        JsonNode alex = players.get(0);
        JsonNode bob = players.get(1);
        assertTrue(alex.isAtCursor());
        assertTrue(bob.isAtCursor());
        List<JsonNode> selected = state.atAnyCursor();
        assertEquals(2, selected.size());
        assertEquals(".players[0]", selected.get(0).whereIAm.toString());
        assertEquals(".players[1]", selected.get(1).whereIAm.toString());
        // Now the cursors are at "score"
        state.cursorDown();
        assertEquals(".players[0].name", state.rootInfo.userCursor.toString());
        assertTrue(alex.isAtCursor("name"));
        assertTrue(bob.isAtCursor("name"));
    }

    @Test
    public void testMultiCursorIntNestedMap() throws Exception {
        JsonNode state = JsonNode.parseJson("""
                {
                  "players": {
                    "Alex": {
                      "score": 10,
                      "stuff": {
                        "foo": "go on",
                        "bar": 12
                      }
                    },
                    "Bob": {
                      "score": 35,
                      "stuff": {
                        "foo": "go on",
                        "bar": 12
                      }
                    }
                  }
                }""");
        assertTrue(state.isAtCursor());
        state.cursorDown();
        assertEquals(".players", state.rootInfo.userCursor.toString());
        state.cursorDownToAllChildren();
        // now we just move the cursor a bit and make sure we don't crash
        for (int i=0; i<20; i++) {
            state.cursorDown();
        }
        for (int i=0; i<20; i++) {
            state.cursorUp();
        }
    }

    @Test
    public void testMultiCursorSameTypeDifferentDepth() throws Exception {
        JsonNode json = JsonNode.parseJson("""
                {
                  "players": [
                    {
                      "name": "Alice"
                    },
                    {
                      "name": "Bob"
                    },
                    {
                      "name": "Charlie",
                      "pet": {
                        "name": "kittycat",
                        "likes": [
                          "pats",
                          "tuna"
                        ]
                      }
                    }
                  ]
                }
                """);
        // Point to "players"
        json.cursorDown();
        JsonNodeList players = (JsonNodeList) json.atCursor();
        // Select all players
        json.cursorDownToAllChildren();
        // All players are selected
        assertTrue(players.get(0).isAtCursor());
        assertTrue(players.get(1).isAtCursor());
        assertTrue(players.get(2).isAtCursor());
        // "Tuna" is not selected
        JsonNodeList likes = (JsonNodeList) ((JsonNodeMap)(((JsonNodeMap) players.get(2)).getChild("pet"))).getChild("likes");
        assertFalse(likes.get(0).isAtCursor());
        assertFalse(likes.get(1).isAtCursor());
    }

    @Test
    public void testMultiCursorPinAndFold() throws Exception {
        JsonNode json = JsonNode.parseJson("""
                [
                  {
                    "name": "Fizbuzz Elementary",
                    "address": "136 Learning Lane",
                    "founded": 1942
                  },
                  {
                    "name": "Mendingorium",
                    "address": "631 Mending Lane"
                  },
                  {
                    "nickname": "The secret place"
                  }
                ]
                """);
        // Fold every object
        json.cursorDownToAllChildren();
        json.cursorDown();
        assertEquals("[0].name", json.rootInfo.userCursor.toString());
        json.setPinnedAtCursors(true);
        json.setFoldedAtCursors(true);
        json.cursorUp();
    }

    @Test
    public void testCursorDownThroughVisible() throws Exception {
        JsonNode json = JsonNode.parseJson("""
                {
                  "school": {
                    "name": "Fizbuzz Elementary",
                    "address": "136 Learning Lane",
                    "founded": 1942
                  },
                  "hospital": {
                    "name": "Mendingorium",
                    "address": "631 Mending Lane"
                  }
                }
                """);
        // Point to "school.address"
        json.cursorDown();
        json.cursorDown();
        json.cursorDown();
        assertEquals(".school.address", json.rootInfo.userCursor.toString());
        // Pin "school.address"
        json.setPinnedAtCursors(true);
        json.cursorUp();
        json.cursorUp();
        // Fold "school"
        json.setFoldedAtCursors(true);

        // Now, going down one should go to school.address
        json.cursorDown();
        assertEquals(".school.address", json.rootInfo.userCursor.toString());
        // And once more should go to hospital
        json.cursorDown();
        assertEquals(".hospital", json.rootInfo.userCursor.toString());

        // Going up should skip to visible too.
        json.cursorUp();
        assertEquals(".school.address", json.rootInfo.userCursor.toString());
        json.cursorUp();
        assertEquals(".school", json.rootInfo.userCursor.toString());
    }

    @Test
    public void testDownThroughFolded() throws Exception {
        JsonNode json = JsonNode.parseJson("""
                [
                  {
                    "name": "Fizbuzz Elementary",
                    "address": "136 Learning Lane",
                    "founded": 1942
                  },
                  {
                    "name": "Mendingorium",
                    "address": "631 Mending Lane"
                  }
                ]
                """);
        // Fold every object
        json.cursorDown();
        assertEquals("[0]", json.rootInfo.userCursor.toString());
        json.setFoldedAtCursors(true);
        json.cursorDown();
        assertEquals("[1]", json.rootInfo.userCursor.toString());
        json.setFoldedAtCursors(true);
        // cursor up/down through them
        json.cursorUp();
        assertEquals("[0]", json.rootInfo.userCursor.toString());
        json.cursorDown();
        assertEquals("[1]", json.rootInfo.userCursor.toString());
    }

    @Test
    public void testNextCursorForFork() throws Exception {
        JsonNode state = JsonNode.parseJson("""
                {
                  "players": [
                    { "name": "Alex",
                      "score": 10
                    },
                    { "name": "Bob",
                      "score": 35
                    },
                    { "name": "Charlie",
                      "score": 70
                    }
                  ]
                }""");
        assertTrue(state.isAtCursor());
        state.cursorDown();
        JsonNodeList players = (JsonNodeList) state.rootInfo.userCursor.getData();
        assertEquals(".players", state.rootInfo.userCursor.toString());
        state.cursorDownToAllChildren();
        // Now we have a primary cursor at "Alex" and secondary cursors at "Bob" and "Charlie".
        assertEquals(".players[0]", state.rootInfo.userCursor.toString());
        List<JsonNode> selected = state.atAnyCursor();
        assertEquals(3, selected.size());
        assertEquals(".players[0]", selected.get(0).whereIAm.toString());
        assertEquals(".players[1]", selected.get(1).whereIAm.toString());
        assertEquals(".players[2]", selected.get(2).whereIAm.toString());

        // Now we move to the next cursor
        state.cursorNextCursor();

        assertEquals(".players[1]", state.rootInfo.userCursor.toString());
        selected = state.atAnyCursor();
        assertEquals(3, selected.size());
        assertEquals(".players[1]", selected.get(0).whereIAm.toString());
        assertEquals(".players[0]", selected.get(1).whereIAm.toString());
        assertEquals(".players[2]", selected.get(2).whereIAm.toString());

        // Again.
        state.cursorNextCursor();

        assertEquals(".players[2]", state.rootInfo.userCursor.toString());
        selected = state.atAnyCursor();
        assertEquals(3, selected.size());
        assertEquals(".players[2]", selected.get(0).whereIAm.toString());
        assertEquals(".players[0]", selected.get(1).whereIAm.toString());
        assertEquals(".players[1]", selected.get(2).whereIAm.toString());
    }

    @Test
    public void testNextCursorForFork2() throws Exception {
        JsonNode state = JsonNode.parseJson("""
                {
                  "players": [
                    { "name": "Alex",
                      "score": 10
                    },
                    { "name": "Bob",
                      "score": 35
                    },
                    { "name": "Charlie",
                      "score": 70
                    }
                  ]
                }""");
        assertTrue(state.isAtCursor());
        state.cursorDown();
        JsonNodeList players = (JsonNodeList) state.rootInfo.userCursor.getData();
        assertEquals(".players", state.rootInfo.userCursor.toString());
        state.cursorDownToAllChildren();
        state.cursorDown();
        // Now we have a primary cursor at "Alex" and secondary cursors at "Bob" and "Charlie".
        assertEquals(".players[0].name", state.rootInfo.userCursor.toString());
        List<JsonNode> selected = state.atAnyCursor();
        assertEquals(3, selected.size());
        assertEquals(".players[0].name", selected.get(0).whereIAm.toString());
        assertEquals(".players[1].name", selected.get(1).whereIAm.toString());
        assertEquals(".players[2].name", selected.get(2).whereIAm.toString());

        // Now we move to the next cursor
        state.cursorNextCursor();

        assertEquals(".players[1].name", state.rootInfo.userCursor.toString());
        selected = state.atAnyCursor();
        assertEquals(3, selected.size());
        assertEquals(".players[1].name", selected.get(0).whereIAm.toString());
        assertEquals(".players[0].name", selected.get(1).whereIAm.toString());
        assertEquals(".players[2].name", selected.get(2).whereIAm.toString());

        // Again.
        state.cursorNextCursor();

        assertEquals(".players[2].name", state.rootInfo.userCursor.toString());
        selected = state.atAnyCursor();
        assertEquals(3, selected.size());
        assertEquals(".players[2].name", selected.get(0).whereIAm.toString());
        assertEquals(".players[0].name", selected.get(1).whereIAm.toString());
        assertEquals(".players[1].name", selected.get(2).whereIAm.toString());
    }

    @Test
    public void testNextCursorForForkTricky() throws Exception {
        JsonNode state = JsonNode.parseJson("""
                {
                  "players": [
                    { "name": "Alex",
                      "score": 10
                    },
                    { "nickname": "Little Bobby Tables",
                      "score": 35
                    },
                    { "name": "Charlie",
                      "score": 70
                    }
                  ]
                }""");
        assertTrue(state.isAtCursor());
        state.cursorDown();
        JsonNodeList players = (JsonNodeList) state.rootInfo.userCursor.getData();
        assertEquals(".players", state.rootInfo.userCursor.toString());
        state.cursorDownToAllChildren();
        state.cursorDown();
        // Now we have a primary cursor at "Alex" and secondary cursors at "Charlie".
        assertEquals(".players[0].name", state.rootInfo.userCursor.toString());
        List<JsonNode> selected = state.atAnyCursor();
        assertEquals(2, selected.size());
        assertEquals(".players[0].name", selected.get(0).whereIAm.toString());
        assertEquals(".players[2].name", selected.get(1).whereIAm.toString());

        // Now we move to the next cursor
        state.cursorNextCursor();

        assertEquals(".players[2].name", state.rootInfo.userCursor.toString());
        selected = state.atAnyCursor();
        assertEquals(2, selected.size());
        assertEquals(".players[2].name", selected.get(0).whereIAm.toString());
        assertEquals(".players[0].name", selected.get(1).whereIAm.toString());
    }
}