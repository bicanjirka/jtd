package td.ui;

import td.effect.EffectKind;
import td.enemy.BodyArchetype;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyMob;
import td.enemy.EnemyMobVisitor;
import td.enemy.Rank;
import td.enemy.SupportAura;
import td.ui.render.CritSparkDraw;
import td.ui.render.EffectPulseDraw;
import td.ui.render.EnemyBarLook;
import td.ui.render.EnemyBarsDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.EnemyLook;
import td.ui.render.EnemyOverlayDraw;
import td.ui.render.EnemyRingDraw;
import td.ui.render.HexGlyph;
import td.ui.render.HexRuneDraw;
import td.ui.render.IceCrystalDraw;
import td.ui.render.MissMarkDraw;
import td.ui.render.Palette;
import td.ui.render.RankBadge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Describes each enemy's body and effects as draw commands, switching on its
 * {@link BodyArchetype}. A live body is interpolated between ticks; a dying one is drawn where it
 * died, since it no longer moves.
 */
public final class EnemyFrameBuilder implements EnemyMobVisitor<Void> {

    static final int CRIT_SPARK_DURATION_TICKS = 8;
    /** Fixed, so a crit looks the same on every enemy. */
    private static final float CRIT_SPARK_FIXED_SCALE = 12f;
    static final int MISS_MARK_DURATION_TICKS = 12;
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
    /** Runes sit in a row over the body; their size is fixed. */
    private static final float RUNE_ROW_OFFSET_FRACTION = 0.6f;
    private static final float RUNE_FIXED_SCALE = 4f;
    private static final float RUNE_SPACING = 11f;
    /** How far a cast with no reach of its own rings out, relative to the body. */
    private static final float CAST_PULSE_RADIUS_FRACTION = 1.6f;
    /** Larger than a cast's: it marks a new mob, not a change on an existing one. */
    private static final float SPAWN_BURST_RADIUS_FRACTION = 2.2f;
    /** A bar's width against the body's scale, and the least it may be: a bar under a tiny body must still read. */
    private static final float BAR_WIDTH_SCALE_FRACTION = 1.8f;
    private static final float BAR_MIN_WIDTH = 8.2f;
    /** Under the furthest the body's art reaches, with room left for a pulsing body. */
    private static final float BAR_GAP = 3f;
    private final List<EnemyDraw> draws = new ArrayList<>();
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
            case DRAGGED -> Palette.STATUS_MARKER_DRAGGED;
            case UNDERTOW -> Palette.STATUS_MARKER_UNDERTOW;
            case CONDEMNED -> Palette.STATUS_MARKER_CONDEMNED;
            case STILLED -> Palette.STATUS_MARKER_STILLED;
            case UNVEILED -> Palette.STATUS_MARKER_UNVEILED;
            case DEAD_ZONE -> Palette.STATUS_MARKER_DEAD_ZONE;
            case FALLOUT -> Palette.STATUS_MARKER_FALLOUT;
            case FRAIL -> Palette.STATUS_MARKER_FRAIL;
            case CRACKED -> Palette.STATUS_MARKER_CRACKED;
            case TARRED -> Palette.STATUS_MARKER_TARRED;
            case BLEEDING -> Palette.STATUS_MARKER_BLEEDING;
            case SOULFIRE -> Palette.STATUS_MARKER_SOULFIRE;
            case SPRINTING, HASTENED -> Palette.STATUS_MARKER_SPRINTING;
            case SPELLPROOF -> Palette.STATUS_MARKER_SPELLPROOF;
            case UNYIELDING -> Palette.STATUS_MARKER_UNYIELDING;
        };
    }

    static EnemyLook lookFor(BodyArchetype archetype) {
        return switch (archetype) {
            case CIRCLE -> EnemyLook.CIRCLE;
            case GHOST -> EnemyLook.GHOST;
            case SQUARE -> EnemyLook.SQUARE;
            case TRIANGLE -> EnemyLook.TRIANGLE;
            case WARDEN -> EnemyLook.WARDEN;
            case WARDEN_EGG -> EnemyLook.WARDEN_EGG;
            case MENDER -> EnemyLook.MENDER;
            case JUGGERNAUT -> EnemyLook.JUGGERNAUT;
            case MITE -> EnemyLook.MITE;
            case COURIER -> EnemyLook.COURIER;
            case SKIRMISHER -> EnemyLook.SKIRMISHER;
            case SHIELDBEARER -> EnemyLook.SHIELDBEARER;
            case SALAMANDER -> EnemyLook.SALAMANDER;
            case YETI -> EnemyLook.YETI;
            case MAGMA -> EnemyLook.MAGMA;
            case GOLEM -> EnemyLook.GOLEM;
            case JAMMER -> EnemyLook.JAMMER;
            case REAVER -> EnemyLook.REAVER;
            case COLOSSUS -> EnemyLook.COLOSSUS;
            case MOLTEN_CORE -> EnemyLook.MOLTEN_CORE;
            case EMBER -> EnemyLook.EMBER;
            case MONOLITH -> EnemyLook.MONOLITH;
            case ECLIPSE -> EnemyLook.ECLIPSE;
            case MIRAGE -> EnemyLook.MIRAGE;
            case MIRROR_IMAGE -> EnemyLook.MIRROR_IMAGE;
            case HERALD -> EnemyLook.HERALD;
            case TALOS -> EnemyLook.TALOS;
            case MINOTAUR -> EnemyLook.MINOTAUR;
            case BRONZE_BULL -> EnemyLook.BRONZE_BULL;
            case GLASS_SENTINEL -> EnemyLook.GLASS_SENTINEL;
            case SANDLING -> EnemyLook.SANDLING;
            case TIMEKEEPER -> EnemyLook.TIMEKEEPER;
            case KEEPER -> EnemyLook.KEEPER;
            case GALE -> EnemyLook.GALE;
            case RAINDROP -> EnemyLook.RAINDROP;
            case RAINCLOUD -> EnemyLook.RAINCLOUD;
            case LIGHTNING -> EnemyLook.LIGHTNING;
            case THUNDER -> EnemyLook.THUNDER;
            case HORNET -> EnemyLook.HORNET;
            case COCOON -> EnemyLook.COCOON;
            case LARVA -> EnemyLook.LARVA;
            case NURSE -> EnemyLook.NURSE;
            case HIVE_GUARD -> EnemyLook.HIVE_GUARD;
            case DRONE -> EnemyLook.DRONE;
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

    public List<CritSparkDraw> buildCritSparks() {
        return this.critSparkDraws;
    }

    public List<EnemyOverlayDraw> buildOverlays() {
        return this.overlayDraws;
    }

    private Void body(EnemyLook look, DefinedEnemyMob mob, float scale, double facingRadians,
            Optional<SupportAura> supportAura) {
        if (mob.isDead()) {
            int age = mob.ticksSinceDeath(this.gameTime).orElse(0);
            if (age <= fadeDurationTicks(mob.getRank())) {
                float fadeProgress = fadeProgress(mob.getRank(), age);
                this.draws.add(new EnemyFadeDraw(look, (float) mob.getX(), (float) mob.getY(), facingRadians, scale, age, fadeProgress));
            }
        } else if (!mob.isInactive()) {
            float x = lerp(mob.getPrevX(), mob.getX(), this.interpolationAlpha);
            float y = lerp(mob.getPrevY(), mob.getY(), this.interpolationAlpha);
            float cloakProgress = this.cloakProgress(mob);
            this.draws.add(new EnemyBodyDraw(look, x, y, facingRadians, mob.getDrawScale(), badgeFor(mob.getRank()),
                    cloakProgress));
            this.critSpark(mob, x, y);
            this.missMark(mob, x + scale, y);
            this.overlays(mob, x, y, scale, supportAura);
            this.pulses(mob, x, y, scale);
            this.bars(mob, look, x, y, scale, cloakProgress);
        }
        return null;
    }

    /** The one place that decides which bars an enemy shows and where, at its still scale, so they ignore a pulse. */
    private void bars(DefinedEnemyMob mob, EnemyLook look, float x, float y, float scale, float cloakProgress) {
        float health = mob.getHealthFraction();
        if (health < 1f) {
            this.overlayDraws.add(new EnemyBarsDraw(x, y + scale * EnemyLooks.of(look).reach() + BAR_GAP,
                    Math.max(BAR_MIN_WIDTH, scale * BAR_WIDTH_SCALE_FRACTION), 1f - cloakProgress,
                    List.of(new EnemyBarsDraw.Bar(EnemyBarLook.HEALTH, health))));
        }
    }

    /** Static overlays for ongoing states: shield bubble, ice crystals, support-aura and disruption rings. */
    private void overlays(DefinedEnemyMob mob, float x, float y, float scale, Optional<SupportAura> supportAura) {
        if (mob.activeEffectKinds().contains(EffectKind.SHIELD)) {
            this.overlayDraws.add(new EnemyRingDraw(Palette.STATUS_MARKER_SHIELD, x, y, scale * SHIELD_BUBBLE_SCALE_FRACTION,
                    SHIELD_BUBBLE_ALPHA * (0.3f + 0.7f * mob.shieldFraction())));
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

    /** A ring wherever the mob just cast an effect or arrived by an ability spawn. */
    private void pulses(DefinedEnemyMob mob, float x, float y, float scale) {
        mob.lastAbilityCast().ifPresent(cast -> {
            int ticksSince = this.gameTime - cast.tick();
            if (ticksSince >= 0 && ticksSince <= EFFECT_PULSE_DURATION_TICKS) {
                float progress = (float) ticksSince / EFFECT_PULSE_DURATION_TICKS;
                // A zero radius would draw an invisible ring.
                float ringRadius = cast.radius() > 0 ? cast.radius() : scale * CAST_PULSE_RADIUS_FRACTION;
                this.overlayDraws.add(new EffectPulseDraw(markerPaletteFor(cast.kind()), x, y, ringRadius, progress));
            }
        });
        OptionalInt spawned = mob.ticksSinceAbilitySpawn(this.gameTime);
        if (within(spawned, EFFECT_PULSE_DURATION_TICKS)) {
            float progress = (float) spawned.getAsInt() / EFFECT_PULSE_DURATION_TICKS;
            this.overlayDraws.add(new EffectPulseDraw(Palette.SPAWN_BURST, x, y, scale * SPAWN_BURST_RADIUS_FRACTION, progress));
        }
    }

    private void critSpark(DefinedEnemyMob mob, float x, float y) {
        OptionalInt crit = mob.ticksSinceCriticalHit(this.gameTime);
        if (within(crit, CRIT_SPARK_DURATION_TICKS)) {
            float fadeProgress = (float) crit.getAsInt() / CRIT_SPARK_DURATION_TICKS;
            this.critSparkDraws.add(new CritSparkDraw(Palette.CRIT_SPARK, x, y, CRIT_SPARK_FIXED_SCALE, fadeProgress));
        }
    }

    /** Beside the body, since the badge and whatever an effect draws fill the space above it. */
    private void missMark(DefinedEnemyMob mob, float rightOfBody, float y) {
        OptionalInt dodged = mob.ticksSinceDodge(this.gameTime);
        if (within(dodged, MISS_MARK_DURATION_TICKS)) {
            this.overlayDraws.add(new MissMarkDraw(rightOfBody, y, (float) dodged.getAsInt() / MISS_MARK_DURATION_TICKS));
        }
    }

    /**
     * Whether {@code since} happened no more than {@code ticks} ago. Plain branches, not a lambda per query: a
     * capturing lambda is built even when nothing happened, for every enemy every frame.
     */
    private static boolean within(OptionalInt since, int ticks) {
        return since.isPresent() && since.getAsInt() <= ticks;
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
            int ticksSinceGained = mob.ticksSinceEffectGained(EffectKind.INVISIBLE, this.gameTime).orElse(0);
            return Math.min(1f, (float) ticksSinceGained / CLOAK_FADE_DURATION_TICKS);
        }
        OptionalInt lost = mob.effectLostWithin(EffectKind.INVISIBLE, this.gameTime, CLOAK_FADE_DURATION_TICKS - 1);
        return lost.isPresent() ? 1f - (float) lost.getAsInt() / CLOAK_FADE_DURATION_TICKS : 0f;
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

    static int fadeDurationTicks(Rank rank) {
        return 3 * rank.ordinal() + 6;
    }

    /** {@code 0} at death to {@code 1} when gone. */
    static float fadeProgress(Rank rank, int ticksSinceDeath) {
        int alpha = 255 - (ticksSinceDeath * (255 / (fadeDurationTicks(rank) + 1)));
        return 1f - (Math.clamp(alpha, 0, 255) / 255f);
    }

    public Void visitDefined(DefinedEnemyMob mob) {
        return this.body(lookFor(mob.archetype()), mob, mob.getBodyScale(), mob.getFacingRadians(), mob.supportAura());
    }
}
