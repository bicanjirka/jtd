package td.balance;

import java.util.List;
import java.util.Locale;

/**
 * A number of a level the report can sweep, to show how fast the competent player's margin moves with it: what
 * a designer reaches for before touching an enemy or a tower. Each is set through {@link Rules} or the play's
 * pacing, never by editing the level.
 */
enum LevelKnob {

    /** Lives added to the level's own. */
    LIVES("lives added", List.of(-2, -1, 0, 2, 5)) {
        @Override
        Setting at(int value) {
            return new Setting(Rules.standard().withLivesBonus(value), LevelPlay.Pacing.standard());
        }
    },
    /** What every kill pays, as a percentage of the authored bounty. Tower XP follows bounty, so gates move too. */
    BOUNTY("bounty %", List.of(60, 80, 100, 125, 150)) {
        @Override
        Setting at(int value) {
            return new Setting(Rules.standard().withBountyPercent(value), LevelPlay.Pacing.standard());
        }
    },
    /** Ticks the player waits between a round clearing and calling the next. */
    PAUSE("pause ticks", List.of(0, 30, 60, 180, 600)) {
        @Override
        Setting at(int value) {
            return new Setting(Rules.standard(), LevelPlay.Pacing.standard().withPauseTicks(value));
        }
    },
    /** The gaps between a wave's spawns, as a percentage of the authored ones. */
    SPACING("spacing %", List.of(50, 75, 100, 150, 200)) {
        @Override
        Setting at(int value) {
            return new Setting(Rules.standard().withSpawnSpacingPercent(value), LevelPlay.Pacing.standard());
        }
    };

    private final String unit;
    private final List<Integer> settings;

    LevelKnob(String unit, List<Integer> settings) {
        this.unit = unit;
        this.settings = settings;
    }

    /** The rules and pacing a play runs under with this knob at {@code value}. */
    abstract Setting at(int value);

    /** What a value counts, for a table header. */
    String unit() {
        return this.unit;
    }

    /** The values a sweep tries, the level as authored among them. */
    List<Integer> settings() {
        return this.settings;
    }

    static LevelKnob named(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT));
    }

    /** How a play is set up for one value of a knob. */
    record Setting(Rules rules, LevelPlay.Pacing pacing) {
    }
}
