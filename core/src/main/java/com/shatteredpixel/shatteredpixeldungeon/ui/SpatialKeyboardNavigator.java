package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.badlogic.gdx.Input;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

/**
 * Reusable keyboard focus manager for menu UI.
 *
 * Industrial-style behaviour:
 * - one primary focus owner
 * - explicit navigation graph first
 * - spatial fallback for grids/dynamic layouts
 * - navigation keys separated from slider value-changing keys
 */
public class SpatialKeyboardNavigator {

    private final ArrayList<KeyboardNode> nodes = new ArrayList<>();
    private int focusedIndex = 0;
    private Runnable onEscape;

    public void clear() {
        for (KeyboardNode node : nodes) {
            if (node.component instanceof KeyboardFocusable) {
                ((KeyboardFocusable) node.component).onKeyboardFocus(false);
            }
        }
        nodes.clear();
        focusedIndex = 0;
    }

    public KeyboardNode add(Component component) {
        if (component == null) return null;
        KeyboardNode node = new KeyboardNode(component);
        nodes.add(node);
        return node;
    }

    public void add(KeyboardNode node) {
        if (node != null && node.component != null) {
            nodes.add(node);
        }
    }

    public void setOnEscape(Runnable onEscape) {
        this.onEscape = onEscape;
    }

    public boolean handleKey(KeyEvent event) {

        if (!event.pressed) return false;

        if (event.code == Input.Keys.ESCAPE) {
            if (onEscape != null) {
                onEscape.run();
                return true;
            }
            return false;
        }

        if (nodes.isEmpty()) return false;

        Component focused = nodes.get(focusedIndex).component;

        switch (event.code) {

            case Input.Keys.TAB:
                moveLinear(1);
                return true;

            case Input.Keys.RIGHT:
                moveDirectional(Direction.RIGHT);
                return true;

            case Input.Keys.LEFT:
                moveDirectional(Direction.LEFT);
                return true;

            case Input.Keys.DOWN:
                moveDirectional(Direction.DOWN);
                return true;

            case Input.Keys.UP:
                moveDirectional(Direction.UP);
                return true;

            case Input.Keys.A:
            case Input.Keys.MINUS:
                if (focused instanceof OptionSlider) {
                    ((OptionSlider) focused).keyboardLeft();
                    return true;
                }
                return false;

            case Input.Keys.D:
            case Input.Keys.PLUS:
            case Input.Keys.EQUALS:
                if (focused instanceof OptionSlider) {
                    ((OptionSlider) focused).keyboardRight();
                    return true;
                }
                return false;

            case Input.Keys.ENTER:
            case Input.Keys.SPACE:
                activateFocused();
                return true;
        }

        return false;
    }

    public void updateFocus() {
        if (nodes.isEmpty()) return;
        if (focusedIndex < 0) focusedIndex = 0;
        if (focusedIndex >= nodes.size()) focusedIndex = nodes.size() - 1;

        for (int i = 0; i < nodes.size(); i++) {
            Component component = nodes.get(i).component;
            if (component instanceof KeyboardFocusable) {
                ((KeyboardFocusable) component).onKeyboardFocus(i == focusedIndex);
            }
        }
    }

    private void activateFocused() {
        Component focused = nodes.get(focusedIndex).component;
        if (focused instanceof KeyboardFocusable) {
            ((KeyboardFocusable) focused).onKeyboardActivate();
        } else if (focused instanceof StyledButton) {
            ((StyledButton) focused).keyboardClick();
        }
    }

    private void moveLinear(int direction) {
        if (nodes.isEmpty()) return;

        int next = focusedIndex;
        for (int attempts = 0; attempts < nodes.size(); attempts++) {
            next += direction;
            if (next >= nodes.size()) next = 0;
            else if (next < 0) next = nodes.size() - 1;

            if (isFocusable(nodes.get(next))) {
                focusedIndex = next;
                updateFocus();
                return;
            }
        }
    }

    private void moveDirectional(Direction direction) {
        KeyboardNode explicitTarget = explicitTarget(nodes.get(focusedIndex), direction);
        if (explicitTarget != null) {
            int idx = nodes.indexOf(explicitTarget);
            if (idx >= 0 && isFocusable(explicitTarget)) {
                focusedIndex = idx;
                updateFocus();
                return;
            }
        }

        moveSpatial(direction);
    }

    private KeyboardNode explicitTarget(KeyboardNode node, Direction direction) {
        switch (direction) {
            case UP:
                return node.up;
            case DOWN:
                return node.down;
            case LEFT:
                return node.left;
            case RIGHT:
                return node.right;
        }
        return null;
    }

    private void moveSpatial(Direction direction) {

        Component current = nodes.get(focusedIndex).component;

        float cLeft = current.left();
        float cRight = current.right();
        float cTop = current.top();
        float cBottom = current.bottom();
        float cCenterX = centerX(current);
        float cCenterY = centerY(current);

        int bestBeamIndex = -1;
        float bestBeamScore = Float.MAX_VALUE;

        int bestFallbackIndex = -1;
        float bestFallbackScore = Float.MAX_VALUE;

        for (int i = 0; i < nodes.size(); i++) {

            if (i == focusedIndex) continue;

            KeyboardNode candidateNode = nodes.get(i);
            if (!isFocusable(candidateNode)) continue;

            Component candidate = candidateNode.component;

            float tLeft = candidate.left();
            float tRight = candidate.right();
            float tTop = candidate.top();
            float tBottom = candidate.bottom();
            float tCenterX = centerX(candidate);
            float tCenterY = centerY(candidate);

            if (!isInDirection(direction, cLeft, cRight, cTop, cBottom, tLeft, tRight, tTop, tBottom)) {
                continue;
            }

            boolean beamOverlap = hasBeamOverlap(direction, cLeft, cRight, cTop, cBottom, tLeft, tRight, tTop, tBottom);
            float primaryDistance = primaryDistance(direction, cLeft, cRight, cTop, cBottom, tLeft, tRight, tTop, tBottom);
            float secondaryDistance = secondaryDistance(direction, cCenterX, cCenterY, tCenterX, tCenterY);
            float distance = euclideanDistance(cCenterX, cCenterY, tCenterX, tCenterY);

            float beamScore = primaryDistance * 100000f + secondaryDistance * 100f + distance;
            float fallbackScore = primaryDistance * 100000f + secondaryDistance * 10000f + distance;

            if (beamOverlap) {
                if (beamScore < bestBeamScore) {
                    bestBeamScore = beamScore;
                    bestBeamIndex = i;
                }
            } else {
                if (fallbackScore < bestFallbackScore) {
                    bestFallbackScore = fallbackScore;
                    bestFallbackIndex = i;
                }
            }
        }

        if (bestBeamIndex != -1) {
            focusedIndex = bestBeamIndex;
            updateFocus();
        } else if (bestFallbackIndex != -1) {
            focusedIndex = bestFallbackIndex;
            updateFocus();
        }
    }

    private boolean isFocusable(KeyboardNode node) {
        return node != null
                && node.component != null
                && node.component.visible
                && node.component.active;
    }

    private boolean isInDirection(Direction direction, float cLeft, float cRight, float cTop, float cBottom,
                                  float tLeft, float tRight, float tTop, float tBottom) {
        switch (direction) {
            case RIGHT:
                return tLeft >= cRight;
            case LEFT:
                return tRight <= cLeft;
            case DOWN:
                return tTop >= cBottom;
            case UP:
                return tBottom <= cTop;
        }
        return false;
    }

    private boolean hasBeamOverlap(Direction direction, float cLeft, float cRight, float cTop, float cBottom,
                                   float tLeft, float tRight, float tTop, float tBottom) {
        if (direction == Direction.LEFT || direction == Direction.RIGHT) {
            return rangesOverlap(cTop, cBottom, tTop, tBottom);
        } else {
            return rangesOverlap(cLeft, cRight, tLeft, tRight);
        }
    }

    private float primaryDistance(Direction direction, float cLeft, float cRight, float cTop, float cBottom,
                                  float tLeft, float tRight, float tTop, float tBottom) {
        switch (direction) {
            case RIGHT:
                return tLeft - cRight;
            case LEFT:
                return cLeft - tRight;
            case DOWN:
                return tTop - cBottom;
            case UP:
                return cTop - tBottom;
        }
        return Float.MAX_VALUE;
    }

    private float secondaryDistance(Direction direction, float cCenterX, float cCenterY, float tCenterX, float tCenterY) {
        if (direction == Direction.LEFT || direction == Direction.RIGHT) {
            return Math.abs(tCenterY - cCenterY);
        } else {
            return Math.abs(tCenterX - cCenterX);
        }
    }

    private boolean rangesOverlap(float aStart, float aEnd, float bStart, float bEnd) {
        return Math.max(aStart, bStart) < Math.min(aEnd, bEnd);
    }

    private float euclideanDistance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float centerX(Component component) {
        return component.left() + component.width() / 2f;
    }

    private float centerY(Component component) {
        return component.top() + component.height() / 2f;
    }

    private enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }
}