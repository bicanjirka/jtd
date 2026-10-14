package td.balance;

import td.effect.EffectKind;
import td.enemy.Rank;
import td.tower.TowerFactory;
import td.wave.Point;

import java.util.List;
import java.util.Map;

/**
 * Everything one play left behind: how it ended, each round, and its log. Built once by
 * {@link RunRecorder#finish}.
 *
 * @param play   which level, seed and player
 * @param result how it ended
 * @param rounds one entry per round started, in order
 * @param log    each enemy's end, what the player did, and the towers left standing
 */
public record PlayRecord(Play play, Result result, List<Round> rounds, Log log) {

    public PlayRecord {
        rounds = List.copyOf(rounds);
    }

    /** Every enemy whose walk ended, killed or leaked. */
    public List<EnemyEnd> ends() {
        return this.log.ends();
    }

    /** What the player did, with when. */
    public List<TimedAction> actions() {
        return this.log.actions();
    }

    /** The towers on the board when it ended. */
    public List<TowerLog> towers() {
        return this.log.towers();
    }

    /** What the enemies went through. */
    public Watch watch() {
        return this.log.watch();
    }

    /** The play's detail, kept apart from its summary. */
    public record Log(List<EnemyEnd> ends, List<TimedAction> actions, List<TowerLog> towers, Watch watch) {

        public Log {
            ends = List.copyOf(ends);
            actions = List.copyOf(actions);
            towers = List.copyOf(towers);
        }
    }

    /**
     * What the enemies went through over the play: the effects they carried, the abilities they used, and the
     * board every few ticks.
     *
     * @param effects  how long each effect was on an enemy, and how often one took hold
     * @param casts    how often each kind at each rank used each ability, keyed by {@link #castKey} or
     *                 {@link #spawnKey}
     * @param timeline the board sampled every {@link Sample#EVERY} ticks, in order
     */
    public record Watch(EffectUse effects, Map<String, Integer> casts, List<Sample> timeline) {

        public Watch {
            casts = Map.copyOf(casts);
            timeline = List.copyOf(timeline);
        }

        /** An ability of {@code kind} that puts {@code effect} on itself or its allies. */
        public static String castKey(String kindKey, EffectKind effect) {
            return kindKey + " casts " + effect.name().toLowerCase(java.util.Locale.ROOT);
        }

        /** An ability that brings {@code child} onto the board, whichever enemy has it. */
        public static String spawnKey(String child) {
            return "spawns " + child;
        }
    }

    /**
     * @param enemyTicks ticks enemies spent on the board alive, summed over every enemy
     * @param ticks      of those, the ticks each effect was on the enemy
     * @param starts     how often each effect took hold of an enemy that did not have it
     */
    public record EffectUse(long enemyTicks, Map<EffectKind, Long> ticks, Map<EffectKind, Integer> starts) {

        public EffectUse {
            ticks = Map.copyOf(ticks);
            starts = Map.copyOf(starts);
        }

        /** The share of enemy time {@code kind} was on an enemy. */
        public double uptime(EffectKind kind) {
            return this.enemyTicks == 0 ? 0 : this.ticks.getOrDefault(kind, 0L) / (double) this.enemyTicks;
        }
    }

    /** Which play this was. */
    public record Play(String level, long seed, String player) {
    }

    /**
     * How the play ended. Lives never ran out during it, so this says both whether the real level was held and by
     * how much it was not.
     *
     * @param lives         the level's real lives
     * @param leaks         enemies that leaked over the whole play, past the last real life too
     * @param roundsCleared rounds the board was emptied of
     * @param roundCount    rounds the level has
     */
    public record Result(int lives, int leaks, int roundsCleared, int roundCount) {

        /** Every round cleared before the leaks used up the real lives. */
        public boolean won() {
            return this.leaks < this.lives && this.roundsCleared == this.roundCount;
        }

        /** Lives the real level would have taken, never more than it had. */
        public int livesLost() {
            return Math.min(this.lives, this.leaks);
        }
    }

    /**
     * One round, from the tick it started to the tick its board was empty or the level ended.
     *
     * @param number   1-based
     * @param ticks    how long it lasted
     * @param economy  credits and lives across it
     * @param pressure how hard it pushed
     */
    public record Round(int number, int ticks, RoundEconomy economy, RoundPressure pressure) {
    }

    /**
     * @param creditsAtStart unspent when the round started: what sat idle through it
     * @param earned         bounty paid by its kills
     * @param spent          what went into towers and upgrades since the round before it ended
     * @param livesLost      lives its leaks cost
     */
    public record RoundEconomy(int creditsAtStart, int earned, Spending spent, int livesLost) {
    }

    /**
     * @param towers   credits spent on new towers, sales taken off
     * @param upgrades credits spent on upgrades
     * @param wanted   the price of what the player would have bought next had it had the credits when the round
     *                 ended, 0 if it wanted nothing it could not afford or does not say
     */
    public record Spending(int towers, int upgrades, int wanted) {

        public int total() {
            return this.towers + this.upgrades;
        }
    }

    /**
     * @param healthIn  the summed full health of every enemy that walked in, spawns included
     * @param leaked    enemies that reached the end
     * @param closest   how far along its path the furthest enemy got before it died, 0 to 1; 1 once one leaked
     * @param damage    damage landed per tower type
     */
    public record RoundPressure(long healthIn, int leaked, double closest, Map<TowerFactory.Type, Long> damage) {

        public RoundPressure {
            damage = Map.copyOf(damage);
        }
    }

    /**
     * The board at one tick.
     *
     * @param round   the round running or coming, 1-based
     * @param walking the enemies walking
     * @param credits unspent
     */
    public record Sample(int tick, int round, Walking walking, int credits) {

        /** Ticks between two samples: a third of a second. */
        public static final int EVERY = 20;

        public long health() {
            return this.walking.health();
        }

        public double furthest() {
            return this.walking.furthest();
        }
    }

    /**
     * @param health   the health left on every enemy walking, in points
     * @param furthest how far along its path the furthest of them is, 0 to 1
     */
    public record Walking(long health, double furthest) {
    }

    /** One enemy as it was when its walk ended. */
    public record Foe(String kind, Rank rank, int maxHealth, int bounty) {
    }

    /**
     * Where and how an enemy's walk ended.
     *
     * @param progress how far along its path, 0 to 1
     */
    public record Spot(double x, double y, double progress) {
    }

    /** One enemy's walk, ended in a kill or a leak, in round {@code round}. */
    public record EnemyEnd(Foe foe, int round, boolean leaked, Spot spot) {
    }

    /**
     * An action and when it was taken: before round {@code round} started ({@code offset} -1), or {@code offset}
     * ticks into it. Counting from the round rather than from the level start lets a script replay on a changed
     * game, whose rounds end at other ticks.
     */
    public record TimedAction(int round, int offset, PlayerAction action) {

        public static final int BEFORE_ROUND = -1;

        public String toLine() {
            return this.round + " " + this.offset + " " + this.action.toLine();
        }

        public static TimedAction parse(String line) {
            String[] words = line.trim().split("\\s+", 3);
            return new TimedAction(Integer.parseInt(words[0]), Integer.parseInt(words[1]),
                    PlayerAction.parse(words[2]));
        }

        /** Whether this comes no later than {@code round} at {@code offset}. */
        boolean dueBy(int round, int offset) {
            return this.round < round || (this.round == round && this.offset <= offset);
        }
    }

    /**
     * A tower standing at the end.
     *
     * @param nodes the ids of the upgrade nodes it owns, in the order bought
     */
    public record TowerLog(TowerFactory.Type type, Point cell, TowerScore score, List<String> nodes) {

        public TowerLog {
            nodes = List.copyOf(nodes);
        }
    }

    /** What a tower did over the whole play. */
    public record TowerScore(long damage, int kills, int xp) {
    }
}
