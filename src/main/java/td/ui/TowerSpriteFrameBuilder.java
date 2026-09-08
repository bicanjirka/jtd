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
import td.ui.render.TurretHeadDraw;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes each tower's static base ({@link TowerSpriteDraw}, unchanged regardless of type -
 * every visit method delegates to the one shared {@link #sprite(Tower)}) and its animated
 * turret head ({@link TurretHeadDraw}, genuinely different per type: an aiming tower reads its
 * own {@link td.tower.TurretAim}, a spinning tower is a function of elapsed time, a pulsing
 * tower varies size instead of heading). The {@link TowerVisitor} dispatch exists so this class
 * never needs an instanceof/cast to reach tower-specific state.
 */
public final class TowerSpriteFrameBuilder implements TowerVisitor<Void> {

    // Radians/second for a continuously-spinning head - opposite signs (clockwise/
    // counterclockwise) and different magnitudes, purely cosmetic so these live here rather
    // than as domain state (see the class doc comment's "function of elapsed time").
    private static final double TOWER_THREE_SPIN_RADIANS_PER_SECOND = 3.5;
    private static final double TOWER_FOUR_SPIN_RADIANS_PER_SECOND = -2.0;

    private final List<TowerSpriteDraw> draws = new ArrayList<>();
    private final List<TurretHeadDraw> headDraws = new ArrayList<>();
    private final double interpolationAlpha;
    private final double animationSeconds;

    public TowerSpriteFrameBuilder(double interpolationAlpha, double animationSeconds) {
        this.interpolationAlpha = interpolationAlpha;
        this.animationSeconds = animationSeconds;
    }

    public List<TowerSpriteDraw> build() {
        return this.draws;
    }

    public List<TurretHeadDraw> buildHeads() {
        return this.headDraws;
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

    private void sprite(Tower tower) {
        this.draws.add(new TowerSpriteDraw(bodyPaletteFor(tower.getType()), tower.getBoardX(), tower.getBoardY(),
                tower.isSelected(), tower.getX(), tower.getY(), tower.getRangeReal()));
    }

    /** A head with no pulsing - every tower but the upgrade tower uses a constant nominal size. */
    private void head(Tower tower, double headingRadians) {
        this.headDraws.add(new TurretHeadDraw(bodyPaletteFor(tower.getType()), tower.getX(), tower.getY(),
                (float) headingRadians, 1.0f));
    }

    public Void visitTowerOne(TowerOne tower) {
        this.sprite(tower);
        this.head(tower, tower.getTurretAim().radiansAt(this.interpolationAlpha));
        return null;
    }

    public Void visitTowerTwo(TowerTwo tower) {
        this.sprite(tower);
        this.head(tower, tower.getTurretAim().radiansAt(this.interpolationAlpha));
        return null;
    }

    public Void visitTowerThree(TowerThree tower) {
        this.sprite(tower);
        this.head(tower, this.animationSeconds * TOWER_THREE_SPIN_RADIANS_PER_SECOND);
        return null;
    }

    public Void visitTowerFour(TowerFour tower) {
        this.sprite(tower);
        this.head(tower, this.animationSeconds * TOWER_FOUR_SPIN_RADIANS_PER_SECOND);
        return null;
    }

    // TowerUpgrade (pulsing) renders base-only for now, same as every tower before turret heads
    // existed - no head() call yet.
    public Void visitTowerUpgrade(TowerUpgrade tower) {
        this.sprite(tower);
        return null;
    }
}
