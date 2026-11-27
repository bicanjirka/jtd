package td.ui;

import td.enemy.EnemyMob;
import td.projectile.ShellLook;
import td.tower.AuraTower;
import td.tower.CinderTower;
import td.tower.MortarTower;
import td.tower.PulseTower;
import td.tower.SeekerTower;
import td.tower.ShotFlight;
import td.tower.SniperTower;
import td.tower.SplashTower;
import td.tower.Tower;
import td.tower.TowerTerm;
import td.tower.TowerVisitor;
import td.tower.aura.PrismArc;
import td.tower.aura.PrismMark;
import td.tower.aura.RevealedEnemy;
import td.tower.cinder.Caught;
import td.tower.cinder.StreamLook;
import td.tower.mortar.BlastMark;
import td.tower.pulse.StoneLook;
import td.tower.seeker.ShatterMark;
import td.tower.sniper.Bullet;
import td.tower.sniper.Execution;
import td.tower.sniper.Hop;
import td.tower.sniper.ShotTrace;
import td.tower.splash.Blast;
import td.tower.splash.Bolt;
import td.tower.splash.BoltKind;
import td.tower.splash.Curse;
import td.tower.splash.Passive;
import td.tower.splash.Stone;
import td.tower.splash.Wisp;
import td.ui.render.BeamDraw;
import td.ui.render.BlastDraw;
import td.ui.render.BlastGroundDraw;
import td.ui.render.BlastLook;
import td.ui.render.BoardDraw;
import td.ui.render.BoardFlashDraw;
import td.ui.render.BulletDraw;
import td.ui.render.BulletRound;
import td.ui.render.CannonballDraw;
import td.ui.render.CasingDraw;
import td.ui.render.CurseDraw;
import td.ui.render.EmbersDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyLook;
import td.ui.render.ExecutionDraw;
import td.ui.render.FlameDraw;
import td.ui.render.FlockDraw;
import td.ui.render.FlockLook;
import td.ui.render.FootDustDraw;
import td.ui.render.IceWakeDraw;
import td.ui.render.LaserDraw;
import td.ui.render.LightArc;
import td.ui.render.LightEdgeDraw;
import td.ui.render.LitEnemyDraw;
import td.ui.render.LoadedMissile;
import td.ui.render.MuzzleBlastDraw;
import td.ui.render.NestDraw;
import td.ui.render.Palette;
import td.ui.render.PumpSteamDraw;
import td.ui.render.RefundMarkDraw;
import td.ui.render.RingDraw;
import td.ui.render.ShadowDraw;
import td.ui.render.ShellCarryDraw;
import td.ui.render.ShellTrail;
import td.ui.render.ShockDraw;
import td.ui.render.SpellBlastDraw;
import td.ui.render.SpellBlastLook;
import td.ui.render.SpellOrbDraw;
import td.ui.render.SpellOrbLook;
import td.ui.render.SplashDraw;
import td.ui.render.SteamDraw;
import td.ui.render.ThunderboltDraw;
import td.ui.render.TowerCue;
import td.ui.render.TowerEffectDraw;
import td.ui.render.TowerLook;
import td.ui.render.TowerPose;
import td.ui.render.TowerStatusDraw;
import td.ui.render.TracerDraw;
import td.ui.render.TrailLook;
import td.ui.render.WardOutlineDraw;
import td.ui.render.ZoneDraw;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.util.TickRate;
import td.wave.Vec2;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.DoubleFunction;

/**
 * Describes each tower's transient effect - beam, splash, pulse, cone, or an aura's ring - as draw
 * commands.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class TowerEffectFrameBuilder implements TowerVisitor<Void> {

    // Two rings half a period apart read as one breathing aura, not a blinking ring.
    /** How far above its cell's centre the Beacon's lantern hangs, where its rays start. */
    private static final float LANTERN_LIFT_CELLS = 0.4f;
    private static final int REVEAL_RAY_TICKS = MomentLength.LONG.ticks();
    private static final float REVEAL_RAY_WIDTH_CELLS = 0.04f;
    private static final float SPOTLIGHT_WIDTH_CELLS = 0.3f;
    /** How long the light's edge shows when it stirs, and how far inside it an enemy is fully lit, in cells. */
    private static final float LIGHT_STIR_TICKS = Math.round(1.5f * TickRate.TICKS_PER_SECOND);
    private static final float LIT_ENEMY_EDGE_CELLS = 0.3f;
    private static final float LIT_ENEMY_PHASE = 1.3f;
    /** Past this many lit enemies in a frame, each is drawn with its pool and rim alone. */
    private static final int LIT_ENEMY_DETAIL_LIMIT = 24;

    private static final float STATUS_MARKER_SCALE_FRACTION = 0.12f;
    private static final float REFUND_MARK_SCALE_FRACTION = 0.3f;

    /** The aim laser's opacity with no Steady Aim stack, and how much each stack adds. */
    /** A bullet's streak is this share of the way from muzzle to enemy, and this wide, in cells. */
    /**
     * The nest's ring round an Aerie's top: its radius in cells, so its far half passes above the tip, and how fast it
     * turns on the wall clock, in radians a second, also while the tower idles.
     */
    private static final float NEST_RADIUS_CELLS = 0.30f;
    private static final double NEST_TURN = 0.6;
    private static final float TRACER_WIDTH_CELLS = 0.028f;
    /** A fork's spell: its head's radius, and how high its flight bows, in cells. */
    private static final float ORB_CELLS = 0.12f;
    private static final float ORB_BOW_CELLS = 0.18f;
    /** A curse: its head's radius, and how high it floats over the enemies it passes, in cells. */
    private static final float CURSE_CELLS = 0.2f;
    private static final float CURSE_HOVER_CELLS = 0.6f;
    /** A spell's or a curse's tail: how long it is in cells, how many points it bends at, and its width at the head. */
    private static final float SPELL_TRAIL_CELLS = 1.1f;
    private static final int SPELL_TRAIL_POINTS = 8;
    private static final float SPELL_TRAIL_WIDTH = 0.55f;
    /** How long a fork's impact, a Thunderstrike's bolt and the shock of a spell's hit are drawn. */
    /** A spell's blast is as brief as a heavy shell's. */
    private static final int SPELL_BLAST_TICKS = MomentLength.SHORT.ticks();
    private static final int THUNDERBOLT_TICKS = MomentLength.LONG.ticks();
    private static final int SHOCK_TICKS = MomentLength.BEAT.ticks();
    /** How much of its way a spark or soul in flight trails behind it. */
    private static final float STREAK_SHARE = 0.25f;
    /** How long the streak a bullet leaves stays drawn once it has arrived: far less than a reload. */
    private static final float TRACER_FADE_TICKS = MomentLength.BLINK.ticks();
    /** The Burst's thrown stone is drawn at this share of a lobbed shot's size, its shadow with it. */
    private static final float STONE_SIZE = 0.8f;
    /** How far a ricochet's arc bows out, as a share of the hop's length. */
    private static final float HOP_BOW = 0.25f;
    private static final float EXECUTION_TICKS = MomentLength.BEAT.ticks();
    /** The hunting scope's lens on the head, in cells along and across its aim, and how far above the rifle's top. */
    private static final double LENS_ALONG_CELLS = 0.09;
    private static final double LENS_ACROSS_CELLS = -0.05;
    private static final double LENS_ABOVE_CELLS = 0.08;
    /** How long a blast's detonation and its arcs stay drawn. */
    private static final float DETONATION_TICKS = MomentLength.SHORT.ticks();
    private static final int SHATTER_TICKS = MomentLength.BEAT.ticks();
    /** The detonation's first flash lasts this share of it. */
    private static final float FLASH_SHARE = 0.25f;
    private static final float DARK_RING_ALPHA = 0.8f;
    /** An arc is drawn as this many zigzag segments, swinging out this share of its length. */
    private static final int ARC_SEGMENTS = 4;
    private static final float ARC_SWING = 0.12f;
    private static final float ARC_MAX_SWING_PIXELS = 6f;

    /** A Shatter throws this many shards, each this long and this thick, in cells. */
    private static final int SHATTER_SHARDS = 10;
    private static final float SHATTER_SHARD_CELLS = 0.18f;
    private static final float SHATTER_STROKE_CELLS = 0.06f;
    private static final float BLAST_CORE_RING_ALPHA = 0.7f;
    private static final float BLAST_EDGE_RING_ALPHA = 0.3f;
    /** A Heavy's fragments: how many fly out, how long each streak is as a share of the edge, and how thick. */
    private static final int BLAST_FRAGMENTS = 8;
    private static final float FRAGMENT_SHARE = 0.25f;
    private static final float FRAGMENT_STROKE_CELLS = 0.05f;
    /**
     * How long each blast is drawn, in ticks: a Normal's burst or a flare's flash, a Heavy's brief fireball, a payload's burst,
     * a nuke's mushroom cloud for as long as its mark stays; and the first ticks of a nuke, which whiten the board.
     */
    private static final int FLASH_BLAST_TICKS = MomentLength.QUICK.ticks();
    private static final int HEAVY_BLAST_TICKS = MomentLength.SHORT.ticks();
    private static final int PAYLOAD_BLAST_TICKS = MomentLength.LONG.ticks();
    private static final int NUKE_BLAST_TICKS = MomentLength.LINGER.ticks();
    private static final int NUKE_WHITEOUT_TICKS = MomentLength.QUICK.ticks();
    /** A stream's tongues take new tips this often, which is all the flicker it needs. */
    private static final int STREAM_FLICKER_TICKS = 3;
    /** The outer tongue fills the cone; the middle one reaches part of it, narrower; the core less again. */
    private static final float STREAM_OUTER_ALPHA = 0.55f;
    private static final float STREAM_MIDDLE_ALPHA = 0.75f;
    private static final float STREAM_MIDDLE_REACH = 0.78f;
    private static final float STREAM_MIDDLE_WIDTH = 0.66f;
    private static final int STREAM_CORE_TIPS = 5;
    /** A Ward's rune circle runs just inside its field's outline. */
    private static final float WARD_INSET_CELLS = 0.07f;
    /** The knell's ring runs out to the field's edge in this long. */
    private static final int KNELL_RING_TICKS = MomentLength.BEAT.ticks();
    /** The Sundial's veil, in cells: narrow at the stone, wider at its tip, with a thin leading edge. */
    /** A spent cartridge leaves from a little above the rifle's top; embers from a little above the turret's. */
    private static final double CASING_ABOVE_CELLS = 0.05;
    private static final double EMBERS_ABOVE_CELLS = 0.06;
    private static final int CASING_TICKS = TowerMomentArt.CASING_LENGTH.ticks();
    private static final int STEAM_TICKS = TowerMomentArt.STEAM_LENGTH.ticks();
    /** A cocked mortar waiting for a target hisses about this often. */
    private static final int HISS_TICKS = Math.round(1.5f * TickRate.TICKS_PER_SECOND);
    /** The veil keeps its strength for most of its turn and fades over the rest. */
    private static final float SHADOW_FADE_SHARE = 0.3f;


    private final GameWorld world;
    /** What is drawn, in the order it was added, each with its place in the board's paint order. */
    private final List<PlacedDraw> draws = new ArrayList<>();
    private final List<ZoneDraw> wards = new ArrayList<>();
    /** The enemies a light has lit this frame, so overlapping lights light one once; membership only. */
    private final Set<EnemyMob> litDrawn = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<FlameDraw.Stream> streams = new ArrayList<>();
    private final Map<EnemyMob, Burning> burning = new LinkedHashMap<>();
    private final int scale;
    private final int gameTime;
    private final double interpolationAlpha;
    private final double animationSeconds;
    private final List<BoardFlashDraw> flashes = new ArrayList<>();
    /** How high each Aerie's nest circles over its centre, by that centre, for the missiles that leave and come home. */
    private final Map<Vec2, Float> nestLifts = new LinkedHashMap<>();
    /** The foot of the tower being visited, where what it does at itself stands. */
    private float towerFoot;

    public TowerEffectFrameBuilder(GameWorld world, int gameTime, double interpolationAlpha, double animationSeconds) {
        this.world = world;
        this.scale = world.level().board().scale();
        this.gameTime = gameTime;
        this.interpolationAlpha = interpolationAlpha;
        this.animationSeconds = animationSeconds;
    }

    /** Position of {@code seconds} in a repeating {@code [0, 1)} cycle. */
    /** A marker in the tower's top-right corner while an enemy disrupts it. */
    public void addStatus(Tower tower) {
        float x = tower.getBoardX() + this.scale * 0.85f;
        float y = tower.getBoardY() + this.scale * 0.15f;
        if (tower.isDisrupted()) {
            this.add(new TowerStatusDraw(Palette.DISRUPTION, x, y, this.scale * STATUS_MARKER_SCALE_FRACTION));
        } else if (tower.use().anyUnused()) {
            this.add(new RefundMarkDraw(x, y, this.scale * REFUND_MARK_SCALE_FRACTION));
        }
    }

    /** What {@code tower} shows round itself and its marks. */
    public void add(Tower tower) {
        this.towerFoot = (float) tower.getY();
        tower.accept(this);
        this.addStatus(tower);
    }

    /** Something {@code tower} does at itself, standing at its foot. */
    private void add(TowerEffectDraw draw) {
        this.addAt(draw, this.towerFoot);
    }

    /** Something that stands, flies or lands at its own place: {@code foot} is the screen row of its ground. */
    private void addAt(BoardDraw draw, float foot) {
        this.draws.add(PlacedDraw.of(draw, foot));
    }

    public List<PlacedDraw> build() {
        if (!this.wards.isEmpty()) {
            this.addAt(new WardOutlineDraw(ZoneOutlineTracer.trace(this.wards), this.scale), 0f);
        }
        if (!this.streams.isEmpty()) {
            List<FlameDraw.Caught> caught = this.burning.entrySet().stream()
                    .map(entry -> this.caught(entry.getKey(), entry.getValue())).toList();
            this.addAt(new FlameDraw(this.streams, caught, this.scale, this.tickSeconds()), 0f);
        }
        return this.draws;
    }

    /**
     * Each bullet as a streak flying from the muzzle to its enemy, white at its head, and the streak the last one left,
     * fading fast; each ricochet as a green streak on an arc from enemy to enemy; and an execution as a ring that
     * bursts into shards.
     */
    public Void visitSniperTower(SniperTower tower) {
        int tier = tower.upgradeTree().highestOpenTier(tower, this.world);
        for (Bullet bullet : tower.bulletsInFlight()) {
            this.tracer(tower, tier, (float) bullet.target().getX(), (float) bullet.target().getY(),
                    this.flown(bullet), bullet.sureCrit(), bullet.shot().rounds(), 1f);
        }
        Optional<ShotTrace> shot = tower.lastShot();
        if (shot.isPresent()) {
            ShotTrace trace = shot.get();
            float age = (this.gameTime - trace.landedAt() + (float) this.interpolationAlpha) / TRACER_FADE_TICKS;
            if (age < 1f) {
                this.tracer(tower, tier, trace.toX(), trace.toY(), 1f, trace.critical(), trace.shot().rounds(), 1f - age);
            }
        }
        for (Hop hop : tower.hopsInFlight()) {
            this.hop(hop, this.flown(hop.bullet()));
        }
        tower.lastExecution().ifPresent(this::execution);
        this.laser(tower, tier);
        this.casing(tower, tier);
        if (tower.frenzied()) {
            TowerFoundations.Foundation foundation = TowerFoundations.of(TowerLook.SNIPER, tier,
                    TowerSpriteFrameBuilder.poseOf(tower, this.gameTime));
            this.add(new EmbersDraw(tower.getX(), (float) (tower.getY() - TowerFoundations.LIFT
                    * (foundation.top() + EMBERS_ABOVE_CELLS) * this.scale), this.scale, this.tickSeconds(),
                    TowerSpriteFrameBuilder.seedOf(tower)));
        }
        return null;
    }

    /** The tick clock in seconds, between the last two ticks. */
    private float tickSeconds() {
        return (float) ((this.gameTime + this.interpolationAlpha) / TickRate.TICKS_PER_SECOND);
    }

    /** {@code ticks} since a moment and the frame's share of the next tick, as a share of a moment {@code window} long. */
    private float progressOf(int ticks, int window) {
        return (ticks + (float) this.interpolationAlpha) / window;
    }

    /** The cartridge its last shot threw from the breech. */
    private void casing(SniperTower tower, int tier) {
        OptionalInt since = tower.ticksSinceFired(this.gameTime, CASING_TICKS);
        if (since.isEmpty()) {
            return;
        }
        TowerFoundations.Foundation foundation = TowerFoundations.of(TowerLook.SNIPER, tier,
                TowerSpriteFrameBuilder.poseOf(tower, this.gameTime));
        this.add(new CasingDraw(tower.getX(), tower.getY(), this.scale,
                (float) tower.getTurretAim().radiansAt(this.interpolationAlpha),
                (float) (foundation.top() + CASING_ABOVE_CELLS), this.progressOf(since.getAsInt(), CASING_TICKS)));
    }

    /** How far {@code bullet} has flown, 0..1. */
    private float flown(Bullet bullet) {
        return Math.min(1f, (this.gameTime - bullet.firedAt() + (float) this.interpolationAlpha)
                / (bullet.landsAt() - bullet.firedAt()));
    }

    /** What a bullet is drawn as: its round, else a crit (one known in flight, or a trace that landed), else plain. */
    static BulletRound roundOf(List<TowerTerm> rounds, boolean critical) {
        if (rounds.contains(TowerTerm.FOURTH_SHOT)) {
            return BulletRound.FOURTH_SHOT;
        }
        if (rounds.contains(TowerTerm.HOLLOW_POINT)) {
            return BulletRound.HOLLOW_POINT;
        }
        if (rounds.contains(TowerTerm.SILVER_ROUND)) {
            return BulletRound.SILVER_ROUND;
        }
        return critical ? BulletRound.CRIT : BulletRound.PLAIN;
    }

    /** A bullet {@code flown} of the way from the Sniper's muzzle to {@code toX, toY}. */
    private void tracer(SniperTower tower, int tier, float toX, float toY, float flown, boolean critical,
                        List<TowerTerm> rounds, float alpha) {
        double heading = Math.atan2(toY - tower.getY(), toX - tower.getX());
        Point2D.Float muzzle = TowerSprites.muzzle(TowerLook.SNIPER, tier,
                TowerSpriteFrameBuilder.poseOf(tower, this.gameTime), tower.getX(), tower.getY(), this.scale, heading);
        this.add(new BulletDraw(roundOf(rounds, critical), muzzle.x, muzzle.y, toX, toY, 0f, flown, alpha,
                this.scale, this.tickSeconds()));
    }

    /**
     * A ricochet {@code flown} of the way along its arc, which bows out to one side of the straight line, drawn as the
     * shot it hopped from; a ricochet follows a crit, so a plain shot hops as one.
     */
    private void hop(Hop hop, float flown) {
        this.add(new BulletDraw(roundOf(hop.rounds(), true), (float) hop.from().x(), (float) hop.from().y(),
                (float) hop.bullet().target().getX(), (float) hop.bullet().target().getY(), HOP_BOW, flown, 1f,
                this.scale, this.tickSeconds()));
    }

    /**
     * Where {@code enemy}'s body stands round its point: its top is drawn there, lifted by any hover, and its sides hang
     * below it. {@code feet} is how far below its point they reach the ground, {@code lift} how far above its feet its
     * body's middle is, in pixels.
     */
    private record Body(float feet, float lift) {

        static Body of(EnemyMob enemy) {
            EnemyLook look = enemy.accept(mob -> EnemyFrameBuilder.lookFor(mob.archetype()));
            EnemyLooks.Form form = EnemyLooks.of(look).form();
            float height = form.height() * enemy.getBodyScale();
            float hover = form.hover() * enemy.getBodyScale();
            return new Body(height - hover, hover + height / 2f);
        }
    }

    /** Between {@code previous} and {@code now} by the frame's share of the next tick. */
    private float lerp(double previous, double now) {
        return (float) (previous + (now - previous) * this.interpolationAlpha);
    }

    /** The headsman's cut on the enemy an execution killed. */
    private void execution(Execution execution) {
        float progress = (this.gameTime - execution.tick() + (float) this.interpolationAlpha) / EXECUTION_TICKS;
        if (progress >= 1f) {
            return;
        }
        Body body = Body.of(execution.enemy());
        this.addAt(new ExecutionDraw((float) execution.at().x(), (float) execution.at().y() + body.feet(),
                body.lift(), this.scale, progress, execution.tick()), (float) execution.enemy().getY());
    }

    /** The hunting scope's laser from its lens to the enemy it singles out, while it does. */
    private void laser(SniperTower tower, int tier) {
        EnemyMob target = tower.getCurrentTarget();
        if (!tower.huntsItsTarget() || target == null) {
            return;
        }
        double aim = tower.getTurretAim().radiansAt(this.interpolationAlpha);
        TowerFoundations.Foundation foundation = TowerFoundations.of(TowerLook.SNIPER, tier,
                TowerSpriteFrameBuilder.poseOf(tower, this.gameTime));
        float lensX = (float) (tower.getX()
                + (Math.cos(aim) * LENS_ALONG_CELLS - Math.sin(aim) * LENS_ACROSS_CELLS) * this.scale);
        float lensY = (float) (tower.getY()
                + (Math.sin(aim) * LENS_ALONG_CELLS + Math.cos(aim) * LENS_ACROSS_CELLS) * this.scale
                - TowerFoundations.LIFT * (foundation.top() + LENS_ABOVE_CELLS) * this.scale);
        Point2D.Float at = target.accept(mob -> new Point2D.Float(this.lerp(mob.getPrevX(), mob.getX()),
                this.lerp(mob.getPrevY(), mob.getY())));
        Body body = Body.of(target);
        this.add(new LaserDraw(lensX, lensY, at.x, at.y + body.feet() - body.lift(), this.scale,
                (float) this.animationSeconds));
    }

    /**
     * Each stone on its lob with its shadow under it, or a fork's spell flying almost straight off its crown; each
     * blast as a detonation that spreads as a ring to its edge and darkens as it grows, or a fork's impact; each bolt
     * that struck as jagged lightning, a Thunderstrike as one great bolt from the sky, and the enemy it hit shocked;
     * each curse as its rune flying to its enemy, and each spark and soul as a streak flying home.
     */
    public Void visitSplashTower(SplashTower tower) {
        int tier = tower.upgradeTree().highestOpenTier(tower, this.world);
        Optional<SpellOrbLook> orb = orbOf(tower.fork());
        for (Stone stone : tower.stonesInFlight()) {
            orb.ifPresentOrElse(look -> this.spellOrb(tower, tier, stone, look), () -> this.stone(tower, tier, stone));
        }
        for (Curse curse : tower.cursesInFlight()) {
            this.curse(tower, tier, curse);
        }
        for (Wisp wisp : tower.wispsInFlight()) {
            Palette tone = wisp.passive() == Passive.STATIC ? Palette.TOWER_SPLASH_ARC : Palette.TOWER_SPLASH_CAST;
            this.streak(tone, wisp.from(), new Vec2(tower.getX(), tower.getY()), wisp.flight());
        }
        Optional<SpellBlastLook> spellBlast = spellBlastOf(tower.fork());
        SplashBlastLook look = SplashBlastLook.of(tower.fork());
        for (Blast blast : tower.recentBlasts()) {
            float x = (float) blast.at().x();
            float y = (float) blast.at().y();
            spellBlast.ifPresentOrElse(spell -> {
                float progress = this.momentProgress(blast.tick(), SPELL_BLAST_TICKS);
                if (progress < 1f) {
                    this.add(new SpellBlastDraw(spell, x, y, blast.core(), blast.edge(), progress, this.scale,
                            blast.tick()));
                }
            }, () -> {
                float progress = (this.gameTime - blast.tick()) / DETONATION_TICKS;
                if (progress < 1f) {
                    this.detonation(x, y, blast.edge(), progress, look);
                }
            });
        }
        List<Bolt> bolts = tower.recentBolts();
        for (int i = 0; i < bolts.size(); i++) {
            this.bolt(bolts.get(i), i, this.crownHeight(tower, tier));
        }
        return null;
    }

    /** What a fork throws in place of a stone; the bare Burst throws stones. */
    private static Optional<SpellOrbLook> orbOf(SplashTower.Fork fork) {
        return switch (fork) {
            case NONE -> Optional.empty();
            case STORMCALLER -> Optional.of(SpellOrbLook.BALL_LIGHTNING);
            case HEXER -> Optional.of(SpellOrbLook.ARCANE_ORB);
        };
    }

    private static Optional<SpellBlastLook> spellBlastOf(SplashTower.Fork fork) {
        return switch (fork) {
            case NONE -> Optional.empty();
            case STORMCALLER -> Optional.of(SpellBlastLook.STORM);
            case HEXER -> Optional.of(SpellBlastLook.HEX);
        };
    }

    /** How far a moment of {@code ticks} that began at {@code tick} has run, between the last two ticks. */
    private float momentProgress(int tick, int ticks) {
        return (float) ((this.gameTime - tick + this.interpolationAlpha) / ticks);
    }

    /**
     * A bolt as jagged lightning, a Thunderstrike's from the sky, and the enemy it hit shocked; a Shockwave's leaves
     * the tower's crown, {@code crownHeight} over its centre.
     */
    private void bolt(Bolt bolt, int order, float crownHeight) {
        if (bolt.kind() == BoltKind.THUNDERSTRIKE) {
            float progress = this.momentProgress(bolt.tick(), THUNDERBOLT_TICKS);
            if (progress < 1f) {
                this.addAt(new ThunderboltDraw((float) bolt.to().x(), (float) bolt.to().y(), progress, this.scale,
                        bolt.tick() * 31 + order), (float) bolt.to().y());
            }
        } else {
            float progress = (this.gameTime - bolt.tick()) / DETONATION_TICKS;
            if (progress < 1f) {
                float lift = bolt.kind() == BoltKind.SHOCKWAVE ? crownHeight : 0f;
                this.lightning((float) bolt.from().x(), (float) bolt.from().y() - lift, (float) bolt.to().x(),
                        (float) bolt.to().y(), order, 1f - progress);
            }
        }
        float shocked = this.momentProgress(bolt.tick(), SHOCK_TICKS);
        if (shocked < 1f) {
            bolt.shocked().ifPresent(enemy -> {
                EnemyBodyDraw body = EnemyFrameBuilder.bodyOf(enemy, this.interpolationAlpha);
                this.addAt(new ShockDraw(body, bolt.critical() || bolt.kind() == BoltKind.THUNDERSTRIKE, shocked,
                        bolt.tick() * 7 + order), body.y());
            });
        }
    }

    /** The point {@code share} of the way from {@code from} to {@code to}. */
    private static Vec2 along(Vec2 from, Vec2 to, double share) {
        return new Vec2(from.x() + (to.x() - from.x()) * share, from.y() + (to.y() - from.y()) * share);
    }

    /** How high a spell is over the ground {@code share} of its way: from its launch onto its landing, bowing a little. */
    private float orbLift(float launch, double share) {
        return (float) (launch * (1 - share) + ORB_BOW_CELLS * this.scale * 4 * share * (1 - share));
    }

    /**
     * A fork's spell over its point on the ground, leaving the crown of its tower and flying almost straight to where
     * its stone would land, its tail bending with it.
     */
    private void spellOrb(SplashTower tower, int tier, Stone stone, SpellOrbLook look) {
        double flown = this.flown(stone.flight());
        Vec2 from = new Vec2(tower.getX(), tower.getY());
        float launch = this.crownHeight(tower, tier);
        Vec2 at = along(from, stone.to(), flown);
        double length = Math.hypot(stone.to().x() - from.x(), stone.to().y() - from.y());
        List<ShellTrail.Point> points = this.tail(length, flown, share -> {
            Vec2 point = along(from, stone.to(), share);
            return new ShellTrail.Point((float) point.x(), (float) (point.y() - this.orbLift(launch, share)));
        });
        TrailLook trail = look == SpellOrbLook.BALL_LIGHTNING ? TrailLook.STORM : TrailLook.ARCANE;
        this.addAt(new SpellOrbDraw(look, (float) at.x(), (float) at.y(), this.orbLift(launch, flown),
                ORB_CELLS * this.scale, new ShellTrail(trail, SPELL_TRAIL_WIDTH, points, this.gameTime),
                (float) this.animationSeconds), (float) at.y());
    }

    /** How high over its centre on the ground a tower's crowning ball is, in pixels. */
    private float crownHeight(SplashTower tower, int tier) {
        return tower.getY() - TowerSprites.crown(TowerSpriteFrameBuilder.bodyLookOf(tower), tier,
                TowerSpriteFrameBuilder.poseOf(tower), tower.getX(), tower.getY(), this.scale).y;
    }

    /**
     * The points of a tail behind a shot {@code flown} of a way {@code length} pixels long, from its head back, each
     * where {@code at} puts that share of the way; never longer than the way flown.
     */
    private List<ShellTrail.Point> tail(double length, double flown, DoubleFunction<ShellTrail.Point> at) {
        double reach = length > 0 ? Math.min(flown, SPELL_TRAIL_CELLS * this.scale / length) : 0;
        List<ShellTrail.Point> points = new ArrayList<>(SPELL_TRAIL_POINTS);
        for (int i = 0; i < SPELL_TRAIL_POINTS; i++) {
            points.add(at.apply(flown - reach * i / (SPELL_TRAIL_POINTS - 1)));
        }
        return List.copyOf(points);
    }

    /**
     * A curse as its rune flying to the enemy it curses, floating over the heads it passes: a cast leaves the crown of
     * its tower, one that forked or jumped leaves from where it was.
     */
    private void curse(SplashTower tower, int tier, Curse curse) {
        EnemyFrameBuilder.runeFor(curse.kind()).ifPresent(glyph -> {
            double flown = this.flown(curse.flight());
            Vec2 from = curse.from();
            Vec2 to = curse.target().position();
            float hover = CURSE_HOVER_CELLS * this.scale;
            boolean cast = Math.hypot(from.x() - tower.getX(), from.y() - tower.getY()) < 1;
            float launch = cast ? this.crownHeight(tower, tier) : hover;
            DoubleFunction<ShellTrail.Point> at = share -> {
                Vec2 point = along(from, to, share);
                return new ShellTrail.Point((float) point.x(),
                        (float) (point.y() - (launch * (1 - share) + hover * share)));
            };
            ShellTrail.Point head = at.apply(flown);
            List<ShellTrail.Point> points = this.tail(Math.hypot(to.x() - from.x(), to.y() - from.y()), flown, at);
            TrailLook trail = switch (glyph) {
                case DOOM -> TrailLook.DOOM;
                case BLIGHT -> TrailLook.BLIGHT;
                case BACKLASH -> TrailLook.BACKLASH;
            };
            this.addAt(new CurseDraw(glyph, head.x(), head.y(), CURSE_CELLS * this.scale,
                    new ShellTrail(trail, SPELL_TRAIL_WIDTH, points, this.gameTime), (float) this.animationSeconds),
                    head.y());
        });
    }

    /** How far along its way something flying is now, between the last two ticks. */
    private double flown(ShotFlight flight) {
        double now = this.gameTime + this.interpolationAlpha;
        return Math.clamp((now - flight.firedAt()) / Math.max(1, flight.landsAt() - flight.firedAt()), 0, 1);
    }

    /**
     * A stone over its point on the ground, lifted on the one lob every thrown shot shares, from the height of the arm
     * that threw it.
     */
    private void stone(SplashTower tower, int tier, Stone stone) {
        double flown = this.flown(stone.flight());
        double heading = Math.atan2(stone.to().y() - tower.getY(), stone.to().x() - tower.getX());
        Point2D.Float muzzle = TowerSprites.muzzle(TowerSpriteFrameBuilder.bodyLookOf(tower), tier,
                TowerSpriteFrameBuilder.poseOf(tower), tower.getX(), tower.getY(), this.scale, heading);
        float x = (float) (muzzle.x + (stone.to().x() - muzzle.x) * flown);
        float y = (float) (tower.getY() + (stone.to().y() - tower.getY()) * flown);
        float launch = (float) (tower.getY() - muzzle.y);
        float peak = ProjectileFrameBuilder.lobPeak(Math.hypot(stone.to().x() - tower.getX(),
                stone.to().y() - tower.getY()) / this.scale, this.scale);
        float lift = ProjectileFrameBuilder.lobLift(launch, peak, flown);
        Palette tone = stone.charged() ? Palette.TOWER_SPLASH_ARC : Palette.TOWER_SPLASH_STONE;
        this.addAt(new CannonballDraw(tone, x, y, lift, ProjectileFrameBuilder.lobShadowScale(flown), STONE_SIZE, x,
                y - lift, Optional.empty(), Optional.empty()), y);
    }

    /** Something flying straight to where it lands, its streak trailing behind its head. */
    private void streak(Palette tone, Vec2 from, Vec2 to, ShotFlight flight) {
        double flown = this.flown(flight);
        double tail = Math.max(0, flown - STREAK_SHARE);
        this.add(new TracerDraw(tone, tone, (float) (from.x() + (to.x() - from.x()) * tail),
                (float) (from.y() + (to.y() - from.y()) * tail), (float) (from.x() + (to.x() - from.x()) * flown),
                (float) (from.y() + (to.y() - from.y()) * flown), TRACER_WIDTH_CELLS * this.scale, 1f));
    }

    private void detonation(float centerX, float centerY, float edge, float progress, SplashBlastLook look) {
        float radius = edge * progress;
        if (progress < FLASH_SHARE) {
            this.add(new SplashDraw(look.flash(), centerX, centerY, edge * (FLASH_SHARE + progress)));
        }
        this.add(new RingDraw(look.detonation(), centerX, centerY, radius, 1f - progress));
        this.add(new RingDraw(look.detonationDark(), centerX, centerY, radius,
                DARK_RING_ALPHA * progress * (1f - progress) * 4f));
    }

    /** The colours of a Burst's blast: the fork it took tints it like its body. */
    private record SplashBlastLook(Palette flash, Palette detonation, Palette detonationDark) {

        static SplashBlastLook of(SplashTower.Fork fork) {
            return switch (fork) {
                case NONE -> new SplashBlastLook(Palette.TOWER_SPLASH_FLASH,
                        Palette.TOWER_SPLASH_DETONATION, Palette.TOWER_SPLASH_DETONATION_DARK);
                case STORMCALLER -> new SplashBlastLook(Palette.TOWER_STORMCALLER_FLASH,
                        Palette.TOWER_STORMCALLER_DETONATION, Palette.TOWER_STORMCALLER_DETONATION_DARK);
                case HEXER -> new SplashBlastLook(Palette.TOWER_HEXER_FLASH,
                        Palette.TOWER_HEXER_DETONATION, Palette.TOWER_HEXER_DETONATION_DARK);
            };
        }
    }

    /** One bolt as a zigzag; which way each segment swings alternates, starting by the bolt's order. */
    private void lightning(float fromX, float fromY, float toX, float toY, int order, float alpha) {
        float dx = toX - fromX;
        float dy = toY - fromY;
        float length = (float) Math.hypot(dx, dy);
        if (length == 0f) {
            return;
        }
        float swing = Math.min(ARC_MAX_SWING_PIXELS, length * ARC_SWING);
        float normalX = -dy / length * swing;
        float normalY = dx / length * swing;
        float x = fromX;
        float y = fromY;
        for (int i = 1; i <= ARC_SEGMENTS; i++) {
            float t = (float) i / ARC_SEGMENTS;
            float side = i == ARC_SEGMENTS ? 0f : ((i + order) % 2 == 0 ? 1f : -1f);
            float nextX = fromX + dx * t + normalX * side;
            float nextY = fromY + dy * t + normalY * side;
            this.add(new BeamDraw(Palette.TOWER_SPLASH_ARC, x, y, nextX, nextY, 2f, alpha));
            x = nextX;
            y = nextY;
        }
    }

    /** The field itself is a ground patch; over it a Ward's rune circle, the knell's ring and the Sundial's veil. */
    public Void visitPulseTower(PulseTower tower) {
        float x = tower.getX();
        float y = tower.getY();
        float field = tower.getRangeReal();
        if (tower.silences()) {
            this.wards.add(new ZoneDraw(x, y, field - this.scale * WARD_INSET_CELLS, 1f, 0f));
        }
        int rang = tower.ticksSinceKnell(this.gameTime);
        if (rang < KNELL_RING_TICKS) {
            float progress = Math.min(1f, (rang + (float) this.interpolationAlpha) / KNELL_RING_TICKS);
            this.add(new RingDraw(ZoneFrameBuilder.fieldPaletteFor(tower.stoneLook()), x, y, field * progress,
                    1f - progress));
        }
        if (tower.isCastingShadow()) {
            float alpha = Math.min(1f, tower.shadowShareLeft() / SHADOW_FADE_SHARE);
            this.add(new ShadowDraw(x, y, (float) tower.shadowRadians(this.interpolationAlpha),
                    tower.shadowReach(), this.scale, alpha, field, tower.stoneLook() == StoneLook.UMBRA));
        }
        return null;
    }

    public Void visitAuraTower(AuraTower tower) {
        float lanternX = tower.getX();
        float lanternY = tower.getY() - this.scale * LANTERN_LIFT_CELLS;
        for (RevealedEnemy reveal : tower.reveals()) {
            float progress = (this.gameTime - reveal.atTick() + (float) this.interpolationAlpha) / REVEAL_RAY_TICKS;
            if (progress >= 0f && progress < 1f) {
                EnemyMob enemy = reveal.enemy();
                this.add(new BeamDraw(Palette.TOWER_AURA_REVEAL_RAY, lanternX, lanternY, (float) enemy.getX(),
                        (float) enemy.getY(), this.scale * REVEAL_RAY_WIDTH_CELLS, 1f - progress));
            }
        }
        tower.heldEnemy().ifPresent(enemy -> this.add(BeamDraw.solid(Palette.TOWER_AURA_SPOTLIGHT, lanternX,
                lanternY, (float) enemy.getX(), (float) enemy.getY(), this.scale * SPOTLIGHT_WIDTH_CELLS)));
        this.lightEdge(tower);
        this.litEnemies(tower);
        if (tower.isSelected()) {
            for (Tower lit : tower.litTowers()) {
                this.add(new TowerStatusDraw(Palette.TOWER_AURA_LIT_MARK, lit.getBoardX() + this.scale * 0.15f,
                        lit.getBoardY() + this.scale * 0.85f, this.scale * STATUS_MARKER_SCALE_FRACTION));
            }
        }
        return null;
    }

    /** The edge of the light, for a moment each time it stirs: the Beacon is built, or what it lights changes. */
    private void lightEdge(AuraTower tower) {
        OptionalInt stirred = tower.stirredAt();
        if (stirred.isEmpty()) {
            return;
        }
        float progress = (this.gameTime - stirred.getAsInt() + (float) this.interpolationAlpha) / LIGHT_STIR_TICKS;
        if (progress < 0f || progress >= 1f) {
            return;
        }
        List<LightArc> arcs = tower.prismArcs().stream()
                .map(arc -> new LightArc(arc.angleRadians(), markPalette(arc.mark()))).toList();
        this.add(new LightEdgeDraw(tower.getX(), tower.getY(), tower.getRangeReal(), progress, arcs));
    }

    /** Every enemy standing in the light, lit from the top left; with Prism the light splits into its marks. */
    private void litEnemies(AuraTower tower) {
        List<EnemyMob> inside = tower.enemiesInLight();
        if (inside.isEmpty()) {
            return;
        }
        List<Palette> fringe = tower.prismArcs().stream().map(PrismArc::mark).distinct()
                .sorted(Comparator.naturalOrder()).map(TowerEffectFrameBuilder::markPalette).toList();
        float edge = LIT_ENEMY_EDGE_CELLS * this.scale;
        int index = 0;
        for (EnemyMob enemy : inside) {
            double dx = enemy.getX() - tower.getX();
            double dy = enemy.getY() - tower.getY();
            float depth = (tower.getRangeReal() - (float) Math.sqrt(dx * dx + dy * dy)) / edge;
            float light = Math.clamp(depth, 0f, 1f);
            if (light > 0f && !enemy.isDead() && !enemy.isHidden() && this.litDrawn.add(enemy)) {
                this.addAt(new LitEnemyDraw((float) enemy.getX(), (float) enemy.getY(), this.scale,
                        light * light * (3f - 2f * light), (float) this.animationSeconds, index * LIT_ENEMY_PHASE, fringe,
                        this.litDrawn.size() > LIT_ENEMY_DETAIL_LIMIT),
                        EnemyFrameBuilder.bodyOf(enemy, this.interpolationAlpha).y());
            }
            index++;
        }
    }

    private static Palette markPalette(PrismMark mark) {
        return switch (mark) {
            case SUNDER -> Palette.STATUS_MARKER_SUNDERED;
            case CRACK -> Palette.STATUS_MARKER_CRACKED;
            case UNRAVEL -> Palette.STATUS_MARKER_UNRAVELED;
            case SINGE -> Palette.STATUS_MARKER_SINGED;
            case VULNERABLE -> Palette.STATUS_MARKER_VULNERABLE;
            case TOLL -> Palette.STATUS_MARKER_TOLL;
        };
    }

    /** Each blast that just went off, the strongest nuke's flash over the board; the shell is a projectile. */
    public Void visitMortarTower(MortarTower tower) {
        for (BlastMark mark : tower.getBlastMarks()) {
            float ticks = this.gameTime - mark.startedAtTick() + (float) this.interpolationAlpha;
            BlastLook look = blastLookOf(mark);
            float progress = ticks / blastTicks(look);
            if (progress < 0f || progress >= 1f) {
                continue;
            }
            this.detonation(mark, look, progress);
            if (mark.look() == ShellLook.NUKE && ticks < NUKE_WHITEOUT_TICKS) {
                this.whiteOut(mark, 1f - ticks / NUKE_WHITEOUT_TICKS);
            }
        }
        this.shellCarry(tower);
        this.stride(tower);
        return null;
    }

    /**
     * The Strider's moments round its body: the spit at its mouth and the dust at its feet as each shell leaves, and
     * the steam off its pump as the pump ends and, now and then, while it waits cocked.
     */
    private void stride(MortarTower tower) {
        OptionalInt shot = tower.ticksSinceFired(this.gameTime, TowerMomentArt.MUZZLE_SMOKE_TICKS - 1);
        float steam = this.pumpSteam(tower);
        if (shot.isEmpty() && steam < 0f) {
            return;
        }
        int tier = tower.upgradeTree().highestOpenTier(tower, this.world);
        TowerPose pose = TowerPose.idle().withBeat(TowerSpriteFrameBuilder.strideOf(tower, this.gameTime))
                .with(TowerCue.LONG_BARREL, tower.reachesPastItsBase());
        if (shot.isPresent()) {
            int since = shot.getAsInt();
            double[] mouth = TowerFoundations.mortarMouth(tier, pose);
            this.add(new MuzzleBlastDraw((float) (tower.getX() + mouth[0] * this.scale),
                    (float) (tower.getY() + (mouth[1] - TowerFoundations.LIFT * mouth[2]) * this.scale), this.scale,
                    this.progressOf(since, TowerMomentArt.MUZZLE_SMOKE_TICKS)));
            if (since >= 1 && since <= TowerMomentArt.FOOT_DUST_TICKS) {
                this.add(new FootDustDraw(tier, tower.getX(), tower.getY(), this.scale,
                        this.progressOf(since - 1, TowerMomentArt.FOOT_DUST_TICKS)));
            }
        }
        if (steam >= 0f) {
            double[] pump = TowerFoundations.mortarPump(tier, pose);
            this.add(new PumpSteamDraw((float) (tower.getX() + pump[0] * this.scale),
                    (float) (tower.getY() + (pump[1] - TowerFoundations.LIFT * pump[2]) * this.scale), this.scale,
                    steam));
        }
    }

    /**
     * How far through a puff of steam a Mortar's pump is, or below zero for none: one as the pump ends, and while it
     * stands ready a hiss every so often, its own beat from its seed.
     */
    private float pumpSteam(MortarTower tower) {
        double loaded = TowerSpriteFrameBuilder.ticksSinceLoaded(tower);
        if (Double.isFinite(loaded)) {
            double since = loaded + this.interpolationAlpha - (TowerFoundations.STRIDE_PUMP_TICKS - 1);
            return since >= 0 && since < TowerMomentArt.PUMP_STEAM_TICKS
                    ? (float) (since / TowerMomentArt.PUMP_STEAM_TICKS) : -1f;
        }
        int beat = Math.floorMod(this.gameTime + TowerSpriteFrameBuilder.seedOf(tower), HISS_TICKS);
        return beat < TowerMomentArt.PUMP_STEAM_TICKS ? this.progressOf(beat, TowerMomentArt.PUMP_STEAM_TICKS) : -1f;
    }

    /** The shell being carried from the rack into the muzzle while it reloads, once the shot's frames are over. */
    private void shellCarry(MortarTower tower) {
        float coolDown = tower.getCoolDownFraction();
        if (coolDown <= 0f
                || tower.ticksSinceFired(this.gameTime, TowerFoundations.STRIDE_SHOT_FRAMES - 1).isPresent()) {
            return;
        }
        TowerPose pose = TowerSpriteFrameBuilder.poseOf(tower, this.gameTime);
        int tier = tower.upgradeTree().highestOpenTier(tower, this.world);
        this.add(new ShellCarryDraw(tier, pose.has(TowerCue.LONG_BARREL), tower.getX(), tower.getY(), this.scale,
                1f - coolDown, pose.has(TowerCue.VOLLEY), pose.stock(), pose.load()));
    }

    /** The detonation a blast goes off in, a Heavy's as its pick marks it; a flare goes off as a Normal shell. */
    private static BlastLook blastLookOf(BlastMark mark) {
        return switch (mark.look()) {
            case NORMAL, FLARE -> BlastLook.NORMAL;
            case HEAVY -> switch (mark.rider()) {
                case NONE -> BlastLook.HEAVY;
                case SHRAPNEL -> BlastLook.SHRAPNEL;
                case CONCUSSION -> BlastLook.CONCUSSION;
            };
            case NAPALM -> BlastLook.NAPALM;
            case TAR -> BlastLook.TAR;
            case CRYO -> BlastLook.CRYO;
            case NUKE -> BlastLook.NUKE;
        };
    }

    private static int blastTicks(BlastLook look) {
        return switch (look) {
            case NORMAL -> FLASH_BLAST_TICKS;
            case HEAVY, SHRAPNEL, CONCUSSION -> HEAVY_BLAST_TICKS;
            case NAPALM, TAR, CRYO -> PAYLOAD_BLAST_TICKS;
            case NUKE -> NUKE_BLAST_TICKS;
        };
    }

    /**
     * A shell's own detonation; a Normal's keeps the rings of its reach round it, and a Heavy with shrapnel throws
     * fragments out over it.
     */
    private void detonation(BlastMark mark, BlastLook look, float progress) {
        int seed = mark.startedAtTick() * 31 + (int) mark.at().x() * 7 + (int) mark.at().y() * 13;
        if (look == BlastLook.NORMAL) {
            this.blastRings(mark, progress);
        }
        BlastDraw blast = new BlastDraw(look, (float) mark.at().x(), (float) mark.at().y(), mark.core(), mark.edge(),
                progress, this.scale, seed);
        this.addAt(new BlastGroundDraw(blast), blast.y());
        this.addAt(blast, blast.y());
        if (look == BlastLook.SHRAPNEL) {
            this.fragments(mark, progress * blastTicks(look) / FLASH_BLAST_TICKS);
        }
    }

    /** A nuke's flash over the whole board. */
    private void whiteOut(BlastMark mark, float strength) {
        this.flashes.add(new BoardFlashDraw((float) mark.at().x(), (float) mark.at().y(), strength));
    }

    /** A nuke's flash whitening the board, while one is going off; of two at once, the stronger shows. */
    public Optional<BoardFlashDraw> boardFlash() {
        return this.flashes.stream().max(Comparator.comparingDouble(BoardFlashDraw::strength));
    }

    /** How high each Aerie's nest circles over its centre, in pixels, keyed by that centre. */
    public Map<Vec2, Float> nestLifts() {
        return Map.copyOf(this.nestLifts);
    }

    /** A white ring round a blast's full-damage core, a fainter one round its edge. */
    private void blastRings(BlastMark mark, float progress) {
        float fade = 1f - progress;
        float x = (float) mark.at().x();
        float y = (float) mark.at().y();
        this.add(new RingDraw(Palette.BLAST_RING, x, y, mark.core(), fade * BLAST_CORE_RING_ALPHA));
        if (mark.edge() > mark.core()) {
            this.add(new RingDraw(Palette.BLAST_RING, x, y, mark.edge(), fade * BLAST_EDGE_RING_ALPHA));
        }
    }

    /** The fragments a Heavy with shrapnel throws out over its blast, {@code progress} through their flight. */
    private void fragments(BlastMark mark, float progress) {
        if (progress < 1f) {
            float fade = 1f - progress;
            float x = (float) mark.at().x();
            float y = (float) mark.at().y();
            float head = mark.edge() * progress;
            float tail = Math.max(0f, head - mark.edge() * FRAGMENT_SHARE);
            for (int i = 0; i < BLAST_FRAGMENTS; i++) {
                double angle = i * 2 * Math.PI / BLAST_FRAGMENTS + mark.startedAtTick();
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                this.add(new TracerDraw(Palette.PROJECTILE_CANNONBALL, Palette.TERM_HEAVY_SHELL, x + cos * tail,
                        y + sin * tail, x + cos * head, y + sin * head, this.scale * FRAGMENT_STROKE_CELLS, fade));
            }
        }
    }

    /**
     * The frost round every enemy an Ice Wake holds, the shards of ice that just burst, the missiles held in the nest,
     * circling the tower's top, and the birds over it while it is quiet; the missile in flight is a projectile.
     */
    public Void visitSeekerTower(SeekerTower tower) {
        for (Vec2 held : tower.iceWakes()) {
            this.add(new IceWakeDraw((float) held.x(), (float) held.y(), tower.iceWakeRadius(), this.scale));
        }
        for (ShatterMark mark : tower.shatterMarks()) {
            float progress = (this.gameTime - mark.startedAtTick() + (float) this.interpolationAlpha)
                    / SHATTER_TICKS;
            if (progress >= 0f && progress < 1f) {
                this.shatter(mark, progress);
            }
        }
        List<LoadedMissile> missiles = tower.nestMissiles().stream()
                .map(kind -> ProjectileFrameBuilder.loadedMissileOf(kind.look())).toList();
        int tier = tower.upgradeTree().highestOpenTier(tower, this.world);
        Point2D.Float top = TowerSprites.nestCentre(TowerSpriteFrameBuilder.bodyLookOf(tower), tier,
                TowerSpriteFrameBuilder.poseOf(tower), tower.getX(), tower.getY(), this.scale);
        this.nestLifts.put(new Vec2(tower.getX(), tower.getY()), tower.getY() - top.y);
        float lifeSeconds = TowerSpriteFrameBuilder.lifeSeconds(tower, this.animationSeconds);
        if (!missiles.isEmpty()) {
            this.add(new NestDraw(top.x, top.y, NEST_RADIUS_CELLS * this.scale, missiles,
                    tower.ringRadians() + lifeSeconds * NEST_TURN,
                    ProjectileFrameBuilder.MISSILE_LENGTH_CELLS * this.scale, tower.releaseReady()));
        }
        this.flock(tower, tier, top, lifeSeconds);
        return null;
    }

    /** The birds over an Aerie: they come once it has been quiet a while and scatter when it fights. */
    private void flock(SeekerTower tower, int tier, Point2D.Float top, float lifeSeconds) {
        OptionalInt quiet = tower.ticksQuiet(this.gameTime);
        if (quiet.isEmpty()) {
            return;
        }
        OptionalInt fighting = tower.ticksFighting(this.gameTime);
        float alpha = (float) this.interpolationAlpha;
        FlockPresence presence = FlockPresence.of(
                (quiet.getAsInt() + (fighting.isPresent() ? 0f : alpha)) / TickRate.TICKS_PER_SECOND,
                fighting.isPresent(), (fighting.orElse(0) + alpha) / TickRate.TICKS_PER_SECOND);
        if (presence.empty()) {
            return;
        }
        FlockLook look = tower.secondMissile().map(kind -> switch (kind) {
            case FROST -> FlockLook.TERNS;
            case RUBY -> FlockLook.EMBERS;
            case SHADE -> FlockLook.CROWS;
        }).orElse(FlockLook.TERNS);
        this.add(new FlockDraw(look, TowerLifeArt.birdCount(look, tier), top.x, top.y, this.scale, lifeSeconds,
                TowerSpriteFrameBuilder.seedOf(tower), (float) presence.arrived(), (float) presence.leaving()));
    }

    /** Shards of the ice thrown out over every enemy the burst reaches, in the colour of the missile that froze it. */
    private void shatter(ShatterMark mark, float progress) {
        float x = (float) mark.at().x();
        float y = (float) mark.at().y();
        Palette ice = MissileArt.paletteOf(ProjectileFrameBuilder.loadedMissileOf(mark.kind().look()));
        float head = mark.radius() * (float) Easing.out(progress);
        float tail = Math.max(0f, head - this.scale * SHATTER_SHARD_CELLS);
        for (int i = 0; i < SHATTER_SHARDS; i++) {
            double angle = i * 2 * Math.PI / SHATTER_SHARDS + mark.startedAtTick();
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            this.add(new TracerDraw(ice, Palette.PROJECTILE_FROST, x + cos * tail, y + sin * tail,
                    x + cos * head, y + sin * head, this.scale * SHATTER_STROKE_CELLS, 1f - progress));
        }
    }

    /** How hot fuel of {@code fuelPerSecond} points looks: 0 the bare flame's, 1 as hot as the flame is drawn. */
    static float heatOf(float fuelPerSecond) {
        return (float) Math.max(0, (fuelPerSecond - CinderTower.FLAME_POINTS)
                / (FlameArt.FULL_FUEL - CinderTower.FLAME_POINTS));
    }

    /**
     * While it streams, its cone from the nozzle to its range, which the frame's flame paints with every other, and each
     * enemy it fed.
     */
    public Void visitCinderTower(CinderTower tower) {
        if (!tower.isStreaming()) {
            this.steam(tower);
            return null;
        }
        StreamLook look = tower.streamLook();
        int flicker = this.gameTime / STREAM_FLICKER_TICKS;
        double heading = tower.getTurretAim().radiansAt(this.interpolationAlpha);
        int tier = tower.upgradeTree().highestOpenTier(tower, this.world);
        Point2D.Float muzzle = TowerSprites.muzzle(TowerSpriteFrameBuilder.bodyLookOf(tower), tier,
                TowerSpriteFrameBuilder.poseOf(tower), tower.getX(), tower.getY(), this.scale, heading);
        // The cone's apex is the tower's centre, where it pours from; it shows from the nozzle out.
        float inner = (float) Math.max(0, (muzzle.x - tower.getX()) * Math.cos(heading)
                + (muzzle.y - tower.getY()) * Math.sin(heading));
        float half = (float) tower.halfWidthRadians();
        this.streams.add(new FlameDraw.Stream(tower.getX(), tower.getY(), (float) heading, inner, tower.getRangeReal(),
                half, heatOf(tower.fuelPerSecond()), look.middle().halfWidthIn(half), look.core().halfWidthIn(half),
                heatOf(tower.fuelPerSecond() * look.coreFactor()), look.soulfire(), look.dragonsBreath(), flicker));
        for (Caught fed : tower.caught()) {
            this.burning.merge(fed.enemy(), new Burning(new Cone(fed.fuelPerSecond(), muzzle, look.soulfire()),
                    fed.inCore(), 1), Burning::with);
        }
        return null;
    }

    /** {@code enemy} standing in the flame, as hot as {@code fire} burns it, lit from its hottest cone's nozzle. */
    private FlameDraw.Caught caught(EnemyMob enemy, Burning fire) {
        Cone hottest = fire.hottest();
        return enemy.accept(mob -> new FlameDraw.Caught(EnemyFrameBuilder.lookFor(mob.archetype()),
                this.lerp(mob.getPrevX(), mob.getX()), this.lerp(mob.getPrevY(), mob.getY()), mob.getBodyScale(),
                (float) mob.getFacingRadians(), fire.heat(), hottest.nozzle().x, hottest.nozzle().y,
                hottest.soulfire()));
    }

    /** One cone's fuel into an enemy, a second, and the nozzle and fire it came from. */
    private record Cone(float fuelPerSecond, Point2D.Float nozzle, boolean soulfire) {
    }

    /**
     * Every cone that fed one enemy this tick: the hottest, whether any held it in its Core, and how many. The hottest
     * sets the heat, so crossfire warms a rim without stealing the white and ice-blue that only upgrades reach.
     */
    private record Burning(Cone hottest, boolean inCore, int cones) {

        Burning with(Burning other) {
            Cone hotter = other.hottest().fuelPerSecond() > this.hottest().fuelPerSecond() ? other.hottest()
                    : this.hottest();
            return new Burning(hotter, this.inCore() || other.inCore(), this.cones() + other.cones());
        }

        float heat() {
            return heatOf(this.hottest().fuelPerSecond()) + (float) (FlameArt.CROSSFIRE_HEAT * (this.cones() - 1))
                    + (this.inCore() ? (float) FlameArt.CORE_HEAT : 0f);
        }
    }

    /** The steam its nozzle lets off as its reload starts. */
    private void steam(CinderTower tower) {
        OptionalInt since = tower.ticksSinceStreamEnded(this.gameTime, STEAM_TICKS);
        if (since.isEmpty()) {
            return;
        }
        int tier = tower.upgradeTree().highestOpenTier(tower, this.world);
        Point2D.Float muzzle = TowerSprites.muzzle(TowerSpriteFrameBuilder.bodyLookOf(tower), tier,
                TowerSpriteFrameBuilder.poseOf(tower), tower.getX(), tower.getY(), this.scale,
                tower.getTurretAim().radiansAt(this.interpolationAlpha));
        this.add(new SteamDraw(muzzle.x, muzzle.y, this.scale, this.progressOf(since.getAsInt(), STEAM_TICKS),
                TowerSpriteFrameBuilder.seedOf(tower)));
    }
}
