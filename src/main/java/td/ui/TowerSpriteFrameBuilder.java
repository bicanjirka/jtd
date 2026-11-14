package td.ui;

import td.tower.AuraTower;
import td.tower.CinderTower;
import td.tower.MortarTower;
import td.tower.PulseTower;
import td.tower.SeekerTower;
import td.tower.SniperTower;
import td.tower.SplashTower;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerVisitor;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.Palette;
import td.ui.render.TowerLook;
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

    private static final double TOWER_AURA_PULSE_RADIANS_PER_SECOND = 2.4;
    private static final float TOWER_AURA_PULSE_MIN_SCALE = 0.8f;
    private static final float TOWER_AURA_PULSE_MAX_SCALE = 1.25f;
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
    private final List<TurretHeadDraw> headDraws = new ArrayList<>();
    private final GameWorld world;
    private final int gameTime;
    private final double interpolationAlpha;
    private final double animationSeconds;

    public TowerSpriteFrameBuilder(GameWorld world, int gameTime, double interpolationAlpha, double animationSeconds) {
        this.world = world;
        this.gameTime = gameTime;
        this.interpolationAlpha = interpolationAlpha;
        this.animationSeconds = animationSeconds;
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
        return this.draws;
    }

    public List<TurretHeadDraw> buildHeads() {
        return this.headDraws;
    }

    private void sprite(Tower tower) {
        this.sprite(tower, lookFor(tower.getType()));
    }

    private void sprite(Tower tower, TowerLook body) {
        this.draws.add(new TowerSpriteDraw(body, tower.getBoardX(), tower.getBoardY(),
                tower.isSelected(), tower.getX(), tower.getY(), tower.getRangeReal(), this.upgradeReady(tower)));
    }

    /** A head of constant size. */
    private void head(Tower tower, double headingRadians) {
        this.headWithScale(tower, headingRadians, 1.0f);
    }

    private void headWithScale(Tower tower, double headingRadians, float scale) {
        this.headDraws.add(new TurretHeadDraw(lookFor(tower.getType()), tower.getX(), tower.getY(),
                (float) headingRadians, scale));
    }

    public Void visitSniperTower(SniperTower tower) {
        this.sprite(tower);
        this.head(tower, tower.getTurretAim().radiansAt(this.interpolationAlpha));
        return null;
    }

    /** The body takes the colour of what the fork made it. */
    public Void visitSplashTower(SplashTower tower) {
        TowerLook body = bodyLookOf(tower);
        this.sprite(tower, body);
        this.headDraws.add(new TurretHeadDraw(body, tower.getX(), tower.getY(),
                (float) tower.getTurretAim().radiansAt(this.interpolationAlpha), 1f));
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
