package td.ui;

import td.effect.EffectKind;
import td.enemy.Affix;
import td.enemy.BodyArchetype;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyMob;
import td.enemy.EnemyMobVisitor;
import td.enemy.Rank;
import td.enemy.SupportAura;
import td.term.Term;
import td.ui.render.AffixPipsDraw;
import td.ui.render.CritSparkDraw;
import td.ui.render.EffectPulseDraw;
import td.ui.render.EnemyBarLook;
import td.ui.render.EnemyBarsDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyBurstDraw;
import td.ui.render.EnemyBurstLook;
import td.ui.render.EnemyDotLook;
import td.ui.render.EnemyDotsDraw;
import td.ui.render.EnemyDraw;
import td.ui.render.EnemyEggDraw;
import td.ui.render.EnemyEmissionDraw;
import td.ui.render.EnemyEmissionLook;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.EnemyIceDraw;
import td.ui.render.EnemyLook;
import td.ui.render.EnemyOverlayDraw;
import td.ui.render.EnemyRingDraw;
import td.ui.render.EnemyTintDraw;
import td.ui.render.EnemyTintLook;
import td.ui.render.HexGlyph;
import td.ui.render.HexRuneDraw;
import td.ui.render.MissMarkDraw;
import td.ui.render.Palette;
import td.ui.render.RankBadge;
import td.ui.render.RankBadgeDraw;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

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
    static final int SHIELD_FLASH_DURATION_TICKS = 4;
    static final int SHIELD_POP_DURATION_TICKS = 7;
    static final int ICE_SHARDS_DURATION_TICKS = 8;
    /** How long plus signs keep rising after health is turned back at once. */
    static final int REWIND_HEAL_DURATION_TICKS = 24;
    /** The share of full health a second at which plus signs reach their most. */
    private static final float MOST_HEALING_PER_SECOND = 0.06f;
    private static final float DAZE_SWAY_RADIANS = 0.09f;
    private static final float DAZE_SWAY_RATE = 5.5f;
    /**
     * A body reaching further than this share of its scale is treated as larger by what must wrap around it; up to
     * here a square's corners still fit an egg made for its scale.
     */
    private static final float HULL_REACH = 1.41f;
    /** What the egg adds to a body's radius across and up: a share of it, up to a limit so a boss's doesn't cover the lane. */
    private static final float EGG_WIDTH_SHARE = 0.5f;
    private static final float EGG_HEIGHT_SHARE = 0.72f;
    private static final float EGG_MOST_WIDTH = 6f;
    private static final float EGG_MOST_HEIGHT = 8.6f;
    private static final float EGG_MARGIN = 2.5f;
    /** Ice grows with a body up to this radius, then more slowly for the same reason. */
    private static final float ICE_FULL_GROWTH_RADIUS = 12f;
    private static final float ICE_LATER_GROWTH = 0.72f;
    /** How far the egg's bottom sits from the centre, against its top. */
    private static final float EGG_BOTTOM_SHARE = 0.92f;
    /** How close the selection ring keeps to what it snaps around. */
    private static final float SELECTION_RING_MARGIN = 2f;
    /** The least of its width a shield's bar shows, so even a sliver of shield can be seen. */
    private static final float SHIELD_BAR_MIN_FILL = 0.08f;
    /** Enemies on one board can have nearby ids; the multiplier spreads their particle timing apart. */
    private static final float SEED_SPREAD = 3.7f;
    private static final int SEED_WRAP = 4096;
    private static final float SUPPORT_AURA_RING_ALPHA = 0.25f;
    private static final float SELECTION_RING_SCALE_FRACTION = 1.6f;
    private static final float SELECTION_RING_ALPHA = 0.9f;
    // Always on and only a few enemies have it, so it can stay faint: it says which towers are being harmed.
    private static final float DISRUPTION_RING_ALPHA = 0.2f;
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
    /** A dot's centre below the body's art, and the bars' top below the dots' centre when there are dots. */
    private static final float DOT_GAP = 3f;
    private static final float DOT_TO_BAR_GAP = 3.3f;
    /** The same on every enemy: a big body's size already says what its rank would. */
    private static final float RANK_BADGE_SIZE = 3.6f;
    private static final float RANK_BADGE_GAP = 5f;
    private static final float AFFIX_PIP_SIZE = RANK_BADGE_SIZE;
    private static final float AFFIX_PIP_STEP = 8.4f;
    /** A long tail or antenna shouldn't lift the badge off the body it belongs to. */
    private static final float RANK_BADGE_REACH_LIMIT = 1.3f;
    private final List<EnemyDraw> draws = new ArrayList<>();
    private final List<CritSparkDraw> critSparkDraws = new ArrayList<>();
    private final List<EnemyOverlayDraw> overlayDraws = new ArrayList<>();
    /** Read last, so every enemy's badge, runes, dots and bars stay readable over every enemy's effects. */
    private final List<EnemyOverlayDraw> markDraws = new ArrayList<>();
    private final List<EnemyOverlayDraw> badgeDraws = new ArrayList<>();
    private final int gameTime;
    private final double interpolationAlpha;
    private final Optional<EnemyMob> selected;
    private final float animationSeconds;

    public EnemyFrameBuilder(int gameTime, double interpolationAlpha) {
        this(gameTime, interpolationAlpha, Optional.empty(), 0.0);
    }

    /** Also rings {@code selected}, the enemy the player is inspecting; emissions run on {@code animationSeconds}. */
    public EnemyFrameBuilder(int gameTime, double interpolationAlpha, Optional<EnemyMob> selected, double animationSeconds) {
        this.gameTime = gameTime;
        this.interpolationAlpha = interpolationAlpha;
        this.selected = selected;
        this.animationSeconds = (float) animationSeconds;
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
            case MALAISE -> Palette.STATUS_MARKER_MALAISE;
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
            case CONDEMNED -> Palette.STATUS_MARKER_CONDEMNED;
            case STILLED -> Palette.STATUS_MARKER_STILLED;
            case DEAD_ZONE -> Palette.STATUS_MARKER_DEAD_ZONE;
            case FRAIL -> Palette.STATUS_MARKER_FRAIL;
            case CRACKED -> Palette.STATUS_MARKER_CRACKED;
            case TARRED -> Palette.STATUS_MARKER_TARRED;
            case BLEEDING -> Palette.STATUS_MARKER_BLEEDING;
            case SOULFIRE -> Palette.STATUS_MARKER_SOULFIRE;
            case SPRINTING, HASTENED -> Palette.STATUS_MARKER_SPRINTING;
            case SPELLPROOF -> Palette.STATUS_MARKER_SPELLPROOF;
            case UNYIELDING, RESOLVE -> Palette.STATUS_MARKER_UNYIELDING;
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
        List<EnemyOverlayDraw> all = new ArrayList<>(this.overlayDraws.size() + this.badgeDraws.size() + this.markDraws.size());
        all.addAll(this.overlayDraws);
        all.addAll(this.badgeDraws);
        all.addAll(this.markDraws);
        return all;
    }

    /** The rank badges and affix pips alone, for a preview that shows a body and what it is and nothing around it. */
    public List<EnemyOverlayDraw> buildBadgeRows() {
        return this.badgeDraws;
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
            this.liveBody(look, mob, scale, facingRadians, supportAura);
        }
        return null;
    }

    /** Adds what is drawn on a live enemy, back to front; a later layer never hides what an earlier one reports. */
    private void liveBody(EnemyLook look, DefinedEnemyMob mob, float scale, double facingRadians, Optional<SupportAura> supportAura) {
        float x = lerp(mob.getPrevX(), mob.getX(), this.interpolationAlpha);
        float y = lerp(mob.getPrevY(), mob.getY(), this.interpolationAlpha);
        float visibility = 1f - this.cloakProgress(mob);
        Set<EffectKind> active = mob.activeEffectKinds();
        float reach = scale * EnemyLooks.of(look).reach();
        float seed = mob.id() % SEED_WRAP * SEED_SPREAD + 1f;
        float hull = Math.max(scale, reach / HULL_REACH);
        float eggX = hull + Math.min(hull * EGG_WIDTH_SHARE, EGG_MOST_WIDTH) + EGG_MARGIN;
        float eggY = hull + Math.min(hull * EGG_HEIGHT_SHARE, EGG_MOST_HEIGHT) + EGG_MARGIN;
        float iceScale = hull <= ICE_FULL_GROWTH_RADIUS ? hull
                : ICE_FULL_GROWTH_RADIUS + (hull - ICE_FULL_GROWTH_RADIUS) * ICE_LATER_GROWTH;
        float above = Math.min(reach, scale * RANK_BADGE_REACH_LIMIT);
        float beneath = reach;
        float ringRadius = scale * SELECTION_RING_SCALE_FRACTION;
        double facing = active.contains(EffectKind.DAZED)
                ? facingRadians + Math.sin(this.animationSeconds * DAZE_SWAY_RATE + seed) * DAZE_SWAY_RADIANS
                : facingRadians;
        float drawScale = mob.getDrawScale();

        if (active.contains(EffectKind.BLEEDING) && (mob.getX() != mob.getPrevX() || mob.getY() != mob.getPrevY())) {
            this.draws.add(new EnemyEmissionDraw(EnemyEmissionLook.BLEED, x, y, hull, 1f, seed, this.animationSeconds, visibility));
        }
        this.draws.add(new EnemyBodyDraw(look, x, y, facing, drawScale, 1f - visibility));
        if (active.contains(EffectKind.POISON)) {
            this.overlayDraws.add(new EnemyTintDraw(look, x, y, facing, drawScale, EnemyTintLook.POISON, 1f, visibility));
            this.emit(EnemyEmissionLook.POISON, x, y, hull, this.fullness(mob, EffectKind.POISON), seed, visibility);
        }
        if (active.contains(EffectKind.CHILL)) {
            float level = this.fullness(mob, EffectKind.CHILL);
            this.overlayDraws.add(new EnemyTintDraw(look, x, y, facing, drawScale, EnemyTintLook.CHILL, level, visibility));
            this.emit(EnemyEmissionLook.CHILL, x, y, hull, level, seed, visibility);
        }
        int iceVariant = (int) Math.floorMod(mob.id(), EnemyEffects.iceVariants());
        if (active.contains(EffectKind.FREEZE)) {
            this.overlayDraws.add(new EnemyTintDraw(look, x, y, facing, drawScale, EnemyTintLook.FREEZE, 1f, visibility));
            this.overlayDraws.add(new EnemyIceDraw(x, y, iceScale, iceVariant, seed, this.animationSeconds, visibility));
            above = Math.max(above, EnemyEffects.iceAbove(iceVariant, iceScale));
            beneath = Math.max(beneath, EnemyEffects.iceBeneath(iceVariant, iceScale));
            ringRadius = Math.max(ringRadius, EnemyEffects.iceReach(iceVariant, iceScale) + SELECTION_RING_MARGIN);
        }
        // A burn reveals, so its art never fades with the cloak.
        if (active.contains(EffectKind.BURN)) {
            this.emit(EnemyEmissionLook.BURN, x, y, hull, this.fullness(mob, EffectKind.BURN), seed, 1f);
        }
        if (active.contains(EffectKind.SOULFIRE)) {
            this.emit(EnemyEmissionLook.SOULFIRE, x, y, hull, this.fullness(mob, EffectKind.SOULFIRE), seed, 1f);
        }
        float healing = this.healingAmount(mob);
        if (healing > 0f) {
            this.emit(EnemyEmissionLook.HEAL, x, y, hull, healing, seed, visibility);
        }
        if (active.contains(EffectKind.DAZED)) {
            this.emit(EnemyEmissionLook.DAZE, x, y, hull, 1f, seed, visibility);
        }
        if (active.contains(EffectKind.SHIELD)) {
            this.overlayDraws.add(new EnemyEggDraw(x, y, eggX, eggY, this.shieldFlash(mob), seed, this.animationSeconds, visibility));
            above = Math.max(above, eggY);
            beneath = Math.max(beneath, eggY * EGG_BOTTOM_SHARE);
            ringRadius = Math.max(ringRadius, Math.max(eggX, eggY) + SELECTION_RING_MARGIN);
        }
        OptionalInt broken = mob.ticksSinceShieldBroken(this.gameTime);
        if (within(broken, SHIELD_POP_DURATION_TICKS)) {
            this.overlayDraws.add(new EnemyBurstDraw(EnemyBurstLook.SHIELD_POP, x, y, eggX, eggY,
                    (float) broken.getAsInt() / SHIELD_POP_DURATION_TICKS, seed, visibility));
        }
        OptionalInt thawed = mob.effectLostWithin(EffectKind.FREEZE, this.gameTime, ICE_SHARDS_DURATION_TICKS);
        if (thawed.isPresent() && !active.contains(EffectKind.FREEZE)) {
            this.overlayDraws.add(new EnemyBurstDraw(EnemyBurstLook.ICE_SHARDS, x, y, eggX, eggY,
                    (float) thawed.getAsInt() / ICE_SHARDS_DURATION_TICKS, seed, visibility));
        }
        this.critSpark(mob, x, y);
        this.overlays(mob, x, y, ringRadius, supportAura);
        this.pulses(mob, x, y, scale);
        this.badge(mob, x, y - above - RANK_BADGE_GAP, visibility);
        this.runes(active, x, y - scale * RUNE_ROW_OFFSET_FRACTION);
        this.missMark(mob, x + scale, y);
        float below = y + beneath;
        boolean dotted = this.dots(active, x, below + DOT_GAP, visibility);
        this.bars(mob, x, dotted ? below + DOT_GAP + DOT_TO_BAR_GAP : below + BAR_GAP, scale, visibility);
    }

    private void emit(EnemyEmissionLook look, float x, float y, float hull, float amount, float seed, float alpha) {
        this.overlayDraws.add(new EnemyEmissionDraw(look, x, y, hull, amount, seed, this.animationSeconds, alpha));
    }

    private float fullness(DefinedEnemyMob mob, EffectKind kind) {
        return (float) mob.effectFullness(kind).orElse(0.0);
    }

    /** How strongly plus signs rise, from {@code 0} for none: at the most for a while after health is turned back. */
    private float healingAmount(DefinedEnemyMob mob) {
        if (within(mob.ticksSinceRewind(this.gameTime), REWIND_HEAL_DURATION_TICKS)) {
            return 1f;
        }
        return Math.min(1f, mob.healingPerSecond() / MOST_HEALING_PER_SECOND);
    }

    /** The one place that decides which bars an enemy shows and where, at its still scale, so they ignore a pulse. */
    private void bars(DefinedEnemyMob mob, float x, float top, float scale, float visibility) {
        float health = mob.getHealthFraction();
        boolean hurt = health < 1f;
        boolean shielded = mob.hasEffect(EffectKind.SHIELD);
        if (!hurt && !shielded) {
            return;
        }
        EnemyBarsDraw.Bar healthBar = new EnemyBarsDraw.Bar(EnemyBarLook.HEALTH, health);
        List<EnemyBarsDraw.Bar> bars = !shielded ? List.of(healthBar)
                : hurt ? List.of(this.shieldBar(mob), healthBar) : List.of(this.shieldBar(mob));
        this.markDraws.add(new EnemyBarsDraw(x, top, Math.max(BAR_MIN_WIDTH, scale * BAR_WIDTH_SCALE_FRACTION),
                visibility, bars));
    }

    /** All its shields on the health bar's scale, so the two compare directly. */
    private EnemyBarsDraw.Bar shieldBar(DefinedEnemyMob mob) {
        float share = mob.shieldGauge().remaining() / mob.getMaxHealthPoints();
        return new EnemyBarsDraw.Bar(EnemyBarLook.SHIELD, Math.clamp(share, SHIELD_BAR_MIN_FILL, 1f));
    }

    private float shieldFlash(DefinedEnemyMob mob) {
        OptionalInt hit = mob.ticksSinceShieldHit(this.gameTime);
        return within(hit, SHIELD_FLASH_DURATION_TICKS) ? 1f - (float) hit.getAsInt() / SHIELD_FLASH_DURATION_TICKS : 0f;
    }

    /** The rank badge and the affix pips as one row centred over the body; a hidden enemy shows no affixes. */
    private void badge(DefinedEnemyMob mob, float x, float y, float visibility) {
        RankBadge badge = badgeFor(mob.getRank());
        List<Affix> affixes = mob.isHidden() ? List.of() : mob.definition().affixes();
        int marks = (badge != RankBadge.NONE ? 1 : 0) + affixes.size();
        float left = x - (marks - 1) * AFFIX_PIP_STEP / 2f;
        if (badge != RankBadge.NONE) {
            this.badgeDraws.add(new RankBadgeDraw(badge, left, y, RANK_BADGE_SIZE, visibility));
            left += AFFIX_PIP_STEP;
        }
        if (!affixes.isEmpty()) {
            this.badgeDraws.add(new AffixPipsDraw(left, y, AFFIX_PIP_SIZE, AFFIX_PIP_STEP, visibility,
                    List.<Term>copyOf(affixes)));
        }
    }

    /** One dot per kind of dot the enemy's effects call for, in a fixed order; whether it drew any. */
    private boolean dots(Set<EffectKind> active, float x, float y, float visibility) {
        if (active.isEmpty()) {
            return false;
        }
        Set<EnemyDotLook> dots = EnumSet.noneOf(EnemyDotLook.class);
        for (EffectKind kind : active) {
            dotFor(kind).ifPresent(dots::add);
        }
        if (dots.isEmpty()) {
            return false;
        }
        this.markDraws.add(new EnemyDotsDraw(x, y, visibility, List.copyOf(dots)));
        return true;
    }

    /** The rings around an enemy: its selection and support aura while selected, and always its disruption. */
    private void overlays(DefinedEnemyMob mob, float x, float y, float selectionRadius, Optional<SupportAura> supportAura) {
        if (this.selected.isPresent() && this.selected.get() == mob) {
            this.overlayDraws.add(new EnemyRingDraw(Palette.SELECTION, x, y, selectionRadius, SELECTION_RING_ALPHA));
            supportAura.ifPresent(aura -> this.overlayDraws.add(
                    new EnemyRingDraw(markerPaletteFor(aura.kind()), x, y, aura.radius(), SUPPORT_AURA_RING_ALPHA)));
        }
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
            this.critSparkDraws.add(new CritSparkDraw(Palette.CRIT_SPARK, x, y, CRIT_SPARK_FIXED_SCALE,
                    sparkRotation(mob.id(), this.gameTime - crit.getAsInt()), fadeProgress));
        }
    }

    /** How a crit's star is turned, from the enemy and the moment alone, so no draw of it can touch a game's results. */
    private static float sparkRotation(long id, int tick) {
        long mixed = (id * 0x9E3779B97F4A7C15L) ^ (tick * 0xC2B2AE3D27D4EB4FL);
        return ((mixed ^ (mixed >>> 29)) & Long.MAX_VALUE) % 628 / 100f;
    }

    /** Beside the body, since the badge and whatever an effect draws fill the space above it. */
    private void missMark(DefinedEnemyMob mob, float rightOfBody, float y) {
        OptionalInt dodged = mob.ticksSinceDodge(this.gameTime);
        if (within(dodged, MISS_MARK_DURATION_TICKS)) {
            this.markDraws.add(new MissMarkDraw(rightOfBody, y, (float) dodged.getAsInt() / MISS_MARK_DURATION_TICKS));
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
    private void runes(Set<EffectKind> active, float x, float y) {
        if (active.isEmpty()) {
            return;
        }
        List<HexGlyph> glyphs = active.stream()
                .map(EnemyFrameBuilder::runeFor)
                .flatMap(Optional::stream)
                .toList();
        float left = x - RUNE_SPACING * (glyphs.size() - 1) / 2f;
        for (int i = 0; i < glyphs.size(); i++) {
            this.markDraws.add(new HexRuneDraw(glyphs.get(i), left + RUNE_SPACING * i, y, RUNE_FIXED_SCALE));
        }
    }

    /** The rune a hex is drawn as; empty for every other kind. */
    static Optional<HexGlyph> runeFor(EffectKind kind) {
        return switch (kind) {
            case DOOM -> Optional.of(HexGlyph.DOOM);
            case BLIGHT -> Optional.of(HexGlyph.BLIGHT);
            case CONTAGION -> Optional.of(HexGlyph.CONTAGION);
            case MALAISE -> Optional.of(HexGlyph.MALAISE);
            case INVERSION -> Optional.of(HexGlyph.INVERSION);
            case SYMPATHY -> Optional.of(HexGlyph.SYMPATHY);
            case RECKONING -> Optional.of(HexGlyph.RECKONING);
            default -> Optional.empty();
        };
    }

    /** The dot an effect with no picture of its own earns, if it earns one; every kind is decided here. */
    static Optional<EnemyDotLook> dotFor(EffectKind kind) {
        return switch (kind) {
            case VULNERABLE, SUNDERED, FRACTURED, BRITTLE, CRACKED, TARRED, TOLL, CONDEMNED, STILLED, RESONATING,
                 SATURATED, UNRAVELED -> Optional.of(EnemyDotLook.DEBUFF);
            case EXPOSED, MARKED, PRIORITY, CHARGED -> Optional.of(EnemyDotLook.SPOTTED);
            case SPRINTING, HASTENED, SPELLPROOF, UNYIELDING, RESOLVE -> Optional.of(EnemyDotLook.BUFF);
            // Drawn on the enemy.
            case BURN, SOULFIRE, POISON, BLEEDING, HEAL, SHIELD, CHILL, FREEZE, DAZED, INVISIBLE -> Optional.empty();
            // A rune.
            case DOOM, BLIGHT, CONTAGION, MALAISE, INVERSION, SYMPATHY, RECKONING -> Optional.empty();
            // The Info pane's, or the field on the ground says it.
            case SCORCHED, SICKENED, REVEALED, SILENCED, ANCHORED, CORRODED, DRAGGED, DEAD_ZONE,
                 FRAIL -> Optional.empty();
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
