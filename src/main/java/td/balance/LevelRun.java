package td.balance;

import td.effect.EffectKind;
import td.tower.TowerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

    /** The parts {@link #upgradeShareByThird} splits a level into. */
    public static final int THIRDS = 3;
    /**
     * Two builds sharing at least this share of their towers, by cell and type, are one layout; under it they
     * are different ways to win.
     */
    public static final double SAME_LAYOUT = 0.5;

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

    /** The most enemy health on the board at once in each round, in points, averaged over {@code player}'s plays. */
    public double[] peakOnBoard(String player) {
        int rounds = this.survey().level().paths().getFirst().waves().size();
        double[] sums = new double[rounds];
        List<PlayRecord> plays = this.playsOf(player);
        for (PlayRecord play : plays) {
            long[] peaks = new long[rounds];
            for (PlayRecord.Sample sample : play.watch().timeline()) {
                if (sample.round() >= 1 && sample.round() <= rounds) {
                    peaks[sample.round() - 1] = Math.max(peaks[sample.round() - 1], sample.health());
                }
            }
            for (int r = 0; r < rounds; r++) {
                sums[r] += peaks[r] / (double) plays.size();
            }
        }
        return sums;
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

    /**
     * The share of what {@code player} spent that went into upgrades, in each third of the level's rounds: early,
     * middle and late.
     */
    public double[] upgradeShareByThird(String player) {
        int rounds = this.survey().level().paths().getFirst().waves().size();
        double[] upgrades = new double[THIRDS];
        double[] total = new double[THIRDS];
        for (PlayRecord play : this.playsOf(player)) {
            for (PlayRecord.Round round : play.rounds()) {
                int third = Math.min(THIRDS - 1, (round.number() - 1) * THIRDS / rounds);
                upgrades[third] += round.economy().spent().upgrades();
                total[third] += round.economy().spent().total();
            }
        }
        return IntStream.range(0, THIRDS).mapToDouble(i -> total[i] <= 0 ? Double.NaN : upgrades[i] / total[i])
                .toArray();
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

    /** Every play of every player, the competent player's variants included. */
    private Stream<PlayRecord> allPlays() {
        return this.plays.values().stream().flatMap(List::stream);
    }

    /** The upgrade nodes of every tower type that no play of any player bought, by tower type. */
    public Map<TowerFactory.Type, List<String>> nodesNeverBought() {
        Set<String> bought = this.allPlays().flatMap(play -> play.towers().stream())
                .flatMap(tower -> tower.nodes().stream()).collect(Collectors.toSet());
        Map<TowerFactory.Type, List<String>> never = new EnumMap<>(TowerFactory.Type.class);
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            never.put(type, this.lab.profile(type).nodeFactors().keySet().stream()
                    .filter(node -> !bought.contains(node)).toList());
        }
        return never;
    }

    /** The abilities of the level's enemies no play of any player saw used. */
    public List<String> abilitiesNeverUsed() {
        Set<String> used = this.allPlays().flatMap(play -> play.watch().casts().keySet().stream())
                .collect(Collectors.toSet());
        return this.lab.abilities().stream().filter(ability -> !used.contains(ability)).toList();
    }

    /** The effects no play of any player saw take hold of an enemy. */
    public List<EffectKind> effectsNeverStarted() {
        Set<EffectKind> started = this.allPlays().flatMap(play -> play.watch().effects().starts().keySet().stream())
                .collect(Collectors.toSet());
        return Arrays.stream(EffectKind.values()).filter(kind -> !started.contains(kind)).toList();
    }

    /** {@code player}'s effect use over all its plays together. */
    public PlayRecord.EffectUse effectUse(String player) {
        long enemyTicks = 0;
        Map<EffectKind, Long> ticks = new EnumMap<>(EffectKind.class);
        Map<EffectKind, Integer> starts = new EnumMap<>(EffectKind.class);
        for (PlayRecord play : this.playsOf(player)) {
            PlayRecord.EffectUse use = play.watch().effects();
            enemyTicks += use.enemyTicks();
            use.ticks().forEach((kind, count) -> ticks.merge(kind, count, Long::sum));
            use.starts().forEach((kind, count) -> starts.merge(kind, count, Integer::sum));
        }
        return new PlayRecord.EffectUse(enemyTicks, ticks, starts);
    }

    /** How often per play {@code player}'s enemies used each ability. */
    public Map<String, Double> abilityUses(String player) {
        Map<String, Double> uses = new LinkedHashMap<>();
        List<PlayRecord> plays = this.playsOf(player);
        for (PlayRecord play : plays) {
            play.watch().casts().forEach((ability, count) -> uses.merge(ability, count / (double) plays.size(),
                    Double::sum));
        }
        return uses;
    }

    /**
     * The winning plays of every player, grouped into layouts: a play joins the first group whose first play
     * shares at least {@link #SAME_LAYOUT} of its towers, by cell and type, and starts a group of its own
     * otherwise. Largest group first.
     */
    public List<List<PlayRecord>> winningLayouts() {
        List<List<PlayRecord>> groups = new ArrayList<>();
        this.plays.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .flatMap(entry -> entry.getValue().stream())
                .filter(play -> play.result().won())
                .forEach(play -> groups.stream()
                        .filter(group -> overlap(group.getFirst(), play) >= SAME_LAYOUT)
                        .findFirst()
                        .ifPresentOrElse(group -> group.add(play), () -> groups.add(new ArrayList<>(List.of(play)))));
        groups.sort(Comparator.comparingInt((List<PlayRecord> group) -> group.size()).reversed());
        return groups;
    }

    /** The share of two builds' towers they have in common, by cell and type, over the towers of either. */
    static double overlap(PlayRecord one, PlayRecord other) {
        Set<String> a = layoutOf(one);
        Set<String> b = layoutOf(other);
        Set<String> both = new HashSet<>(a);
        both.retainAll(b);
        Set<String> either = new HashSet<>(a);
        either.addAll(b);
        return either.isEmpty() ? 1 : both.size() / (double) either.size();
    }

    private static Set<String> layoutOf(PlayRecord play) {
        return play.towers().stream().map(tower -> tower.type() + "@" + tower.cell()).collect(Collectors.toSet());
    }

    /** The share of rounds in which {@code player} bought a tower or an upgrade, per play. */
    public Estimate roundsWithAPurchase(String player) {
        return Estimate.of(this.playsOf(player), play -> {
            Set<Integer> rounds = purchaseRounds(play);
            return rounds.size() / (double) Math.max(1, play.result().roundCount());
        });
    }

    /** The most rounds in a row in which {@code player} bought nothing, per play. */
    public Estimate longestWithoutAPurchase(String player) {
        return Estimate.of(this.playsOf(player), play -> {
            Set<Integer> rounds = purchaseRounds(play);
            int longest = 0;
            int run = 0;
            for (int round = 1; round <= play.result().roundCount(); round++) {
                run = rounds.contains(round) ? 0 : run + 1;
                longest = Math.max(longest, run);
            }
            return longest;
        });
    }

    private static Set<Integer> purchaseRounds(PlayRecord play) {
        return play.actions().stream()
                .filter(action -> switch (action.action()) {
                    case PlayerAction.Build ignored -> true;
                    case PlayerAction.Upgrade ignored -> true;
                    case PlayerAction.Sell ignored -> false;
                })
                .map(PlayRecord.TimedAction::round).collect(Collectors.toSet());
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
