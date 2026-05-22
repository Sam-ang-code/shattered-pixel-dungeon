package com.shatteredpixel.shatteredpixeldungeon.ui;

/**
 * Common contract for UI widgets that can be controlled by keyboard navigation.
 *
 * The navigator owns focus movement. Each widget owns its own behaviour when it
 * is focused, activated, or receives widget-specific adjustment keys.
 */
public interface KeyboardFocusable {

    void onKeyboardFocus(boolean focused);

    void onKeyboardActivate();

    /**
     * @return true if the widget consumed the left action, false if focus should move.
     */
    boolean onKeyboardLeft();

    /**
     * @return true if the widget consumed the right action, false if focus should move.
     */
    boolean onKeyboardRight();
}

