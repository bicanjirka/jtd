package td.wave;

import td.cell.Cell;
import td.tower.Tower;

import java.awt.*;

/**
 * Minimal test double for {@link Cell} that just records whether/how
 * {@link #enable(boolean)} was called, so tests can assert on it without
 * needing a real {@code CellNormal} (which requires a {@code Context}).
 */
class RecordingCell implements Cell {

    private Boolean lastEnableArg;

    boolean wasDisabled() {
        return Boolean.FALSE.equals(lastEnableArg);
    }

    boolean enableWasCalled() {
        return lastEnableArg != null;
    }

    @Override
    public void paintEffect(Graphics2D g2) {
    }

    @Override
    public void setHighlight(highlightType highlight) {
    }

    @Override
    public void setHighlightRange(float range) {
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
