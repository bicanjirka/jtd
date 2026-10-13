package td.balance;

import td.tower.TowerFactory;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * A player who tries things before committing. Before each round it rehearses that round and the next the
 * competent player's way; if that leaks, it rehearses a dozen other ways of spending (without upgrades, saving
 * harder, spending everything, only upgrades, or building only one tower type for the round), each a replay of the
 * play so far with that change, and plays the round the way that leaked least. It costs a dozen plays per round, so it is an
 * audit, not a player for every report: where it does much better than the competent player, the competent
 * player's rules are missing something; where it does no better, the level, not the player, decides.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
final class StrongPlayer implements Player {

    /** It rehearses the coming round and this many after it, to see what a choice costs later. */
    private static final int ROUNDS_AFTER = 1;
    private static final double PER_LEAK = 1000;

    private final TowerLab lab;

    private Player current;
    private int roundChosen = -1;

    StrongPlayer(TowerLab lab) {
        this.lab = lab;
        this.current = new CompetentPlayer(lab, CompetentPlayer.Style.standard());
    }

    static PlayerSpec spec(String name, Map<String, TowerLab> labs) {
        return new PlayerSpec(name, (survey, seed) -> new StrongPlayer(labs.get(survey.level().name())));
    }

    /** The ways of spending it tries for a round. */
    static List<CompetentPlayer.Style> candidates() {
        CompetentPlayer.Style standard = CompetentPlayer.Style.standard();
        List<CompetentPlayer.Style> styles = new ArrayList<>();
        styles.add(standard);
        styles.add(standard.withoutUpgrades());
        styles.add(standard.withSaveFactor(0));
        styles.add(standard.withSaveFactor(3));
        styles.add(standard.withTypes(EnumSet.noneOf(TowerFactory.Type.class)));
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            styles.add(standard.withTypes(EnumSet.of(type)));
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
        CompetentPlayer.Style standard = CompetentPlayer.Style.standard();
        double standardScore = this.rehearse(turn, soFar, standard, round, through);
        if (standardScore < PER_LEAK) {
            return new CompetentPlayer(this.lab, standard);
        }
        CompetentPlayer.Style best = standard;
        double bestScore = standardScore;
        for (CompetentPlayer.Style style : candidates()) {
            double score = style.equals(standard) ? standardScore : this.rehearse(turn, soFar, style, round, through);
            if (score < bestScore) {
                bestScore = score;
                best = style;
            }
        }
        return new CompetentPlayer(this.lab, leaks(bestScore) < leaks(standardScore) ? best : standard);
    }

    private static double leaks(double score) {
        return Math.floor(score / PER_LEAK);
    }

    private double rehearse(Turn turn, List<PlayRecord.TimedAction> soFar, CompetentPlayer.Style style, int round,
                            int through) {
        Player trial = new RoundSwitchPlayer(soFar, round, new CompetentPlayer(this.lab, style),
                new CompetentPlayer(this.lab, CompetentPlayer.Style.standard()));
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
}
