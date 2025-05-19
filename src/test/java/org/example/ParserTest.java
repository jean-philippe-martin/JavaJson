package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class ParserTest {

    @Test
    public void testParseMap() throws Exception {
        JsonNode json = JsonNode.parseJson(
            "{\n"+
            "    \"one\": \"hello\",\n"+
            "    \"two\": \"world\"\n"+
            "}");
        json.cursorDown();
        assertEquals(".one", json.rootInfo.userCursor.toString());
    }

    @Test
    public void testParseList() throws Exception {
        JsonNode json = JsonNode.parseJson("[\"hello\",\"world\"]");
        json.cursorDown();
        assertEquals("[0]", json.rootInfo.userCursor.toString());
    }

    @Test
    public void testParseInteger() throws Exception {
        JsonNode json = JsonNode.parseJson("1980");
        json.cursorDown();
        assertEquals("", json.rootInfo.userCursor.toString());
    }

    @Test
    public void testParseDouble() throws Exception {
        JsonNode json = JsonNode.parseJson("2.2");
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

    @Test
    public void testParseSingleLine() throws Exception {
        JsonNode json = JsonNode.parseLines(new String[] {
                "12.0"
        });
        // Even though a single line that is valid JSON can be construed as JSONL,
        // we should really keep this as JSON.
        assertTrue(json instanceof JsonNodeValue);
    }

    @Test
    public void testParseSingleLine2() throws Exception {
        JsonNode json = JsonNode.parseLines(new String[] {
                "\"hello\"",
                "",
                ""
        });
        // Even though a single line that is valid JSON can be construed as JSONL,
        // we should really keep this as JSON.
        // Even if the file has multiple lines, but only one has data.
        assertTrue(json instanceof JsonNodeValue);
    }

    @Test
    public void testParseBoolean() throws Exception {
        JsonNode json = JsonNode.parseLines(new String[] {
                "true"
        });
        // Even though a single line that is valid JSON can be construed as JSONL,
        // we should really keep this as JSON.
        // Even if the file has multiple lines, but only one has data.
        assertTrue(json instanceof JsonNodeValue);
    }

    @Test
    public void testParseNull() throws Exception {
        JsonNode json = JsonNode.parseLines(new String[] {
                "{",
                "  \"null_value\": null",
                "}"
        });
        // Even though a single line that is valid JSON can be construed as JSONL,
        // we should really keep this as JSON.
        // Even if the file has multiple lines, but only one has data.
        assertTrue(json instanceof JsonNodeMap);
        JsonNodeMap map = (JsonNodeMap) json;
        map.getChild("null_value");
    }

    @Test
    public void testParseNormalString() throws Exception {
        JsonNode json = JsonNode.parseJson("\"hello\"");
        Object val = json.getValue();
        assert(val instanceof String);
        String s = (String)val;
        assertEquals("hello", s);
    }

    @Test
    public void testParseStringWithUnicode() throws Exception {
        JsonNode json = JsonNode.parseJson("\"hello\\u0048\"");
        Object val = json.getValue();
        assert(val instanceof String);
        String s = (String)val;
        assertEquals("helloH", s);
    }

    @Test
    public void testParseStringWithNull() throws Exception {
        JsonNode json = JsonNode.parseJson("\"hello\\u0000bye\"");
        Object val = json.getValue();
        assert(val instanceof String);
        String s = (String)val;
        assertEquals("hello\0bye", s);
    }

    @Test
    public void testParseStringWithNewline() throws Exception {
        JsonNode json = JsonNode.parseJson("\"hello\\nbye\"");
        Object val = json.getValue();
        assert(val instanceof String);
        String s = (String)val;
        assertEquals("hello\nbye", s);
    }

    @Test
    public void testParseStringEscapeNewline() throws Exception {
        // The input string has an escape for a newline inside of it.
        JsonNode json = JsonNode.parseJsonIgnoreEscapes("\"hello\\nbye\"");
        // We want to NOT interpret the escape
        Object val = json.getValue();
        assert(val instanceof String);
        String s = (String)val;
        assertEquals("hello\\nbye", s);
    }

    @Test
    public void testParseMapEscapeNewline() throws Exception {
        // The input string has an escape for a newline inside of it.
        JsonNode json = JsonNode.parseJsonIgnoreEscapes("{\"hello\":\"world\",\"bye\":\"toil\\nnewline\"}");
        // We want to NOT interpret the escape
        assert(json instanceof JsonNodeMap);
        JsonNodeMap jsm = (JsonNodeMap) json;
        String bye = (String)jsm.getChild("bye").getValue();
        assertEquals("toil\\nnewline", bye);
    }


}
