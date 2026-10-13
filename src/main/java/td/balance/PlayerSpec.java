package td.balance;

import java.util.function.BiFunction;

/**
 * A kind of player, by name: makes a fresh {@link Player} for each play of a level.
 *
 * @param name   what reports call it
 * @param create builds the player for one play, from the level's survey and the play's seed
 */
public record PlayerSpec(String name, BiFunction<BoardSurvey, Long, Player> create) {

    Player createFor(BoardSurvey survey, long seed) {
        return this.create.apply(survey, seed);
    }
}
