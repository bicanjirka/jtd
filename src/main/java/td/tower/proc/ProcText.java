package td.tower.proc;

import td.effect.EffectKind;
import td.enemy.Rank;
import td.term.Term;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/** How this package's records put hits, effect kinds and ranks into words. */
final class ProcText {

    private ProcText() {
    }

    /** One hit of {@code kind}: "shot", "beam hit", "[Arc]". */
    static TermText noun(HitKind kind) {
        return switch (kind) {
            case DIRECT -> TermText.of("shot");
            case PIERCE -> TermText.of("piercing hit");
            case BOUNCE -> TermText.of("bounce");
            case BLAST -> TermText.of("blast");
            case SHRAPNEL -> TermText.of(TowerTerm.SHRAPNEL).plus(" hit");
            case ARC -> TermText.of(TowerTerm.ARC);
            case BEAM -> TermText.of("beam hit");
            case MISSILE -> TermText.of("missile");
            case ZAP -> TermText.of("zap");
            case WAVE -> TermText.of("wave");
            case FIELD -> TermText.of("field pulse");
        };
    }

    /** Hits of {@code kind}: "shots", "beam hits", "[Arc] hits". */
    static TermText plural(HitKind kind) {
        return switch (kind) {
            case DIRECT -> TermText.of("shots");
            case PIERCE -> TermText.of("piercing hits");
            case BOUNCE -> TermText.of("bounces");
            case BLAST -> TermText.of("blasts");
            case SHRAPNEL -> TermText.of(TowerTerm.SHRAPNEL).plus(" hits");
            case ARC -> TermText.of(TowerTerm.ARC).plus(" hits");
            case BEAM -> TermText.of("beam hits");
            case MISSILE -> TermText.of("missiles");
            case ZAP -> TermText.of("zaps");
            case WAVE -> TermText.of("waves");
            case FIELD -> TermText.of("field pulses");
        };
    }

    /** {@code kinds} in enum order, each through {@code word}, listed with {@code last}: "shots and bounces". */
    static TermText hits(Set<HitKind> kinds, String last, Function<HitKind, TermText> word) {
        return listed(kinds.stream().sorted().map(word).toList(), last);
    }

    /** {@code kinds} in enum order as mentions: "[Freeze] or [Daze]". */
    static TermText kinds(Collection<EffectKind> kinds, String last) {
        return listed(kinds.stream().sorted().map(TermText::of).toList(), last);
    }

    /** "a, b and c". */
    static TermText listed(List<TermText> parts, String last) {
        if (parts.size() <= 1) {
            return parts.isEmpty() ? TermText.empty() : parts.getFirst();
        }
        return TermText.join(", ", parts.subList(0, parts.size() - 1)).plus(" " + last + " ").plus(parts.getLast());
    }

    /** "Elite and Boss": {@code rank} and every rank above it. */
    static String ranksFrom(Rank rank) {
        List<TermText> ranks = Arrays.stream(Rank.values()).filter(r -> r.compareTo(rank) >= 0)
                .map(r -> TermText.of(r.name().charAt(0) + r.name().substring(1).toLowerCase(Locale.ROOT))).toList();
        return listed(ranks, "and").plain();
    }

    /** "a" or "an", whichever reads before {@code term}'s title. */
    static String article(Term term) {
        return "AEIOU".indexOf(term.title().charAt(0)) >= 0 ? "an" : "a";
    }
}
