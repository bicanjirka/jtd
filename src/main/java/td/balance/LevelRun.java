package td.balance;

import td.tower.TowerFactory;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

/**
 * Everything the report measured on one level: each player's plays of the level as authored, seed by seed, and
 * each player's margin on the same seeds, with the lab. Seeds line up across players, so two players compare seed
 * by seed.
 *
 * @param lab     what the lab measured on the level
 * @param plays   each player's plays, by player name, in seed order
 * @param margins each player's margin per seed, by player name, in the same order
 */
public record LevelRun(TowerLab lab, Map<String, List<PlayRecord>> plays, Map<String, int[]> margins) {

    public LevelRun {
        plays = Map.copyOf(plays);
        margins = Map.copyOf(margins);
    }

    public BoardSurvey survey() {
        return this.lab.survey();
    }

    public List<PlayRecord> playsOf(String player) {
        return this.plays.getOrDefault(player, List.of());
    }

    public boolean has(String player) {
        return this.plays.containsKey(player);
    }

    public Estimate winRate(String player) {
        return Estimate.proportion(this.playsOf(player), play -> play.result().won());
    }

    public Estimate livesLost(String player) {
        return Estimate.of(this.playsOf(player), play -> play.result().livesLost());
    }

    /** Lives lost as a share of the level's lives. */
    public Estimate livesLostShare(String player) {
        return Estimate.of(this.playsOf(player), play -> play.result().livesLost() / (double) play.result().lives());
    }

    public Estimate leaks(String player) {
        return Estimate.of(this.playsOf(player), play -> play.result().leaks());
    }

    public Estimate margin(String player) {
        int[] values = this.margins.get(player);
        return values == null ? Estimate.of(new double[0]) : Estimate.of(Arrays.stream(values).asDoubleStream().toArray());
    }

    /**
     * How many times {@code player}'s margin is {@code other}'s, seed by seed: the ratio of their geometric means,
     * with an interval from the per-seed log ratios. Pairing by seed takes out what the seeds share.
     */
    public Estimate marginRatio(String player, String other) {
        int[] a = this.margins.get(player);
        int[] b = this.margins.get(other);
        if (a == null || b == null) {
            return Estimate.of(new double[0]);
        }
        double[] logs = IntStream.range(0, Math.min(a.length, b.length))
                .mapToDouble(i -> Math.log(Math.max(1, a[i])) - Math.log(Math.max(1, b[i]))).toArray();
        Estimate log = Estimate.of(logs);
        return new Estimate(Math.exp(log.value()), Math.exp(log.low()), Math.exp(log.high()), log.n());
    }

    /** The mean of {@code value} over {@code player}'s plays of each round, round by round. */
    public double[] perRound(String player, ToDoubleFunction<PlayRecord.Round> value) {
        int rounds = this.survey().level().paths().getFirst().waves().size();
        double[] sums = new double[rounds];
        int[] counts = new int[rounds];
        for (PlayRecord play : this.playsOf(player)) {
            for (PlayRecord.Round round : play.rounds()) {
                sums[round.number() - 1] += value.applyAsDouble(round);
                counts[round.number() - 1]++;
            }
        }
        return IntStream.range(0, rounds).mapToDouble(i -> counts[i] == 0 ? Double.NaN : sums[i] / counts[i]).toArray();
    }

    /** Each tower type's share of all the damage {@code player}'s towers landed, averaged over plays. */
    public Map<TowerFactory.Type, Double> damageShares(String player) {
        Map<TowerFactory.Type, Double> shares = new EnumMap<>(TowerFactory.Type.class);
        List<PlayRecord> plays = this.playsOf(player);
        for (PlayRecord play : plays) {
            Map<TowerFactory.Type, Long> damage = new EnumMap<>(TowerFactory.Type.class);
            for (PlayRecord.Round round : play.rounds()) {
                round.pressure().damage().forEach((type, amount) -> damage.merge(type, amount, Long::sum));
            }
            double total = damage.values().stream().mapToLong(Long::longValue).sum();
            if (total > 0) {
                damage.forEach((type, amount) -> shares.merge(type, amount / total / plays.size(), Double::sum));
            }
        }
        return shares;
    }

    /** How many towers of each type {@code player} had standing at the end, averaged over plays. */
    public Map<TowerFactory.Type, Double> towerCounts(String player) {
        Map<TowerFactory.Type, Double> counts = new EnumMap<>(TowerFactory.Type.class);
        List<PlayRecord> plays = this.playsOf(player);
        for (PlayRecord play : plays) {
            play.towers().forEach(tower -> counts.merge(tower.type(), 1.0 / plays.size(), Double::sum));
        }
        return counts;
    }

    /** The share of {@code player}'s plays that bought each node, by node id. */
    public Map<String, Double> nodePickRates(String player) {
        Map<String, Double> rates = new LinkedHashMap<>();
        List<PlayRecord> plays = this.playsOf(player);
        for (PlayRecord play : plays) {
            play.towers().stream().flatMap(tower -> tower.nodes().stream()).distinct()
                    .forEach(node -> rates.merge(node, 1.0 / plays.size(), Double::sum));
        }
        return rates;
    }

    /** Leaks per play by the kind that leaked, for {@code player}. */
    public Map<String, Double> leaksByKind(String player) {
        Map<String, Double> leaks = new LinkedHashMap<>();
        List<PlayRecord> plays = this.playsOf(player);
        for (PlayRecord play : plays) {
            play.ends().stream().filter(PlayRecord.EnemyEnd::leaked)
                    .forEach(end -> leaks.merge(TowerLab.kindKey(end.foe().kind(), end.foe().rank()),
                            1.0 / plays.size(), Double::sum));
        }
        return leaks;
    }

    /** The round each lost play of {@code player} was lost in, counted. */
    public Map<Integer, Integer> lossRounds(String player) {
        Map<Integer, Integer> rounds = new java.util.TreeMap<>();
        for (PlayRecord play : this.playsOf(player)) {
            int lives = play.result().lives();
            int leaked = 0;
            for (PlayRecord.Round round : play.rounds()) {
                leaked += round.pressure().leaked();
                if (leaked >= lives) {
                    rounds.merge(round.number(), 1, Integer::sum);
                    break;
                }
            }
        }
        return rounds;
    }
}
