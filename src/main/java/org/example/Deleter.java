package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes what we want to delete.
 **/
public class Deleter {

    public enum TARGET {
        EVERYTHING,
        CHILDREN
    }
    public enum SUBJECT {
        THAT,
        UNLESS
    }

    public enum MOD {
        IS,
        // a direct child matches filter
        CONTAINS_DIRECT,
        // any descendant matches filter
        CONTAINS_INDIRECT,
    }

    public static class FILTER {
        public static final int NOTHING = 0;
        public static final int PINNED = 2;
        public static final int SELECTED = 4;
        public static final int VISIBLE = 8;
    }

    public static class OPTIONS {
        public static final int KEEP_NOTHING = 0;
        public static final int KEEP_PARENTS = 1;
        public static final int KEEP_ORPHANS = 2;
        public static final int KEEP_ALL = 3;
    }

    TARGET target;
    SUBJECT subject;
    MOD mod;
    int filter; // bitmask from FILTER
    int options; // bitmark from OPTIONS
    Cursor userCursor;
    // cache of node -> whether it's deleted
    Map<JsonNode, Boolean> shouldDelete = new HashMap<>();

    static final Deleter DELETE_ALL = new Deleter(null, TARGET.EVERYTHING, SUBJECT.UNLESS, MOD.IS, FILTER.NOTHING, OPTIONS.KEEP_PARENTS);
    static final Deleter DELETE_NEVER = new Deleter(null, TARGET.EVERYTHING, SUBJECT.THAT, MOD.IS, FILTER.NOTHING, OPTIONS.KEEP_ALL);


    public Deleter(JsonNode root, TARGET target, SUBJECT subject, MOD mod, int filter, int options) {
        this.target = target;
        this.subject = subject;
        this.mod = mod;
        this.filter = filter;
        this.options = options;
        if (root==null) {
            this.userCursor = null;
        } else {
            this.userCursor = root.rootInfo.userCursor;
        }
        precomputeTargets(root);
    }

    static public Deleter always() {
        return DELETE_ALL;
    }

    static public Deleter never() {
        return DELETE_NEVER;
    }


    private void precomputeTargets(JsonNode root) {
        if (target==TARGET.CHILDREN) {
            JsonNode mainCursor = root.rootInfo.userCursor.getData();
            var it = mainCursor.iterateChildren(true);
            while (null!=it) {
                JsonNode child = it.get();
                boolean chosen = innerTargets(child, false, 0);
                it = it.next();
            }
            // At this point we've cached the decision for all children of the main cursor.
        } else {
            innerTargets(root, true, 0);
        }
    }

    public boolean targets(JsonNode node) {
        boolean chosen = false;
        if (shouldDelete.containsKey(node)) {
            return shouldDelete.get(node);
        }
        return false;
    }

    // call on either everything or children of main cursor, depending on TARGET.
    private boolean innerTargets(JsonNode node, boolean recurse, int level) {
        if (null==node) return false;
        boolean chosen = false;
        if (shouldDelete.containsKey(node)) {
            return shouldDelete.get(node);
        }
        if (mod==MOD.IS) {
            chosen = matchesFilter(node);
            if (subject == SUBJECT.UNLESS) {
                chosen = !chosen;
                if (!chosen) {
                    markAllChildren(node, false);
                }
            } else if (chosen) {
                markAllChildren(node, true);
            }
        } else {
            // mod==MOD.CONTAINS*
            var it = node.iterateChildren(false);
            if (it!=null && it.isAggregate()) it = it.next();
            if (null==it) {
                // no children.
                chosen = subject==SUBJECT.UNLESS;
            } else {
                // we have children
                if (mod == MOD.CONTAINS_DIRECT) {
                    boolean contains = false;
                    while (it != null) {
                        JsonNode kid = it.get();
                        contains |= matchesFilter(kid);
                        if (contains) break;
                        it = it.next();
                    }
                    chosen = contains;
                    if (subject == SUBJECT.UNLESS) {
                        chosen = !chosen;
                    }
                } else if (mod == MOD.CONTAINS_INDIRECT) {
                    boolean contains;
                    if (level>0) {
                        contains = matchesFilterRecursively(node);
                    } else {
                        // first level, it's our kids only.
                        contains = false;
                        while (it != null) {
                            JsonNode kid = it.get();
                            contains |= matchesFilterRecursively(kid);
                            if (contains) break;
                            it = it.next();
                        }
                    }
                    chosen = contains;
                    if (subject == SUBJECT.UNLESS) {
                        chosen = !chosen;
                    }
                } else {
                    throw new RuntimeException("unknown mod");
                }
            }
        }
        if (chosen) {
            if (subject==SUBJECT.UNLESS && recurse) {
                // we're deleting this, but "delete everything unless x" means if a child matches X, we keep them.
                var it = node.iterateChildren(true);
                if (it!=null && it.isAggregate()) it = it.next();
                boolean anyChildNotChosen = false;
                while (it!=null) {
                    if (!innerTargets(it.get(), true, level+1)) {
                        anyChildNotChosen = true;
                    }
                    it = it.next();
                }
                if (anyChildNotChosen && (options & OPTIONS.KEEP_PARENTS)!=0) {
                    // keep our skeleton around
                    chosen = false;
                }
            } else {
                // normal case: children go boom.
                markAllChildren(node, true);
            }
        } else if (recurse) {
            // we're not deleted, but our children may be. Let's check.
            var it = node.iterateChildren(true);
            if (it!=null && it.isAggregate()) it = it.next();
            while (it!=null) {
                innerTargets(it.get(), true, level+1);
                it = it.next();
            }
        }
        shouldDelete.put(node, chosen);
        return chosen;
    }

    private void markAllChildren(JsonNode dad, boolean whetherDelete) {
        if (null==dad) return;
        var it = dad.iterateChildren(true);
        while (it!=null) {
            JsonNode kid = it.get();
            markAllChildren(kid, whetherDelete);
            shouldDelete.put(kid, whetherDelete);
            it = it.next();
        }
    }

    private boolean matchesFilter(JsonNode node) {
        if ((filter & FILTER.PINNED) != 0) {
            if (node.pinned) return true;
        }
        if ((filter & FILTER.SELECTED) != 0) {
            if (node.isAtCursor()) return true;
        }
        if ((filter & FILTER.VISIBLE) != 0) {
            if (node.isVisible()) return true;
        }
        return false;
    }

    // true if this node or any of its children matches the filter.
    private boolean matchesFilterRecursively(JsonNode node) {
        if (matchesFilter(node)) return true;
        var it = node.iterateChildren(true);
        while (it!=null) {
            if (it.isAggregate()) {
                it = it.next();
                continue;
            }
            if (matchesFilterRecursively(it.get())) return true;
            it = it.next();
        }
        return false;
    }

    public String explain() {
        StringBuilder ret = new StringBuilder("Delete");
        switch (target) {
            case EVERYTHING:
                if (subject== SUBJECT.THAT && filter==FILTER.NOTHING) {
                    ret.append(" nothing.");
                    return ret.toString();
                }
                ret.append(" everything");
                break;
            case CHILDREN:
                ret.append(" children of the main cursor");
                break;
        }
        switch (subject) {
            case THAT:
                ret.append(" that");
                break;
            case UNLESS:
                if (filter==FILTER.NOTHING) {
                    // no exception
                    ret.append(".");
                    return ret.toString();
                }
                ret.append(" unless it");
                break;
        }
        switch (mod) {
            case IS:
                ret.append(" is");
                break;
            case CONTAINS_DIRECT:
                ret.append(" contains (directly)");
                break;
            case CONTAINS_INDIRECT:
                ret.append(" (or a descendant) contains");
                break;
        }
        if (filter== FILTER.PINNED) {
            ret.append(" a pin.");
        } else if (filter == FILTER.SELECTED) {
            ret.append(" a cursor.");
        } else if (filter == FILTER.VISIBLE) {
            ret.append(" visible.");
        } else {
            List<String> filterWords = new ArrayList<>();
            if ((filter & FILTER.PINNED) != 0) filterWords.add("pinned");
            if ((filter & FILTER.SELECTED) != 0) filterWords.add("selected");
            if ((filter & FILTER.VISIBLE) != 0) filterWords.add("visible");
            ret.append(" ");
            for (int i=0; i<filterWords.size(); i++) {
                if (i+1==filterWords.size()) {
                    ret.append(" or ");
                } else if (i>0) {
                    ret.append(", ");
                }
                ret.append(filterWords.get(i));
            }
            ret.append(".");
        }

        return ret.toString();
    }

}
