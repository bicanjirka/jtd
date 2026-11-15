package td.ui;

import td.projectile.ShellLook;
import td.tower.AuraTower;
import td.tower.CinderTower;
import td.tower.MortarTower;
import td.tower.PulseTower;
import td.tower.SeekerTower;
import td.tower.SniperTower;
import td.tower.SplashTower;
import td.tower.Tower;
import td.tower.TowerTerm;
import td.tower.TowerVisitor;
import td.tower.mortar.BlastMark;
import td.tower.splash.Passive;
import td.ui.render.BeamDraw;
import td.ui.render.CannonballDraw;
import td.ui.render.ConeDraw;
import td.ui.render.FlashDraw;
import td.ui.render.LeyLineDraw;
import td.ui.render.NestDraw;
import td.ui.render.Palette;
import td.ui.render.PulseDraw;
import td.ui.render.RefundMarkDraw;
import td.ui.render.RingDraw;
import td.ui.render.SplashDraw;
import td.ui.render.TowerEffectDraw;
import td.ui.render.TowerLook;
import td.ui.render.TowerStatusDraw;
import td.ui.render.TracerDraw;
import td.util.GameWorld;
import td.wave.Vec2;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Describes each tower's transient effect - beam, splash, pulse, cone, or an aura's ring - as draw
 * commands.
 */
public final class TowerEffectFrameBuilder implements TowerVisitor<Void> {

    // Two rings half a period apart read as one breathing aura, not a blinking ring.
    private static final double AURA_PERIOD_SECONDS = 1.8;
    private static final double[] AURA_PHASE_OFFSETS = {0.0, 0.5};

    private static final float STATUS_MARKER_SCALE_FRACTION = 0.12f;
    private static final float REFUND_MARK_SCALE_FRACTION = 0.3f;

    /** The aim laser's opacity with no Steady Aim stack, and how much each stack adds. */
    /** A bullet's streak is this share of the way from muzzle to enemy, and this wide, in cells. */
    private static final float TRACER_LENGTH = 0.45f;
    private static final float TRACER_WIDTH_CELLS = 0.028f;
    /** How much of its way a curse, spark or soul in flight trails behind it. */
    private static final float STREAK_SHARE = 0.25f;
    /** How long the streak a bullet leaves stays drawn once it has arrived: far less than a reload. */
    private static final float TRACER_FADE_TICKS = 4f;
    /** The Fourth Shot's bolt round its bullet: wider and fainter. */
    private static final float BOLT_WIDTH_CELLS = 0.09f;
    private static final float BOLT_ALPHA = 0.35f;
    /** How far a ricochet's arc bows out, as a share of the hop's length. */
    private static final float HOP_BOW = 0.25f;
    private static final float EXECUTION_TICKS = 10f;
    private static final float EXECUTION_RING_CELLS = 0.3f;
    private static final float EXECUTION_SHARD_FLIGHT_CELLS = 0.5f;
    private static final float EXECUTION_SHARD_CELLS = 0.12f;
    private static final int EXECUTION_SHARDS = 8;
    /** How bright a Ley Line is drawn while no enemy stands on it. */
    private static final float LEY_LINE_IDLE_ALPHA = 0.45f;
    /** How long a blast's detonation and its arcs stay drawn. */
    private static final float DETONATION_TICKS = 8f;
    /** The detonation's first flash lasts this share of it. */
    private static final float FLASH_SHARE = 0.25f;
    private static final float DARK_RING_ALPHA = 0.8f;
    /** An arc is drawn as this many zigzag segments, swinging out this share of its length. */
    private static final int ARC_SEGMENTS = 4;
    private static final float ARC_SWING = 0.12f;
    private static final float ARC_MAX_SWING_PIXELS = 6f;

    /** The nest's missiles circle at this fraction of a cell from the tower, a turn every few seconds. */
    private static final float NEST_ORBIT_FRACTION = 0.7f;
    private static final float APPRENTICE_MARK_CELLS = 0.55f;
    private static final float RALLY_SPOT_CELLS = 1f;
    private static final float NUKE_FLASH_SHARE = 0.6f;
    private static final float BLAST_MARK_ALPHA = 0.4f;
    private static final float NUKE_FLASH_ALPHA = 0.8f;
    private static final float BLAST_CORE_RING_ALPHA = 0.7f;
    private static final float BLAST_EDGE_RING_ALPHA = 0.3f;
    /** A Heavy's fragments: how many fly out, how long each streak is as a share of the edge, and how thick. */
    private static final int BLAST_FRAGMENTS = 8;
    private static final float FRAGMENT_SHARE = 0.25f;
    private static final float FRAGMENT_STROKE_CELLS = 0.05f;
    private static final double NEST_ORBIT_RADIANS_PER_TICK = 0.08;

    /** The Pulse's ripples: how many rings, how fast they spread, and how much faster for each Toll stack. */
    private static final float ZAP_STROKE = 2.5f;
    private static final int RIPPLE_RINGS = 3;
    private static final double RIPPLE_CYCLES_PER_SECOND = 0.6;
    private static final double RIPPLE_SPEEDUP_PER_TOLL = 0.4;

    private final GameWorld world;
    private final List<TowerEffectDraw> draws = new ArrayList<>();
    private final int scale;
    private final int gameTime;
    private final double interpolationAlpha;
    private final double animationSeconds;

    public TowerEffectFrameBuilder(GameWorld world, int gameTime, double interpolationAlpha, double animationSeconds) {
        this.world = world;
        this.scale = world.level().board().scale();
        this.gameTime = gameTime;
        this.interpolationAlpha = interpolationAlpha;
        this.animationSeconds = animationSeconds;
    }

    /** Position of {@code seconds} in a repeating {@code [0, 1)} cycle. */
    private static double phaseFraction(double seconds, double phaseOffset) {
        double t = (seconds / AURA_PERIOD_SECONDS + phaseOffset) % 1.0;
        return t < 0 ? t + 1.0 : t;
    }

    /** A marker in the tower's top-right corner while an enemy disrupts it. */
    public void addStatus(Tower tower) {
        float x = tower.getBoardX() + this.scale * 0.85f;
        float y = tower.getBoardY() + this.scale * 0.15f;
        if (tower.isDisrupted()) {
            this.draws.add(new TowerStatusDraw(Palette.DISRUPTION, x, y, this.scale * STATUS_MARKER_SCALE_FRACTION));
        } else if (tower.use().anyUnused()) {
            this.draws.add(new RefundMarkDraw(x, y, this.scale * REFUND_MARK_SCALE_FRACTION));
        }
    }

    public List<TowerEffectDraw> build() {
        return this.draws;
    }

    /**
     * Each bullet as a streak flying from the muzzle to its enemy, white at its head, and the streak the last one left,
     * fading fast; each ricochet as a green streak on an arc from enemy to enemy; and an execution as a ring that
     * bursts into shards.
     */
    public Void visitSniperTower(SniperTower tower) {
        int tier = tower.upgradeTree().highestOpenTier(tower, this.world);
        for (SniperTower.Bullet bullet : tower.bulletsInFlight()) {
            this.tracer(tower, tier, (float) bullet.target().getX(), (float) bullet.target().getY(),
                    this.flown(bullet), bullet.sureCrit(), bullet.shot().rounds(), 1f);
        }
        Optional<SniperTower.ShotTrace> shot = tower.lastShot();
        if (shot.isPresent()) {
            SniperTower.ShotTrace trace = shot.get();
            float age = (this.gameTime - trace.landedAt() + (float) this.interpolationAlpha) / TRACER_FADE_TICKS;
            if (age < 1f) {
                this.tracer(tower, tier, trace.toX(), trace.toY(), 1f, trace.critical(), trace.shot().rounds(), 1f - age);
            }
        }
        for (SniperTower.Hop hop : tower.hopsInFlight()) {
            this.hop(hop, this.flown(hop.bullet()));
        }
        tower.lastExecution().ifPresent(this::execution);
        return null;
    }

    /** How far {@code bullet} has flown, 0..1. */
    private float flown(SniperTower.Bullet bullet) {
        return Math.min(1f, (this.gameTime - bullet.firedAt() + (float) this.interpolationAlpha)
                / (bullet.landsAt() - bullet.firedAt()));
    }

    /** A streak {@code flown} of the way from the Sniper's muzzle to {@code toX, toY}. */
    private void tracer(SniperTower tower, int tier, float toX, float toY, float flown, boolean critical,
                        List<TowerTerm> rounds, float alpha) {
        double heading = Math.atan2(toY - tower.getY(), toX - tower.getX());
        Point2D.Float muzzle = TowerSprites.muzzle(TowerLook.SNIPER, tier, TowerSpriteFrameBuilder.poseOf(tower),
                tower.getX(), tower.getY(), this.scale, heading);
        float from = Math.max(0f, flown - TRACER_LENGTH);
        Palette tail = rounds.contains(TowerTerm.SILVER_ROUND) ? Palette.TOWER_SNIPER_SILVER
                : rounds.contains(TowerTerm.HOLLOW_POINT) ? Palette.DAMAGE_PHYSICAL
                : critical ? Palette.TOWER_SNIPER_TRACER : Palette.TOWER_SNIPER_BODY;
        float fromX = muzzle.x + (toX - muzzle.x) * from;
        float fromY = muzzle.y + (toY - muzzle.y) * from;
        float headX = muzzle.x + (toX - muzzle.x) * flown;
        float headY = muzzle.y + (toY - muzzle.y) * flown;
        if (rounds.contains(TowerTerm.FOURTH_SHOT)) {
            this.draws.add(new TracerDraw(Palette.TOWER_SNIPER_TRACER, Palette.TOWER_SNIPER_TRACER, fromX, fromY, headX,
                    headY, BOLT_WIDTH_CELLS * this.scale, alpha * BOLT_ALPHA));
        }
        this.draws.add(new TracerDraw(tail, Palette.TOWER_SNIPER_TRACER, fromX, fromY, headX, headY,
                TRACER_WIDTH_CELLS * this.scale, alpha));
    }

    /** A ricochet {@code flown} of the way along its arc, which bows out to one side of the straight line. */
    private void hop(SniperTower.Hop hop, float flown) {
        float fromX = (float) hop.from().x();
        float fromY = (float) hop.from().y();
        float toX = (float) hop.bullet().target().getX();
        float toY = (float) hop.bullet().target().getY();
        float bowX = (fromX + toX) / 2f - (toY - fromY) * HOP_BOW;
        float bowY = (fromY + toY) / 2f + (toX - fromX) * HOP_BOW;
        float tail = Math.max(0f, flown - TRACER_LENGTH);
        float middle = (tail + flown) / 2f;
        float[] points = {tail, middle, flown};
        for (int i = 0; i < points.length - 1; i++) {
            float a = points[i];
            float b = points[i + 1];
            this.draws.add(new TracerDraw(Palette.TOWER_SNIPER_BODY, Palette.TOWER_SNIPER_BODY,
                    bezier(fromX, bowX, toX, a), bezier(fromY, bowY, toY, a),
                    bezier(fromX, bowX, toX, b), bezier(fromY, bowY, toY, b), TRACER_WIDTH_CELLS * this.scale, 1f));
        }
    }

    private static float bezier(float from, float bow, float to, float t) {
        float rest = 1f - t;
        return rest * rest * from + 2f * rest * t * bow + t * t * to;
    }

    /** A thin white ring on the enemy an execution killed, bursting into shards that fly out as it fades. */
    private void execution(SniperTower.Execution execution) {
        float progress = (this.gameTime - execution.tick() + (float) this.interpolationAlpha) / EXECUTION_TICKS;
        if (progress >= 1f) {
            return;
        }
        float x = (float) execution.at().x();
        float y = (float) execution.at().y();
        float fade = 1f - progress;
        this.draws.add(new RingDraw(Palette.TOWER_SNIPER_TRACER, x, y, EXECUTION_RING_CELLS * this.scale, fade));
        float inner = (EXECUTION_RING_CELLS + EXECUTION_SHARD_FLIGHT_CELLS * progress) * this.scale;
        float outer = inner + EXECUTION_SHARD_CELLS * this.scale;
        for (int i = 0; i < EXECUTION_SHARDS; i++) {
            double angle = 2 * Math.PI * i / EXECUTION_SHARDS;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            this.draws.add(new TracerDraw(Palette.TOWER_SNIPER_TRACER, Palette.TOWER_SNIPER_TRACER,
                    x + cos * inner, y + sin * inner, x + cos * outer, y + sin * outer,
                    TRACER_WIDTH_CELLS * this.scale, fade));
        }
    }

    /**
     * Each stone on its lob with its shadow under it, each blast as a detonation that spreads as a ring to its edge and
     * darkens as it grows, each bolt that struck as jagged lightning, and each curse, spark and soul as a streak flying
     * to where it lands.
     */
    public Void visitSplashTower(SplashTower tower) {
        int tier = tower.upgradeTree().highestOpenTier(tower, this.world);
        for (SplashTower.Stone stone : tower.stonesInFlight()) {
            this.stone(tower, tier, stone);
        }
        for (SplashTower.Curse curse : tower.cursesInFlight()) {
            this.streak(Palette.TOWER_SPLASH_CAST, curse.from(), new Vec2(curse.target().getX(), curse.target().getY()),
                    curse.flight());
        }
        for (SplashTower.Wisp wisp : tower.wispsInFlight()) {
            Palette tone = wisp.passive() == Passive.STATIC ? Palette.TOWER_SPLASH_ARC : Palette.TOWER_SPLASH_CAST;
            this.streak(tone, wisp.from(), new Vec2(tower.getX(), tower.getY()), wisp.flight());
        }
        BlastLook look = BlastLook.of(tower.fork());
        for (SplashTower.Blast blast : tower.recentBlasts()) {
            float progress = (this.gameTime - blast.tick()) / DETONATION_TICKS;
            if (progress < 1f) {
                this.detonation((float) blast.at().x(), (float) blast.at().y(), blast.edge(), progress, look);
            }
        }
        List<SplashTower.Bolt> bolts = tower.recentBolts();
        for (int i = 0; i < bolts.size(); i++) {
            SplashTower.Bolt bolt = bolts.get(i);
            float progress = (this.gameTime - bolt.tick()) / DETONATION_TICKS;
            if (progress < 1f) {
                this.lightning((float) bolt.from().x(), (float) bolt.from().y(), (float) bolt.to().x(),
                        (float) bolt.to().y(), i, 1f - progress);
            }
        }
        return null;
    }

    /** How far along its way something flying is now, between the last two ticks. */
    private double flown(SplashTower.Flight flight) {
        double now = this.gameTime + this.interpolationAlpha;
        return Math.clamp((now - flight.firedAt()) / Math.max(1, flight.landsAt() - flight.firedAt()), 0, 1);
    }

    /**
     * A stone over its point on the ground, lifted on the one lob every thrown shot shares, from the height of the arm
     * that threw it.
     */
    private void stone(SplashTower tower, int tier, SplashTower.Stone stone) {
        double flown = this.flown(stone.flight());
        double heading = Math.atan2(stone.to().y() - tower.getY(), stone.to().x() - tower.getX());
        Point2D.Float muzzle = TowerSprites.muzzle(TowerSpriteFrameBuilder.bodyLookOf(tower), tier,
                TowerSpriteFrameBuilder.poseOf(tower), tower.getX(), tower.getY(), this.scale, heading);
        float x = (float) (muzzle.x + (stone.to().x() - muzzle.x) * flown);
        float y = (float) (tower.getY() + (stone.to().y() - tower.getY()) * flown);
        float released = (float) ((y - (muzzle.y + (stone.to().y() - muzzle.y) * flown)));
        float peak = ProjectileFrameBuilder.lobPeak(stone.flight().landsAt() - stone.flight().firedAt(), this.scale);
        float lift = ProjectileFrameBuilder.lobLift(peak, flown) + released;
        Palette tone = stone.charged() ? Palette.TOWER_SPLASH_ARC : Palette.PROJECTILE_CANNONBALL;
        this.draws.add(new CannonballDraw(tone, x, y, lift, ProjectileFrameBuilder.lobShadowScale(lift - released, peak), 1f, x,
                y - lift, Optional.empty()));
    }

    /** Something flying straight to where it lands, its streak trailing behind its head. */
    private void streak(Palette tone, Vec2 from, Vec2 to, SplashTower.Flight flight) {
        double flown = this.flown(flight);
        double tail = Math.max(0, flown - STREAK_SHARE);
        this.draws.add(new TracerDraw(tone, tone, (float) (from.x() + (to.x() - from.x()) * tail),
                (float) (from.y() + (to.y() - from.y()) * tail), (float) (from.x() + (to.x() - from.x()) * flown),
                (float) (from.y() + (to.y() - from.y()) * flown), TRACER_WIDTH_CELLS * this.scale, 1f));
    }

    private void detonation(float centerX, float centerY, float edge, float progress, BlastLook look) {
        float radius = edge * progress;
        if (progress < FLASH_SHARE) {
            this.draws.add(new SplashDraw(look.flash(), centerX, centerY, edge * (FLASH_SHARE + progress)));
        }
        this.draws.add(new RingDraw(look.detonation(), centerX, centerY, radius, 1f - progress));
        this.draws.add(new RingDraw(look.detonationDark(), centerX, centerY, radius,
                DARK_RING_ALPHA * progress * (1f - progress) * 4f));
    }

    /** The colours of a Burst's blast: the fork it took tints it like its body. */
    private record BlastLook(Palette flash, Palette detonation, Palette detonationDark) {

        static BlastLook of(SplashTower.Fork fork) {
            return switch (fork) {
                case NONE -> new BlastLook(Palette.TOWER_SPLASH_FLASH,
                        Palette.TOWER_SPLASH_DETONATION, Palette.TOWER_SPLASH_DETONATION_DARK);
                case STORMCALLER -> new BlastLook(Palette.TOWER_STORMCALLER_FLASH,
                        Palette.TOWER_STORMCALLER_DETONATION, Palette.TOWER_STORMCALLER_DETONATION_DARK);
                case HEXER -> new BlastLook(Palette.TOWER_HEXER_FLASH,
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
            this.draws.add(new BeamDraw(Palette.TOWER_SPLASH_ARC, x, y, nextX, nextY, 2f, alpha));
            x = nextX;
            y = nextY;
        }
    }

    /** The field's faint disc, and rings rippling out of the tower faster the more Toll its enemies hold. */
    public Void visitPulseTower(PulseTower tower) {
        this.addZap(tower);
        Palette tint = rippleFor(tower.getFieldLook());
        tower.getLeyLine().ifPresent(line -> this.draws.add(new LeyLineDraw(tint, line.fromX(), line.fromY(), line.toX(),
                line.toY(), tower.isLeyLinePinned(), tower.isLeyLineBusy() ? 1f : LEY_LINE_IDLE_ALPHA)));
        if (!tower.isFiring()) {
            return null;
        }
        float radius = tower.getFieldRadius();
        this.draws.add(new PulseDraw(Palette.TOWER_PULSE_RING, tower.getX(), tower.getY(), radius));
        if (tower.hasGravityWell()) {
            this.draws.add(new PulseDraw(Palette.TOWER_PULSE_WELL, tower.getX(), tower.getY(), radius * 2f / 3f));
            this.draws.add(new PulseDraw(Palette.TOWER_PULSE_WELL, tower.getX(), tower.getY(), radius / 3f));
        }
        if (tower.bites()) {
            this.draws.add(new RingDraw(Palette.TOWER_PULSE_RIM_BITE, tower.getX(), tower.getY(), radius, 1f));
        }
        double cyclesPerSecond = RIPPLE_CYCLES_PER_SECOND * (1.0 + RIPPLE_SPEEDUP_PER_TOLL * tower.getHighestToll());
        for (int i = 0; i < RIPPLE_RINGS; i++) {
            double phase = (this.animationSeconds * cyclesPerSecond + (double) i / RIPPLE_RINGS) % 1.0;
            this.draws.add(new RingDraw(tint, tower.getX(), tower.getY(), (float) (phase * radius), (float) (1.0 - phase)));
        }
        return null;
    }

    private static Palette rippleFor(PulseTower.FieldLook look) {
        return switch (look) {
            case PLAIN -> Palette.TOWER_PULSE_RIPPLE;
            case WARD -> Palette.TOWER_PULSE_RIPPLE_WARD;
            case GRAVITY -> Palette.TOWER_PULSE_RIPPLE_GRAVITY;
        };
    }

    /** A zap's bolts, bright at first and fading as they age. */
    private void addZap(PulseTower tower) {
        int age = tower.ticksSinceZap(this.gameTime);
        if (age >= tower.zapFlashTicks()) {
            return;
        }
        float alpha = 1f - (float) age / tower.zapFlashTicks();
        for (PulseTower.Zap bolt : tower.getZaps()) {
            this.draws.add(new BeamDraw(Palette.TOWER_PULSE_ZAP, bolt.fromX(), bolt.fromY(), bolt.toX(), bolt.toY(),
                    ZAP_STROKE, alpha));
        }
    }


    public Void visitAuraTower(AuraTower tower) {
        float maxRadius = tower.getRangeReal();
        for (double phaseOffset : AURA_PHASE_OFFSETS) {
            double phase = phaseFraction(this.animationSeconds, phaseOffset);
            this.draws.add(new RingDraw(Palette.TOWER_AURA_RING, tower.getX(), tower.getY(),
                    (float) (phase * maxRadius), (float) (1.0 - phase)));
        }
        Tower apprentice = tower.mentorsApprentice() ? tower.apprentice() : null;
        if (apprentice != null) {
            this.draws.add(new RingDraw(Palette.TOWER_AURA_MENTEE, apprentice.getX(), apprentice.getY(),
                    this.scale * APPRENTICE_MARK_CELLS, 1f));
        }
        if (tower.isSelected()) {
            tower.rallySpot().ifPresent(spot -> this.draws.add(new RingDraw(Palette.TOWER_AURA_RALLY,
                    (float) spot.x(), (float) spot.y(), this.scale * RALLY_SPOT_CELLS, 0.9f)));
        }
        return null;
    }

    /** The flash of each blast that just went off, and a nuke's ring; the shell is a projectile. */
    public Void visitMortarTower(MortarTower tower) {
        for (BlastMark mark : tower.getBlastMarks()) {
            float progress = (this.gameTime - mark.startedAtTick() + (float) this.interpolationAlpha)
                    / MortarTower.BLAST_MARK_TICKS;
            if (progress >= 0f && progress < 1f) {
                this.blastMark(mark, progress);
            }
        }
        tower.getNukeFlash().ifPresent(flash -> {
            float progress = (this.gameTime - flash.startedAtTick() + (float) this.interpolationAlpha)
                    / MortarTower.NUKE_FLASH_TICKS;
            if (progress >= 0f && progress < 1f) {
                float fade = 1f - progress;
                this.draws.add(new FlashDraw(Palette.TOWER_MORTAR_NUKE_FLASH, (float) flash.x(), (float) flash.y(),
                        flash.radius() * NUKE_FLASH_SHARE, fade * NUKE_FLASH_ALPHA));
                this.draws.add(new RingDraw(Palette.TOWER_MORTAR_NUKE_FLASH, (float) flash.x(), (float) flash.y(),
                        flash.radius() * progress, fade));
            }
        });
        return null;
    }

    /**
     * A blast's flash over its edge, a white ring round its full-damage core and a fainter one, or a nuke's yellow-green
     * one, round its edge; a Heavy with shrapnel throws fragments out over it.
     */
    private void blastMark(BlastMark mark, float progress) {
        float fade = 1f - progress;
        float x = (float) mark.at().x();
        float y = (float) mark.at().y();
        this.draws.add(new FlashDraw(ProjectileFrameBuilder.paletteFor(mark.look()), x, y, mark.edge(),
                fade * BLAST_MARK_ALPHA));
        this.draws.add(new RingDraw(Palette.BLAST_RING, x, y, mark.core(), fade * BLAST_CORE_RING_ALPHA));
        if (mark.edge() > mark.core()) {
            boolean nuke = mark.look() == ShellLook.NUKE;
            this.draws.add(new RingDraw(nuke ? Palette.BLAST_NUKE_RING : Palette.BLAST_RING, x, y, mark.edge(),
                    fade * (nuke ? BLAST_CORE_RING_ALPHA : BLAST_EDGE_RING_ALPHA)));
        }
        if (mark.fragments()) {
            float head = mark.edge() * progress;
            float tail = Math.max(0f, head - mark.edge() * FRAGMENT_SHARE);
            for (int i = 0; i < BLAST_FRAGMENTS; i++) {
                double angle = i * 2 * Math.PI / BLAST_FRAGMENTS + mark.startedAtTick();
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                this.draws.add(new TracerDraw(Palette.PROJECTILE_CANNONBALL, Palette.TERM_HEAVY_SHELL, x + cos * tail,
                        y + sin * tail, x + cos * head, y + sin * head, this.scale * FRAGMENT_STROKE_CELLS, fade));
            }
        }
    }

    /** The missiles banked in the nest, circling the tower; the missile in flight is a projectile. */
    public Void visitSeekerTower(SeekerTower tower) {
        int stored = tower.getNestStored();
        if (stored > 0) {
            double phase = (this.gameTime + this.interpolationAlpha) * NEST_ORBIT_RADIANS_PER_TICK;
            this.draws.add(new NestDraw(Palette.PROJECTILE_MISSILE, tower.getX(), tower.getY(),
                    NEST_ORBIT_FRACTION * this.scale, stored, phase));
        }
        return null;
    }

    /** One cone per wave in flight, its progress interpolated between ticks. */
    public Void visitCinderTower(CinderTower tower) {
        for (CinderTower.FlameWave wave : tower.getInFlightWaves()) {
            float previousProgress = waveProgress(wave, this.gameTime - 1);
            float currentProgress = waveProgress(wave, this.gameTime);
            float progress = previousProgress + (currentProgress - previousProgress) * (float) this.interpolationAlpha;
            this.draws.add(new ConeDraw(conePaletteFor(wave), tower.getX(), tower.getY(),
                    (float) wave.headingRadians(), tower.getRangeReal(), (float) wave.halfWidthRadians(), progress,
                    wave.stokeSteps()));
        }
        return null;
    }

    private static Palette conePaletteFor(CinderTower.FlameWave wave) {
        if (wave.isSoulfire()) {
            return Palette.TOWER_CINDER_CONE_SOUL;
        }
        return switch (wave.look()) {
            case ORANGE -> Palette.TOWER_CINDER_CONE;
            case WHITE -> Palette.TOWER_CINDER_CONE_WHITE;
            case SEARING -> Palette.TOWER_CINDER_CONE_SEARING;
        };
    }

    private static float waveProgress(CinderTower.FlameWave wave, int atTick) {
        float travelled = (float) (atTick - wave.firedAtTick()) / wave.travelTicks();
        return Math.clamp(travelled, 0f, 1f);
    }
}
