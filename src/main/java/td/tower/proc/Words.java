package td.tower.proc;

import td.effect.EffectKind;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** How this package's records say an effect kind, a hit kind or a list of them. */
final class Words {

    private Words() {
    }

    /** "Toll", "Vulnerable": an effect kind as a name. */
    static String name(EffectKind kind) {
        String lower = kind.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /** "shielded", "frozen": what an enemy under {@code kind} is. */
    static String state(EffectKind kind) {
        return switch (kind) {
            case SHIELD -> "shielded";
            case FREEZE -> "frozen";
            case DAZED -> "dazed";
            default -> kind.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }

    /** "shots", "the zap": what hits of {@code kind} are called. */
    static String hits(HitKind kind) {
        return switch (kind) {
            case DIRECT -> "shots";
            case PIERCE -> "piercing hits";
            case BOUNCE -> "bounces";
            case BLAST -> "blasts";
            case SHRAPNEL -> "shrapnel";
            case ARC -> "arcs";
            case BEAM -> "beam hits";
            case MISSILE -> "missiles";
            case ZAP -> "the zap";
            case WAVE -> "waves";
            case FIELD -> "the field";
        };
    }

    /** "a, b and c". */
    static String listed(List<String> words, String last) {
        if (words.size() <= 1) {
            return words.isEmpty() ? "" : words.getFirst();
        }
        return String.join(", ", words.subList(0, words.size() - 1)) + " " + last + " " + words.getLast();
    }

    static String hitsOf(Set<HitKind> kinds) {
        return listed(kinds.stream().sorted().map(Words::hits).toList(), "and");
    }

    static String statesOf(Set<EffectKind> kinds) {
        return "vs " + listed(kinds.stream().sorted().map(Words::state).toList(), "or");
    }

    /** "3rd", "5th". */
    static String ordinal(int n) {
        int tens = n % 100;
        String suffix = tens >= 11 && tens <= 13 ? "th" : switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
        return n + suffix;
    }

    static String capitalized(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
