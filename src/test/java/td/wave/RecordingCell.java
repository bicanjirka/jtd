package td.wave;

import td.cell.Cell;
import td.tower.Tower;

/**
 * Minimal test double for {@link Cell} that just records whether/how
 * {@link #enable(boolean)} was called, so tests can assert on it without
 * needing a real {@code CellNormal} (which requires a {@code Context}).
 * Public so other test packages (e.g. building a {@code Cell[][]} grid to
 * finalise a path for enemy-movement tests) can reuse it too.
 */
public class RecordingCell implements Cell {

    private Boolean lastEnableArg;
    private highlightType highlight = highlightType.none;
    private float highlightRange = 0;

    public boolean wasDisabled() {
        return Boolean.FALSE.equals(lastEnableArg);
    }

    public boolean enableWasCalled() {
        return lastEnableArg != null;
    }

    @Override
    public void setHighlight(highlightType highlight) {
        this.highlight = highlight;
    }

    @Override
    public highlightType getHighlight() {
        return this.highlight;
    }

    @Override
    public void setHighlightRange(float range) {
        this.highlightRange = range;
    }

    @Override
    public float getHighlightRange() {
        return this.highlightRange;
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public void enable(boolean b) {
        this.lastEnableArg = b;
    }

    @Override
    public boolean buildable() {
        return true;
    }

    @Override
    public boolean hasTower() {
        return false;
    }

    @Override
    public void unSetTower() {
    }

    @Override
    public Tower getTower() {
        return null;
    }

    @Override
    public void setTower(Tower tower) {
    }
}
