package td.enemy;

import td.stat.EnemyStat;

import java.util.Locale;

/** Number and name wording shared by the traits' {@code describe()} lines. */
final class TraitText {

    private TraitText() {
    }

    /** Hundredths as points, without a trailing {@code .0}. */
    static String points(int hundredths) {
        return decimal(hundredths / 100f);
    }

    static String decimal(float value) {
        if (value == Math.round(value)) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    static String capitalized(String word) {
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }

    static String effectName(EnemyStat resistance) {
        return switch (resistance) {
            case SLOW_RESIST -> "slow";
            case BURN_RESIST -> "burn";
            case FREEZE_RESIST -> "freeze";
            default -> resistance.name().toLowerCase(Locale.ROOT);
        };
    }
}
