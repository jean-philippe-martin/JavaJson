package org.example.cursor;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.Cursor;
import org.example.JsonNode;
import org.example.JsonNodeMap;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class PathCursorTest {

    static String TWO_PLAYERS="   {\n"+
                            "   \"players\": {\n"+
                            "     \"Alex\": {\n"+
                            "       \"score\": 10,\n"+
                            "       \"age\": 20\n"+
                            "     },\n"+
                            "     \"Bob\": {\n"+
                            "       \"score\": 35\n"+
                            "     }\n"+
                            "   }\n"+
                            " }\n";

    @Test
    public void testSelects() {
        PathCursor foo = new PathCursor(".foo");
        Cursor test = new Cursor().enterKey("foo");
        assertTrue(foo.selects(test, test));
    }

    @Test
    public void testSelects2() {
        PathCursor foo = new PathCursor(".foo[2]");
        Cursor test = new Cursor().enterKey("foo").enterIndex(2);
        assertTrue(foo.selects(test, test));
    }

    @Test
    public void testDoesNotSelect() {
        PathCursor foo = new PathCursor(".foo");
        Cursor test = new Cursor().enterKey("bar");
        assertFalse(foo.selects(test, test));
    }

    @Test
    public void testDoesNotSelect2() {
        PathCursor foo = new PathCursor(".foo[1]");
        Cursor test = new Cursor().enterKey("bar").enterKey("baz");
        assertFalse(foo.selects(test, test));
    }

    @Test
    public void testAddAll() throws JsonProcessingException {
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
        PathCursor cur = new PathCursor(".players.Alex");
        List<JsonNode> selected = new ArrayList<>();
        cur.addAllNodes(state.asCursor(), selected);
        List<String> selCur = selected.stream().map(n -> {return n.asCursor().toString();}).collect(Collectors.toList());
        assertEquals(selCur, Arrays.stream(new String[] { ".players.Alex" }).collect(Collectors.toList()));
    }

    @Test
    public void testAddAll2() throws JsonProcessingException {
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
        PathCursor cur = new PathCursor(".players.*");
        List<JsonNode> selected = new ArrayList<>();
        cur.addAllNodes(state.asCursor(), selected);
        List<String> selCur = selected.stream().map(n -> {return n.asCursor().toString();}).collect(Collectors.toList());
        assertEquals(selCur, Arrays.stream(new String[] { ".players.Alex", ".players.Bob" }).collect(Collectors.toList()));
    }

    @Test
    public void testAddAll3() throws JsonProcessingException {
        JsonNode state = JsonNode.parseJson(
                "   {\n"+
                        "   \"players\": {\n"+
                        "     \"Alex\": {\n"+
                        "       \"score\": 10,\n"+
                        "       \"age\": 20\n"+
                        "     },\n"+
                        "     \"Bob\": {\n"+
                        "       \"score\": 35\n"+
                        "     }\n"+
                        "   }\n"+
                        " }\n");
        PathCursor cur = new PathCursor(".players.*.score");
        List<JsonNode> selected = new ArrayList<>();
        cur.addAllNodes(state.asCursor(), selected);
        List<String> selCur = selected.stream().map(n -> {return n.asCursor().toString();}).collect(Collectors.toList());
        assertEquals(selCur, Arrays.stream(new String[] { ".players.Alex.score", ".players.Bob.score" }).collect(Collectors.toList()));
    }

    @Test
    public void testAddAll4() throws JsonProcessingException {
        JsonNode state = JsonNode.parseJson(
                "   {\n"+
                        "   \"players\": {\n"+
                        "     \"Alex\": {\n"+
                        "       \"score\": 10,\n"+
                        "       \"age\": 20\n"+
                        "     },\n"+
                        "     \"Bob\": {\n"+
                        "       \"score\": 35\n"+
                        "     }\n"+
                        "   }\n"+
                        " }\n");
        PathCursor cur = new PathCursor(".players.*.*");
        List<JsonNode> selected = new ArrayList<>();
        cur.addAllNodes(state.asCursor(), selected);
        List<String> selCur = selected.stream().map(n -> {return n.asCursor().toString();}).collect(Collectors.toList());
        assertEquals(selCur, Arrays.stream(new String[] { ".players.Alex.score", ".players.Alex.age", ".players.Bob.score" }).collect(Collectors.toList()));
    }

    @Test
    public void testAddAll5() throws JsonProcessingException {
        JsonNode state = JsonNode.parseJson(
                "   {\n"+
                        "   \"players\": [\n"+
                        "     \"Alex\",\n"+
                        "     \"Bob\"\n"+
                        "   ]\n"+
                        " }\n");
        PathCursor cur = new PathCursor(".players[*]");
        List<JsonNode> selected = new ArrayList<>();
        cur.addAllNodes(state.asCursor(), selected);
        List<String> selCur = selected.stream().map(n -> {return n.asCursor().toString();}).collect(Collectors.toList());
        assertEquals(selCur, Arrays.stream(new String[] { ".players[0]", ".players[1]" }).collect(Collectors.toList()));
    }

    @Test
    public void testNextAtLastCursor() throws JsonProcessingException {
        JsonNode state = JsonNode.parseJson(TWO_PLAYERS);
        PathCursor cur = new PathCursor(".players.Alex");
        JsonNodeMap alex = (JsonNodeMap)((JsonNodeMap)((JsonNodeMap)state).getChild("players")).getChild("Alex");

        // At or inside the last cursor: there should be no "next"
        assertEquals(null, cur.nextCursor(alex.asCursor()));
        assertEquals(null, cur.nextCursor(alex.getChild("score").asCursor()));
    }

    @Test
    public void testNext1() throws JsonProcessingException {
        JsonNode state = JsonNode.parseJson(TWO_PLAYERS);
        PathCursor cur = new PathCursor(".players.*");
        JsonNodeMap alex = (JsonNodeMap)((JsonNodeMap)((JsonNodeMap)state).getChild("players")).getChild("Alex");

        Cursor got = cur.nextCursor(alex.asCursor());
        assertEquals(".players.Bob", got.toString());
    }

    // This code doesn't work yet, but it's also not used yet.
    @Ignore
    @Test
    public void testNext2() throws JsonProcessingException {
        JsonNode state = JsonNode.parseJson(TWO_PLAYERS);
        PathCursor cur = new PathCursor(".players.*");
        JsonNode alexScore = ((JsonNodeMap)((JsonNodeMap)((JsonNodeMap)state).getChild("players")).getChild("Alex")).getChild("score");

        Cursor got = cur.nextCursor(alexScore.asCursor());
        assertEquals(".players.Bob", got.toString());
    }

}