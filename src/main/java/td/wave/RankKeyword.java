package td.wave;

import td.enemy.Rank;

import java.util.Optional;
import java.util.stream.Stream;

/** A wave script's rank keyword: the {@link Rank} it names. */
enum RankKeyword {
    GRUNT("grunt", Rank.GRUNT),
    SOLDIER("soldier", Rank.SOLDIER),
    VETERAN("veteran", Rank.VETERAN),
    ELITE("elite", Rank.ELITE),
    BOSS("boss", Rank.BOSS);

    private final String token;
    private final Rank rank;

    RankKeyword(String token, Rank rank) {
        this.token = token;
        this.rank = rank;
    }

    static Optional<RankKeyword> of(String token) {
        return Stream.of(values()).filter(keyword -> keyword.token.equals(token)).findFirst();
    }

    /** How a script writes {@code rank}. */
    static String tokenFor(Rank rank) {
        return Stream.of(values()).filter(keyword -> keyword.rank == rank).findFirst().orElseThrow().token;
    }

    String token() {
        return this.token;
    }

    Rank rank() {
        return this.rank;
    }
}
