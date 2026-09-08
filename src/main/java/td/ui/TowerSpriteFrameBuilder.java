package td.ui;

import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerFour;
import td.tower.TowerOne;
import td.tower.TowerThree;
import td.tower.TowerTwo;
import td.tower.TowerUpgrade;
import td.tower.TowerVisitor;
import td.ui.render.Palette;
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

    /**
     * The one place a tower type names its body's {@link Palette} role - deliberately no
     * {@code default}, so a new {@link TowerFactory.type} is a compile error here until its
     * art is wired up, the same way {@link Java2DFrameRenderer}'s draw-command switches are.
     */
    public static Palette bodyPaletteFor(TowerFactory.type type) {
        return switch (type) {
            case first -> Palette.TOWER_ONE_BODY;
            case second -> Palette.TOWER_TWO_BODY;
            case third -> Palette.TOWER_THREE_BODY;
            case fourth -> Palette.TOWER_FOUR_BODY;
            case upgrade -> Palette.TOWER_UPGRADE_BODY;
        };
    }

    private Void sprite(Tower tower) {
        this.draws.add(new TowerSpriteDraw(bodyPaletteFor(tower.getType()), tower.getBoardX(), tower.getBoardY(),
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
