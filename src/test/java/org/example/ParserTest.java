package org.example;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParserTest {

    @Test
    public void testParseMap() throws Exception {
        JsonNode json = JsonNode.parseJson("""
            {
                "one": "hello",
                "two": "world"
            }""");
        json.cursorDown();
        assertEquals(".one", json.rootInfo.userCursor.toString());
    }

    @Test
    public void testParseList() throws Exception {
        JsonNode json = JsonNode.parseJson("""
            [
                "hello",
                "world"
            ]""");
        json.cursorDown();
        assertEquals("[0]", json.rootInfo.userCursor.toString());
    }

    @Test
    public void testParseString() throws Exception {
        JsonNode json = JsonNode.parseJson("""
            "hello"
            """);
        json.cursorDown();
        assertEquals("", json.rootInfo.userCursor.toString());
    }

    @Test
    public void testParseInteger() throws Exception {
        JsonNode json = JsonNode.parseJson("""
            1980
            """);
        json.cursorDown();
        assertEquals("", json.rootInfo.userCursor.toString());
    }

    @Test
    public void testParseDouble() throws Exception {
        JsonNode json = JsonNode.parseJson("""
            2.2
            """);
        json.cursorDown();
        assertEquals("", json.rootInfo.userCursor.toString());
    }

    @Test
    public void testParseJsonL() throws Exception {
        JsonNode json = JsonNode.parseLines(new String[] {
                "1.0",
                "2.1"
        });
        assertTrue(json instanceof JsonNodeList);
    }

    @Test
    public void testParseJsonL2() throws Exception {
        JsonNode json = JsonNode.parseLines(new String[] {
                "12.0",
                "[ 1.0, 2.0 ]",
                "{ \"name\": \"foo\" }"
        });
        assertTrue(json instanceof JsonNodeList);
    }

    // empty lines are still valid JSONL
    @Test
    public void testParseJsonL3() throws Exception {
        JsonNode json = JsonNode.parseLines(new String[] {
                "12.0",
                "[ 1.0, 2.0 ]",
                "{ \"name\": \"foo\" }",
                "",
                ""
        });
        assertTrue(json instanceof JsonNodeList);
    }


}
