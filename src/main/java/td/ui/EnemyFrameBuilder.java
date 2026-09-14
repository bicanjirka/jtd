package td.ui;

import td.effect.EffectKind;
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
import td.ui.render.StatusMarkerDraw;

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

    // How far above the body the marker row sits, and how far apart consecutive markers are,
    // both as a fraction of the mob's own body scale - so the row scales with the mob's size
    // rather than needing a fixed pixel offset that would look wrong at a different board scale.
    private static final float MARKER_ROW_OFFSET_FRACTION = 1.6f;
    private static final float MARKER_SPACING_FRACTION = 1.1f;
    private static final float MARKER_SCALE_FRACTION = 0.35f;

    private final List<EnemyDraw> draws = new ArrayList<>();
    private final List<StatusMarkerDraw> markerDraws = new ArrayList<>();
    private final int gameTime;
    private final double interpolationAlpha;

    public EnemyFrameBuilder(int gameTime, double interpolationAlpha) {
        this.gameTime = gameTime;
        this.interpolationAlpha = interpolationAlpha;
    }

    public List<EnemyDraw> build() {
        return this.draws;
    }

    public List<StatusMarkerDraw> buildMarkers() {
        return this.markerDraws;
    }

    private static float lerp(double from, double to, double alpha) {
        return (float) (from + (to - from) * alpha);
    }

    private static Palette markerPaletteFor(EffectKind kind) {
        return switch (kind) {
            case SLOW -> Palette.STATUS_MARKER_SLOW;
            case BURN -> Palette.STATUS_MARKER_BURN;
            case FREEZE -> Palette.STATUS_MARKER_FREEZE;
        };
    }

    private Void body(Palette palette, AbstractEnemyMob mob, float scale, double facingRadians) {
        if (mob.isDead()) {
            if (!mob.isFadeComplete(this.gameTime)) {
                int age = mob.ticksSinceDeath(this.gameTime);
                float fadeProgress = 1f - (mob.fadeAlpha(age) / 255f);
                this.draws.add(new EnemyFadeDraw(palette, (float) mob.getX(), (float) mob.getY(), facingRadians, scale, age, fadeProgress));
            }
        } else if (!mob.isInactive()) {
            float x = lerp(mob.getPrevX(), mob.getX(), this.interpolationAlpha);
            float y = lerp(mob.getPrevY(), mob.getY(), this.interpolationAlpha);
            this.draws.add(new EnemyBodyDraw(palette, x, y, facingRadians, scale, mob.getHealthFraction()));
            this.markers(mob, x, y, scale);
        }
        return null;
    }

    private void markers(AbstractEnemyMob mob, float x, float y, float scale) {
        float markerY = y - scale * MARKER_ROW_OFFSET_FRACTION;
        float markerX = x - scale;
        for (EffectKind kind : mob.activeEffectKinds()) {
            this.markerDraws.add(new StatusMarkerDraw(markerPaletteFor(kind), markerX, markerY, scale * MARKER_SCALE_FRACTION));
            markerX += scale * MARKER_SPACING_FRACTION;
        }
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
