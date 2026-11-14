package td.tower.splash;

import td.term.TermText;
import td.tower.TowerTerm;
import td.tower.ability.AbilityUnits;

/** What a passive pays each tower that tagged an enemy, by the enemy's max health. */
public final class PassivePayout {

    private static final int[] UNDER = {150, 500, 1500};
    private static final float[] UNITS = {0.25f, 0.5f, 1f};
    private static final float FROM_THE_TOP = 2f;

    private PassivePayout() {
    }

    /** What an enemy of {@code maxHealthPoints} pays. */
    public static AbilityUnits forMaxHealth(int maxHealthPoints) {
        for (int i = 0; i < UNDER.length; i++) {
            if (maxHealthPoints < UNDER[i]) {
                return AbilityUnits.of(UNITS[i]);
            }
        }
        return AbilityUnits.of(FROM_THE_TOP);
    }

    /** "a quarter of a Spark for an enemy under 150 max health, a half under 500, ...", in {@code currency}. */
    public static TermText describe(TowerTerm currency) {
        return TermText.of("a quarter of a ").plus(currency).plus(" for an enemy under " + UNDER[0]
                + " max health, a half under " + UNDER[1] + ", " + whole(UNITS[2]) + " under " + UNDER[2]
                + ", and " + whole(FROM_THE_TOP) + " from " + UNDER[2] + " up");
    }

    private static String whole(float units) {
        return Integer.toString(Math.round(units));
    }
}
