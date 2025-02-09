package org.example;

import org.junit.Test;

import static org.junit.Assert.*;


public class UniqueFieldsTest {

    @Test
    public void testAgg() throws Exception {
        JsonNode json = JsonNode.parseLines(new String[] {
                "[",
                "  {\"a\":10, \"b\":11}, ",
                "  {\"b\":20, \"c\":21}]"
        });
        assertTrue(json instanceof JsonNodeList);
        JsonNodeList jsonList = (JsonNodeList) json;

        AggUniqueFields aggregator = new AggUniqueFields(jsonList);
        aggregator.write(jsonList);

        JsonNode ag = jsonList.aggregate;

        assertNotNull(ag);
        assertTrue(ag instanceof JsonNodeMap);

        JsonNodeMap agMap = (JsonNodeMap) ag;
        assertEquals("  50%", agMap.getChild("a").aggregateComment);
        assertEquals("=100%", agMap.getChild("b").aggregateComment);
        assertEquals("  50%", agMap.getChild("c").aggregateComment);
    }

    @Test
    public void testAggDeep() throws Exception {
        JsonNode json = JsonNode.parseLines(new String[] {
                "[",
                "  {\"x\": ",
                "      [",
                "        {\"a\":10, \"b\":11}, ",
                "        {\"b\":20, \"c\":21}",
                "      ]}]"
        });
        assertTrue(json instanceof JsonNodeList);
        JsonNodeList jsonList = (JsonNodeList) json;

        AggUniqueFields aggregator = new AggUniqueFields(jsonList);
        aggregator.write(jsonList);

        JsonNode ag = jsonList.aggregate;

        assertNotNull(ag);
        assertTrue(ag instanceof JsonNodeMap);

        JsonNodeMap agMap = (JsonNodeMap) ag;
        assertTrue(agMap.getKeysInOrder().contains("x"));
        JsonNodeMap kid = (JsonNodeMap) agMap.getChild("x");
    }

    @Test
    public void testNavigateAgg() throws Exception {
        JsonNode json = JsonNode.parseLines(new String[]{
                "[",
                "  {\"a\":10, \"b\":11}, ",
                "  {\"b\":20, \"c\":21}]"
        });
        assertTrue(json instanceof JsonNodeList);
        JsonNodeList jsonList = (JsonNodeList) json;
        Operation agg = new Operation.AggUniqueFields(jsonList, true);
        agg.run();

        // Make sure we can navigate and not explode

        for (int i = 0; i < 10; i++) {
            jsonList.cursorDown();
        }

        for (int i = 0; i < 10; i++) {
            jsonList.cursorUp();
        }

        // Try to remove the aggregate while the cursor is in it.

        jsonList.cursorDown();
        Operation rem = new Operation.AggUniqueFields(jsonList, false);
        JsonNode didit = rem.run();

        assertNotNull(didit);

    }


    }
