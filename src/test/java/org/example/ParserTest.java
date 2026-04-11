package org.example;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

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

    /** Hjson allows comments; standard JSON does not. */
    @Test
    public void testParseWithComments() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                "  // line comment\n" +
                "  \"count\": 42,\n" +
                "  /* block\n" +
                "     comment */ \"label\": \"ok\"\n" +
                "}");
        assertTrue(json instanceof JsonNodeMap);
        JsonNodeMap map = (JsonNodeMap) json;
        assertEquals(42, map.getChild("count").getValue());
        assertEquals("ok", map.getChild("label").getValue());
    }

    @Test
    public void testLeadingTriviaBeforeValue() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                "  \"k\":\n" +
                "  // above value\n" +
                "  99\n" +
                "}");
        JsonNodeMap map = (JsonNodeMap) json;
        JsonNode k = map.getChild("k");
        assertTrue(k.hasValueLeadingTrivia());
        assertTrue(k.getValueLeadingTrivia().contains("above value"));
        assertEquals(99, k.getValue());
    }

    @Test
    public void testInlineCommentAfterCommaButBeforeNewline() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                "  \"k\": \"hello\",\n" +
                "  \"l\": \"beautiful\", // beautiful comment\n" +
                "  \"m\": \"world\"\n" +
                "}");
        JsonNodeMap map = (JsonNodeMap) json;
        JsonNode l = map.getChild("l");
        assertTrue(l.hasValueTrailingTrivia());
        assertTrue(l.getValueTrailingTrivia().contains("beautiful comment"));
        assertEquals("beautiful", l.getValue());
    }

    @Test
    public void testInlineCommentAfterCommaAndAfterNewline() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                "  \"k\": \"hello\",\n" +
                "  \"l\": \"beautiful\",\n" + 
                "  // world comment\n" +
                "  \"m\": \"world\"\n" +
                "}");
        JsonNodeMap map = (JsonNodeMap) json;
        JsonNode l = map.getChild("l");
        JsonNode m = map.getChild("m");
        assertFalse(l.hasValueTrailingTrivia());
        assertTrue(map.getKeyLeadingTrivia("m").contains("world comment"));
        assertEquals("beautiful", l.getValue());
    }

    @Test
    public void testTrailingTriviaInlineAfterComma() throws Exception {
        JsonNode json = JsonNode.parseJson("{\"debugMode\": true, // after true\n\"logLevel\": \"x\"}");
        JsonNodeMap map = (JsonNodeMap) json;
        JsonNode dm = map.getChild("debugMode");
        assertTrue(dm.hasValueTrailingTrivia());
        assertTrue(dm.getValueTrailingTrivia().contains("after true"));
    }

    @Test
    public void testTrailingTriviaAfterArrayElement() throws Exception {
        JsonNode json = JsonNode.parseJson("[\"featureA\", // for A\n\"featureB\"]");
        JsonNodeList list = (JsonNodeList) json;
        assertTrue(list.get(0).hasValueTrailingTrivia());
        assertTrue(list.get(0).getValueTrailingTrivia().contains("for A"));
    }

    @Test
    public void testTriviaLineSplitUtility() {
        TriviaLineSplit noNl = TriviaLineSplit.split(" // only same line ");
        assertEquals(" // only same line ", noNl.sameLineBeforeFirstNewline);
        assertEquals("", noNl.fromFirstNewlineInclusive);

        TriviaLineSplit twoLines = TriviaLineSplit.split(" // same\n  // next");
        assertEquals(" // same", twoLines.sameLineBeforeFirstNewline);
        assertTrue(twoLines.fromFirstNewlineInclusive.startsWith("\n"));
        assertTrue(twoLines.fromFirstNewlineInclusive.contains("next"));
    }

    @Test
    public void testJsonNodeMapLineSplitsFromParse() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                "  // before key a\n" +
                "  \"a\":\n" +
                "  // before value 1\n" +
                "  1\n" +
                "}");
        JsonNodeMap map = (JsonNodeMap) json;
        assertNotNull(map.getKeyLeadingTrivia("a"));
        assertTrue(map.getKeyLeadingTrivia("a").contains("before key a"));
        assertTrue(map.getChild("a").getValueLeadingTrivia().contains("before value 1"));
        assertNull(map.getKeyTrailingTrivia("a"));
    }

    @Test
    public void testLeadingBeforeValueSameLineAsColon() throws Exception {
        JsonNode json = JsonNode.parseJson("{\"x\": // on colon line\n1}");
        JsonNodeMap map = (JsonNodeMap) json;
        assertTrue(map.getKeyTrailingTrivia("x").contains("on colon line"));
        // Continuation after the line comment is newline/space before "1", not a // comment
        String cont = map.getChild("x").getValueLeadingTrivia();
        assertNotNull(cont);
        assertFalse(cont.contains("//"));
    }

    
    
    @Test
    public void testJsonNodeMapComments() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                "  // before key a\n" +
                "  \"a\" /* inline with key */ : // also inline \n" +
                "  // before value 1\n" +
                "  1\n" +
                "}");
        JsonNodeMap map = (JsonNodeMap) json;
        JsonNode a = map.getChild("a");
        assertTrue(map.getKeyLeadingTrivia("a").contains("before key a"));
        assertTrue(map.getKeyTrailingTrivia("a").contains("inline with key"));
        assertTrue(map.getKeyTrailingTrivia("a").contains("also inline"));
        assertTrue(a.getValueLeadingTrivia().contains("before value 1"));
    }

    @Test
    public void testCommentsHjsonFileInlineOnDebugMode() throws Exception {
        Path p = Path.of("testdata/comments.hjson");
        String src = Files.readString(p);
        JsonNode root = JsonNode.parseJson(src);
        JsonNodeMap settings = (JsonNodeMap) ((JsonNodeMap) root).getChild("settings");
        JsonNode dm = settings.getChild("debugMode");
        assertTrue(dm.hasValueTrailingTrivia(), "inline // after true should be preserved");
        assertTrue(dm.getValueTrailingTrivia().contains("Another inline comment"));
    }


    @Test
    public void testJsonNodeListComments() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "[\n" +
                "  // before a\n" +
                "  \"a\",\n" +
                "  \"b\", // after b\n" +
                "  \"c\"\n" +
                "]");
        JsonNodeList lst = (JsonNodeList) json;
        assertNotNull(lst.get(0).getValueLeadingTrivia());
        assertTrue(lst.get(0).getValueLeadingTrivia().contains("before a"));
        assertNotNull(lst.get(1).getValueTrailingTrivia());
        assertTrue(lst.get(1).getValueTrailingTrivia().contains("after b"));
        assertNull(lst.get(2).getValueTrailingTrivia());
    }


}
