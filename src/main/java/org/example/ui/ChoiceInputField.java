package org.example.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A text-driven input field but you have to choose between a pre-set list of options.
 * You type the prefix and then you get to pick from a drop-down if you want.
 */
public class ChoiceInputField {

    // Focused looks different to indicate we're reading user input
    private boolean focused = true;
    // Is the drop-down dropped down?
    private boolean showChoices = false;

    // The valid choices. Always has length at least 1.
    private final @NotNull String[] choices;
    private int choiceIndex;
    // the choices we show in the drop-down
    private List<String> displayChoices;
    // which one is currently selected
    private int displayIndex = 0;
    // how far down we're scrolling
    private int displayScroll = 0;

    private int dropdownSize = 5;

    // What the user typed
    private @NotNull String typed = "";
    // What we automatically add so the full text is one of the choices.
    private @NotNull String continuation = "";

    public ChoiceInputField(@NotNull String[] choices) {
        if (choices.length==0) {
            this.choices = new String[] {""};
        } else {
            this.choices = choices.clone();
        }
        Arrays.sort(this.choices);
        choiceIndex = 0;
        typed = "";
        continuation = this.choices[0];
    }

    public void draw(TextGraphics where) {
        TextGraphics g2 = where.newTextGraphics(TerminalPosition.TOP_LEFT_CORNER.withRelativeColumn(2), where.getSize().withRows(1).withRelativeColumns(-4));
        if (focused) {
            if (showChoices) {
                TextGraphics g3 = where.newTextGraphics(TerminalPosition.TOP_LEFT_CORNER.withRelative(1,1), where.getSize().withRelative(-2,-1));
                int size = g3.getSize().getRows();
                size = Math.min(size, dropdownSize);
                g3 = g3.newTextGraphics(TerminalPosition.TOP_LEFT_CORNER, g3.getSize().withRows(size));
                g3.setForegroundColor(where.getForegroundColor());
                g3.fill(' ');
                TerminalPosition c = TerminalPosition.TOP_LEFT_CORNER.withRelative(1,1);
                Optional<TerminalPosition> brackets = Optional.empty();
                if (displayIndex < displayScroll) { displayScroll = displayIndex; }
                if (displayIndex - displayScroll > g3.getSize().getRows() - 3) {
                    // it would fall off the end of the box
                    displayScroll = displayIndex - g3.getSize().getRows() + 3;
                }
                for (int i=0; i<displayChoices.size(); i++) {
                    if (i<displayScroll) continue;
                    if (i>displayScroll+g3.getSize().getRows()) break;
                    if (i!=displayIndex) {
                        g3.putString(c, displayChoices.get(i));
                    } else {
                        g3.putString(c, displayChoices.get(i), SGR.REVERSE);
                        brackets = Optional.of(c);
                    }
                    c = c.withRelativeRow(1);
                }
                g3.setForegroundColor(TextColor.ANSI.CYAN);
                Rectangle.drawSingle(g3, TerminalPosition.TOP_LEFT_CORNER, g3.getSize());
                if (brackets.isPresent()) {
                    c = brackets.get();
                    g3.putString(c.withRelativeColumn(-1), ">");
                    g3.putString(c.withRelative(g3.getSize().getColumns()-2, 0), "<");
                }
            } else {
                TextColor old = where.getForegroundColor();
                where.setForegroundColor(TextColor.ANSI.CYAN);
                where.putString(TerminalPosition.TOP_LEFT_CORNER.withRelative(0, 0), ">");
                where.putString(TerminalPosition.TOP_LEFT_CORNER.withRelative(where.getSize().getColumns()-1, 0), "<");
                where.setForegroundColor(old);
            }
            g2.setForegroundColor(TextColor.ANSI.YELLOW_BRIGHT);
            g2.setBackgroundColor(TextColor.Indexed.fromRGB(0, 0, 128));
            g2.fill(' ');
            g2.putString(TerminalPosition.TOP_LEFT_CORNER, typed);
            g2.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
            g2.putString(TerminalPosition.TOP_LEFT_CORNER.withRelativeColumn(typed.length()), continuation);
        } else {
            g2.putString(TerminalPosition.TOP_LEFT_CORNER, typed + continuation);
        }
    }

    /** @return True if they processed the key. */
    public boolean update(KeyStroke key) {
        if (!focused) return false;
        if (key.getKeyType()== KeyType.Backspace) {
            if (!typed.isEmpty()) {
                typed = typed.substring(0,typed.length()-1);
                continuation = choices[choiceIndex].substring(typed.length());
                updateDisplayChoices(typed);
            }
        } else if (key.getKeyType()==KeyType.Tab) {
            showChoices = !showChoices;
            displayIndex = 0;
            displayScroll = 0;
            displayChoices = Arrays.stream(choices).filter(x -> x.startsWith(typed)).toList();
            // 2 for the border
            dropdownSize = displayChoices.size() + 2;
        } else if (key.getKeyType()==KeyType.ArrowUp) {
            if (!showChoices) return false;
            if (displayIndex>0) displayIndex -= 1;
            continuation = displayChoices.get(displayIndex).substring(typed.length());
        } else if (key.getKeyType()==KeyType.ArrowDown) {
            if (!showChoices) return false;
            displayIndex += 1;
            if (displayIndex>=displayChoices.size()) displayIndex = displayChoices.size()-1;
            continuation = displayChoices.get(displayIndex).substring(typed.length());
        } else if (key.getKeyType()==KeyType.Enter) {
            if (!showChoices) return false;
            typed = displayChoices.get(displayIndex);
            continuation = "";
            showChoices = false;
            updateChoiceIndex(typed);
        } else if (key.getCharacter() != null) {
            // normal key
            char k = key.getCharacter();
            if (k<' ') { return false; }
            boolean accepted = updateChoice(k);
            if (accepted) {
                typed += k;
                continuation = choices[choiceIndex].substring(typed.length());
                updateDisplayChoices(typed);
            }
        } else {
            // We don't handle that key
            return false;
        }
        return true;
    }

    // finds an exact match
    private void updateChoiceIndex(String choiceStr) {
        for (int i=0; i<choices.length; i++) {
            if (choices[i].equals(choiceStr)) {
                choiceIndex = i;
                return;
            }
        }
    }

    private void updateDisplayChoices(String typed) {
        if (!showChoices) return;
        String oldDisplayed = displayChoices.get(displayIndex);
        displayChoices = Arrays.stream(choices).filter(x -> x.startsWith(typed)).toList();
        displayIndex=0;
        displayScroll=0;
        String displayed = displayChoices.get(displayIndex);
        if (oldDisplayed.startsWith(typed) && !oldDisplayed.equals(displayed)) {
            // the previous choice is still an option, make sure it's still highlighted.
            for (int i=0; i<displayChoices.size(); i++) {
                if (displayChoices.get(i).equals(oldDisplayed)) {
                    displayIndex=i;
                    break;
                }
            }
        }
        continuation = displayChoices.get(displayIndex).substring(typed.length());
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    /** What the user chose */
    public String getChoice() {
        return choices[choiceIndex];
    }

    /** What the user typed so far (autocompletion excluded) */
    public @NotNull String getTyped() {
        return typed;
    }

    private boolean updateChoice(char c) {
        String cand = typed + c;
        String debugMsg = "";
        int oldChoiceIndex = choiceIndex;
        // If we backspaced into a place where an earlier choice is appropriate, switch to that.
        while (choiceIndex>0 && choices[choiceIndex-1].startsWith(cand)) {
            choiceIndex--;
        }
        debugMsg += " cand='" + cand + "' stage 1 choice: '" + choices[choiceIndex] +"'";
        if (choices[choiceIndex].startsWith(cand)) {
            return true;
        }
        // Before cand? Move forward
        while (choices[choiceIndex].compareTo(cand)<0) {
            if (choiceIndex==choices.length-1) break;
            choiceIndex += 1;
        }
        debugMsg += " stage 2 choice: '" + choices[choiceIndex] + "'";

        debugMsg += " choice[" + choiceIndex+"] .startsWith(cand)? " + choices[choiceIndex].startsWith(cand) + " choices.compareTo(cand)=" + (choices[choiceIndex].compareTo(cand));
        // Now move back to first the earliest string that starts with "cand" (or is before)
        while ((!choices[choiceIndex].startsWith(cand)) && choices[choiceIndex].compareTo(cand)>0) {
            if (choiceIndex==0) break;
            choiceIndex -= 1;
            debugMsg += " choice[" + choiceIndex+"] .startsWith(cand)? " + choices[choiceIndex].startsWith(cand) + " choices.compareTo(cand)=" + (choices[choiceIndex].compareTo(cand));
        }
        debugMsg += " choice[" + choiceIndex+"] .startsWith(cand)? " + choices[choiceIndex].startsWith(cand) + " choices.compareTo(cand)=" + (choices[choiceIndex].compareTo(cand));
        debugMsg += " stage 3 choice: " + choices[choiceIndex];
        // At this point, if there's an option in choices that starts with 'cand', we should have found it
        if (choices[choiceIndex].startsWith(cand)) {
            return true;
        }
        debugMsg += " choice does not start with '" + cand + "', returning to choice " + choices[oldChoiceIndex];
        debugMsg += " choices=" + String.join(",", choices);
        choiceIndex = oldChoiceIndex;
        return false;
    }


}
