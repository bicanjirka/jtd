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
import td.tower.TowerTerm;
import td.tower.TowerVisitor;
import td.tower.TurretAim;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.Palette;
import td.ui.render.TowerCue;
import td.ui.render.TowerLook;
import td.ui.render.TowerPose;
import td.ui.render.TowerSpriteDraw;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

/**
 * Describes each tower: its look, the tier its foundation shows, the pose its moving part is in, where its head aims
 * and whether it can upgrade. A tower's pose is built in its own visit method, from what that tower publishes.
 */
public final class TowerSpriteFrameBuilder implements TowerVisitor<Void> {

    /** A tier's rise takes half a second on the tick clock, so it pauses and speeds with the game. */
    /** How long the stone shows a ring of its knell. */
    private static final int KNELL_FLASH_TICKS = Math.round(TickRate.TICKS_PER_SECOND * 0.4f);
    private static final int RISE_TICKS = Math.round(TickRate.TICKS_PER_SECOND / 2);
    /** A placed tower settles onto its cell a little faster than a tier rises. */
    private static final int SETTLE_TICKS = Math.round(TickRate.TICKS_PER_SECOND * 0.35f);
    /** Neighbours' lights run this far apart, a cell to the next, so a row of towers never breathes as one. */
    private static final float LIFE_OFFSET_SECONDS = 0.83f;
    /** The life clock starts over this often, so it keeps its precision in a float through a long game. */
    private static final double LIFE_WRAP_SECONDS = 3600.0;
    /** A head with nothing to aim at scans slowly, this far to either side of where it rests. */
    private static final double SCAN_RADIANS = Math.toRadians(7);
    private static final double SCAN_SECONDS = 5.2;
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
            return bodyLookOf(tower);
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
            return tower.streamLook().soulfire() ? TowerLook.SOULFIRE : TowerLook.CINDER;
        }
    };
    private static final Palette[] TIER_PALETTES = {Palette.TOWER_UPGRADE_TIER_0, Palette.TOWER_UPGRADE_TIER_1,
        Palette.TOWER_UPGRADE_TIER_2, Palette.TOWER_UPGRADE_TIER_3};

    private final List<TowerSpriteDraw> draws = new ArrayList<>();
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

    /** The pose a Burst is in: its arm reloading, and charged while an ability is full and waits. */
    static TowerPose poseOf(SplashTower tower) {
        return poseOfReload(tower.getCoolDownFraction()).with(TowerCue.CHARGED, tower.holdsAFullAbility());
    }

    /** The stone the identity pick made it, which its rune shows. */
    static TowerLook bodyLookOf(PulseTower tower) {
        return switch (tower.stoneLook()) {
            case PLAIN -> TowerLook.PULSE;
            case BURDEN -> TowerLook.BURDEN;
            case GRAVITY -> TowerLook.GRAVITY;
            case UMBRA -> TowerLook.UMBRA;
        };
    }

    /** The pose an Obelisk is in: its rune bright while it holds an enemy at full Toll, flashing as it rings. */
    static TowerPose poseOf(PulseTower tower, int gameTime) {
        TowerPose pose = TowerPose.idle().with(TowerCue.CHARGED, tower.holdsFullToll());
        return tower.ticksSinceKnell(gameTime) < KNELL_FLASH_TICKS ? pose.fired() : pose;
    }

    static TowerLook bodyLookOf(SplashTower tower) {
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

    /** The pose of a tower that fires on a reload: thrown back by the shot, easing forward as the reload runs. */
    static TowerPose poseOfReload(float coolDownFraction) {
        return coolDownFraction <= 0f ? TowerPose.idle() : TowerPose.idle().reloading(1f - coolDownFraction);
    }

    /** The pose a Sniper's rifle is in: where it draws its barrel is where its bullets leave from. */
    static TowerPose poseOf(SniperTower tower) {
        return poseOfReload(tower.getCoolDownFraction()).withStacks(tower.steadyAimStacks())
                .with(TowerCue.LONG_BARREL, tower.reachesPastItsBase())
                .with(TowerCue.FOURTH_SHOT, tower.nextShotIs(TowerTerm.FOURTH_SHOT))
                .with(TowerCue.SILVER_ROUND, tower.nextShotIs(TowerTerm.SILVER_ROUND))
                .with(TowerCue.HOLLOW_POINT, tower.nextShotIs(TowerTerm.HOLLOW_POINT))
                .with(TowerCue.FRENZY, tower.frenzied())
                .with(TowerCue.HEADHUNTER, tower.huntsItsTarget());
    }

    /** The pose a Scorcher's nozzle is in: where it draws its tip is where its stream leaves from. */
    static TowerPose poseOf(CinderTower tower) {
        return poseOfReload(tower.reloadLeft()).with(TowerCue.STREAMING, tower.isStreaming())
                .with(TowerCue.DRAGONS_BREATH, tower.streamLook().dragonsBreath())
                .with(TowerCue.LONG_BARREL, tower.reachesPastItsBase());
    }

    /** The pose a Mortar's barrel is in: the shell at its magazine's front in the bore, a tray while it loads a volley. */
    static TowerPose poseOf(MortarTower tower) {
        return poseOfReload(tower.getCoolDownFraction())
                .withShell(ProjectileFrameBuilder.loadedShellOf(tower.magazine().front().look()))
                .with(TowerCue.VOLLEY, tower.loadingCount() > 1)
                .with(TowerCue.LONG_BARREL, tower.reachesPastItsBase());
    }

    /** An upgrade tier's colour role, for its perk icons; a tier above the last takes the last's. */
    static Palette tierPaletteFor(int tier) {
        return TIER_PALETTES[Math.min(tier, TIER_PALETTES.length - 1)];
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

    /** A tower that does not aim. */
    private void sprite(Tower tower, TowerLook body, TowerPose pose) {
        this.sprite(tower, body, pose, 0.0);
    }

    private void sprite(Tower tower, TowerLook body, TowerPose pose, double headingRadians) {
        UpgradeTree tree = tower.upgradeTree();
        TierRise rise = tower.tierRise();
        float lifeSeconds = this.lifeSeconds(tower);
        this.draws.add(new TowerSpriteDraw(body, tree.highestOpenTier(tower, this.world), pose, tower.getBoardX(),
                tower.getBoardY(), tower.isSelected(), tower.getX(), tower.getY(), tower.getRangeReal(),
                (float) headingRadians, this.upgradeReady(tower), rise.from(),
                this.progress(rise.ticksSince(this.gameTime, RISE_TICKS), RISE_TICKS),
                this.progress(tower.ticksSincePlaced(this.gameTime, SETTLE_TICKS), SETTLE_TICKS), lifeSeconds));
    }

    /** How far a moment that began {@code ticks} ago has run, 0..1; 1 when it is over or never began. */
    private float progress(OptionalInt ticks, int window) {
        return ticks.isPresent() ? Math.min(1f, (ticks.getAsInt() + (float) this.interpolationAlpha) / window) : 1f;
    }

    /** The wall clock {@code tower}'s own lights and idle motion run on. */
    private float lifeSeconds(Tower tower) {
        return (float) ((this.animationSeconds
                + (tower.getBoardX() * 7 + tower.getBoardY() * 13) * LIFE_OFFSET_SECONDS) % LIFE_WRAP_SECONDS);
    }

    /** {@code resting} is a head with nothing to aim at: it scans slowly about its heading until it has. */
    private void aimed(Tower tower, TowerPose pose, TurretAim aim, boolean resting) {
        double scan = resting ? SCAN_RADIANS * Math.sin(2 * Math.PI * this.lifeSeconds(tower) / SCAN_SECONDS) : 0.0;
        this.sprite(tower, bodyLookOf(tower), pose, aim.radiansAt(this.interpolationAlpha) + scan);
    }

    public Void visitSniperTower(SniperTower tower) {
        this.aimed(tower, poseOf(tower), tower.getTurretAim(), tower.getCurrentTarget() == null);
        return null;
    }

    /** The body takes the look of what the fork made it. */
    public Void visitSplashTower(SplashTower tower) {
        this.sprite(tower, bodyLookOf(tower), poseOf(tower));
        return null;
    }

    public Void visitPulseTower(PulseTower tower) {
        this.sprite(tower, bodyLookOf(tower), poseOf(tower, this.gameTime));
        return null;
    }

    public Void visitAuraTower(AuraTower tower) {
        this.sprite(tower, TowerLook.AURA, TowerPose.idle());
        return null;
    }

    public Void visitMortarTower(MortarTower tower) {
        this.aimed(tower, poseOf(tower), tower.getTurretAim(), tower.getCoolDownFraction() <= 0f);
        return null;
    }

    public Void visitSeekerTower(SeekerTower tower) {
        this.sprite(tower, TowerLook.SEEKER, TowerPose.idle());
        return null;
    }

    public Void visitCinderTower(CinderTower tower) {
        this.aimed(tower, poseOf(tower), tower.getTurretAim(), false);
        return null;
    }
}
