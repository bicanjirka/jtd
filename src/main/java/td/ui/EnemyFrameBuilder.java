package td.ui;

import td.effect.EffectKind;
import td.enemy.AbstractEnemyMob;
import td.enemy.BodyArchetype;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyMobVisitor;
import td.enemy.Rank;
import td.enemy.SupportAura;
import td.enemy.Trait;
import td.enemy.TraitMarker;
import td.ui.render.CritSparkDraw;
import td.ui.render.EffectPulseDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.EnemyOverlayDraw;
import td.ui.render.EnemyRingDraw;
import td.ui.render.Palette;
import td.ui.render.PulseDirection;
import td.ui.render.RankBadge;
import td.ui.render.StatusMarkerDraw;
import td.ui.render.TraitMarkerDraw;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
     * The trait-marker row sits below the body, mirroring the effect row's own offset/spacing/
     * scale above it - same visual language, opposite side, so the two rows never collide and
     * both scale with the mob's own size the same way.
     */
    private static final float TRAIT_MARKER_ROW_OFFSET_FRACTION = 1.6f;
    /**
     * How long a critical hit's spark stays visible - 8 ticks is 0.4s at the normal tick rate,
     * a brief flash rather than a lingering marker.
     */
    static final int CRIT_SPARK_DURATION_TICKS = 8;
    /**
     * How long a cloak fade-in/fade-out takes, in ticks - same duration as the crit spark, for
     * the same "brief, legible transition" reasoning.
     */
    static final int CLOAK_FADE_DURATION_TICKS = 8;
    /**
     * A shield bubble is drawn a bit outside the body itself, in the shield marker's colour, at
     * a fixed, subtle alpha - it's a persistent state indicator, not a flash, so it stays faint
     * enough not to compete with the body underneath it.
     */
    private static final float SHIELD_BUBBLE_SCALE_FRACTION = 1.3f;
    private static final float SHIELD_BUBBLE_ALPHA = 0.5f;
    /**
     * A support-aura ring's radius is typically many times the body's own scale (a Ghost
     * Elite's shroud reaches 100px), so it needs a much fainter alpha than the shield bubble to
     * avoid reading as a solid, competing shape rather than a background reach indicator.
     */
    private static final float SUPPORT_AURA_RING_ALPHA = 0.12f;
    /**
     * How long a gain/loss/cast/spawn pulse takes to fully grow-or-shrink and fade - same
     * duration as the crit spark and the cloak fade, for the same brief-and-legible reasoning,
     * kept as its own constant since it means something different (a ring's lifetime, not a
     * spark's or a cloak's).
     */
    static final int EFFECT_PULSE_DURATION_TICKS = 8;
    /**
     * A gain/loss/cast pulse's ring size, as a fraction of the mob's own body scale - just
     * outside the body, reading as a ripple from it, the same relative size the shield bubble
     * uses.
     */
    private static final float GAIN_LOSS_PULSE_RADIUS_FRACTION = 1.6f;
    /**
     * An ability-driven spawn's arrival burst is drawn a bit larger than a gain/loss pulse - it
     * marks a new mob appearing, not a status change on one already there.
     */
    private static final float SPAWN_BURST_RADIUS_FRACTION = 2.2f;
    private final List<EnemyDraw> draws = new ArrayList<>();
    private final List<StatusMarkerDraw> markerDraws = new ArrayList<>();
    private final List<CritSparkDraw> critSparkDraws = new ArrayList<>();
    private final List<EnemyOverlayDraw> overlayDraws = new ArrayList<>();
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

    private static Palette traitMarkerPaletteFor(TraitMarker marker) {
        return switch (marker) {
            case PERCENT_RESIST -> Palette.TRAIT_MARKER_PERCENT_RESIST;
            case FLAT_RESIST -> Palette.TRAIT_MARKER_FLAT_RESIST;
            case CRITICAL_IMMUNE -> Palette.TRAIT_MARKER_CRITICAL_IMMUNE;
            case HURT_SPEED -> Palette.TRAIT_MARKER_HURT_SPEED;
            case BURN_IMMUNE -> Palette.TRAIT_MARKER_BURN_IMMUNE;
            case FREEZE_IMMUNE -> Palette.TRAIT_MARKER_FREEZE_IMMUNE;
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
            case MENDER -> Palette.ENEMY_MENDER;
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

    public List<EnemyOverlayDraw> buildOverlays() {
        return this.overlayDraws;
    }

    private Void body(Palette palette, AbstractEnemyMob mob, float scale, double facingRadians,
            Optional<SupportAura> supportAura, List<Trait> traits) {
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
                    badgeFor(mob.getRank()), this.cloakProgress(mob)));
            this.markers(mob, x, y, scale);
            this.critSpark(mob, x, y, scale);
            this.overlays(mob, x, y, scale, supportAura);
            this.pulses(mob, x, y, scale);
            this.traitMarkers(x, y, scale, traits);
        }
        return null;
    }

    /**
     * A row of hollow diamonds below the body, one per always-on {@link Trait} this mob carries -
     * see {@link Trait#marker()}. Capped at {@link #MAX_VISIBLE_MARKERS}, the same overflow
     * discipline the timed effect row above the body already uses.
     */
    private void traitMarkers(float x, float y, float scale, List<Trait> traits) {
        float markerY = y + scale * TRAIT_MARKER_ROW_OFFSET_FRACTION;
        float markerX = x - scale;
        int shown = 0;
        for (Trait trait : traits) {
            if (shown == MAX_VISIBLE_MARKERS) {
                this.overlayDraws.add(new TraitMarkerDraw(Palette.TRAIT_MARKER_OVERFLOW, markerX, markerY, scale * MARKER_SCALE_FRACTION));
                return;
            }
            this.overlayDraws.add(new TraitMarkerDraw(traitMarkerPaletteFor(trait.marker()), markerX, markerY, scale * MARKER_SCALE_FRACTION));
            markerX += scale * MARKER_SPACING_FRACTION;
            shown++;
        }
    }

    /**
     * A shield bubble while {@link EffectKind#SHIELD} is active, and a support-aura ring for a
     * definition that projects an effect onto nearby allies at a radius - both static rings, not
     * timed ones, since each reflects an ongoing state (an active shield, an authored ability)
     * rather than a one-shot event.
     */
    private void overlays(AbstractEnemyMob mob, float x, float y, float scale, Optional<SupportAura> supportAura) {
        if (mob.activeEffectKinds().contains(EffectKind.SHIELD)) {
            this.overlayDraws.add(new EnemyRingDraw(Palette.STATUS_MARKER_SHIELD, x, y, scale * SHIELD_BUBBLE_SCALE_FRACTION, SHIELD_BUBBLE_ALPHA));
        }
        supportAura.ifPresent(aura -> this.overlayDraws.add(
                new EnemyRingDraw(markerPaletteFor(aura.kind()), x, y, aura.radius(), SUPPORT_AURA_RING_ALPHA)));
    }

    /**
     * A ring wherever this mob just changed state: gaining or losing an effect kind, casting an
     * ability-applied effect, or arriving via an ability-driven spawn.
     */
    private void pulses(AbstractEnemyMob mob, float x, float y, float scale) {
        for (EffectKind kind : EffectKind.values()) {
            if (kind == EffectKind.INVISIBLE) {
                // The cloak fade on the body itself is this transition - see cloakProgress.
                continue;
            }
            this.addTransitionPulse(x, y, scale, kind, mob.ticksSinceEffectGained(kind, this.gameTime), PulseDirection.OUTWARD);
            this.addTransitionPulse(x, y, scale, kind, mob.ticksSinceEffectLost(kind, this.gameTime), PulseDirection.INWARD);
        }
        mob.lastAbilityCast().ifPresent(cast -> {
            int ticksSince = this.gameTime - cast.tick();
            if (ticksSince >= 0 && ticksSince <= EFFECT_PULSE_DURATION_TICKS) {
                float progress = (float) ticksSince / EFFECT_PULSE_DURATION_TICKS;
                // A SelfTarget cast carries radius 0 - fall back to the same ripple size a
                // gain/loss pulse uses, since a literal zero-radius ring would be invisible.
                float ringRadius = cast.radius() > 0 ? cast.radius() : scale * GAIN_LOSS_PULSE_RADIUS_FRACTION;
                this.overlayDraws.add(new EffectPulseDraw(markerPaletteFor(cast.kind()), x, y, ringRadius, progress, PulseDirection.OUTWARD));
            }
        });
        int ticksSinceSpawn = mob.ticksSinceAbilitySpawn(this.gameTime);
        if (ticksSinceSpawn >= 0 && ticksSinceSpawn <= EFFECT_PULSE_DURATION_TICKS) {
            float progress = (float) ticksSinceSpawn / EFFECT_PULSE_DURATION_TICKS;
            this.overlayDraws.add(new EffectPulseDraw(Palette.SPAWN_BURST, x, y, scale * SPAWN_BURST_RADIUS_FRACTION, progress, PulseDirection.OUTWARD));
        }
    }

    private void addTransitionPulse(float x, float y, float scale, EffectKind kind, int ticksSince, PulseDirection direction) {
        if (ticksSince < 0 || ticksSince > EFFECT_PULSE_DURATION_TICKS) {
            return;
        }
        float progress = (float) ticksSince / EFFECT_PULSE_DURATION_TICKS;
        this.overlayDraws.add(new EffectPulseDraw(markerPaletteFor(kind), x, y, scale * GAIN_LOSS_PULSE_RADIUS_FRACTION, progress, direction));
    }

    private void critSpark(AbstractEnemyMob mob, float x, float y, float scale) {
        int ticksSince = mob.ticksSinceCriticalHit(this.gameTime);
        if (ticksSince >= 0 && ticksSince <= CRIT_SPARK_DURATION_TICKS) {
            float fadeProgress = (float) ticksSince / CRIT_SPARK_DURATION_TICKS;
            this.critSparkDraws.add(new CritSparkDraw(Palette.CRIT_SPARK, x, y, scale, fadeProgress));
        }
    }

    /**
     * 0 (fully solid) to 1 (fully cloaked) - ramps up over {@link #CLOAK_FADE_DURATION_TICKS}
     * after {@link EffectKind#INVISIBLE} is gained, holds at 1 while it stays active, and ramps
     * back down over the same window after it's lost. Derived the same way
     * {@code EnemyFadeDraw.fadeProgress} is derived from ticks since death - see
     * {@code AbstractEnemyMob.ticksSinceEffectGained}/{@code ticksSinceEffectLost}.
     * <p>
     * {@code ticksSinceGained} is clamped at 0 rather than used raw: an ability that applies
     * {@code INVISIBLE} (e.g. the Ghost's vanish) does so via {@code DefinedEnemyMob
     * .evaluateAbilities}, which runs <em>after</em> {@code AbstractEnemyMob.doTick} has already
     * called {@code EffectTransitions.observe} for this tick - so on the exact tick a mob first
     * becomes invisible, {@code activeEffectKinds()} already reports it but the transition isn't
     * observed until next tick, and {@code ticksSinceEffectGained} still returns {@code -1}.
     * Unclamped, that produces a negative progress and, since visibility is {@code 1 -
     * progress}, an out-of-range alpha - reproduced by actually running the game (a real ability
     * firing crashed the renderer; no unit test caught it because none built a frame on the same
     * tick an ability fires). Clamping to 0 is also the correct reading: "not yet observed" while
     * already active means it just started.
     */
    private float cloakProgress(AbstractEnemyMob mob) {
        if (mob.activeEffectKinds().contains(EffectKind.INVISIBLE)) {
            int ticksSinceGained = Math.max(0, mob.ticksSinceEffectGained(EffectKind.INVISIBLE, this.gameTime));
            return Math.min(1f, (float) ticksSinceGained / CLOAK_FADE_DURATION_TICKS);
        }
        int ticksSinceLost = mob.ticksSinceEffectLost(EffectKind.INVISIBLE, this.gameTime);
        if (ticksSinceLost < 0 || ticksSinceLost >= CLOAK_FADE_DURATION_TICKS) {
            return 0f;
        }
        return 1f - (float) ticksSinceLost / CLOAK_FADE_DURATION_TICKS;
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
        return this.body(paletteFor(mob.archetype()), mob, mob.getBodyScale(), mob.getFacingRadians(), mob.supportAura(), mob.traits());
    }
}
