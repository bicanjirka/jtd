package td.ui;

import td.effect.EffectKind;
import td.enemy.AbstractEnemyMob;
import td.enemy.BodyArchetype;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyMobVisitor;
import td.enemy.Rank;
import td.ui.render.CritSparkDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.Palette;
import td.ui.render.RankBadge;
import td.ui.render.StatusMarkerDraw;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes each enemy's body and death-fade animation as {@link EnemyDraw}
 * commands - {@link #visitDefined} switches on {@link BodyArchetype}, not on any Java type,
 * since body shape/palette come from a {@code DefinedEnemyMob}'s {@link td.enemy.EnemyDefinition}
 * rather than from which concrete class it is. An alive body's position is interpolated between
 * the mob's previous and current tick position (see {@code interpolationAlpha});
 * a fading (dead) mob is frozen at its death position and drawn as-is, since it
 * has stopped moving - interpolating it against alpha would make it slide back
 * and forth every frame between two positions that never change again.
 */
public final class EnemyFrameBuilder implements EnemyMobVisitor<Void> {

    /**
     * How many real effect markers show before the rest collapse into one overflow marker.
     */
    static final int MAX_VISIBLE_MARKERS = 3;
    // How far above the body the marker row sits, and how far apart consecutive markers are,
    // both as a fraction of the mob's own body scale - so the row scales with the mob's size
    // rather than needing a fixed pixel offset that would look wrong at a different board scale.
    private static final float MARKER_ROW_OFFSET_FRACTION = 1.6f;
    private static final float MARKER_SPACING_FRACTION = 1.1f;
    private static final float MARKER_SCALE_FRACTION = 0.35f;
    /**
     * How long a critical hit's spark stays visible - 8 ticks is 0.4s at the normal tick rate,
     * a brief flash rather than a lingering marker.
     */
    static final int CRIT_SPARK_DURATION_TICKS = 8;
    private final List<EnemyDraw> draws = new ArrayList<>();
    private final List<StatusMarkerDraw> markerDraws = new ArrayList<>();
    private final List<CritSparkDraw> critSparkDraws = new ArrayList<>();
    private final int gameTime;
    private final double interpolationAlpha;

    public EnemyFrameBuilder(int gameTime, double interpolationAlpha) {
        this.gameTime = gameTime;
        this.interpolationAlpha = interpolationAlpha;
    }

    private static float lerp(double from, double to, double alpha) {
        return (float) (from + (to - from) * alpha);
    }

    private static Palette markerPaletteFor(EffectKind kind) {
        return switch (kind) {
            case SLOW -> Palette.STATUS_MARKER_SLOW;
            case BURN -> Palette.STATUS_MARKER_BURN;
            case FREEZE -> Palette.STATUS_MARKER_FREEZE;
            case SHIELD -> Palette.STATUS_MARKER_SHIELD;
            case INVISIBLE -> Palette.STATUS_MARKER_INVISIBLE;
            case HEAL -> Palette.STATUS_MARKER_HEAL;
        };
    }

    private static Palette paletteFor(BodyArchetype archetype) {
        return switch (archetype) {
            case CIRCLE -> Palette.ENEMY_CIRCLE;
            case GHOST -> Palette.ENEMY_GHOST;
            case SQUARE -> Palette.ENEMY_SQUARE;
            case TRIANGLE -> Palette.ENEMY_TRIANGLE;
            case WARDEN -> Palette.ENEMY_WARDEN;
            case WARDEN_EGG -> Palette.ENEMY_WARDEN_EGG;
        };
    }

    private static RankBadge badgeFor(Rank rank) {
        return switch (rank) {
            case GRUNT -> RankBadge.NONE;
            case SOLDIER -> RankBadge.ONE_CHEVRON;
            case VETERAN -> RankBadge.TWO_CHEVRON;
            case ELITE -> RankBadge.STAR;
            case BOSS -> RankBadge.SKULL;
        };
    }

    public List<EnemyDraw> build() {
        return this.draws;
    }

    public List<StatusMarkerDraw> buildMarkers() {
        return this.markerDraws;
    }

    public List<CritSparkDraw> buildCritSparks() {
        return this.critSparkDraws;
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
            this.draws.add(new EnemyBodyDraw(palette, x, y, facingRadians, scale, mob.getHealthFraction(),
                    badgeFor(mob.getRank())));
            this.markers(mob, x, y, scale);
            this.critSpark(mob, x, y, scale);
        }
        return null;
    }

    private void critSpark(AbstractEnemyMob mob, float x, float y, float scale) {
        int ticksSince = mob.ticksSinceCriticalHit(this.gameTime);
        if (ticksSince >= 0 && ticksSince <= CRIT_SPARK_DURATION_TICKS) {
            float fadeProgress = (float) ticksSince / CRIT_SPARK_DURATION_TICKS;
            this.critSparkDraws.add(new CritSparkDraw(Palette.CRIT_SPARK, x, y, scale, fadeProgress));
        }
    }

    private void markers(AbstractEnemyMob mob, float x, float y, float scale) {
        float markerY = y - scale * MARKER_ROW_OFFSET_FRACTION;
        float markerX = x - scale;
        int shown = 0;
        for (EffectKind kind : mob.activeEffectKinds()) {
            if (shown == MAX_VISIBLE_MARKERS) {
                // A 4th+ simultaneous effect collapses into one overflow marker rather than
                // growing the row further - legible even on a heavily-buffed enemy in a packed
                // wave (see docs/features/FEATURE-enemy-traits-and-effects.md's V1 Scope).
                this.markerDraws.add(new StatusMarkerDraw(Palette.STATUS_MARKER_OVERFLOW, markerX, markerY, scale * MARKER_SCALE_FRACTION));
                return;
            }
            this.markerDraws.add(new StatusMarkerDraw(markerPaletteFor(kind), markerX, markerY, scale * MARKER_SCALE_FRACTION));
            markerX += scale * MARKER_SPACING_FRACTION;
            shown++;
        }
    }

    public Void visitDefined(DefinedEnemyMob mob) {
        return this.body(paletteFor(mob.archetype()), mob, mob.getBodyScale(), mob.getFacingRadians());
    }
}
