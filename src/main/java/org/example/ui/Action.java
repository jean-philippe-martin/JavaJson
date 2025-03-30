package org.example.ui;

public enum Action {
    NOTHING,
    QUIT,
    // Movement
    NAV_NEXT_LINE,
    NAV_PREV_LINE,
    NAV_NEXT_COUSIN,
    NAV_PREV_COUSIN,
    // Transform
    GROUPBY,
    UNION,
    // Action at main cursor
    COPY_AT_CURSORS,
    ADD_COPY_AT_CURSORS,
    // Menus
    SHOW_MAIN_MENU,
    SHOW_HELP_SCREEN,
    SHOW_FIND_MENU,
    SHOW_ACTION_MENU,
    SHOW_PASTE_MENU,
    SHOW_SORT_MENU,
    SHOW_AGGREGATE_MENU,
    SHOW_DELETE_MENU,
    HIDE_MENU
}
