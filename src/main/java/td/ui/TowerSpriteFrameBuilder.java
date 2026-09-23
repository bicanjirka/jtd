package td.ui;

import td.tower.AuraTower;
import td.tower.CinderTower;
import td.tower.MortarTower;
import td.tower.PulseTower;
import td.tower.SeekerTower;
import td.tower.SniperTower;
import td.tower.SonarTower;
import td.tower.SplashTower;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerVisitor;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
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
    // SonarTower is deliberately not in this group: its head tracks the scan that decides what
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

    /**
     * The one place a tower type names its body's {@link Palette} role - deliberately no
     * {@code default}, so a new {@link TowerFactory.Type} is a compile error here until its
     * art is wired up, the same way {@link Java2DFrameRenderer}'s draw-command switches are.
     */
    public static Palette bodyPaletteFor(TowerFactory.Type type) {
        return switch (type) {
            case SNIPER -> Palette.TOWER_SNIPER_BODY;
            case SPLASH -> Palette.TOWER_SPLASH_BODY;
            case SONAR -> Palette.TOWER_SONAR_BODY;
            case PULSE -> Palette.TOWER_PULSE_BODY;
            case AURA -> Palette.TOWER_AURA_BODY;
            case MORTAR -> Palette.TOWER_MORTAR_BODY;
            case SEEKER -> Palette.TOWER_SEEKER_BODY;
            case CINDER -> Palette.TOWER_CINDER_BODY;
        };
    }

    /**
     * The specialization-ring role for a tower's chosen {@code HEAD} node, if any - the first
     * node in {@code upgradeTree()}'s {@code HEAD} nodes gets {@code TOWER_UPGRADE_PATH_A}, the
     * second gets {@code TOWER_UPGRADE_PATH_B}, regardless of tower type, so the accent is one
     * consistent two-colour language rather than a role per tower per node. A temporary
     * stand-in for the real per-slot marker set phase 5 adds - see
     * {@code docs/features/FEATURE-tower-upgrade-trees.md}.
     */
    private static Optional<Palette> accentPaletteFor(Tower tower) {
        UpgradeNode chosen = tower.upgrades().tip(UpgradeSlot.HEAD).orElse(null);
        if (chosen == null) {
            return Optional.empty();
        }
        int index = tower.upgradeTree().nodesIn(UpgradeSlot.HEAD).indexOf(chosen);
        return Optional.of(index == 0 ? Palette.TOWER_UPGRADE_PATH_A : Palette.TOWER_UPGRADE_PATH_B);
    }

    public List<TowerSpriteDraw> build() {
        return this.draws;
    }

    public List<TurretHeadDraw> buildHeads() {
        return this.headDraws;
    }

    private void sprite(Tower tower) {
        this.draws.add(new TowerSpriteDraw(bodyPaletteFor(tower.getType()), tower.getBoardX(), tower.getBoardY(),
                tower.isSelected(), tower.getX(), tower.getY(), tower.getRangeReal(), accentPaletteFor(tower)));
    }

    /**
     * A head with a constant nominal size - every tower but the (pulsing) Aura tower.
     */
    private void head(Tower tower, double headingRadians) {
        this.headWithScale(tower, headingRadians, 1.0f);
    }

    private void headWithScale(Tower tower, double headingRadians, float scale) {
        this.headDraws.add(new TurretHeadDraw(bodyPaletteFor(tower.getType()), tower.getX(), tower.getY(),
                (float) headingRadians, scale));
    }

    public Void visitSniperTower(SniperTower tower) {
        this.sprite(tower);
        this.head(tower, tower.getTurretAim().radiansAt(this.interpolationAlpha));
        return null;
    }

    public Void visitSplashTower(SplashTower tower) {
        this.sprite(tower);
        this.head(tower, tower.getTurretAim().radiansAt(this.interpolationAlpha));
        return null;
    }

    public Void visitSonarTower(SonarTower tower) {
        this.sprite(tower);
        // The head is the scan: it must point exactly where the beam is, or the tower appears
        // to shoot enemies it is not facing.
        this.head(tower, tower.sweepRadiansAt(this.interpolationAlpha));
        return null;
    }

    public Void visitPulseTower(PulseTower tower) {
        this.sprite(tower);
        this.head(tower, this.animationSeconds * TOWER_FOUR_SPIN_RADIANS_PER_SECOND);
        return null;
    }

    public Void visitAuraTower(AuraTower tower) {
        this.sprite(tower);
        double phase = 0.5 + 0.5 * Math.sin(this.animationSeconds * TOWER_AURA_PULSE_RADIANS_PER_SECOND);
        float scale = (float) (TOWER_AURA_PULSE_MIN_SCALE + (TOWER_AURA_PULSE_MAX_SCALE - TOWER_AURA_PULSE_MIN_SCALE) * phase);
        this.headWithScale(tower, 0.0, scale);
        return null;
    }

    public Void visitMortarTower(MortarTower tower) {
        this.sprite(tower);
        this.head(tower, tower.getTurretAim().radiansAt(this.interpolationAlpha));
        return null;
    }

    public Void visitSeekerTower(SeekerTower tower) {
        this.sprite(tower);
        this.head(tower, tower.getTurretAim().radiansAt(this.interpolationAlpha));
        return null;
    }

    public Void visitCinderTower(CinderTower tower) {
        this.sprite(tower);
        // The head is a facing indicator only - the wide flame cone itself is a transient
        // TowerEffectDraw (ConeDraw), not part of the turret head's own shape.
        this.head(tower, tower.getTurretAim().radiansAt(this.interpolationAlpha));
        return null;
    }
}
