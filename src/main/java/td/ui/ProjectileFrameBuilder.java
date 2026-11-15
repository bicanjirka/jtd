package td.ui;

import td.projectile.CannonballProjectile;
import td.projectile.MissileLook;
import td.projectile.MissileProjectile;
import td.projectile.ProjectileVisitor;
import td.projectile.ShellLook;
import td.ui.render.CannonballDraw;
import td.ui.render.LoadedShell;
import td.ui.render.MissileDraw;
import td.ui.render.Palette;
import td.ui.render.ProjectileDraw;
import td.ui.render.SmokeDraw;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Describes each projectile, interpolated between ticks. A missile's facing comes from its movement
 * delta, which is safe because a missile moves many pixels per tick.
 */
public final class ProjectileFrameBuilder implements ProjectileVisitor<Void> {

    /** A puff's radius in pixels when fresh, and how opaque it starts. */
    private static final float SMOKE_RADIUS = 2f;
    private static final float SMOKE_ALPHA = 0.5f;
    /**
     * The one pull on every lobbed shot, in cells per tick per tick: a shot that flies longer climbs higher,
     * so every lob arcs alike whatever its speed and distance.
     */
    private static final float LOB_PULL_CELLS = 0.03f;
    /** No lob climbs higher than this, in cells, so a slow shell stays on the board. */
    private static final float LOB_TOP_CELLS = 2f;
    /** At its peak the shadow is this much smaller than on the ground. */
    private static final float SHADOW_SHRINK = 0.45f;

    private final List<ProjectileDraw> draws = new ArrayList<>();
    private final double interpolationAlpha;
    private final int scale;

    public ProjectileFrameBuilder(double interpolationAlpha, int scale) {
        this.interpolationAlpha = interpolationAlpha;
        this.scale = scale;
    }

    private static float lerp(double from, double to, double alpha) {
        return (float) (from + (to - from) * alpha);
    }

    private static Palette paletteFor(MissileLook look) {
        return switch (look) {
            case STANDARD -> Palette.PROJECTILE_MISSILE;
            case CRYO -> Palette.PROJECTILE_CRYO;
            case ARCANE -> Palette.PROJECTILE_ARCANE;
            case EMP -> Palette.PROJECTILE_EMP;
            case TRACER -> Palette.PROJECTILE_TRACER;
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

    /** A burning, freezing or glowing shell leaves a trail in flight. */
    private static boolean trails(LoadedShell kind) {
        return switch (kind) {
            case NAPALM, CRYO, FLARE -> true;
            case NORMAL, HEAVY, NUKE, TAR -> false;
        };
    }

    public List<ProjectileDraw> build() {
        return this.draws;
    }

    /**
     * A lobbed shot flies its arc as if thrown up and pulled down: its peak grows with the square of its
     * flight time.
     */
    private float peakOf(CannonballProjectile projectile) {
        return lobPeak(projectile.distance() / projectile.stats().speed(), this.scale);
    }

    /** How high a lob that flies {@code ticks} climbs at its peak, in pixels. */
    static float lobPeak(double ticks, int scale) {
        return (float) Math.min(LOB_TOP_CELLS * scale, LOB_PULL_CELLS * scale * ticks * ticks / 8);
    }

    /** How high a lob with this {@code peak} is once it has flown {@code flown} of its way. */
    static float lobLift(float peak, double flown) {
        double share = Math.clamp(flown, 0, 1);
        return (float) (peak * 4 * share * (1 - share));
    }

    /** How small a lob's shadow is at {@code lift} under this {@code peak}. */
    static float lobShadowScale(float lift, float peak) {
        return peak > 0 ? 1f - SHADOW_SHRINK * lift / peak : 1f;
    }

    /** How high a lob with this {@code peak} is over its ground point at {@code x, y}. */
    private static float liftAt(CannonballProjectile projectile, float peak, double x, double y) {
        double distance = projectile.distance();
        return lobLift(peak, distance > 0 ? projectile.flownAt(x, y) / distance : 1);
    }

    @Override
    public Void visitCannonball(CannonballProjectile projectile) {
        float x = lerp(projectile.getPrevX(), projectile.getX(), this.interpolationAlpha);
        float y = lerp(projectile.getPrevY(), projectile.getY(), this.interpolationAlpha);
        float peak = this.peakOf(projectile);
        float lift = liftAt(projectile, peak, x, y);
        float shadowScale = lobShadowScale(lift, peak);
        LoadedShell kind = loadedShellOf(projectile.look());
        boolean streak = projectile.stats().streak() || trails(kind);
        float tailX = streak ? (float) projectile.getPrevX() : x;
        float tailY = streak ? (float) projectile.getPrevY()
                - liftAt(projectile, peak, projectile.getPrevX(), projectile.getPrevY()) : y - lift;
        this.draws.add(new CannonballDraw(paletteFor(projectile.look()), x, y, lift, shadowScale,
                projectile.stats().size(), tailX, tailY, Optional.of(kind)));
        return null;
    }

    @Override
    public Void visitMissile(MissileProjectile projectile) {
        float x = lerp(projectile.getPrevX(), projectile.getX(), this.interpolationAlpha);
        float y = lerp(projectile.getPrevY(), projectile.getY(), this.interpolationAlpha);
        double facingRadians = Math.atan2(projectile.getY() - projectile.getPrevY(), projectile.getX() - projectile.getPrevX());
        List<MissileProjectile.TrailPoint> trail = projectile.trail();
        for (int i = 0; i < trail.size(); i++) {
            // The oldest puff is the faintest and the widest, as it has had longest to spread.
            float age = (trail.size() - i) / (float) (trail.size() + 1);
            this.draws.add(new SmokeDraw(Palette.PROJECTILE_SMOKE, (float) trail.get(i).x(), (float) trail.get(i).y(),
                    SMOKE_RADIUS * (1f + age) * projectile.stats().size(), SMOKE_ALPHA * (1f - age)));
        }
        this.draws.add(new MissileDraw(paletteFor(projectile.stats().look()), x, y, facingRadians,
                projectile.stats().size()));
        return null;
    }
}
