package org.example;

import org.example.cursor.FindCursor;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
    public void testMultiCursorIntoMap() throws Exception {
        JsonNode state = JsonNode.parseJson(
               "   {\n"+
               "   \"players\": {\n"+
               "     \"Alex\": {\n"+
               "       \"score\": 10\n"+
               "     },\n"+
               "     \"Bob\": {\n"+
               "       \"score\": 35\n"+
               "     }\n"+
               "   }\n"+
               " }\n");
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
        JsonNode state = JsonNode.parseJson(
               " {\n"+
               "   \"players\": [\n"+
               "     { \"name\": \"Alex\",\n"+
               "       \"score\": 10\n"+
               "     },\n"+
               "     { \"name\": \"Bob\",\n"+
               "       \"score\": 35\n"+
               "     }\n"+
               "   ]\n"+
               " }\n");
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
    public void testMultiCursorIntoRootArray() throws Exception {
        JsonNode state = JsonNode.parseJson(
               " [\n"+
               "   {\n"+
               "     \"color\": \"red\"\n"+
               "   },\n"+
               "   {\n"+
               "     \"color\": \"blue\"\n"+
               "   }\n"+
               " ]\n");
        JsonNodeList stateArray = (JsonNodeList)state;
        assertTrue(state.isAtCursor());

        state.cursorDownToAllChildren();

        assertEquals("[0]", state.rootInfo.userCursor.toString());
        List<JsonNode> selected = state.atAnyCursor();
        assertEquals(2, selected.size());
        assertEquals("[0]", selected.get(0).whereIAm.toString());
        assertEquals("[1]", selected.get(1).whereIAm.toString());
        assertTrue(stateArray.get(0).isAtCursor());
        assertTrue(stateArray.get(1).isAtCursor());
        state.cursorDown();

        selected = state.atAnyCursor();
        assertEquals(2, selected.size());
        assertEquals("[0].color", selected.get(0).whereIAm.toString());
        assertEquals("[1].color", selected.get(1).whereIAm.toString());
        assertFalse(stateArray.get(0).isAtCursor());
        assertFalse(stateArray.get(1).isAtCursor());
    }

    @Test
    public void testMultiCursorIntNestedMap() throws Exception {
        JsonNode state = JsonNode.parseJson(
               " {\n"+
               "   \"players\": {\n"+
               "     \"Alex\": {\n"+
               "       \"score\": 10,\n"+
               "       \"stuff\": {\n"+
               "         \"foo\": \"go on\",\n"+
               "         \"bar\": 12\n"+
               "       }\n"+
               "     },\n"+
               "     \"Bob\": {\n"+
               "       \"score\": 35,\n"+
               "       \"stuff\": {\n"+
               "         \"foo\": \"go on\",\n"+
               "         \"bar\": 12\n"+
               "       }\n"+
               "     }\n"+
               "   }\n"+
               " }\n");
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
        JsonNode json = JsonNode.parseJson(
               " {\n"+
               "   \"players\": [\n"+
               "     {\n"+
               "       \"name\": \"Alice\"\n"+
               "     },\n"+
               "     {\n"+
               "       \"name\": \"Bob\"\n"+
               "     },\n"+
               "     {\n"+
               "       \"name\": \"Charlie\",\n"+
               "       \"pet\": {\n"+
               "         \"name\": \"kittycat\",\n"+
               "         \"likes\": [\n"+
               "           \"pats\",\n"+
               "           \"tuna\"\n"+
               "         ]\n"+
               "       }\n"+
               "     }\n"+
               "   ]\n"+
               " }\n");
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
        JsonNode json = JsonNode.parseJson(
               " [\n"+
               "   {\n"+
               "     \"name\": \"Fizbuzz Elementary\",\n"+
               "     \"address\": \"136 Learning Lane\",\n"+
               "     \"founded\": 1942\n"+
               "   },\n"+
               "   {\n"+
               "     \"name\": \"Mendingorium\",\n"+
               "     \"address\": \"631 Mending Lane\"\n"+
               "   },\n"+
               "   {\n"+
               "     \"nickname\": \"The secret place\"\n"+
               "   }\n"+
               " ]\n");
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
        JsonNode json = JsonNode.parseJson(
               " {\n"+
               "   \"school\": {\n"+
               "     \"name\": \"Fizbuzz Elementary\",\n"+
               "     \"address\": \"136 Learning Lane\",\n"+
               "     \"founded\": 1942\n"+
               "   },\n"+
               "   \"hospital\": {\n"+
               "     \"name\": \"Mendingorium\",\n"+
               "     \"address\": \"631 Mending Lane\"\n"+
               "   }\n"+
               " }\n");
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
        JsonNode json = JsonNode.parseJson(
               " [\n"+
               "   {\n"+
               "     \"name\": \"Fizbuzz Elementary\",\n"+
               "     \"address\": \"136 Learning Lane\",\n"+
               "     \"founded\": 1942\n"+
               "   },\n"+
               "   {\n"+
               "     \"name\": \"Mendingorium\",\n"+
               "     \"address\": \"631 Mending Lane\"\n"+
               "   }\n"+
               " ]\n");
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
        JsonNode state = JsonNode.parseJson(
               " {\n"+
               "   \"players\": [\n"+
               "     { \"name\": \"Alex\",\n"+
               "       \"score\": 10\n"+
               "     },\n"+
               "     { \"name\": \"Bob\",\n"+
               "       \"score\": 35\n"+
               "     },\n"+
               "     { \"name\": \"Charlie\",\n"+
               "       \"score\": 70\n"+
               "     }\n"+
               "   ]\n"+
               " }\n");
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
        JsonNode state = JsonNode.parseJson(
               " {\n"+
               "   \"players\": [\n"+
               "     { \"name\": \"Alex\",\n"+
               "       \"score\": 10\n"+
               "     },\n"+
               "     { \"name\": \"Bob\",\n"+
               "       \"score\": 35\n"+
               "     },\n"+
               "     { \"name\": \"Charlie\",\n"+
               "       \"score\": 70\n"+
               "     }\n"+
               "   ]\n"+
               " }\n");
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
        JsonNode state = JsonNode.parseJson(
               " {\n"+
               "   \"players\": [\n"+
               "     { \"name\": \"Alex\",\n"+
               "       \"score\": 10\n"+
               "     },\n"+
               "     { \"nickname\": \"Little Bobby Tables\",\n"+
               "       \"score\": 35\n"+
               "     },\n"+
               "     { \"name\": \"Charlie\",\n"+
               "       \"score\": 70\n"+
               "     }\n"+
               "   ]\n"+
               " }\n");
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

    @Test
    public void testNextCursorInFind() throws Exception {
        JsonNode node = JsonNode.parseJson(
               " {\n"+
               "   \"red\": {\n"+
               "     \"code\": \"#ff0000\",\n"+
               "     \"closest\": \"pink\"\n"+
               "   },\n"+
               "   \"pink\": {\n"+
               "     \"code\": \"#ff1493\",\n"+
               "     \"closest\": \"red\"\n"+
               "   },\n"+
               "   \"orange\": {\n"+
               "     \"code\": \"#ffa500\",\n"+
               "     \"closest\": \"red\"\n"+
               "   },\n"+
               "   \"green\": {\n"+
               "     \"code\": \"#00ff00\"\n"+
               "   }\n"+
               " }\n");
        assertTrue(node.isAtCursor());
        node.rootInfo.setSecondaryCursors(new FindCursor("red"));
        assertEquals("", node.rootInfo.userCursor.toString());

        // Now we move to the next cursor
        node.cursorNextCursor();

        assertEquals(".red", node.rootInfo.userCursor.toString());

        node.cursorNextCursor();

        assertEquals(".pink.closest", node.rootInfo.userCursor.toString());

        node.cursorNextCursor();

        assertEquals(".orange.closest", node.rootInfo.userCursor.toString());
    }

    @Test
    public void testPrevCursorInFind() throws Exception {
        JsonNode node = JsonNode.parseJson(
               " {\n"+
               "   \"red\": {\n"+
               "     \"code\": \"#ff0000\",\n"+
               "     \"closest\": \"pink\"\n"+
               "   },\n"+
               "   \"pink\": {\n"+
               "     \"code\": \"#ff1493\",\n"+
               "     \"closest\": \"red\"\n"+
               "   },\n"+
               "   \"orange\": {\n"+
               "     \"code\": \"#ffa500\",\n"+
               "     \"closest\": \"red\"\n"+
               "   },\n"+
               "   \"green\": {\n"+
               "     \"code\": \"#00ff00\"\n"+
               "   }\n"+
               " }\n");
        assertTrue(node.isAtCursor());
        node.rootInfo.setSecondaryCursors(new FindCursor("red"));
        // go to the end
        JsonNode lastChild = node;
        for (int i=0; i<100; i++) {
            JsonNode x = lastChild.lastChild();
            if (x==null || x==lastChild) break;
            lastChild = x;
        }
        node.rootInfo.setPrimaryCursor(lastChild.whereIAm);
        assertEquals(".green.code", node.rootInfo.userCursor.toString());

        // Now we move to the next cursor
        node.cursorPrevCursor();

        assertEquals(".orange.closest", node.rootInfo.userCursor.toString());

        node.cursorPrevCursor();

        assertEquals(".pink.closest", node.rootInfo.userCursor.toString());

        node.cursorPrevCursor();

        assertEquals(".red", node.rootInfo.userCursor.toString());
    }
}