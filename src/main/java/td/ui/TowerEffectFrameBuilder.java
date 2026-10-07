package td.ui;

import td.damage.DamageType;
import td.enemy.EnemyMob;
import td.tower.AuraTower;
import td.tower.CinderTower;
import td.tower.MortarTower;
import td.tower.PulseTower;
import td.tower.SeekerTower;
import td.tower.SniperTower;
import td.tower.SonarTower;
import td.tower.SplashTower;
import td.tower.Tower;
import td.tower.TowerVisitor;
import td.tower.upgrade.StandardBaseSlot;
import td.ui.render.BeamDraw;
import td.ui.render.ConeDraw;
import td.ui.render.NestDraw;
import td.ui.render.Palette;
import td.ui.render.PulseDraw;
import td.ui.render.RingDraw;
import td.ui.render.SplashDraw;
import td.ui.render.TowerEffectDraw;
import td.ui.render.TowerStatusDraw;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Describes each tower's transient effect - beam, splash, pulse, cone, or an aura's ring - as draw
 * commands.
 */
public final class TowerEffectFrameBuilder implements TowerVisitor<Void> {

    // Two rings half a period apart read as one breathing aura, not a blinking ring.
    private static final double AURA_PERIOD_SECONDS = 1.8;
    private static final double[] AURA_PHASE_OFFSETS = {0.0, 0.5};

    private static final float STATUS_MARKER_SCALE_FRACTION = 0.12f;

    /** The aim laser's opacity with no Steady Aim stack, and how much each stack adds. */
    private static final float LASER_BASE_ALPHA = 0.2f;
    private static final float LASER_ALPHA_PER_STACK = 0.15f;
    private static final float SILVER_WIDTH_FACTOR = 1.5f;
    private static final float PING_RING_ALPHA = 0.6f;
    private static final float HELD_BEAM_ALPHA = 0.35f;
    /** The crosshair's ring and arms, as fractions of a cell. */
    private static final float CROSSHAIR_RADIUS_FRACTION = 0.45f;
    private static final float CROSSHAIR_ARM_FRACTION = 0.65f;
    private static final float CROSSHAIR_ALPHA = 0.8f;
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
    private static final double NEST_ORBIT_RADIANS_PER_TICK = 0.08;

    /** The Pulse's ripples: how many rings, how fast they spread, and how much faster for each Toll stack. */
    private static final float ZAP_STROKE = 2.5f;
    private static final int RIPPLE_RINGS = 3;
    private static final double RIPPLE_CYCLES_PER_SECOND = 0.6;
    private static final double RIPPLE_SPEEDUP_PER_TOLL = 0.4;

    private final List<TowerEffectDraw> draws = new ArrayList<>();
    private final int scale;
    private final int gameTime;
    private final double interpolationAlpha;
    private final double animationSeconds;

    public TowerEffectFrameBuilder(int scale, int gameTime, double interpolationAlpha, double animationSeconds) {
        this.scale = scale;
        this.gameTime = gameTime;
        this.interpolationAlpha = interpolationAlpha;
        this.animationSeconds = animationSeconds;
    }

    private static float beamWidth(float coolDownFraction) {
        return 3.0f * coolDownFraction;
    }

    /** Position of {@code seconds} in a repeating {@code [0, 1)} cycle. */
    private static double phaseFraction(double seconds, double phaseOffset) {
        double t = (seconds / AURA_PERIOD_SECONDS + phaseOffset) % 1.0;
        return t < 0 ? t + 1.0 : t;
    }

    /** A marker in the tower's top-right corner while an enemy disrupts it. */
    public void addStatus(Tower tower) {
        if (tower.isDisrupted()) {
            this.draws.add(new TowerStatusDraw(Palette.DISRUPTION, tower.getBoardX() + this.scale * 0.85f,
                    tower.getBoardY() + this.scale * 0.15f, this.scale * STATUS_MARKER_SCALE_FRACTION));
        }
    }

    public List<TowerEffectDraw> build() {
        return this.draws;
    }

    /**
     * The aim laser, brighter with each Steady Aim stack, and the last shot's trace to where it
     * really ended, thinning as the Sniper reloads.
     */
    public Void visitSniperTower(SniperTower tower) {
        EnemyMob target = tower.getCurrentTarget();
        OptionalInt stacks = tower.steadyAimStacks();
        if (target != null && !target.isDead() && stacks.isPresent()) {
            float alpha = Math.min(1f, LASER_BASE_ALPHA + LASER_ALPHA_PER_STACK * stacks.getAsInt());
            this.draws.add(new BeamDraw(Palette.TOWER_SNIPER_LASER, tower.getX(), tower.getY(),
                    (float) target.getX(), (float) target.getY(), 1f, alpha));
        }
        float width = beamWidth(tower.getCoolDownFraction());
        Optional<SniperTower.ShotTrace> shot = tower.lastShot();
        if (shot.isPresent() && width > 0f) {
            SniperTower.ShotTrace trace = shot.get();
            boolean silver = trace.type() == DamageType.MAGIC;
            this.draws.add(BeamDraw.solid(this.tracePalette(tower, trace), tower.getX(), tower.getY(),
                    trace.toX(), trace.toY(), silver ? width * SILVER_WIDTH_FACTOR : width));
        }
        return null;
    }

    private Palette tracePalette(SniperTower tower, SniperTower.ShotTrace trace) {
        if (trace.critical()) {
            return Palette.CRIT_SPARK;
        }
        if (trace.type() == DamageType.MAGIC) {
            return Palette.TOWER_SNIPER_SILVER;
        }
        return tower.upgrades().owns(StandardBaseSlot.TRANSCENDENT_ID) ? Palette.TOWER_SNIPER_TRACER
                : Palette.TOWER_SNIPER_BEAM;
    }

    /**
     * The shot to its target, the blast as a detonation that spreads as a ring and darkens as it
     * grows, the arcs it carried as jagged lightning, and a cast as a beam to each enemy it cursed.
     */
    public Void visitSplashTower(SplashTower tower) {
        float progress = tower.ticksSinceShot(this.gameTime) / DETONATION_TICKS;
        for (SplashTower.Blast blast : tower.getBlasts()) {
            EnemyMob target = blast.primary();
            this.draws.add(BeamDraw.solid(Palette.TOWER_SPLASH_BEAM, tower.getX(), tower.getY(),
                    (float) target.getX(), (float) target.getY(), beamWidth(tower.getCoolDownFraction())));
            if (progress < 1f) {
                this.detonation(blast.area(), progress);
            }
        }
        if (progress < 1f) {
            List<SplashTower.Trace> arcs = tower.getArcs();
            for (int i = 0; i < arcs.size(); i++) {
                this.lightning(arcs.get(i), i, 1f - progress);
            }
            for (SplashTower.Trace cast : tower.getCasts()) {
                this.draws.add(new BeamDraw(Palette.TOWER_SPLASH_CAST, cast.fromX(), cast.fromY(), cast.toX(), cast.toY(),
                        2f, 1f - progress));
            }
        }
        return null;
    }

    private void detonation(SplashTower.Blast.Area area, float progress) {
        float radius = area.radius() * progress;
        if (progress < FLASH_SHARE) {
            this.draws.add(new SplashDraw(Palette.TOWER_SPLASH_FLASH, area.centerX(), area.centerY(),
                    area.radius() * (FLASH_SHARE + progress)));
        }
        this.draws.add(new RingDraw(Palette.TOWER_SPLASH_DETONATION, area.centerX(), area.centerY(), radius,
                1f - progress));
        this.draws.add(new RingDraw(Palette.TOWER_SPLASH_DETONATION_DARK, area.centerX(), area.centerY(), radius,
                DARK_RING_ALPHA * progress * (1f - progress) * 4f));
    }

    /** One arc as a zigzag; which way each segment swings alternates, starting by the arc's order in the shot. */
    private void lightning(SplashTower.Trace arc, int order, float alpha) {
        float dx = arc.toX() - arc.fromX();
        float dy = arc.toY() - arc.fromY();
        float length = (float) Math.hypot(dx, dy);
        if (length == 0f) {
            return;
        }
        float swing = Math.min(ARC_MAX_SWING_PIXELS, length * ARC_SWING);
        float normalX = -dy / length * swing;
        float normalY = dx / length * swing;
        float x = arc.fromX();
        float y = arc.fromY();
        for (int i = 1; i <= ARC_SEGMENTS; i++) {
            float t = (float) i / ARC_SEGMENTS;
            float side = i == ARC_SEGMENTS ? 0f : ((i + order) % 2 == 0 ? 1f : -1f);
            float nextX = arc.fromX() + dx * t + normalX * side;
            float nextY = arc.fromY() + dy * t + normalY * side;
            this.draws.add(new BeamDraw(Palette.TOWER_SPLASH_ARC, x, y, nextX, nextY, 2f, alpha));
            x = nextX;
            y = nextY;
        }
    }

    /**
     * A fading beam to each enemy recently caught, blue once the beam deals magic; the ring each
     * revolution sends out; a crosshair over each enemy it pinged; and a held beam's line. A
     * spinning scan line is not drawn: the turret head shows it.
     */
    public Void visitSonarTower(SonarTower tower) {
        for (SonarTower.SonarHit hit : tower.getRecentHits()) {
            Palette palette = hit.type() == DamageType.MAGIC ? Palette.TOWER_SONAR_MAGIC_BEAM : Palette.TOWER_SONAR_BEAM;
            this.draws.add(BeamDraw.solid(palette, tower.getX(), tower.getY(), hit.x(), hit.y(),
                    beamWidth(tower.hitFade(hit, this.gameTime))));
        }
        float ping = tower.pingRingProgress(this.gameTime);
        if (ping >= 0f) {
            this.draws.add(new RingDraw(Palette.TOWER_SONAR_PING, tower.getX(), tower.getY(),
                    ping * tower.getRangeReal(), PING_RING_ALPHA * (1f - ping)));
        }
        for (EnemyMob pinged : tower.pinged()) {
            if (!pinged.isDead()) {
                this.crosshair((float) pinged.getX(), (float) pinged.getY());
            }
        }
        if (tower.holdsBeam()) {
            double heading = tower.sweepRadiansAt(this.interpolationAlpha);
            float range = tower.getRangeReal();
            this.draws.add(new BeamDraw(Palette.TOWER_SONAR_BEAM, tower.getX(), tower.getY(),
                    (float) (tower.getX() + Math.cos(heading) * range), (float) (tower.getY() + Math.sin(heading) * range),
                    2f, HELD_BEAM_ALPHA));
        }
        return null;
    }

    /** A ring with a cross through it, centred on {@code (x, y)}. */
    private void crosshair(float x, float y) {
        float arm = this.scale * CROSSHAIR_ARM_FRACTION;
        this.draws.add(new RingDraw(Palette.TOWER_SONAR_CROSSHAIR, x, y, this.scale * CROSSHAIR_RADIUS_FRACTION,
                CROSSHAIR_ALPHA));
        this.draws.add(new BeamDraw(Palette.TOWER_SONAR_CROSSHAIR, x - arm, y, x + arm, y, 1f, CROSSHAIR_ALPHA));
        this.draws.add(new BeamDraw(Palette.TOWER_SONAR_CROSSHAIR, x, y - arm, x, y + arm, 1f, CROSSHAIR_ALPHA));
    }

    /** The field's faint disc, and rings rippling out of the tower faster the more Toll its enemies hold. */
    public Void visitPulseTower(PulseTower tower) {
        this.addZap(tower);
        if (!tower.isFiring()) {
            return null;
        }
        this.draws.add(new PulseDraw(Palette.TOWER_PULSE_RING, tower.getX(), tower.getY(), tower.getRangeReal()));
        double cyclesPerSecond = RIPPLE_CYCLES_PER_SECOND * (1.0 + RIPPLE_SPEEDUP_PER_TOLL * tower.getHighestToll());
        for (int i = 0; i < RIPPLE_RINGS; i++) {
            double phase = (this.animationSeconds * cyclesPerSecond + (double) i / RIPPLE_RINGS) % 1.0;
            this.draws.add(new RingDraw(Palette.TOWER_PULSE_RIPPLE, tower.getX(), tower.getY(),
                    (float) (phase * tower.getRangeReal()), (float) (1.0 - phase)));
        }
        return null;
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
        for (Tower buffed : tower.buffedTowers()) {
            this.draws.add(BeamDraw.solid(Palette.TOWER_AURA_LINK, tower.getX(), tower.getY(),
                    buffed.getX(), buffed.getY(), 1.0f));
        }
        return null;
    }

    /** Nothing: the shell is drawn as a projectile. */
    public Void visitMortarTower(MortarTower tower) {
        return null;
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
            this.draws.add(new ConeDraw(Palette.TOWER_CINDER_CONE, tower.getX(), tower.getY(),
                    (float) wave.headingRadians(), tower.getRangeReal(), (float) wave.halfWidthRadians(), progress));
        }
        return null;
    }

    private static float waveProgress(CinderTower.FlameWave wave, int atTick) {
        float travelled = (float) (atTick - wave.firedAtTick()) / CinderTower.WAVE_TRAVEL_TICKS;
        return Math.max(0f, Math.min(1f, travelled));
    }
}
