package td.ui;

import td.effect.EffectCategory;
import td.effect.EffectKind;
import td.enemy.BodyArchetype;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyMob;
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
import td.ui.render.HexGlyph;
import td.ui.render.HexRuneDraw;
import td.ui.render.IceCrystalDraw;
import td.ui.render.Palette;
import td.ui.render.PulseDirection;
import td.ui.render.RankBadge;
import td.ui.render.StatusMarkerDraw;
import td.ui.render.TraitMarkerDraw;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Describes each enemy's body, markers and effects as draw commands, switching on its
 * {@link BodyArchetype}. A live body is interpolated between ticks; a dying one is drawn where it
 * died, since it no longer moves.
 */
public final class EnemyFrameBuilder implements EnemyMobVisitor<Void> {

    /** Markers shown before the rest collapse into one overflow marker. */
    static final int MAX_VISIBLE_MARKERS = 3;
    // Fractions of body scale, so the row clears a large body; the glyph size is fixed.
    private static final float MARKER_ROW_OFFSET_FRACTION = 1.6f;
    private static final float MARKER_SPACING_FRACTION = 1.1f;
    /**
     * Pixels from a marker that carries a count to the next one: its count is drawn to its right in
     * a fixed-size font, so the gap can't shrink with the enemy.
     */
    static final float COUNTED_MARKER_SPACING = 24f;
    /** Marker glyph size, fixed so a marker reads the same on a small or a huge body. */
    private static final float MARKER_FIXED_SCALE = 4.5f;
    /** The trait row sits below the body, mirroring the effect row above it. */
    private static final float TRAIT_MARKER_ROW_OFFSET_FRACTION = 1.6f;
    static final int CRIT_SPARK_DURATION_TICKS = 8;
    /** Fixed, so a crit looks the same on every enemy. */
    private static final float CRIT_SPARK_FIXED_SCALE = 12f;
    static final int CLOAK_FADE_DURATION_TICKS = 8;
    /**
     * Shield bubble size relative to the body, drawn at a faint fixed alpha since it marks a
     * lasting state.
     */
    private static final float SHIELD_BUBBLE_SCALE_FRACTION = 1.3f;
    private static final float SHIELD_BUBBLE_ALPHA = 0.5f;
    /** Ice overlay size relative to the body. */
    private static final float FREEZE_CRYSTAL_SCALE_FRACTION = 1.35f;
    /**
     * Much fainter than the shield bubble, since an aura ring can be many times the body's size.
     */
    private static final float SUPPORT_AURA_RING_ALPHA = 0.12f;
    private static final float SELECTION_RING_SCALE_FRACTION = 1.6f;
    private static final float SELECTION_RING_ALPHA = 0.9f;
    // Stronger than a support aura: it threatens the player's towers rather than helping allies.
    private static final float DISRUPTION_RING_ALPHA = 0.35f;
    static final int EFFECT_PULSE_DURATION_TICKS = 8;
    /** Runes sit in a row over the body, below the markers; their size is fixed, like a marker's. */
    private static final float RUNE_ROW_OFFSET_FRACTION = 0.6f;
    private static final float RUNE_FIXED_SCALE = 4f;
    private static final float RUNE_SPACING = 11f;
    /** Pulse ring size relative to the body. */
    private static final float GAIN_LOSS_PULSE_RADIUS_FRACTION = 1.6f;
    /** Larger than a status pulse: it marks a new mob, not a change on an existing one. */
    private static final float SPAWN_BURST_RADIUS_FRACTION = 2.2f;
    private final List<EnemyDraw> draws = new ArrayList<>();
    private final List<StatusMarkerDraw> markerDraws = new ArrayList<>();
    private final List<CritSparkDraw> critSparkDraws = new ArrayList<>();
    private final List<EnemyOverlayDraw> overlayDraws = new ArrayList<>();
    private final int gameTime;
    private final double interpolationAlpha;
    private final Optional<EnemyMob> selected;

    public EnemyFrameBuilder(int gameTime, double interpolationAlpha) {
        this(gameTime, interpolationAlpha, Optional.empty());
    }

    /** Also rings {@code selected}, the enemy the player is inspecting. */
    public EnemyFrameBuilder(int gameTime, double interpolationAlpha, Optional<EnemyMob> selected) {
        this.gameTime = gameTime;
        this.interpolationAlpha = interpolationAlpha;
        this.selected = selected;
    }

    private static float lerp(double from, double to, double alpha) {
        return (float) (from + (to - from) * alpha);
    }

    static Palette markerPaletteFor(EffectKind kind) {
        return switch (kind) {
            case CHILL -> Palette.STATUS_MARKER_CHILL;
            case BURN -> Palette.STATUS_MARKER_BURN;
            case FREEZE -> Palette.STATUS_MARKER_FREEZE;
            case SHIELD -> Palette.STATUS_MARKER_SHIELD;
            case INVISIBLE -> Palette.STATUS_MARKER_INVISIBLE;
            case HEAL -> Palette.STATUS_MARKER_HEAL;
            case VULNERABLE -> Palette.STATUS_MARKER_VULNERABLE;
            case REVEALED -> Palette.STATUS_MARKER_REVEALED;
            case POISON -> Palette.STATUS_MARKER_POISON;
            case SCORCHED -> Palette.STATUS_MARKER_SCORCHED;
            case SICKENED -> Palette.STATUS_MARKER_SICKENED;
            case SUNDERED -> Palette.STATUS_MARKER_SUNDERED;
            case EXPOSED -> Palette.STATUS_MARKER_EXPOSED;
            case MARKED -> Palette.STATUS_MARKER_MARKED;
            case PRIORITY -> Palette.STATUS_MARKER_PRIORITY;
            case RESONATING -> Palette.STATUS_MARKER_RESONATING;
            case FRACTURED -> Palette.STATUS_MARKER_FRACTURED;
            case DAZED -> Palette.STATUS_MARKER_DAZED;
            case SATURATED -> Palette.STATUS_MARKER_SATURATED;
            case CHARGED -> Palette.STATUS_MARKER_CHARGED;
            case DOOM -> Palette.STATUS_MARKER_DOOM;
            case BLIGHT -> Palette.STATUS_MARKER_BLIGHT;
            case CONTAGION -> Palette.STATUS_MARKER_CONTAGION;
            case RIME -> Palette.STATUS_MARKER_RIME;
            case ASH -> Palette.STATUS_MARKER_ASH;
            case INVERSION -> Palette.STATUS_MARKER_INVERSION;
            case SYMPATHY -> Palette.STATUS_MARKER_SYMPATHY;
            case RECKONING -> Palette.STATUS_MARKER_RECKONING;
            case SILENCED -> Palette.STATUS_MARKER_SILENCED;
            case ANCHORED -> Palette.STATUS_MARKER_ANCHORED;
            case UNRAVELED -> Palette.STATUS_MARKER_UNRAVELED;
            case BRITTLE -> Palette.STATUS_MARKER_BRITTLE;
            case TOLL -> Palette.STATUS_MARKER_TOLL;
            case CORRODED -> Palette.STATUS_MARKER_CORRODED;
            case UNDERTOW -> Palette.STATUS_MARKER_UNDERTOW;
            case DEAD_ZONE -> Palette.STATUS_MARKER_DEAD_ZONE;
            case KILL_ZONE -> Palette.STATUS_MARKER_KILL_ZONE;
            case CRACKED -> Palette.STATUS_MARKER_CRACKED;
            case TARRED -> Palette.STATUS_MARKER_TARRED;
        };
    }

    static Palette traitMarkerPaletteFor(TraitMarker marker) {
        return switch (marker) {
            case PERCENT_RESIST -> Palette.TRAIT_MARKER_PERCENT_RESIST;
            case PHYSICAL_RESIST -> Palette.TRAIT_MARKER_PHYSICAL_RESIST;
            case MAGIC_RESIST -> Palette.TRAIT_MARKER_MAGIC_RESIST;
            case FLAT_RESIST -> Palette.TRAIT_MARKER_FLAT_RESIST;
            case CRITICAL_IMMUNE -> Palette.TRAIT_MARKER_CRITICAL_IMMUNE;
            case HURT_SPEED -> Palette.TRAIT_MARKER_HURT_SPEED;
            case BURN_IMMUNE -> Palette.TRAIT_MARKER_BURN_IMMUNE;
            case FREEZE_IMMUNE -> Palette.TRAIT_MARKER_FREEZE_IMMUNE;
            case EFFECT_RESIST -> Palette.TRAIT_MARKER_EFFECT_RESIST;
        };
    }

    static Palette paletteFor(BodyArchetype archetype) {
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

    static RankBadge badgeFor(Rank rank) {
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

    private Void body(Palette palette, DefinedEnemyMob mob, float scale, double facingRadians,
            Optional<SupportAura> supportAura, List<Trait> traits) {
        if (mob.isDead()) {
            int age = mob.ticksSinceDeath(this.gameTime);
            if (age <= fadeDurationTicks(mob.getRank())) {
                float fadeProgress = fadeProgress(mob.getRank(), age);
                this.draws.add(new EnemyFadeDraw(palette, (float) mob.getX(), (float) mob.getY(), facingRadians, scale, age, fadeProgress));
            }
        } else if (!mob.isInactive()) {
            float x = lerp(mob.getPrevX(), mob.getX(), this.interpolationAlpha);
            float y = lerp(mob.getPrevY(), mob.getY(), this.interpolationAlpha);
            this.draws.add(new EnemyBodyDraw(palette, x, y, facingRadians, scale, mob.getHealthFraction(),
                    badgeFor(mob.getRank()), this.cloakProgress(mob)));
            this.markers(mob, x, y, scale);
            this.critSpark(mob, x, y);
            this.overlays(mob, x, y, scale, supportAura);
            this.pulses(mob, x, y, scale);
            this.traitMarkers(x, y, scale, traits);
        }
        return null;
    }

    /** One hollow diamond per trait below the body, capped like the effect row. */
    private void traitMarkers(float x, float y, float scale, List<Trait> traits) {
        float markerY = y + scale * TRAIT_MARKER_ROW_OFFSET_FRACTION;
        float markerX = x - scale;
        int shown = 0;
        for (Trait trait : traits) {
            if (shown == MAX_VISIBLE_MARKERS) {
                this.overlayDraws.add(new TraitMarkerDraw(Palette.TRAIT_MARKER_OVERFLOW, markerX, markerY, MARKER_FIXED_SCALE));
                return;
            }
            this.overlayDraws.add(new TraitMarkerDraw(traitMarkerPaletteFor(trait.marker()), markerX, markerY, MARKER_FIXED_SCALE));
            markerX += scale * MARKER_SPACING_FRACTION;
            shown++;
        }
    }

    /** Static overlays for ongoing states: shield bubble, ice crystals, support-aura and disruption rings. */
    private void overlays(DefinedEnemyMob mob, float x, float y, float scale, Optional<SupportAura> supportAura) {
        if (mob.activeEffectKinds().contains(EffectKind.SHIELD)) {
            this.overlayDraws.add(new EnemyRingDraw(Palette.STATUS_MARKER_SHIELD, x, y, scale * SHIELD_BUBBLE_SCALE_FRACTION, SHIELD_BUBBLE_ALPHA));
        }
        if (mob.activeEffectKinds().contains(EffectKind.FREEZE)) {
            this.overlayDraws.add(new IceCrystalDraw(Palette.FREEZE_CRYSTAL, x, y, scale * FREEZE_CRYSTAL_SCALE_FRACTION));
        }
        if (this.selected.isPresent() && this.selected.get() == mob) {
            this.overlayDraws.add(new EnemyRingDraw(Palette.SELECTION, x, y, scale * SELECTION_RING_SCALE_FRACTION,
                    SELECTION_RING_ALPHA));
        }
        this.runes(mob, x, y - scale * RUNE_ROW_OFFSET_FRACTION);
        supportAura.ifPresent(aura -> this.overlayDraws.add(
                new EnemyRingDraw(markerPaletteFor(aura.kind()), x, y, aura.radius(), SUPPORT_AURA_RING_ALPHA)));
        mob.definition().disruption().ifPresent(aura -> this.overlayDraws.add(
                new EnemyRingDraw(Palette.DISRUPTION, x, y, aura.radius(), DISRUPTION_RING_ALPHA)));
    }

    /**
     * A ring wherever the mob just gained or lost an effect, cast one, or arrived by an ability
     * spawn.
     */
    private void pulses(DefinedEnemyMob mob, float x, float y, float scale) {
        for (EffectKind kind : EffectKind.values()) {
            if (kind == EffectKind.INVISIBLE) {
                continue;
            }
            this.addTransitionPulse(x, y, scale, kind, mob.ticksSinceEffectGained(kind, this.gameTime), PulseDirection.OUTWARD);
            this.addTransitionPulse(x, y, scale, kind, mob.ticksSinceEffectLost(kind, this.gameTime), PulseDirection.INWARD);
        }
        mob.lastAbilityCast().ifPresent(cast -> {
            int ticksSince = this.gameTime - cast.tick();
            if (ticksSince >= 0 && ticksSince <= EFFECT_PULSE_DURATION_TICKS) {
                float progress = (float) ticksSince / EFFECT_PULSE_DURATION_TICKS;
                // A zero radius would draw an invisible ring.
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

    private void critSpark(DefinedEnemyMob mob, float x, float y) {
        int ticksSince = mob.ticksSinceCriticalHit(this.gameTime);
        if (ticksSince >= 0 && ticksSince <= CRIT_SPARK_DURATION_TICKS) {
            float fadeProgress = (float) ticksSince / CRIT_SPARK_DURATION_TICKS;
            this.critSparkDraws.add(new CritSparkDraw(Palette.CRIT_SPARK, x, y, CRIT_SPARK_FIXED_SCALE, fadeProgress));
        }
    }

    /**
     * 0 (solid) to 1 (cloaked): ramps up after invisibility is gained, holds, and ramps down after
     * it is lost.
     * <p>
     * Clamped at 0 because an ability can apply invisibility after this tick's transitions were
     * observed, leaving the gain unrecorded for one tick; unclamped, that produced an invalid
     * alpha.
     */
    private float cloakProgress(DefinedEnemyMob mob) {
        if (mob.activeEffectKinds().contains(EffectKind.REVEALED)) {
            return 0f;
        }
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

    private void markers(DefinedEnemyMob mob, float x, float y, float scale) {
        float markerY = y - scale * MARKER_ROW_OFFSET_FRACTION;
        float markerX = x - scale;
        int shown = 0;
        List<MarkerGroup> groups = markerGroups(mob.activeEffectKinds());
        for (MarkerGroup group : groups) {
            if (shown == MAX_VISIBLE_MARKERS) {
                this.markerDraws.add(new StatusMarkerDraw(Palette.STATUS_MARKER_OVERFLOW, markerX, markerY,
                        MARKER_FIXED_SCALE, groups.size() - MAX_VISIBLE_MARKERS));
                return;
            }
            this.markerDraws.add(new StatusMarkerDraw(markerPaletteFor(group.first()), markerX, markerY,
                    MARKER_FIXED_SCALE, group.others()));
            float spacing = scale * MARKER_SPACING_FRACTION;
            markerX += group.others() > 0 ? Math.max(spacing, COUNTED_MARKER_SPACING) : spacing;
            shown++;
        }
    }

    /** A rune for each hex on {@code mob}, in a row centred on {@code (x, y)}. */
    private void runes(DefinedEnemyMob mob, float x, float y) {
        List<HexGlyph> glyphs = mob.activeEffectKinds().stream()
                .map(EnemyFrameBuilder::runeFor)
                .flatMap(Optional::stream)
                .toList();
        float left = x - RUNE_SPACING * (glyphs.size() - 1) / 2f;
        for (int i = 0; i < glyphs.size(); i++) {
            this.overlayDraws.add(new HexRuneDraw(glyphs.get(i), left + RUNE_SPACING * i, y, RUNE_FIXED_SCALE));
        }
    }

    /** The rune a hex is drawn as; empty for every other kind. */
    static Optional<HexGlyph> runeFor(EffectKind kind) {
        return switch (kind) {
            case DOOM -> Optional.of(HexGlyph.DOOM);
            case BLIGHT -> Optional.of(HexGlyph.BLIGHT);
            case CONTAGION -> Optional.of(HexGlyph.CONTAGION);
            case RIME -> Optional.of(HexGlyph.RIME);
            case ASH -> Optional.of(HexGlyph.ASH);
            case INVERSION -> Optional.of(HexGlyph.INVERSION);
            case SYMPATHY -> Optional.of(HexGlyph.SYMPATHY);
            case RECKONING -> Optional.of(HexGlyph.RECKONING);
            default -> Optional.empty();
        };
    }

    /** What one marker stands for: the first kind of a category, and how many more of it are active. */
    record MarkerGroup(EffectKind first, int others) {
    }

    /**
     * One marker per effect while they fit, else one per category, counting the other kinds of that
     * category on the enemy.
     */
    static List<MarkerGroup> markerGroups(Set<EffectKind> active) {
        List<MarkerGroup> groups = new ArrayList<>();
        if (active.size() <= MAX_VISIBLE_MARKERS) {
            active.forEach(kind -> groups.add(new MarkerGroup(kind, 0)));
            return groups;
        }
        Map<EffectCategory, List<EffectKind>> byCategory = new EnumMap<>(EffectCategory.class);
        for (EffectKind kind : active) {
            byCategory.computeIfAbsent(kind.category(), category -> new ArrayList<>()).add(kind);
        }
        byCategory.values().forEach(kinds -> groups.add(new MarkerGroup(kinds.getFirst(), kinds.size() - 1)));
        return groups;
    }

    static int fadeDurationTicks(Rank rank) {
        return 3 * rank.ordinal() + 6;
    }

    /**
     * {@code 0} at death to {@code 1} when gone. Clamped, because {@code ticksSinceDeath} is
     * {@code -1} between a mid-tick death and the next {@code doTick}.
     */
    static float fadeProgress(Rank rank, int ticksSinceDeath) {
        int alpha = 255 - (ticksSinceDeath * (255 / (fadeDurationTicks(rank) + 1)));
        return 1f - (Math.min(255, Math.max(alpha, 0)) / 255f);
    }

    public Void visitDefined(DefinedEnemyMob mob) {
        return this.body(paletteFor(mob.archetype()), mob, mob.getBodyScale(), mob.getFacingRadians(), mob.supportAura(), mob.traits());
    }
}
