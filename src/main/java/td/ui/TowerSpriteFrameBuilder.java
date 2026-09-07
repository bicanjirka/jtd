package td.ui;

import td.tower.Tower;
import td.tower.TowerFour;
import td.tower.TowerOne;
import td.tower.TowerThree;
import td.tower.TowerTwo;
import td.tower.TowerUpgrade;
import td.tower.TowerVisitor;
import td.ui.render.TowerSpriteDraw;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a tower's sprite draw. The same regardless of tower type, so every
 * visit method delegates to the one shared builder routine - the
 * {@link TowerVisitor} dispatch exists so this class never needs an
 * instanceof/cast to reach tower-specific state.
 */
public final class TowerSpriteFrameBuilder implements TowerVisitor<Void> {

    private final List<TowerSpriteDraw> draws = new ArrayList<>();

    public List<TowerSpriteDraw> build() {
        return this.draws;
    }

    private Void sprite(Tower tower) {
        this.draws.add(new TowerSpriteDraw(tower.getName(), tower.getBoardX(), tower.getBoardY(),
                tower.isSelected(), tower.getX(), tower.getY(), tower.getRangeReal()));
        return null;
    }

    public Void visitTowerOne(TowerOne tower) {
        return this.sprite(tower);
    }

    public Void visitTowerTwo(TowerTwo tower) {
        return this.sprite(tower);
    }

    public Void visitTowerThree(TowerThree tower) {
        return this.sprite(tower);
    }

    public Void visitTowerFour(TowerFour tower) {
        return this.sprite(tower);
    }

    public Void visitTowerUpgrade(TowerUpgrade tower) {
        return this.sprite(tower);
    }
}
