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
import td.tower.upgrade.StandardBaseSlot;
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
 * Describes each tower's base sprite, the same for every type, and its turret head, which differs:
 * aimed, spinning with elapsed time, or pulsing in size.
 */
public final class TowerSpriteFrameBuilder implements TowerVisitor<Void> {

    // Radians/second of a continuously spinning head.
    private static final double TOWER_FOUR_SPIN_RADIANS_PER_SECOND = -2.0;

    // The enchant halo reuses this clock, keeping both pulses in phase.
    private static final double TOWER_AURA_PULSE_RADIANS_PER_SECOND = 2.4;
    private static final float TOWER_AURA_PULSE_MIN_SCALE = 0.8f;
    private static final float TOWER_AURA_PULSE_MAX_SCALE = 1.25f;
    private static final double TRANSCENDENT_HALO_SECONDS_PER_TURN = 12.0;

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

    /** A tower type's body colour role. No {@code default}, so a new type fails to compile here. */
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

    /** An upgrade slot's colour role, for its pips, chevrons and info rows. */
    static Palette slotPaletteFor(UpgradeSlot slot) {
        return switch (slot) {
            case BASE -> Palette.TOWER_UPGRADE_BASE;
            case HEAD -> Palette.TOWER_UPGRADE_HEAD;
            case SPECIAL -> Palette.TOWER_UPGRADE_SPECIAL;
        };
    }

    /**
     * One mark per slot, in order: how many nodes are owned, and whether an affordable, ungated
     * node is offered.
     */
    private List<SlotMarkDraw> slotMarksFor(Tower tower) {
        List<UpgradeNode> offered = tower.offeredUpgrades(this.world);
        List<SlotMarkDraw> marks = new ArrayList<>(UpgradeSlot.values().length);
        for (UpgradeSlot slot : UpgradeSlot.values()) {
            boolean ready = false;
            for (int i = 0; i < offered.size() && !ready; i++) {
                UpgradeNode node = offered.get(i);
                ready = node.slot() == slot && node.gateMet(tower, this.world)
                        && this.world.economy().canPay(node.price());
            }
            marks.add(new SlotMarkDraw(slotPaletteFor(slot), tower.upgrades().countIn(slot), ready));
        }
        return marks;
    }

    /** {@code 0} without a {@code SPECIAL} upgrade; otherwise the aura's pulse phase. */
    private float enchantPulseFor(Tower tower) {
        if (tower.upgrades().countIn(UpgradeSlot.SPECIAL) == 0) {
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
                this.slotMarksFor(tower), this.enchantPulseFor(tower),
                tower.upgrades().owns(StandardBaseSlot.TRANSCENDENT_ID),
                (float) (this.animationSeconds / TRANSCENDENT_HALO_SECONDS_PER_TURN % 1.0)));
    }

    /** A head of constant size. */
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
        double heading = tower.sweepRadiansAt(this.interpolationAlpha);
        this.head(tower, heading);
        if (tower.hasTwinBeam()) {
            this.head(tower, heading + Math.PI);
        }
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
