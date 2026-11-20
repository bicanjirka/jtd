package td.ui;

import td.projectile.CannonballProjectile;
import td.projectile.MissileLook;
import td.projectile.MissileProjectile;
import td.projectile.ProjectileVisitor;
import td.projectile.ShellLook;
import td.ui.render.CannonballDraw;
import td.ui.render.LoadedMissile;
import td.ui.render.LoadedShell;
import td.ui.render.MissileDraw;
import td.ui.render.Palette;
import td.ui.render.ProjectileDraw;
import td.ui.render.ShellTrail;
import td.ui.render.SmokeDraw;
import td.ui.render.TrailLook;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Describes each projectile, interpolated between ticks. */
public final class ProjectileFrameBuilder implements ProjectileVisitor<Void> {

    /** A puff's radius in pixels when fresh, and how opaque it starts. */
    private static final float SMOKE_RADIUS = 2f;
    private static final float SMOKE_ALPHA = 0.5f;
    /** How long a missile of the standard size is drawn, in cells: the same in the nest as in flight. */
    static final float MISSILE_LENGTH_CELLS = 0.22f;
    /**
     * The one steep arc of every lobbed shot, in cells: it climbs this high over its launch, and this much more for
     * every cell it flies on the ground, so even a short lob goes up high and drops almost straight down.
     */
    private static final float LOB_RISE_CELLS = 1.1f;
    private static final float LOB_RISE_PER_CELL = 0.32f;
    /** No lob climbs higher than this, in cells, so a long one stays on the board. */
    private static final float LOB_TOP_CELLS = 2.6f;
    /**
     * A Mortar's shell leaves from its muzzle, this high over the tower's centre on screen, in cells: the muzzle's
     * height whatever the tier, its flash and smoke hiding the difference, so the shell keeps no link to its tower.
     */
    static final float SHELL_LAUNCH_CELLS = 0.55f;
    /** At its peak the shadow is this much smaller than on the ground. */
    private static final float SHADOW_SHRINK = 0.45f;
    /** A shell's tail runs back over the ground it flew in this many ticks, drawn through this many points. */
    private static final float TRAIL_TICKS = 7f;
    private static final int TRAIL_POINTS = 10;
    /** A Normal carrying a share of a payload draws a thinner tail than the payload shell itself. */
    private static final float CARRIED_TRAIL_WIDTH = 0.6f;

    private final List<ProjectileDraw> draws = new ArrayList<>();
    private final double interpolationAlpha;
    private final int scale;
    private final int tick;

    /** A builder for the frame at {@code tick}, which a tail's flicker follows. */
    public ProjectileFrameBuilder(int tick, double interpolationAlpha, int scale) {
        this.tick = tick;
        this.interpolationAlpha = interpolationAlpha;
        this.scale = scale;
    }

    private static float lerp(double from, double to, double alpha) {
        return (float) (from + (to - from) * alpha);
    }

    /** The drawing of a missile of {@code look}: the same in the nest as in flight. */
    static LoadedMissile loadedMissileOf(MissileLook look) {
        return switch (look) {
            case STANDARD -> LoadedMissile.FROST;
            case RUBY -> LoadedMissile.RUBY;
            case SHADE -> LoadedMissile.SHADE;
        };
    }

    static Palette paletteFor(ShellLook look) {
        return switch (look) {
            case NORMAL, HEAVY -> Palette.PROJECTILE_CANNONBALL;
            case FLARE -> Palette.ZONE_FLARE;
            case NAPALM -> Palette.PROJECTILE_NAPALM;
            case TAR -> Palette.PROJECTILE_TAR;
            case CRYO -> Palette.PROJECTILE_FROST;
            case NUKE -> Palette.PROJECTILE_NUKE;
        };
    }

    /** The drawing of a shell of {@code look}: the same in flight as in the bore and in the pane. */
    static LoadedShell loadedShellOf(ShellLook look) {
        return switch (look) {
            case NORMAL -> LoadedShell.NORMAL;
            case HEAVY -> LoadedShell.HEAVY;
            case NUKE -> LoadedShell.NUKE;
            case NAPALM -> LoadedShell.NAPALM;
            case TAR -> LoadedShell.TAR;
            case CRYO -> LoadedShell.CRYO;
            case FLARE -> LoadedShell.FLARE;
        };
    }

    /** The tail a shell of {@code look} draws in flight: every payload's, a flare's and a nuke's glow. */
    private static Optional<TrailLook> trailOf(ShellLook look) {
        return switch (look) {
            case NAPALM -> Optional.of(TrailLook.NAPALM);
            case TAR -> Optional.of(TrailLook.TAR);
            case CRYO -> Optional.of(TrailLook.CRYO);
            case FLARE -> Optional.of(TrailLook.FLARE);
            case NUKE -> Optional.of(TrailLook.NUKE);
            case NORMAL, HEAVY -> Optional.empty();
        };
    }

    public List<ProjectileDraw> build() {
        return this.draws;
    }

    /** The peak of a shell's arc, from its flight's length on the ground. */
    private float peakOf(CannonballProjectile projectile) {
        return lobPeak(projectile.distance() / this.scale, this.scale);
    }

    /** How high a lob that flies {@code groundCells} on the ground climbs over its launch at its peak, in pixels. */
    static float lobPeak(double groundCells, int scale) {
        return (float) Math.min(LOB_TOP_CELLS * scale, (LOB_RISE_CELLS + LOB_RISE_PER_CELL * groundCells) * scale);
    }

    /**
     * How high a lob is once it has flown {@code flown} of its way: from its {@code launch} height, which it leaves
     * behind as it goes, up its arc of this {@code peak} and down onto the ground.
     */
    static float lobLift(float launch, float peak, double flown) {
        double share = Math.clamp(flown, 0, 1);
        return (float) (launch * (1 - share) + peak * 4 * share * (1 - share));
    }

    /** How small a lob's shadow is once it has flown {@code flown} of its way: smallest at the arc's peak. */
    static float lobShadowScale(double flown) {
        double share = Math.clamp(flown, 0, 1);
        return (float) (1 - SHADOW_SHRINK * 4 * share * (1 - share));
    }

    /** How far a shell has flown of its way at its ground point {@code x, y}, 0..1. */
    private static double flownShare(CannonballProjectile projectile, double x, double y) {
        double distance = projectile.distance();
        return distance > 0 ? projectile.flownAt(x, y) / distance : 1;
    }

    /** How high a shell under this {@code peak} is over its ground point at {@code x, y}. */
    private float liftAt(CannonballProjectile projectile, float peak, double x, double y) {
        return lobLift(SHELL_LAUNCH_CELLS * this.scale, peak, flownShare(projectile, x, y));
    }

    @Override
    public Void visitCannonball(CannonballProjectile projectile) {
        float x = lerp(projectile.getPrevX(), projectile.getX(), this.interpolationAlpha);
        float y = lerp(projectile.getPrevY(), projectile.getY(), this.interpolationAlpha);
        float peak = this.peakOf(projectile);
        float lift = this.liftAt(projectile, peak, x, y);
        float shadowScale = lobShadowScale(flownShare(projectile, x, y));
        LoadedShell kind = loadedShellOf(projectile.look());
        Optional<ShellTrail> trail = trailOf(projectile.look()).map(look -> this.trail(projectile, peak, x, y, look, 1f))
                .or(() -> projectile.carried().flatMap(ProjectileFrameBuilder::trailOf)
                        .map(look -> this.trail(projectile, peak, x, y, look, CARRIED_TRAIL_WIDTH)));
        boolean streak = projectile.stats().streak() && trail.isEmpty();
        float tailX = streak ? (float) projectile.getPrevX() : x;
        float tailY = streak ? (float) projectile.getPrevY()
                - this.liftAt(projectile, peak, projectile.getPrevX(), projectile.getPrevY()) : y - lift;
        this.draws.add(new CannonballDraw(paletteFor(projectile.look()), x, y, lift, shadowScale,
                projectile.stats().size(), tailX, tailY, Optional.of(kind), trail));
        return null;
    }

    /**
     * The tail of a shell now over {@code x, y}: back along the ground it came over, each point lifted to where the arc
     * had it, so the tail bends with the arc. It is no longer than the way flown so far.
     */
    private ShellTrail trail(CannonballProjectile projectile, float peak, float x, float y, TrailLook look,
                             float width) {
        double dx = projectile.getX() - projectile.getPrevX();
        double dy = projectile.getY() - projectile.getPrevY();
        double step = Math.hypot(dx, dy);
        double length = step > 0 ? Math.min(projectile.flownAt(x, y), TRAIL_TICKS * projectile.stats().speed()) : 0;
        List<ShellTrail.Point> points = new ArrayList<>(TRAIL_POINTS);
        for (int i = 0; i < TRAIL_POINTS; i++) {
            double back = length * i / (TRAIL_POINTS - 1);
            double px = x - (step > 0 ? dx / step * back : 0);
            double py = y - (step > 0 ? dy / step * back : 0);
            points.add(new ShellTrail.Point((float) px, (float) py - this.liftAt(projectile, peak, px, py)));
        }
        return new ShellTrail(look, width * projectile.stats().size(), List.copyOf(points), this.tick);
    }

    @Override
    public Void visitMissile(MissileProjectile projectile) {
        float x = lerp(projectile.getPrevX(), projectile.getX(), this.interpolationAlpha);
        float y = lerp(projectile.getPrevY(), projectile.getY(), this.interpolationAlpha);
        List<MissileProjectile.TrailPoint> trail = projectile.trail();
        for (int i = 0; i < trail.size(); i++) {
            // The oldest puff is the faintest and the widest, as it has had longest to spread.
            float age = (trail.size() - i) / (float) (trail.size() + 1);
            this.draws.add(new SmokeDraw(Palette.PROJECTILE_SMOKE, (float) trail.get(i).x(), (float) trail.get(i).y(),
                    SMOKE_RADIUS * (1f + age) * projectile.stats().size(), SMOKE_ALPHA * (1f - age)));
        }
        this.draws.add(new MissileDraw(loadedMissileOf(projectile.stats().look()), x, y, projectile.headingRadians(),
                MISSILE_LENGTH_CELLS * this.scale * projectile.stats().size()));
        return null;
    }
}
