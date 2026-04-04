package org.example;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import org.example.ui.TextWidth;
import org.example.ui.Theme;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Drawer {

    static final int INDENT = 2;

    static final Pattern colorPattern = Pattern.compile("#([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})");

    private static final String UNICODE_ELLIPSIS = "\u2026";

    static @Nullable DecimalFormat decimalFormat;

    /** Screen lines used to draw preserved source comments (leading + trailing; folded counts as 1 each). */
    public static int countCommentDisplayLines(JsonNode json) {
        return countLeadingCommentDisplayLines(json) + countTrailingCommentDisplayLines(json);
    }

    private static int countLeadingCommentDisplayLines(JsonNode json) {
        if (!json.hasValueLeadingTrivia()) {
            return 0;
        }
        if (json.getCommentFolded()) {
            return 1;
        }
        int n = 0;
        for (String s : Objects.requireNonNull(json.getValueLeadingTrivia()).replace('\r', '\n').split("\n", -1)) {
            if (!s.trim().isEmpty()) {
                n++;
            }
        }
        return n > 0 ? n : 1;
    }

    private static int countTrailingCommentDisplayLines(JsonNode json) {
        if (!json.hasValueTrailingTrivia()) {
            return 0;
        }
        if (json.getCommentFolded()) {
            return 1;
        }
        int n = 0;
        for (String s : Objects.requireNonNull(json.getValueTrailingTrivia()).replace('\r', '\n').split("\n", -1)) {
            if (!s.trim().isEmpty()) {
                n++;
            }
        }
        return n > 0 ? n : 1;
    }

    /** One-line synthetic comment display when folded; avoids "// //" when trivia already starts with // or ,. */
    private static String syntheticLineForFoldedSourceComment(String oneLine) {
        if (oneLine.startsWith("//") || oneLine.startsWith("#") || oneLine.startsWith("/*") || oneLine.startsWith(",")) {
            return oneLine;
        }
        return "// " + oneLine;
    }

    // Where on the screen we drew the cursor.
    // If that was too low, maybe you'll want to adjust and try again?
    private int cursorScreenLine = 0;
    private boolean drewCursor = false;
    // number of "sub-cursor" steps available. This is to allow you to scroll through
    // a string that takes up multiple lines on the screen.
    public int substepsAvailable;
    public int substep;
    public Cursor substepCursor  = null;
    // 0=dunno, 1=down, -1=up
    public int directionOfTravel = 0;

    public static void printMaybeReversed(TextGraphics g, TerminalPosition pos, String s, boolean bolded) {
        if (bolded) {
            g.putString(pos, s, SGR.REVERSE);
        } else {
            g.putString(pos, s);
        }
    }

    public Drawer() {
        this(null);
    }

    public Drawer(@Nullable Locale defaultLocale) {
        if (null==defaultLocale) defaultLocale = Locale.getDefault(Locale.Category.DISPLAY);
        NumberFormat numberFormat = NumberFormat.getInstance(defaultLocale);
        if (!(numberFormat instanceof DecimalFormat)) {
            decimalFormat = null;
        } else {
            // Set the decimal format to what I like.
            // Ideaally at some point we want settings to override this.
            decimalFormat = (DecimalFormat) numberFormat;
            DecimalFormatSymbols symbols = decimalFormat.getDecimalFormatSymbols();
            symbols.setGroupingSeparator('\'');
            decimalFormat.setDecimalFormatSymbols(symbols);
        }
    }

    public int getCursorLineLastTime() {
        return this.cursorScreenLine;
    }

    /**
     * Draw preserved Hjson trivia above a value. Returns number of terminal rows used.
     */
    private int printSourceCommentTrivia(TextGraphics g, TerminalPosition start, int initialOffset, JsonNode json, Deleter deleter) {
        if (!json.hasValueLeadingTrivia()) {
            return 0;
        }
        TextGraphics cg = Theme.withColor(g, Theme.selected.synthetic);
        possiblyChangeToDeletedColors(cg, json, deleter);
        int w = Math.max(1, g.getSize().getColumns() - start.getColumn() - initialOffset);
        String raw = Objects.requireNonNull(json.getValueLeadingTrivia()).replace('\r', '\n');
        if (json.getCommentFolded()) {
            String oneLine = raw.replace('\n', ' ').trim();
            String prefixed = syntheticLineForFoldedSourceComment(oneLine);
            int ellW = TextWidth.length(UNICODE_ELLIPSIS);
            if (TextWidth.length(prefixed) > w && w > ellW) {
                int fit = TextWidth.charsInSpace(prefixed, 0, w - ellW);
                if (fit < prefixed.length()) {
                    prefixed = prefixed.substring(0, fit) + UNICODE_ELLIPSIS;
                }
            }
            printMaybeReversed(cg, start.withRelativeColumn(initialOffset), prefixed, json.isAtCursor());
            return 1;
        }
        int down = 0;
        for (String part : raw.split("\n", -1)) {
            String t = part.trim();
            if (t.isEmpty()) {
                continue;
            }
            String line = (t.startsWith("//") || t.startsWith("#") || t.startsWith("/*")) ? t : "// " + t;
            printMaybeReversed(cg, start.withRelative(initialOffset, down), line, json.isAtCursor());
            down++;
        }
        if (down == 0) {
            printMaybeReversed(cg, start.withRelativeColumn(initialOffset), "// ", json.isAtCursor());
            return 1;
        }
        return down;
    }

    /**
     * Preserved Hjson trivia after a value (e.g. {@code , // inline}). Starts at {@code at}; wraps using
     * {@code continuationColumn} when the first column is past the terminal width.
     *
     * @return extra rows used
     */
    private int printValueTrailingTrivia(TextGraphics g, TerminalPosition at, int continuationColumn, JsonNode json, Deleter deleter) {
        if (!json.hasValueTrailingTrivia()) {
            return 0;
        }
        TextGraphics cg = Theme.withColor(g, Theme.selected.synthetic);
        possiblyChangeToDeletedColors(cg, json, deleter);
        TerminalPosition pos = at;
        if (pos.getColumn() >= g.getSize().getColumns()) {
            pos = new TerminalPosition(Math.min(continuationColumn, Math.max(0, g.getSize().getColumns() - 1)), pos.getRow() + 1);
        }
        int startCol = pos.getColumn();
        int w = Math.max(1, g.getSize().getColumns() - startCol);
        String raw = Objects.requireNonNull(json.getValueTrailingTrivia()).replace('\r', '\n');
        if (json.getCommentFolded()) {
            String oneLine = raw.replace('\n', ' ').trim();
            String display = syntheticLineForFoldedSourceComment(oneLine);
            int ellW = TextWidth.length(UNICODE_ELLIPSIS);
            if (TextWidth.length(display) > w && w > ellW) {
                int fit = TextWidth.charsInSpace(display, 0, w - ellW);
                if (fit < display.length()) {
                    display = display.substring(0, fit) + UNICODE_ELLIPSIS;
                }
            }
            printMaybeReversed(cg, pos, display, json.isAtCursor());
            return 1;
        }
        int down = 0;
        for (String part : raw.split("\n", -1)) {
            String t = part.trim();
            if (t.isEmpty()) {
                continue;
            }
            boolean rawLine = t.startsWith("//") || t.startsWith("#") || t.startsWith("/*") || t.startsWith(",");
            String line = rawLine ? t : "// " + t;
            TerminalPosition linePos = down == 0 ? pos : new TerminalPosition(continuationColumn, pos.getRow() + down);
            int lineW = down == 0 ? w : Math.max(1, g.getSize().getColumns() - continuationColumn);
            if (TextWidth.length(line) > lineW && lineW > TextWidth.length(UNICODE_ELLIPSIS)) {
                int fit = TextWidth.charsInSpace(line, 0, lineW - TextWidth.length(UNICODE_ELLIPSIS));
                if (fit < line.length()) {
                    line = line.substring(0, fit) + UNICODE_ELLIPSIS;
                }
            }
            printMaybeReversed(cg, linePos, line, json.isAtCursor());
            down++;
        }
        return down;
    }

    // inFoldedContext = we're folded, only print pinned rows.
    public int printJsonMap(TextGraphics g, JsonNodeMap jsonMap, TerminalPosition start, int initialOffset, boolean inFoldedContext, boolean inSyntheticContext, Deleter deleter) {
        TextGraphics myG = Theme.clone(g);
        boolean beingDeleted = possiblyChangeToDeletedColors(myG, jsonMap, deleter);
        int line = 0;
        Collection<String> keys = jsonMap.getKeysInOrder();
        int indent = start.getColumn();
        TerminalPosition pos = start;

        int triviaLines = printSourceCommentTrivia(g, pos, initialOffset, jsonMap, deleter);
        pos = pos.withRelativeRow(triviaLines);
        line += triviaLines;

        // we mark out aggregate data so it is visually distinct.
        String prefix = "";
        if (inSyntheticContext) prefix = "//   ";

        // In a folded context, we only show pinned things.
        if (jsonMap.getFolded()) inFoldedContext = true;
        // Pinning an object means we show the whole object
        if (jsonMap.getPinned()) inFoldedContext = false;

        if (inFoldedContext) {
            if (!jsonMap.hasPins()) {
                TerminalPosition bpos = pos.withRelativeColumn(initialOffset);
                printMaybeReversed(myG, bpos,  "{ ... }", jsonMap.isAtCursor());
                int trailRows = printValueTrailingTrivia(g, new TerminalPosition(bpos.getColumn(), bpos.getRow() + 1), bpos.getColumn(), jsonMap, deleter);
                return line + 1 + trailRows;
            }
            // we contain at least one thing that'll be shown, so open up.
            printMaybeReversed(myG, pos.withRelativeColumn(initialOffset),   "{ ...", jsonMap.isAtCursor());
        } else {
            printMaybeReversed(myG, pos.withRelativeColumn(initialOffset),  "{", jsonMap.isAtCursor());
        }

        if (jsonMap.getAnnotation()!=null && !jsonMap.getAnnotation().isEmpty()) {
            String countAnno = " // " + jsonMap.getAnnotation();
            TextGraphics green = Theme.withColor(g, Theme.selected.synthetic);
            green.putString(pos.withRelativeColumn(initialOffset+1), countAnno);
        }

        int myIndent = INDENT;
        //if (inSyntheticContext) myIndent += 3;
        pos = pos.withRelativeColumn(myIndent).withRelativeRow(1);

        line += 1;
        for (JsonNodeIterator it = jsonMap.iterateChildren(true); it!=null; it=it.next()) {
            JsonNode child = it.get();
            String key = (String)it.key();
            if (inFoldedContext && !child.hasPins()) {
                // skip this child
                continue;
            }
            String aggComment = "";
            if (inSyntheticContext && child.aggregateComment != null && !child.aggregateComment.isEmpty()) {
                aggComment = child.aggregateComment + " ";
                printMaybeReversed(g, pos.withColumn(2), "//", false);
            }

            TextGraphics g2 = Theme.clone(g);
            possiblyChangeToDeletedColors(g2, child, deleter);
            TextGraphics g_key = Theme.withColor(g, Theme.selected.key);
            possiblyChangeToDeletedColors(g_key, child, deleter);
            printMaybeReversed(g2, pos, aggComment, jsonMap.isAtCursor(key));
            TerminalPosition pos2 = pos;
            if (!it.isAggregate()) {
                // skip key for aggregate.
                printMaybeReversed(g_key, pos.withRelativeColumn(TextWidth.length(aggComment)), "\"" + key + "\"", jsonMap.isAtCursor(key));
                pos2 = pos.withRelativeColumn(TextWidth.length(aggComment) + 2 + TextWidth.length(key));
            }
            // normal case, user data.
            if (child instanceof JsonNodeValue) {
                int height;
                if (inSyntheticContext) {
                    printGutterIndicator(g, pos, child, 1, deleter);
                    height = 1;
                } else {
                    g2 = Theme.clone(g2);
                    possiblyChangeToDeletedColors(g2, child, deleter);
                    JsonNodeValue v = (JsonNodeValue) child;
                    Object val = v.getValue();
                    TerminalPosition pos4 = pos2;
                    if (it.isAggregate()) {
                        g2 = Theme.withColor(g, Theme.selected.synthetic);
                        g2.putString(pos4.withColumn(2), "//");
                        //String intro = jsonMap.aggregateComment + "() ";
                        String intro = key + "() ";
                        if (pos4.getColumn()<=5) {
                            // move to the right to make room for the comment symbols
                            pos4 = pos4.withColumn(5);
                        }
                        g2.putString(pos4, intro);
                        //pos4 = pos.withColumn(pos4.getColumn() + intro.length());
                        pos4 = pos.withRelativeColumn(TextWidth.length(intro)+1);
                    } else {
                        g2.putString(pos4, ": ");
                    }
                    height = printJsonSubtree(g2, pos, pos4.getColumn() - pos.getColumn() + 2, child, inFoldedContext, inSyntheticContext || v.isSynthetic(), deleter);
                }
                line += height;
                pos = pos.withRelativeRow(height);
            } else {
                myG.putString(pos2, ": ");
                int childOffset = TextWidth.length(aggComment) + TextWidth.length(key) + 4;
                int childHeight = printJsonSubtree(g, pos, childOffset, child, inFoldedContext, inSyntheticContext, deleter);
                line += childHeight;
                pos = pos.withRelativeRow(childHeight);
            }
            // stop drawing if we're off the screen.
            if (drewCursor && pos.getRow() > g.getSize().getRows() + 10) break;
        }
        line += 1;
        pos = pos.withRelativeColumn(-myIndent);
        g.putString(pos.withColumn(2), prefix);
        myG.putString(pos, "}");
        int mapTrailRows = printValueTrailingTrivia(g, new TerminalPosition(pos.getColumn(), pos.getRow() + 1), pos.getColumn(), jsonMap, deleter);
        return line + mapTrailRows;
    }

    public boolean tryCursorUp(Cursor userCursor) {
        directionOfTravel=-1;
        if (userCursor!=substepCursor) return false;
        if (substep==0) return false;
        substep--;
        return true;
    }

    public boolean tryCursorDown(Cursor userCursor) {
        directionOfTravel=1;
        if (userCursor!=substepCursor) return false;
        if (substep>=substepsAvailable-1) return false;
        substep++;
        return true;
    }

    // Returns how many lines it went down, beyond the initial one.
    // jsonObj can be String, List, LinkedHashMap<String, Object>, ...
    public int printJsonTree(TextGraphics g, TerminalPosition start, int initialOffset, JsonNode json, Deleter deleter) {
        this.drewCursor = false;
        return printJsonSubtree(g, start, initialOffset, json, false, false, deleter);
    }

    public void printGutterIndicator(TextGraphics g, TerminalPosition start, JsonNode json, int lines, Deleter deleter) {
        // Make sure the text is on top of the indicators.
        g = g.newTextGraphics(TerminalPosition.TOP_LEFT_CORNER, new TerminalSize(start.getColumn(), g.getSize().getRows()));
        if (json.isAtPrimaryCursor()) {
            int offset = 0;
            boolean valueHasSubsteps = (json instanceof JsonNodeValue)
                    && (json.getValue() instanceof String || json.hasValueLeadingTrivia() || json.hasValueTrailingTrivia());
            if (json.whereIAm == substepCursor) {
                if (valueHasSubsteps) {
                    substepsAvailable = lines;
                } else {
                    substepsAvailable = 1;
                }
                if (substep >= substepsAvailable) substep = substepsAvailable-1;
                offset = substep;
            } else {
                if (valueHasSubsteps) {
                    substepsAvailable = lines;
                } else {
                    substepsAvailable = 1;
                }
                substepCursor = json.whereIAm;
                if (directionOfTravel == -1) {
                    // going up, start at bottom
                    substep = substepsAvailable-1;
                    offset = substep;
                } else {
                    substep = 0;
                    offset = substep;
                }
            }
            if (json.parent!=null) {
                g.putString(start.withColumn(0).withRelativeRow(offset), ">>");
            }
            this.cursorScreenLine = start.getRow() + offset;
            this.drewCursor = true;
        }
        if (deleter!=null && deleter.targets(json)) {
            g.putString(start.withColumn(0), "×");
        }
        if (json.getPinned()) {
            // draw the pin
            g.putString(start.withColumn(0), "P");
        }
        if (json.isAtFork()) {
            g.putString(start.withColumn(0), "*");
        }
    }

    // Returns how many lines it went down, beyond the initial one.
    // jsonObj can be String, List, LinkedHashMap<String, Object>, ...
    public int printJsonSubtree(TextGraphics g, TerminalPosition start, int initialOffset, JsonNode json, boolean inFoldedContext, boolean inSyntheticContext, Deleter deleter) {
        int lines = innerPrintJsonSubtree(g, start, initialOffset, json, inFoldedContext, inSyntheticContext, deleter);
        printGutterIndicator(g, start, json, lines, deleter);
        return lines;
    }

    public int innerPrintJsonSubtree(TextGraphics g, TerminalPosition start, int initialOffset, JsonNode json, boolean inFoldedContext, boolean inSyntheticContext, Deleter deleter) {
        TextGraphics myG = Theme.clone(g);
        boolean beingDeleted = possiblyChangeToDeletedColors(myG, json, deleter);
        int line = 0;
        if (json instanceof JsonNodeValue) {
            JsonNodeValue jsonValue = (JsonNodeValue) json;
            int lines = 0;
            if (inFoldedContext && !json.hasPins()) {
                // skip
                return 0;
            }
            int commentH = printSourceCommentTrivia(g, start, initialOffset, jsonValue, deleter);
            start = start.withRelativeRow(commentH);
            lines += commentH;

            String annotation = jsonValue.getAnnotation();
            boolean skipSynAnn = json.shouldSkipSyntheticAnnotation();
            int annLinesDrawn = 0;
            if (!annotation.isEmpty() && !skipSynAnn) {
                TextGraphics gg = Theme.withColor(g, Theme.selected.synthetic);
                printMaybeReversed(gg, start.withRelativeColumn(initialOffset), "// " + annotation, false);
                start = start.withRelativeRow(1);
                lines++;
                annLinesDrawn = 1;
            }

            Object value = jsonValue.getValue();
            int fallbackCol = start.getColumn() + initialOffset;
            if (null==value) {
                var g_num = Theme.withColor(g, Theme.selected.value_null);
                possiblyChangeToDeletedColors(g_num, json, deleter);
                String numStr = formatNumber(value);
                printMaybeReversed(g_num, start.withRelativeColumn(initialOffset), numStr, json.isAtCursor());
                TerminalPosition afterVal = start.withRelativeColumn(initialOffset + TextWidth.length(numStr));
                int trows = printValueTrailingTrivia(g, afterVal, fallbackCol, json, deleter);
                if (json.isAtPrimaryCursor()) {
                    substepsAvailable = commentH + annLinesDrawn + 1 + trows;
                }
                return lines + 1 + trows;
            } else if (value instanceof String) {
                String str = "\"" + (String)value + "\"";
                TextGraphics g_str = Theme.withColor(g, Theme.selected.value_str);
                possiblyChangeToDeletedColors(g_str, json, deleter);
                // todo: use actual screen width
                int w = g.getSize().getColumns() - start.getColumn() - initialOffset;
                int down = 0;
                String lastChunk = str;
                if (jsonValue.getFolded()) {
                    // show only one line, regardless of length
                    int charsUntilEllipsis = TextWidth.charsInSpace(str, 0, w-3);
                    if (charsUntilEllipsis<str.length()) {
                        // not enough room for the whole string
                        int charsLeft = TextWidth.charsInSpace(str, charsUntilEllipsis, 3);
                        if (charsUntilEllipsis+charsLeft<str.length()) {
                            // even if we don't put the ellipsis, not enough room for the string. So, let's cut.
                            str = str.substring(0, charsUntilEllipsis) + "...";
                        }
                    }
                    printMaybeReversed(g_str, start.withRelativeColumn(initialOffset), str, json.isAtCursor());
                    lastChunk = str;
                    down = 1;
                } else {
                    int index = 0;
                    int zeroes = 0;
                    while (index < str.length() && zeroes<2) {
                        int room = TextWidth.charsInSpace(str, index, w);
                        String oneLine = str.substring(index, index+room);
                        printMaybeReversed(g_str, start.withRelative(initialOffset, down), oneLine, json.isAtCursor());
                        lastChunk = oneLine;
                        index += room;
                        down++;
                        if (room==0) zeroes++;
                    }
                }
                lines += down;

                //printMaybeReversed(g_str, start.withRelativeColumn(initialOffset), str, json.isAtCursor());
                // Is this a color? Add a sample.
                Matcher colorMatcher = colorPattern.matcher(str);
                boolean found = colorMatcher.find();
                if (found && colorMatcher.groupCount()==3) {
                    int cr = Integer.parseInt(colorMatcher.group(1), 16);
                    int cg = Integer.parseInt(colorMatcher.group(2), 16);
                    int cb = Integer.parseInt(colorMatcher.group(3), 16);
                    TextGraphics gg = Theme.withColor(g, Theme.selected.synthetic);
                    gg.putString(start.withRelativeColumn(initialOffset + 3 + str.length()), "//");
                    TextColor col = TextColor.Indexed.fromRGB(cr, cg, cb);
                    gg.setForegroundColor(col);
                    gg.putString(start.withRelativeColumn(initialOffset + 6 + str.length()), "██");
                }
                TerminalPosition lastStrPos = start.withRelative(initialOffset, down - 1);
                int trailAtCol = lastStrPos.getColumn() + TextWidth.length(lastChunk);
                int trows = printValueTrailingTrivia(g, new TerminalPosition(trailAtCol, lastStrPos.getRow()), fallbackCol, json, deleter);
                if (json.isAtPrimaryCursor()) {
                    substepsAvailable = commentH + annLinesDrawn + down + trows;
                }
                return lines + trows;
            } else {
                var g_num = Theme.withColor(g, Theme.selected.value_num);
                possiblyChangeToDeletedColors(g_num, json, deleter);
                String numStr = formatNumber(value);
                printMaybeReversed(g_num, start.withRelativeColumn(initialOffset), numStr, json.isAtCursor());
                TerminalPosition afterVal = start.withRelativeColumn(initialOffset + TextWidth.length(numStr));
                int trows = printValueTrailingTrivia(g, afterVal, fallbackCol, json, deleter);
                if (json.isAtPrimaryCursor()) {
                    substepsAvailable = commentH + annLinesDrawn + 1 + trows;
                }
                return lines + 1 + trows;
            }
        }
        if (json instanceof JsonNodeList) {
            JsonNodeList jsonList = (JsonNodeList)json;
            inFoldedContext = (jsonList.folded || inFoldedContext) && !jsonList.getPinned();
            TerminalPosition pos = start;
            int listTrivia = printSourceCommentTrivia(g, pos, initialOffset, jsonList, deleter);
            pos = pos.withRelativeRow(listTrivia);
            int listLine = listTrivia;
            TerminalPosition pos2 = pos.withRelativeColumn(initialOffset);
            JsonNode dad = jsonList.getParent();
            if (json.isAtCursor() && (dad==null || dad instanceof JsonNodeList)) {
                // we have no label, so let's make the bracket bold.
                myG.putString(pos2, "[", SGR.REVERSE);
            } else {
                myG.putString(pos2, "[");
            }
            pos2 = pos2.withRelativeColumn(1);
            if (inFoldedContext) {
                if (jsonList.hasPins()) {
                    myG.putString(pos2, " ...");
                    pos2 = pos2.withRelativeColumn(4);
                } else {
                    myG.putString(pos2, " ... ]");
                    pos2 = pos2.withRelativeColumn(6);
                }
            }
            String countAnno = " // " + jsonList.getAnnotation();
            if (!jsonList.getAnnotation().isEmpty()) countAnno += ", ";
            int c = jsonList.childCount();
            countAnno += c;
            if (c==1) countAnno += " entry";
            else countAnno += " entries";
            TextGraphics green = Theme.withColor(g, Theme.selected.synthetic);
            green.putString(pos2, countAnno);
            if (inFoldedContext && !jsonList.hasPins()) {
                return listLine + 1;
            }
            TerminalPosition pos3 = pos.withRelativeColumn(INDENT);
            listLine += 1;
            pos3 = pos3.withRelativeRow(1);

            for (JsonNodeIterator it = jsonList.iterateChildren(true); it!=null; it=it.next()) {
                JsonNode child = it.get();
                if (inFoldedContext && !child.hasPins()) {
                    // skip that one, we're folded and it's not pinned.
                    continue;
                }
                TextColor oldColor = g.getForegroundColor();
                String intro = "";
                TerminalPosition pos4 = pos3;
                if (it.isAggregate()) {
                    g.setForegroundColor(Theme.selected.synthetic);
                    g.putString(pos4.withColumn(2), "//");
                    intro = jsonList.aggregateComment + "() ";
                    if (pos4.getColumn()<=5) {
                        // move to the right to make room for the comment symbols
                        pos4 = pos4.withColumn(5);
                    }
                    g.putString(pos4, intro);
                }
                int height = printJsonSubtree(g, pos4, intro.length(), child, inFoldedContext, inSyntheticContext || it.isAggregate(), deleter);
                g.setForegroundColor(oldColor);
                listLine += height;
                pos3 = pos3.withRelativeRow(height);
                // stop drawing if we're off the screen.
                if (drewCursor && pos3.getRow() > g.getSize().getRows() + 10) break;
            }
            pos = pos3.withRelativeColumn(-INDENT);
            myG.putString(pos, "]");
            int listTrailRows = printValueTrailingTrivia(g, new TerminalPosition(pos.getColumn(), pos.getRow() + 1), pos.getColumn(), jsonList, deleter);
            return listLine + 1 + listTrailRows;
        }
        else if (json instanceof JsonNodeMap) {
            JsonNodeMap jsonMap = (JsonNodeMap) json;
            if (inFoldedContext && !(jsonMap.getPinned() || jsonMap.hasPins())) {
                return 0; // hidden in the fold
            }
            return printJsonMap(g, jsonMap, start, initialOffset, inFoldedContext, inSyntheticContext, deleter);
        }

        throw new RuntimeException("Unrecognized type: " + json.getClass());
    }

    public static String formatNumber(Object maybeNumber) {
        if (null==maybeNumber) return "null";
        String str = maybeNumber.toString();
        if (decimalFormat!=null && (maybeNumber instanceof Long || maybeNumber instanceof Double || maybeNumber instanceof Integer)) {
            str = decimalFormat.format(maybeNumber);
        }
        return str;
    }

    // returns true if we changed to deleted colors.
    private boolean possiblyChangeToDeletedColors(TextGraphics g, JsonNode node, Deleter deleter) {
        if (null==deleter) return false;
        var shouldDelete = deleter.targets(node);
        if (shouldDelete) {
            g.setForegroundColor(Theme.selected.deleting_row_fg);
            g.setBackgroundColor(Theme.selected.deleting_row_bg);
        }
        return shouldDelete;
    }

}
