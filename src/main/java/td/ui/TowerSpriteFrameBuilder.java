package td.ui;

import td.tower.AuraTower;
import td.tower.CinderTower;
import td.tower.MortarTower;
import td.tower.PulseTower;
import td.tower.SeekerTower;
import td.tower.SniperTower;
import td.tower.SplashTower;
import td.tower.TierRise;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerVisitor;
import td.tower.TurretAim;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.Palette;
import td.ui.render.TowerLook;
import td.ui.render.TowerSpriteDraw;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

/** Describes each tower: its look, the tier its foundation shows, where its head aims and whether it can upgrade. */
public final class TowerSpriteFrameBuilder implements TowerVisitor<Void> {

    /** A tier's rise takes half a second on the tick clock, so it pauses and speeds with the game. */
    private static final int RISE_TICKS = Math.round(TickRate.TICKS_PER_SECOND / 2);
    /** Back to front: the northern rows first, so a tower covers the foot of nothing in front of it. */
    private static final Comparator<TowerSpriteDraw> BACK_TO_FRONT = Comparator.comparingDouble(TowerSpriteDraw::centerY)
            .thenComparingDouble(TowerSpriteDraw::centerX);
    private static final TowerVisitor<TowerLook> BODY_LOOK = new TowerVisitor<>() {
        public TowerLook visitSniperTower(SniperTower tower) {
            return TowerLook.SNIPER;
        }

        public TowerLook visitSplashTower(SplashTower tower) {
            return bodyLookOf(tower);
        }

        public TowerLook visitPulseTower(PulseTower tower) {
            return TowerLook.PULSE;
        }

        public TowerLook visitAuraTower(AuraTower tower) {
            return TowerLook.AURA;
        }

        public TowerLook visitMortarTower(MortarTower tower) {
            return TowerLook.MORTAR;
        }

        public TowerLook visitSeekerTower(SeekerTower tower) {
            return TowerLook.SEEKER;
        }

        public TowerLook visitCinderTower(CinderTower tower) {
            return TowerLook.CINDER;
        }
    };
    private static final Palette[] TIER_PALETTES = {Palette.TOWER_UPGRADE_TIER_0, Palette.TOWER_UPGRADE_TIER_1,
        Palette.TOWER_UPGRADE_TIER_2, Palette.TOWER_UPGRADE_TIER_3};

    private final List<TowerSpriteDraw> draws = new ArrayList<>();
    private final GameWorld world;
    private final int gameTime;
    private final double interpolationAlpha;

    public TowerSpriteFrameBuilder(GameWorld world, int gameTime, double interpolationAlpha) {
        this.world = world;
        this.gameTime = gameTime;
        this.interpolationAlpha = interpolationAlpha;
    }

    /** A tower type's look. No {@code default}, so a new type fails to compile here. */
    public static TowerLook lookFor(TowerFactory.Type type) {
        return switch (type) {
            case SNIPER -> TowerLook.SNIPER;
            case SPLASH -> TowerLook.SPLASH;
            case PULSE -> TowerLook.PULSE;
            case AURA -> TowerLook.AURA;
            case MORTAR -> TowerLook.MORTAR;
            case SEEKER -> TowerLook.SEEKER;
            case CINDER -> TowerLook.CINDER;
        };
    }

    /** The look {@code tower}'s body wears now: its type's, or what its fork made it. */
    static TowerLook bodyLookOf(Tower tower) {
        return tower.accept(BODY_LOOK);
    }

    private static TowerLook bodyLookOf(SplashTower tower) {
        return switch (tower.fork()) {
            case NONE -> TowerLook.SPLASH;
            case STORMCALLER -> TowerLook.STORMCALLER;
            case HEXER -> TowerLook.HEXER;
        };
    }

    /** A tower type's tone in text and icons, the colour of its look. */
    public static Palette bodyPaletteFor(TowerFactory.Type type) {
        return switch (type) {
            case SNIPER -> Palette.TOWER_SNIPER_BODY;
            case SPLASH -> Palette.TOWER_SPLASH_BODY;
            case PULSE -> Palette.TOWER_PULSE_BODY;
            case AURA -> Palette.TOWER_AURA_BODY;
            case MORTAR -> Palette.TOWER_MORTAR_BODY;
            case SEEKER -> Palette.TOWER_SEEKER_BODY;
            case CINDER -> Palette.TOWER_CINDER_BODY;
        };
    }

    /** An upgrade tier's colour role, for its perk icons; a tier above the last takes the last's. */
    static Palette tierPaletteFor(int tier) {
        return TIER_PALETTES[Math.min(tier, TIER_PALETTES.length - 1)];
    }

    /** Whether {@code tower} has its top tier open. */
    static boolean crowned(Tower tower, GameWorld world) {
        UpgradeTree tree = tower.upgradeTree();
        return tree.tiers().size() > 1 && tree.highestOpenTier(tower, world) == tree.tiers().size() - 1;
    }

    /** Whether {@code tower} can buy an upgrade or a tier's unlock now. */
    private boolean upgradeReady(Tower tower) {
        List<UpgradeNode> offered = tower.offeredUpgrades(this.world);
        for (int i = 0; i < offered.size(); i++) {
            if (this.world.economy().canPay(offered.get(i).price())) {
                return true;
            }
        }
        return false;
    }

    public List<TowerSpriteDraw> build() {
        this.draws.sort(BACK_TO_FRONT);
        return this.draws;
    }

    /** A tower whose head, if its look has one, never turns. */
    private void sprite(Tower tower) {
        this.sprite(tower, lookFor(tower.getType()), 0.0);
    }

    private void sprite(Tower tower, TowerLook body, double headingRadians) {
        UpgradeTree tree = tower.upgradeTree();
        TierRise rise = tower.tierRise();
        OptionalInt ticks = rise.ticksSince(this.gameTime, RISE_TICKS);
        float progress = ticks.isPresent()
                ? Math.min(1f, (ticks.getAsInt() + (float) this.interpolationAlpha) / RISE_TICKS) : -1f;
        this.draws.add(new TowerSpriteDraw(body, tree.highestOpenTier(tower, this.world), tower.getBoardX(),
                tower.getBoardY(), tower.isSelected(), tower.getX(), tower.getY(), tower.getRangeReal(),
                (float) headingRadians, this.upgradeReady(tower), rise.from(), progress < 1f ? progress : -1f));
    }

    private void aimed(Tower tower, TurretAim aim) {
        this.sprite(tower, lookFor(tower.getType()), aim.radiansAt(this.interpolationAlpha));
    }

    public Void visitSniperTower(SniperTower tower) {
        this.aimed(tower, tower.getTurretAim());
        return null;
    }

    /** The body takes the look of what the fork made it. */
    public Void visitSplashTower(SplashTower tower) {
        this.sprite(tower, bodyLookOf(tower), tower.getTurretAim().radiansAt(this.interpolationAlpha));
        return null;
    }

    public Void visitPulseTower(PulseTower tower) {
        this.sprite(tower);
        return null;
    }

    public Void visitAuraTower(AuraTower tower) {
        this.sprite(tower);
        return null;
    }

    public Void visitMortarTower(MortarTower tower) {
        this.aimed(tower, tower.getTurretAim());
        return null;
    }

    public Void visitSeekerTower(SeekerTower tower) {
        this.sprite(tower);
        return null;
    }

    public Void visitCinderTower(CinderTower tower) {
        this.aimed(tower, tower.getTurretAim());
        return null;
    }
}
