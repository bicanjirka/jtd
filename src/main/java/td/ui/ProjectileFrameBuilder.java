package td.ui;

import td.projectile.CannonballProjectile;
import td.projectile.MissileLook;
import td.projectile.MissileProjectile;
import td.projectile.ProjectileVisitor;
import td.ui.render.CannonballDraw;
import td.ui.render.MissileDraw;
import td.ui.render.Palette;
import td.ui.render.ProjectileDraw;
import td.ui.render.SmokeDraw;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes each projectile, interpolated between ticks. A missile's facing comes from its movement
 * delta, which is safe because a missile moves many pixels per tick.
 */
public final class ProjectileFrameBuilder implements ProjectileVisitor<Void> {

    /** A puff's radius in pixels when fresh, and how opaque it starts. */
    private static final float SMOKE_RADIUS = 2f;
    private static final float SMOKE_ALPHA = 0.5f;

    private final List<ProjectileDraw> draws = new ArrayList<>();
    private final double interpolationAlpha;

    public ProjectileFrameBuilder(double interpolationAlpha) {
        this.interpolationAlpha = interpolationAlpha;
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

    public List<ProjectileDraw> build() {
        return this.draws;
    }

    @Override
    public Void visitCannonball(CannonballProjectile projectile) {
        float x = lerp(projectile.getPrevX(), projectile.getX(), this.interpolationAlpha);
        float y = lerp(projectile.getPrevY(), projectile.getY(), this.interpolationAlpha);
        this.draws.add(new CannonballDraw(Palette.PROJECTILE_CANNONBALL, x, y, projectile.stats().size()));
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
