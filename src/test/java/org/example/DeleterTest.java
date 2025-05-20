package org.example;

import org.example.Deleter.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class DeleterTest {

    public static class OneFishPond {
        public JsonNodeMap root;
        public JsonNodeMap one;
        public JsonNodeMap fish;
        public JsonNode pond;
        public JsonNodeMap two;
        public JsonNodeMap world;
        public JsonNode records;

        public OneFishPond() throws Exception {
            root = (JsonNodeMap)JsonNode.parseJson(
                    "{\n" +
                            "    \"one\": {\n" +
                            "        \"fish\": {\n" +
                            "            \"pond\": 1\n" +
                            "        }\n" +
                            "    },\n" +
                            "    \"two\": {\n" +
                            "        \"world\": {\n" +
                            "           \"records\": 1\n" +
                            "        }\n" +
                            "    }\n" +
                            "}");
            one = (JsonNodeMap)((JsonNodeMap) root).getChild("one");
            fish = (JsonNodeMap)one.getChild("fish");
            pond = fish.getChild("pond");
            two = (JsonNodeMap)((JsonNodeMap) root).getChild("two");
            world = (JsonNodeMap)two.getChild("world");
            records = world.getChild("records");
        }

        public void assertTargeting(Deleter del, String sevenSymbols) {
            StringBuilder got = new StringBuilder();
            got.append(del.targets(root)?"D":".");
            got.append(del.targets(one)?"D":".");
            got.append(del.targets(fish)?"D":".");
            got.append(del.targets(pond)?"D":".");
            got.append(del.targets(two)?"D":".");
            got.append(del.targets(world)?"D":".");
            got.append(del.targets(records)?"D":".");
            assertEquals(sevenSymbols, got.toString());
        }

    }

    @Test
    public void deleteEverythingThatIsPinned() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                        "    \"one\": \"hello\",\n" +
                        "    \"two\": \"world\"\n" +
                        "}");
        JsonNode two = ((JsonNodeMap) json).getChild("two");
        two.setPinned(true);

        Deleter del = new Deleter(json, TARGET.EVERYTHING, SUBJECT.THAT, MOD.IS, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        assertEquals(false, del.targets(json));
        JsonNode one = ((JsonNodeMap) json).getChild("one");
        assertEquals(false, del.targets(one));
        assertEquals(true, del.targets(two));
    }

    // contents of pinned are deleted too.
    @Test
    public void deleteEverythingThatIsPinned2() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                        "    \"one\": \"hello\",\n" +
                        "    \"two\": {\n" +
                        "        \"world\": 1\n" +
                        "    }\n" +
                        "}");
        JsonNode two = ((JsonNodeMap) json).getChild("two");
        two.setPinned(true);

        Deleter del = new Deleter(json, TARGET.EVERYTHING, SUBJECT.THAT, MOD.IS, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        assertEquals(false, del.targets(json));
        JsonNode one = ((JsonNodeMap) json).getChild("one");
        assertEquals(false, del.targets(one));
        assertEquals(true, del.targets(two));
        JsonNode world = ((JsonNodeMap) two).getChild("world");
        assertEquals(true, del.targets(world));
    }

    @Test
    public void deleteEverythingExceptPinned() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                        "    \"one\": \"hello\",\n" +
                        "    \"two\": \"world\"\n" +
                        "}");
        JsonNode one = ((JsonNodeMap) json).getChild("one");
        JsonNode two = ((JsonNodeMap) json).getChild("two");
        two.setPinned(true);

        Deleter del = new Deleter(json, TARGET.EVERYTHING, SUBJECT.UNLESS, MOD.IS, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        assertEquals(true, del.targets(json));
        assertEquals(true, del.targets(one));
        assertEquals(false, del.targets(two));
    }

    // children of pinned are saved too.
    @Test
    public void deleteEverythingExceptPinned2_KeepChildren() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                        "    \"one\": {\n" +
                        "        \"fish\": 1\n" +
                        "    },\n" +
                        "    \"two\": {\n" +
                        "        \"world\": 1\n" +
                        "    }\n" +
                        "}");
        JsonNodeMap one = (JsonNodeMap)((JsonNodeMap) json).getChild("one");
        JsonNodeMap two = (JsonNodeMap)((JsonNodeMap) json).getChild("two");
        two.setPinned(true);

        Deleter del = new Deleter(json, TARGET.EVERYTHING, SUBJECT.UNLESS, MOD.IS, FILTER.PINNED, OPTIONS.KEEP_CHILDREN);
        assertEquals(true, del.targets(json)); // parent of saved node, but we're not keeping parents.
        assertEquals(true, del.targets(one));
        JsonNode fish = one.getChild("fish");
        assertEquals(true, del.targets(fish));
        assertEquals(false, del.targets(two)); // pinned
        JsonNode world = two.getChild("world");
        assertEquals(false, del.targets(world)); // child of saved node
    }

    // children of pinned are not saved.
    @Test
    public void deleteEverythingExceptPinned2_NoKeepChildren() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                        "    \"one\": {\n" +
                        "        \"fish\": 1\n" +
                        "    },\n" +
                        "    \"two\": {\n" +
                        "        \"world\": 1\n" +
                        "    }\n" +
                        "}");
        JsonNodeMap one = (JsonNodeMap)((JsonNodeMap) json).getChild("one");
        JsonNodeMap two = (JsonNodeMap)((JsonNodeMap) json).getChild("two");
        two.setPinned(true);

        Deleter del = new Deleter(json, TARGET.EVERYTHING, SUBJECT.UNLESS, MOD.IS, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        assertEquals(true, del.targets(json)); // parent of saved node, but we're not keeping parents.
        assertEquals(true, del.targets(one));
        JsonNode fish = one.getChild("fish");
        assertEquals(true, del.targets(fish));
        assertEquals(false, del.targets(two)); // pinned
        JsonNode world = two.getChild("world");
        assertEquals(true, del.targets(world)); // child of saved node
    }

    @Test
    public void deleteEverythingExceptPinned2KeepParents() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                        "    \"one\": {\n" +
                        "        \"fish\": 1\n" +
                        "    },\n" +
                        "    \"two\": {\n" +
                        "        \"world\": 1\n" +
                        "    }\n" +
                        "}");
        JsonNodeMap one = (JsonNodeMap)((JsonNodeMap) json).getChild("one");
        JsonNodeMap two = (JsonNodeMap)((JsonNodeMap) json).getChild("two");
        JsonNode world = two.getChild("world");

        world.setPinned(true);

        Deleter del = new Deleter(json, TARGET.EVERYTHING, SUBJECT.UNLESS, MOD.IS, FILTER.PINNED, OPTIONS.KEEP_PARENTS);
        assertEquals(false, del.targets(json)); // parent of saved node, and we're keeping parents
        assertEquals(true, del.targets(one));
        JsonNode fish = one.getChild("fish");
        assertEquals(true, del.targets(fish));
        assertEquals(false, del.targets(two)); // parent of pinned
        assertEquals(false, del.targets(world)); // pinned
    }

    @Test
    public void deleteEverythingThatContainsPinned1() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                        "    \"one\": {\n" +
                        "        \"fish\": 1\n" +
                        "    },\n" +
                        "    \"two\": {\n" +
                        "        \"world\": 1\n" +
                        "    }\n" +
                        "}");
        JsonNodeMap one = (JsonNodeMap)((JsonNodeMap) json).getChild("one");
        JsonNode fish = one.getChild("fish");
        JsonNodeMap two = (JsonNodeMap)((JsonNodeMap) json).getChild("two");
        JsonNode world = two.getChild("world");

        two.setPinned(true);

        Deleter del = new Deleter(json, TARGET.EVERYTHING, SUBJECT.THAT, MOD.CONTAINS_DIRECT, FILTER.PINNED, OPTIONS.KEEP_PARENTS);
        assertEquals(true, del.targets(json)); // its child "two" is pinned.
        assertEquals(true, del.targets(one));
        assertEquals(true, del.targets(fish));
        assertEquals(true, del.targets(two));
        assertEquals(true, del.targets(world));
    }

    @Test
    public void deleteEverythingThatContainsPinned2() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                        "    \"one\": {\n" +
                        "        \"fish\": 1\n" +
                        "    },\n" +
                        "    \"two\": {\n" +
                        "        \"world\": 1\n" +
                        "    }\n" +
                        "}");
        JsonNodeMap one = (JsonNodeMap)((JsonNodeMap) json).getChild("one");
        JsonNode fish = one.getChild("fish");
        JsonNodeMap two = (JsonNodeMap)((JsonNodeMap) json).getChild("two");
        JsonNode world = two.getChild("world");

        world.setPinned(true);

        Deleter del = new Deleter(json, TARGET.EVERYTHING, SUBJECT.THAT, MOD.CONTAINS_DIRECT, FILTER.PINNED, OPTIONS.KEEP_PARENTS);
        assertEquals(false, del.targets(json));
        assertEquals(false, del.targets(one));
        assertEquals(false, del.targets(fish));
        assertEquals(true, del.targets(two));  // contains pinned
        assertEquals(true, del.targets(world));
    }

    @Test
    public void deleteEverythingThatContainsPinned3() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                        "    \"one\": {\n" +
                        "        \"fish\": {\n" +
                        "            \"bucket\": 1\n" +
                        "        }\n" +
                        "    },\n" +
                        "    \"two\": {\n" +
                        "        \"world\": 1\n" +
                        "    }\n" +
                        "}");
        JsonNodeMap one = (JsonNodeMap)((JsonNodeMap) json).getChild("one");
        JsonNodeMap fish = (JsonNodeMap) one.getChild("fish");
        JsonNode bucket = fish.getChild("bucket");
        JsonNodeMap two = (JsonNodeMap)((JsonNodeMap) json).getChild("two");
        JsonNode world = two.getChild("world");

        bucket.setPinned(true);

        Deleter del = new Deleter(json, TARGET.EVERYTHING, SUBJECT.THAT, MOD.CONTAINS_DIRECT, FILTER.PINNED, OPTIONS.KEEP_PARENTS);
        assertEquals(false, del.targets(json)); // its child "two" is pinned.
        assertEquals(false, del.targets(one));
        assertEquals(true, del.targets(fish)); // contains pinned
        assertEquals(true, del.targets(bucket));  // its parent is gone
        assertEquals(false, del.targets(world));
    }

    @Test
    public void deleteEverythingThatContainsPinnedRecursively1() throws Exception {
        JsonNode json = JsonNode.parseJson(
                "{\n" +
                        "    \"one\": {\n" +
                        "        \"fish\": 1\n" +
                        "    },\n" +
                        "    \"two\": {\n" +
                        "        \"world\": 1\n" +
                        "    }\n" +
                        "}");
        JsonNodeMap one = (JsonNodeMap)((JsonNodeMap) json).getChild("one");
        JsonNode fish = one.getChild("fish");
        JsonNodeMap two = (JsonNodeMap)((JsonNodeMap) json).getChild("two");
        JsonNode world = two.getChild("world");

        two.setPinned(true);

        Deleter del = new Deleter(json, TARGET.EVERYTHING, SUBJECT.THAT, MOD.CONTAINS_INDIRECT, FILTER.PINNED, OPTIONS.KEEP_PARENTS);
        assertEquals(true, del.targets(json)); // its child "two" is pinned.
        assertEquals(true, del.targets(one));
        assertEquals(true, del.targets(fish));
        assertEquals(true, del.targets(two));
        assertEquals(true, del.targets(world));
    }

    @Test
    public void deleteEverythingThatContainsPinnedRecursively2() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.world.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.EVERYTHING, SUBJECT.THAT, MOD.CONTAINS_INDIRECT, FILTER.PINNED, OPTIONS.KEEP_PARENTS);
        poem.assertTargeting(del, "DDDDDDD");
    }

    @Test
    public void deleteEverythingThatContainsPinnedRecursively3() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.pond.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.EVERYTHING, SUBJECT.THAT, MOD.CONTAINS_INDIRECT, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // root gets deleted, so "one" gets deleted too.
        // This is a place we could use "keep orphans"
        poem.assertTargeting(del, "DDDDDDD");
    }

    @Test
    public void deleteChildrenThatArePinned_NoPins() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.root.cursorDown(); // one
        poem.root.cursorDown(); // fish
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.THAT, MOD.IS, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // no pin, so nothing gets deleted.
        poem.assertTargeting(del, ".......");
    }

    @Test
    public void deleteChildrenThatArePinned2() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.root.cursorDown(); // one
        poem.root.cursorDown(); // fish
        poem.pond.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.THAT, MOD.IS, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // the only child is "pond" and yes it is pinned, so here we go!
        poem.assertTargeting(del, "...D...");
    }

    @Test
    public void deleteChildrenThatArePinned_ButOnlyGrandkids() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.root.cursorDown(); // one
        poem.root.setPinned(true);
        poem.pond.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.THAT, MOD.IS, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // no child is pinned.
        poem.assertTargeting(del, ".......");
    }

    @Test
    public void deleteChildrenThatArePinned4() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.two.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.THAT, MOD.IS, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // "two" and everything under it goes.
        poem.assertTargeting(del, "....DDD");
    }

    @Test
    public void deleteChildrenThatArePinned_NoChildren() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.root.cursorDown(); // one
        poem.root.cursorDown(); // fish
        poem.root.cursorDown(); // pond
        poem.two.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.THAT, MOD.IS, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // No children! No one gets deleted.
        poem.assertTargeting(del, ".......");
    }

    @Test
    public void deleteChildrenThatContainPinned_ButIsPinned() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.one.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.THAT, MOD.CONTAINS_DIRECT, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // "one" is pinned themselves but doesn't contain a pinned child.
        poem.assertTargeting(del, ".......");
    }

    @Test
    public void deleteChildrenThatContainPinned_Yes() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.fish.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.THAT, MOD.CONTAINS_DIRECT, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // "one" contains a pinned child, so it goes.
        poem.assertTargeting(del, ".DDD...");
    }

    @Test
    public void deleteChildrenThatContainPinned_ButGrandchild() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.pond.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.THAT, MOD.CONTAINS_DIRECT, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // "one" contains a pinned grandchild, that doesn't count here.
        poem.assertTargeting(del, ".......");
    }


    @Test
    public void deleteChildrenThatContainPinnedRecursively_ButIsPinned() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.one.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.THAT, MOD.CONTAINS_INDIRECT, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // "one" is pinned themselves but doesn't contain a pinned descendant.
        poem.assertTargeting(del, ".......");
    }

    @Test
    public void deleteChildrenThatContainPinnedRecursively_Yes() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.fish.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.THAT, MOD.CONTAINS_INDIRECT, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // "one" contains a pinned child, so it goes.
        poem.assertTargeting(del, ".DDD...");
    }

    @Test
    public void deleteChildrenThatContainPinnedRecursively_ButGrandchild() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.pond.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.THAT, MOD.CONTAINS_INDIRECT, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // "one" contains a pinned grandchild, so it goes.
        poem.assertTargeting(del, ".DDD...");
    }

    @Test
    public void deleteChildrenUnlessPinned() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.two.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.UNLESS, MOD.IS, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // "two" is pinned, "one" is not.
        poem.assertTargeting(del, ".DDD...");
    }

    @Test
    public void deleteChildrenUnlessPinned_2() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.root.cursorDown();
        poem.two.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.UNLESS, MOD.IS, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // no child is pinned
        poem.assertTargeting(del, "..DD...");
    }

    @Test
    public void deleteChildrenUnlessPinned_3() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.root.cursorDown();
        poem.fish.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.UNLESS, MOD.IS, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // the child is pinned
        poem.assertTargeting(del, ".......");
    }

    @Test
    public void deleteChildrenUnlessPinned_4() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.root.cursorDown();
        poem.pond.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.UNLESS, MOD.IS, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // the child is not pinned (it contains pinned though)
        poem.assertTargeting(del, "..DD...");
    }

    @Test
    public void deleteChildrenUnlessContainsPinned_ButSelf() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.two.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.UNLESS, MOD.CONTAINS_DIRECT, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // "two" does not contain a pin (it is pinned itself)
        poem.assertTargeting(del, ".DDDDDD");
    }

    @Test
    public void deleteChildrenUnlessContainsPinned_YesChild() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.world.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.UNLESS, MOD.CONTAINS_DIRECT, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // "two" contains a pin
        poem.assertTargeting(del, ".DDD...");
    }

    @Test
    public void deleteChildrenUnlessContainsPinned_ButGrandChild() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.records.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.UNLESS, MOD.CONTAINS_DIRECT, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // "two" contains a pin, but indirectly.
        poem.assertTargeting(del, ".DDDDDD");
    }

    @Test
    public void deleteChildrenUnlessContainsPinnedRecursively_Yes() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.records.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.UNLESS, MOD.CONTAINS_INDIRECT, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // "two" contains a pin, but indirectly. So it gets kept.
        poem.assertTargeting(del, ".DDD...");
    }

    @Test
    public void deleteChildrenUnlessContainsPinnedRecursively_YesDirect() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.world.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.UNLESS, MOD.CONTAINS_INDIRECT, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // "two" contains a pin, directly. So it gets kept.
        poem.assertTargeting(del, ".DDD...");
    }

    @Test
    public void deleteChildrenUnlessContainsPinnedRecursively_YesSelf() throws Exception {
        OneFishPond poem = new OneFishPond();
        poem.two.setPinned(true);
        Deleter del = new Deleter(poem.root, TARGET.CHILDREN, SUBJECT.UNLESS, MOD.CONTAINS_INDIRECT, FILTER.PINNED, OPTIONS.KEEP_NOTHING);
        // "two" is pinned. Does this count as "containing a pin? No. Root gets kept, though.
        poem.assertTargeting(del, ".DDDDDD");
    }

    /*

    {
      "one": {
        "fish"
      }
      "two": {
        "world"
      }
    }

    We want to be able to keep only the pinned nodes (and their children), removing
    even their containers above.
    "delete everything except pinned"
    [ ] keep parents
    [x] keep children

    We want to be able to keep the skeleton containers, but remove everything that is not
    (say) pinned. We keep the children of the pinned node though.
    "delete everything except pinned"
    [x] keep parents
    [x] keep children

    We want to be able to delete all pinned nodes and their children.
    "delete everything that is pinned"
    (the parents are kept as a matter of course, the kids are gone as a matter of course too)

    Delete every child of main cursor that has a pin anywhere below it:
    "delete every child of main cursor that:"
    ( ) is pinned
    ( ) has pinned child
    (X) has pinned descendant

    So maybe

    "delete
    ( )everything
    (*)every child of main cursor

    ( )that
    ( )unless it

    ( )is
    ( )has child
    (*)has descendant
    either:
    [X] pinned
    [ ] selected
    [ ] visible

    options:
    [ ] keep parents (only visible for "unless" and not "has descendants")
    [ ] keep children (only visible for "unless")
    "


    */

}
