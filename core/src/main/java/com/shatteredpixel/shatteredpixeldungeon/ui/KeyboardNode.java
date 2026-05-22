package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.watabou.noosa.ui.Component;

/**
 * A focus node used by SpatialKeyboardNavigator.
 *
 * Explicit up/down/left/right links are used first. If a link is absent, the
 * navigator falls back to spatial search. This gives dashboard-like UI precise
 * behaviour while still supporting grids and dynamic layouts.
 */
public class KeyboardNode {

    public final Component component;

    public KeyboardNode up;
    public KeyboardNode down;
    public KeyboardNode left;
    public KeyboardNode right;

    public KeyboardNode(Component component) {
        this.component = component;
    }

    public KeyboardNode up(KeyboardNode node) {
        this.up = node;
        if (node != null) node.down = this;
        return this;
    }

    public KeyboardNode down(KeyboardNode node) {
        this.down = node;
        if (node != null) node.up = this;
        return this;
    }

    public KeyboardNode left(KeyboardNode node) {
        this.left = node;
        if (node != null) node.right = this;
        return this;
    }

    public KeyboardNode right(KeyboardNode node) {
        this.right = node;
        if (node != null) node.left = this;
        return this;
    }
}
