package td.ui;

import td.enemy.AbstractEnemyMob;
import td.enemy.EnemyMobCircle;
import td.enemy.EnemyMobEmpty;
import td.enemy.EnemyMobGhost;
import td.enemy.EnemyMobSquare;
import td.enemy.EnemyMobTriangle;
import td.enemy.EnemyMobVisitor;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.Palette;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes each enemy's body and death-fade animation as {@link EnemyDraw}
 * commands - one visit method per concrete type, since body shape/palette
 * genuinely differ by kind. An alive body's position is interpolated between
 * the mob's previous and current tick position (see {@code interpolationAlpha});
 * a fading (dead) mob is frozen at its death position and drawn as-is, since it
 * has stopped moving - interpolating it against alpha would make it slide back
 * and forth every frame between two positions that never change again.
 */
public final class EnemyFrameBuilder implements EnemyMobVisitor<Void> {

    private final List<EnemyDraw> draws = new ArrayList<>();
    private final int gameTime;
    private final double interpolationAlpha;

    public EnemyFrameBuilder(int gameTime, double interpolationAlpha) {
        this.gameTime = gameTime;
        this.interpolationAlpha = interpolationAlpha;
    }

    public List<EnemyDraw> build() {
        return this.draws;
    }

    private static float lerp(int from, int to, double alpha) {
        return (float) (from + (to - from) * alpha);
    }

    private Void body(Palette palette, AbstractEnemyMob mob, float scale, double facingRadians) {
        if (mob.isDead()) {
            if (!mob.isFadeComplete(this.gameTime)) {
                int age = mob.ticksSinceDeath(this.gameTime);
                float fadeProgress = 1f - (mob.fadeAlpha(age) / 255f);
                this.draws.add(new EnemyFadeDraw(palette, mob.getX(), mob.getY(), facingRadians, scale, age, fadeProgress));
            }
        } else if (!mob.isInactive()) {
            float x = lerp(mob.getPrevX(), mob.getX(), this.interpolationAlpha);
            float y = lerp(mob.getPrevY(), mob.getY(), this.interpolationAlpha);
            this.draws.add(new EnemyBodyDraw(palette, x, y, facingRadians, scale, mob.getHealthFraction()));
        }
        return null;
    }

    public Void visitCircle(EnemyMobCircle mob) {
        return this.body(Palette.ENEMY_CIRCLE, mob, mob.getBodyScale(), 0.0);
    }

    public Void visitGhost(EnemyMobGhost mob) {
        return this.body(Palette.ENEMY_GHOST, mob, mob.getBodyScale(), 0.0);
    }

    public Void visitSquare(EnemyMobSquare mob) {
        return this.body(Palette.ENEMY_SQUARE, mob, mob.getBodyScale(), mob.getFacingRadians());
    }

    public Void visitTriangle(EnemyMobTriangle mob) {
        return this.body(Palette.ENEMY_TRIANGLE, mob, mob.getBodyScale(), mob.getFacingRadians());
    }

    public Void visitEmpty(EnemyMobEmpty mob) {
        return null;
    }
}
