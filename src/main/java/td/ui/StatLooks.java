package td.ui;

import td.economy.StatusTerm;
import td.effect.EffectKind;
import td.stat.EnemyStat;
import td.stat.StatTerm;
import td.term.Term;
import td.tower.TowerStat;
import td.tower.TowerStatRow;
import td.tower.TowerTerm;
import td.ui.render.IconFace;
import td.ui.render.Palette;
import td.ui.render.SheetLine.Glyph;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The mark of every stat: a painted glyph in the colour the board already gives the thing it measures, or the face of
 * the effect it belongs to. Filled is what a tower brings, hollow the enemy's counter to it. One exhaustive switch per
 * stat family, built into one map so no caller branches on a type.
 */
final class StatLooks {

    private static final Map<String, Entry> BY_KEY = build();

    private StatLooks() {
    }

    /** {@code stat}'s mark. */
    static StatLook of(StatTerm stat) {
        return BY_KEY.get(stat.key()).look();
    }

    /** The stat {@code term} names, when it names one. */
    static Optional<StatTerm> statOf(Term term) {
        return Optional.ofNullable(BY_KEY.get(term.key())).map(Entry::stat);
    }

    /** Every stat the game has, for a rule every one keeps. */
    static Stream<StatTerm> all() {
        return BY_KEY.values().stream().map(Entry::stat);
    }

    private static Map<String, Entry> build() {
        Map<String, Entry> looks = new HashMap<>();
        Arrays.stream(EnemyStat.values()).forEach(stat -> looks.put(stat.key(), new Entry(stat, enemy(stat))));
        Arrays.stream(TowerStat.values()).forEach(stat -> looks.put(stat.key(), new Entry(stat, tower(stat))));
        Arrays.stream(TowerStatRow.values()).forEach(row -> looks.put(row.key(), new Entry(row, row(row))));
        return Map.copyOf(looks);
    }

    private static StatLook enemy(EnemyStat stat) {
        return switch (stat) {
            case MOVE_SPEED -> StatLook.plain(Glyph.CHEVRON);
            case ARMOR -> StatLook.toned(Glyph.HOLLOW_SHIELD, Palette.DAMAGE_PHYSICAL);
            case MAGIC_RESIST -> StatLook.toned(Glyph.HOLLOW_SHIELD, Palette.DAMAGE_MAGIC);
            case PLATING -> StatLook.toned(Glyph.FILLED_SHIELD, Palette.DAMAGE_PHYSICAL);
            case INSULATION -> StatLook.toned(Glyph.FILLED_SHIELD, Palette.DAMAGE_MAGIC);
            case RESILIENCE -> StatLook.toned(Glyph.HOLLOW_SPARK, Palette.CRIT_SPARK);
            case AGILITY -> StatLook.plain(Glyph.CROSSHAIR);
            case PHYSICAL_DAMAGE_TAKEN -> StatLook.toned(Glyph.CRACKED_SHIELD, Palette.DAMAGE_PHYSICAL);
            case MAGIC_DAMAGE_TAKEN -> StatLook.toned(Glyph.CRACKED_SHIELD, Palette.DAMAGE_MAGIC);
            case PERIODIC_DAMAGE_TAKEN -> StatLook.plain(Glyph.CRACKED_SHIELD);
            case SPIRIT, REGENERATION -> StatLook.toned(Glyph.PLUS, Palette.STATUS_MARKER_HEAL);
            case CHILL_RESIST -> StatLook.warded(EffectKind.CHILL);
            case BURN_RESIST -> StatLook.warded(EffectKind.BURN);
            case FREEZE_RESIST -> StatLook.warded(EffectKind.FREEZE);
            case STEALTH -> StatLook.face(TermLooks.faceOf(EffectKind.INVISIBLE));
        };
    }

    private static StatLook tower(TowerStat stat) {
        return switch (stat) {
            case RANGE -> StatLook.plain(Glyph.RING);
            case DEAD_ZONE -> StatLook.plain(Glyph.BROKEN_RING);
            case AREA_RADIUS, BLAST_EDGE -> StatLook.plain(Glyph.RING_CENTRE);
            case CONE_WIDTH -> StatLook.plain(Glyph.WEDGE);
            case DAMAGE -> StatLook.plain(Glyph.DOT);
            case ATTACK_SPEED -> StatLook.plain(Glyph.CHEVRON);
            case BRACKET_DAMAGE_STEP -> StatLook.toned(Glyph.CHEVRON, Palette.DAMAGE_PHYSICAL);
            case CRIT_CHANCE, CRIT_MULTIPLIER -> StatLook.toned(Glyph.SPARK, Palette.CRIT_SPARK);
            case AIM_STACKS -> StatLook.face(TermLooks.faceOf(TowerTerm.STEADY_AIM));
            case ARMOR_PENETRATION, ARMOR_PENETRATION_FLAT -> StatLook.toned(Glyph.PIERCE, Palette.DAMAGE_PHYSICAL);
            case MAGIC_PENETRATION -> StatLook.toned(Glyph.PIERCE, Palette.DAMAGE_MAGIC);
            case FLAT_PENETRATION -> StatLook.plain(Glyph.PIERCE);
            case ACCURACY -> StatLook.plain(Glyph.CROSSHAIR_FILLED);
            case EFFECT_DURATION -> StatLook.plain(Glyph.FILLED_DIAMOND);
            case BUFF_DURATION -> StatLook.toned(Glyph.FILLED_DIAMOND, Palette.TERM_FRAME_TOWER_BUFF);
            case BURN_DURATION, BURN_POOL_CAP -> StatLook.face(TermLooks.faceOf(EffectKind.BURN));
            case STOKE_STEP, STOKE_GRACE -> StatLook.face(TermLooks.faceOf(TowerTerm.STOKE));
            case BURN_MARK_RATE -> StatLook.face(TermLooks.faceOf(EffectKind.SCORCHED));
            case FREEZE_DURATION -> StatLook.face(TermLooks.faceOf(EffectKind.FREEZE));
            case SATURATION_CAP, SATURATION_INNER_STACKS -> StatLook.face(TermLooks.faceOf(EffectKind.SATURATED));
            case TOLL_CAP, TOLL_RATE, TOLL_LINGER -> StatLook.face(TermLooks.faceOf(EffectKind.TOLL));
            case WITHER_INTERVAL -> StatLook.face(TermLooks.faceOf(EffectKind.VULNERABLE));
            case PROJECTILE_SPEED, PROJECTILE_SIZE, WAVE_SPEED -> StatLook.plain(Glyph.COMET);
            case NEST_CAPACITY, MISSILES_PER_LAUNCH, PING_COUNT -> StatLook.plain(Glyph.PIP);
            case BOUNTY -> StatLook.face(TermLooks.faceOf(StatusTerm.CASH));
            case DISRUPTION_TAKEN -> StatLook.toned(Glyph.FILLED_DIAMOND, Palette.DISRUPTION);
            case AURA_POWER -> StatLook.toned(Glyph.RING, Palette.TERM_AURA);
        };
    }

    private static StatLook row(TowerStatRow row) {
        return switch (row) {
            case PHYSICAL_DAMAGE, PHYSICAL_FIELD_DAMAGE -> StatLook.toned(Glyph.DOT, Palette.DAMAGE_PHYSICAL);
            case MAGIC_DAMAGE, MAGIC_FIELD_DAMAGE -> StatLook.toned(Glyph.DOT, Palette.DAMAGE_MAGIC);
            case FIRE_RATE, SWEEP_TIME, WAVE_DPS -> StatLook.plain(Glyph.CHEVRON);
            case PHYSICAL_DPS -> StatLook.toned(Glyph.CHEVRON, Palette.DAMAGE_PHYSICAL);
            case MAGIC_DPS, BURN_DPS -> StatLook.toned(Glyph.CHEVRON, Palette.DAMAGE_MAGIC);
            case PROJECTILE_SPEED, PROJECTILE_SIZE -> StatLook.plain(Glyph.COMET);
        };
    }

    private record Entry(StatTerm stat, StatLook look) {
    }

    /**
     * A stat's mark: a glyph, in a tone or unmarked, or a face painted in its place.
     */
    record StatLook(Glyph glyph, Optional<Palette> tone, Optional<IconFace> face) {

        static StatLook plain(Glyph glyph) {
            return new StatLook(glyph, Optional.empty(), Optional.empty());
        }

        static StatLook toned(Glyph glyph, Palette tone) {
            return new StatLook(glyph, Optional.of(tone), Optional.empty());
        }

        static StatLook face(IconFace face) {
            return new StatLook(Glyph.DOT, Optional.empty(), Optional.of(face));
        }

        /** An effect's face with the ward a trait icon puts on what keeps that effect out. */
        static StatLook warded(EffectKind kind) {
            return face(TermLooks.faceOf(kind).withWard(IconFace.Ward.RESISTED));
        }

        /** The colour a mention of the stat writes its word in. */
        Color word() {
            return this.face.map(found -> TermIconPainter.ink(found.tone()))
                    .orElseGet(() -> this.tone.map(Java2DFrameRenderer::colorFor).orElse(Hud.FOREGROUND));
        }
    }
}
