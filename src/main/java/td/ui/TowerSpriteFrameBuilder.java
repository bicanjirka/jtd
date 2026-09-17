package td.ui;

import td.tower.Tower;
import td.tower.TowerAura;
import td.tower.TowerCinder;
import td.tower.TowerFactory;
import td.tower.TowerFour;
import td.tower.TowerMortar;
import td.tower.TowerOne;
import td.tower.TowerSeeker;
import td.tower.TowerThree;
import td.tower.TowerTwo;
import td.tower.TowerVisitor;
import td.tower.upgrade.UpgradePath;
import td.ui.render.Palette;
import td.ui.render.TowerSpriteDraw;
import td.ui.render.TurretHeadDraw;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Describes each tower's static base ({@link TowerSpriteDraw}, unchanged regardless of type -
 * every visit method delegates to the one shared {@link #sprite(Tower)}) and its animated
 * turret head ({@link TurretHeadDraw}, genuinely different per type: an aiming tower reads its
 * own {@link td.tower.TurretAim}, a spinning tower is a function of elapsed time, a pulsing
 * tower varies size instead of heading). The {@link TowerVisitor} dispatch exists so this class
 * never needs an instanceof/cast to reach tower-specific state.
 */
public final class TowerSpriteFrameBuilder implements TowerVisitor<Void> {

    // Radians/second for a continuously-spinning head - purely cosmetic, so this lives here
    // rather than as domain state (see the class doc comment's "function of elapsed time").
    // TowerThree is deliberately not in this group: its head tracks the scan that decides what
    // it shoots, so it reads that instead.
    private static final double TOWER_FOUR_SPIN_RADIANS_PER_SECOND = -2.0;

    // The aura tower's head pulses (scale, not heading) via a sine wave instead of spinning -
    // same "function of elapsed time" reasoning as the two constants above.
    private static final double TOWER_AURA_PULSE_RADIANS_PER_SECOND = 2.4;
    private static final float TOWER_AURA_PULSE_MIN_SCALE = 0.8f;
    private static final float TOWER_AURA_PULSE_MAX_SCALE = 1.25f;

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
     * {@code default}, so a new {@link TowerFactory.Type} is a compile error here until its
     * art is wired up, the same way {@link Java2DFrameRenderer}'s draw-command switches are.
     */
    public static Palette bodyPaletteFor(TowerFactory.Type type) {
        return switch (type) {
            case first -> Palette.TOWER_ONE_BODY;
            case second -> Palette.TOWER_TWO_BODY;
            case third -> Palette.TOWER_THREE_BODY;
            case fourth -> Palette.TOWER_FOUR_BODY;
            case aura -> Palette.TOWER_AURA_BODY;
            case mortar -> Palette.TOWER_MORTAR_BODY;
            case seeker -> Palette.TOWER_SEEKER_BODY;
            case cinder -> Palette.TOWER_CINDER_BODY;
        };
    }

    private void sprite(Tower tower) {
        this.draws.add(new TowerSpriteDraw(bodyPaletteFor(tower.getType()), tower.getBoardX(), tower.getBoardY(),
                tower.isSelected(), tower.getX(), tower.getY(), tower.getRangeReal(), accentPaletteFor(tower)));
    }

    /**
     * The specialization-ring role for a tower's chosen upgrade path, if any - path A gets
     * {@code TOWER_UPGRADE_PATH_A}, path B gets {@code TOWER_UPGRADE_PATH_B}, regardless of
     * tower type, so the accent is one consistent two-colour language rather than a role per
     * tower per path. {@code indexOf} works because {@code chooseUpgradePath} always stores
     * back one of the exact instances {@code availablePaths()} itself returned.
     */
    private static Optional<Palette> accentPaletteFor(Tower tower) {
        UpgradePath chosen = tower.getChosenPath();
        if (chosen == null) {
            return Optional.empty();
        }
        int index = tower.availablePaths().indexOf(chosen);
        return Optional.of(index == 0 ? Palette.TOWER_UPGRADE_PATH_A : Palette.TOWER_UPGRADE_PATH_B);
    }

    /** A head with a constant nominal size - every tower but the (pulsing) Aura tower. */
    private void head(Tower tower, double headingRadians) {
        this.headWithScale(tower, headingRadians, 1.0f);
    }

    private void headWithScale(Tower tower, double headingRadians, float scale) {
        this.headDraws.add(new TurretHeadDraw(bodyPaletteFor(tower.getType()), tower.getX(), tower.getY(),
                (float) headingRadians, scale));
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
        // The head is the scan: it must point exactly where the beam is, or the tower appears
        // to shoot enemies it is not facing.
        this.head(tower, tower.sweepRadiansAt(this.interpolationAlpha));
        return null;
    }

    public Void visitTowerFour(TowerFour tower) {
        this.sprite(tower);
        this.head(tower, this.animationSeconds * TOWER_FOUR_SPIN_RADIANS_PER_SECOND);
        return null;
    }

    public Void visitTowerAura(TowerAura tower) {
        this.sprite(tower);
        double phase = 0.5 + 0.5 * Math.sin(this.animationSeconds * TOWER_AURA_PULSE_RADIANS_PER_SECOND);
        float scale = (float) (TOWER_AURA_PULSE_MIN_SCALE + (TOWER_AURA_PULSE_MAX_SCALE - TOWER_AURA_PULSE_MIN_SCALE) * phase);
        this.headWithScale(tower, 0.0, scale);
        return null;
    }

    public Void visitTowerMortar(TowerMortar tower) {
        this.sprite(tower);
        this.head(tower, tower.getTurretAim().radiansAt(this.interpolationAlpha));
        return null;
    }

    public Void visitTowerSeeker(TowerSeeker tower) {
        this.sprite(tower);
        this.head(tower, tower.getTurretAim().radiansAt(this.interpolationAlpha));
        return null;
    }

    public Void visitTowerCinder(TowerCinder tower) {
        this.sprite(tower);
        // The head is a facing indicator only - the wide flame cone itself is a transient
        // TowerEffectDraw (ConeDraw), not part of the turret head's own shape.
        this.head(tower, tower.getTurretAim().radiansAt(this.interpolationAlpha));
        return null;
    }
}
