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
import td.ui.render.SlotMarkDraw;
import td.ui.render.TowerSpriteDraw;
import td.ui.render.TurretHeadDraw;
import td.util.GameWorld;

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

    // Radians/second of a continuously spinning head.
    private static final double TOWER_FOUR_SPIN_RADIANS_PER_SECOND = -2.0;

    // The enchant halo reuses this clock, keeping both pulses in phase.
    private static final double TOWER_AURA_PULSE_RADIANS_PER_SECOND = 2.4;
    private static final float TOWER_AURA_PULSE_MIN_SCALE = 0.8f;
    private static final float TOWER_AURA_PULSE_MAX_SCALE = 1.25f;

    /**
     * One role per {@link UpgradeSlot}, in slot order - see {@link #slotMarksFor}.
     */
    private static final Palette[] SLOT_PALETTES =
            {Palette.TOWER_UPGRADE_BASE, Palette.TOWER_UPGRADE_HEAD, Palette.TOWER_UPGRADE_SPECIAL};

    private final List<TowerSpriteDraw> draws = new ArrayList<>();
    private final List<TurretHeadDraw> headDraws = new ArrayList<>();
    private final GameWorld world;
    private final double interpolationAlpha;
    private final double animationSeconds;

    public TowerSpriteFrameBuilder(GameWorld world, double interpolationAlpha, double animationSeconds) {
        this.world = world;
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
     * One {@link SlotMarkDraw} per {@link UpgradeSlot}, always three, in slot order - {@code
     * level} is how many nodes this tower owns in that slot, {@code ready} is whether the slot
     * currently offers a node whose gate is met and which is affordable right now. Two different
     * signals ("what did I pick" vs. "what could I pick right now"), computed once here rather
     * than twice at the render layer.
     */
    private List<SlotMarkDraw> slotMarksFor(Tower tower) {
        List<UpgradeNode> offered = tower.offeredUpgrades(this.world);
        List<SlotMarkDraw> marks = new ArrayList<>(UpgradeSlot.values().length);
        for (UpgradeSlot slot : UpgradeSlot.values()) {
            int level = tower.upgrades().inSlot(slot).size();
            boolean ready = offered.stream().anyMatch(node -> node.slot() == slot
                    && node.gate().isSatisfied(tower, this.world)
                    && this.world.economy().canPay(node.price()));
            marks.add(new SlotMarkDraw(SLOT_PALETTES[slot.ordinal()], level, ready));
        }
        return marks;
    }

    /**
     * {@code 0} for a tower with nothing owned in {@code SPECIAL}; otherwise the same 0..1
     * sine phase the Aura tower's own pulse already uses, so a specialized tower's halo pulses
     * at the same rate a player has already learned to read.
     */
    private float enchantPulseFor(Tower tower) {
        if (tower.upgrades().tip(UpgradeSlot.SPECIAL).isEmpty()) {
            return 0f;
        }
        return (float) (0.5 + 0.5 * Math.sin(this.animationSeconds * TOWER_AURA_PULSE_RADIANS_PER_SECOND));
    }

    public List<TowerSpriteDraw> build() {
        return this.draws;
    }

    public List<TurretHeadDraw> buildHeads() {
        return this.headDraws;
    }

    private void sprite(Tower tower) {
        this.draws.add(new TowerSpriteDraw(bodyPaletteFor(tower.getType()), tower.getBoardX(), tower.getBoardY(),
                tower.isSelected(), tower.getX(), tower.getY(), tower.getRangeReal(),
                this.slotMarksFor(tower), this.enchantPulseFor(tower)));
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
        this.head(tower, tower.getTurretAim().radiansAt(this.interpolationAlpha));
        return null;
    }
}
