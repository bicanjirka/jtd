package td.balance;

import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * A player who tries things before committing. Before each round it rehearses that round and the next the
 * competent player's way; if that leaks, it rehearses a dozen other ways of spending (without upgrades, saving
 * harder, spending everything, only upgrades, or building only one tower type for the round), each a replay of the
 * play so far with that change, and plays the round the way that leaked least. Only if every one of those leaks does
 * it rehearse selling: its weakest damage towers for what they fetch, spent again the competent way. It costs a dozen plays per round, so it is an
 * audit, not a player for every report: where it does much better than the competent player, the competent
 * player's rules are missing something; where it does no better, the level, not the player, decides.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
final class StrongPlayer implements Player {

    /** It rehearses the coming round and this many after it, to see what a choice costs later. */
    private static final int ROUNDS_AFTER = 1;
    private static final double PER_LEAK = 1000;
    /** The most towers a selling rehearsal sells before a round. */
    private static final int MOST_SOLD = 2;

    private final TowerLab lab;
    /** The way it plays when nothing leaks, and what every other way it tries is held to. */
    private final CompetentPlayer.Style base;

    private Player current;
    private int roundChosen = -1;

    StrongPlayer(TowerLab lab, CompetentPlayer.Style base) {
        this.lab = lab;
        this.base = base;
        this.current = new CompetentPlayer(lab, base);
    }

    static PlayerSpec spec(String name, Map<String, TowerLab> labs) {
        return spec(name, labs, CompetentPlayer.Style.standard());
    }

    /** A strong player held to {@code base}: the towers and nodes it may buy, whatever way it tries. */
    static PlayerSpec spec(String name, Map<String, TowerLab> labs, CompetentPlayer.Style base) {
        return new PlayerSpec(name, (survey, seed) -> new StrongPlayer(labs.get(survey.level().name()), base));
    }

    /** The ways of spending it tries for a round, each held to {@code base}. */
    static List<CompetentPlayer.Style> candidates(CompetentPlayer.Style base) {
        List<CompetentPlayer.Style> styles = new ArrayList<>();
        styles.add(base);
        styles.add(base.withoutUpgrades());
        styles.add(base.withSaveFactor(0));
        styles.add(base.withSaveFactor(3));
        styles.add(base.withTypes(EnumSet.noneOf(TowerFactory.Type.class)));
        for (TowerFactory.Type type : base.types()) {
            styles.add(base.withTypes(EnumSet.of(type)));
        }
        return styles;
    }

    @Override
    public void act(Turn turn) {
        if (turn.betweenRounds() && turn.round() != this.roundChosen) {
            this.roundChosen = turn.round();
            this.current = this.choose(turn);
        }
        this.current.act(turn);
    }

    /**
     * The competent way, unless it leaks in the rounds rehearsed and another way leaks less. Changing course only to
     * stop a leak keeps it from trading the long game for a closer call it did not need to avoid.
     */
    private Player choose(Turn turn) {
        List<PlayRecord.TimedAction> soFar = turn.actionsSoFar();
        int round = turn.round();
        int through = Math.min(turn.roundCount(), round + ROUNDS_AFTER);
        CompetentPlayer.Style standard = this.base;
        double standardScore = this.rehearse(turn, soFar, standard, round, through);
        if (standardScore < PER_LEAK) {
            return new CompetentPlayer(this.lab, standard);
        }
        List<CompetentPlayer.Style> styles = candidates(this.base);
        // Each rehearsal is a play of its own, so they run side by side; the scores are read back in order.
        double[] scores = styles.parallelStream()
                .mapToDouble(style -> style.equals(standard) ? standardScore
                        : this.rehearse(turn, soFar, style, round, through))
                .toArray();
        CompetentPlayer.Style best = standard;
        double bestScore = standardScore;
        for (int i = 0; i < styles.size(); i++) {
            if (scores[i] < bestScore) {
                bestScore = scores[i];
                best = styles.get(i);
            }
        }
        Player chosen = new CompetentPlayer(this.lab, leaks(bestScore) < leaks(standardScore) ? best : standard);
        double chosenLeaks = Math.min(leaks(bestScore), leaks(standardScore));
        if (chosenLeaks == 0) {
            return chosen;
        }
        // Selling loses a quarter of what was paid, so it is the last resort: tried only when nothing else holds.
        double[] selling = IntStream.rangeClosed(1, MOST_SOLD).parallel()
                .mapToDouble(sold -> this.rehearse(turn, soFar, new SellingPlayer(sold, this.competent()), round,
                        through))
                .toArray();
        for (int sold = 1; sold <= MOST_SOLD && chosenLeaks > 0; sold++) {
            if (leaks(selling[sold - 1]) < chosenLeaks) {
                chosenLeaks = leaks(selling[sold - 1]);
                chosen = new SellingPlayer(sold, this.competent());
            }
        }
        return chosen;
    }

    private Player competent() {
        return new CompetentPlayer(this.lab, this.base);
    }

    private static double leaks(double score) {
        return Math.floor(score / PER_LEAK);
    }

    private double rehearse(Turn turn, List<PlayRecord.TimedAction> soFar, CompetentPlayer.Style style, int round,
                            int through) {
        return this.rehearse(turn, soFar, new CompetentPlayer(this.lab, style), round, through);
    }

    private double rehearse(Turn turn, List<PlayRecord.TimedAction> soFar, Player during, int round, int through) {
        Player trial = new RoundSwitchPlayer(soFar, round, during, this.competent());
        return score(turn.rehearse(trial, through), round);
    }

    /** Lower is better: every leak from {@code round} on outweighs how close the rest came. */
    private static double score(PlayRecord rehearsed, int round) {
        return rehearsed.rounds().stream().filter(played -> played.number() >= round)
                .mapToDouble(played -> played.pressure().leaked() * PER_LEAK + played.pressure().closest())
                .sum();
    }

    /**
     * Replays {@code soFar}, plays round {@code round} with {@code during}, and every round after it with
     * {@code after}.
     */
    @ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
    private static final class RoundSwitchPlayer implements Player {

        private final ScriptedPlayer script;
        private final int round;
        private final Player during;
        private final Player after;

        RoundSwitchPlayer(List<PlayRecord.TimedAction> soFar, int round, Player during, Player after) {
            this.script = new ScriptedPlayer(soFar);
            this.round = round;
            this.during = during;
            this.after = after;
        }

        @Override
        public void act(Turn turn) {
            if (turn.round() < this.round) {
                this.script.act(turn);
            } else if (turn.round() == this.round) {
                this.during.act(turn);
            } else {
                this.after.act(turn);
            }
        }
    }

    /**
     * Before the round, sells the {@code count} damage towers that have dealt least for what they would fetch, then
     * spends the competent way. Support towers are kept: what they give shows in the others' damage, not their own.
     */
    @ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
    private static final class SellingPlayer implements Player {

        private final int count;
        private final Player then;

        private boolean sold;

        SellingPlayer(int count, Player then) {
            this.count = count;
            this.then = then;
        }

        @Override
        public void act(Turn turn) {
            if (!this.sold) {
                this.sold = true;
                turn.towers().stream()
                        .filter(tower -> tower.getType() != TowerFactory.Type.AURA
                                && tower.getType() != TowerFactory.Type.PULSE)
                        .sorted(Comparator.comparingDouble(
                                (Tower tower) -> tower.getDamageDealt() / (double) Math.max(1, tower.getSellPrice())))
                        .limit(this.count)
                        .toList()
                        .forEach(turn::sell);
            }
            this.then.act(turn);
        }
    }
}
