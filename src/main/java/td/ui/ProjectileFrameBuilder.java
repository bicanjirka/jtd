package td.ui;

import td.projectile.CannonballProjectile;
import td.projectile.MissileProjectile;
import td.projectile.ProjectileVisitor;
import td.ui.render.CannonballDraw;
import td.ui.render.MissileDraw;
import td.ui.render.Palette;
import td.ui.render.ProjectileDraw;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes each projectile, interpolated between ticks. A missile's facing comes from its movement
 * delta, which is safe because a missile moves many pixels per tick.
 */
public final class ProjectileFrameBuilder implements ProjectileVisitor<Void> {

    private final List<ProjectileDraw> draws = new ArrayList<>();
    private final double interpolationAlpha;

    public ProjectileFrameBuilder(double interpolationAlpha) {
        this.interpolationAlpha = interpolationAlpha;
    }

    private static float lerp(double from, double to, double alpha) {
        return (float) (from + (to - from) * alpha);
    }

    public List<ProjectileDraw> build() {
        return this.draws;
    }

    @Override
    public Void visitCannonball(CannonballProjectile projectile) {
        float x = lerp(projectile.getPrevX(), projectile.getX(), this.interpolationAlpha);
        float y = lerp(projectile.getPrevY(), projectile.getY(), this.interpolationAlpha);
        this.draws.add(new CannonballDraw(Palette.PROJECTILE_CANNONBALL, x, y));
        return null;
    }

    @Override
    public Void visitMissile(MissileProjectile projectile) {
        float x = lerp(projectile.getPrevX(), projectile.getX(), this.interpolationAlpha);
        float y = lerp(projectile.getPrevY(), projectile.getY(), this.interpolationAlpha);
        double facingRadians = Math.atan2(projectile.getY() - projectile.getPrevY(), projectile.getX() - projectile.getPrevX());
        this.draws.add(new MissileDraw(Palette.PROJECTILE_MISSILE, x, y, facingRadians));
        return null;
    }
}
