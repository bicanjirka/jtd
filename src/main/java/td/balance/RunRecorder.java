package td.balance;

import td.GameEngine;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyInspection;
import td.enemy.EnemyMob;
import td.level.LevelOutcome;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.util.ThreadConfined;
import td.wave.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Watches a headless play through the engine's public state, polled after every tick: which enemies walked in,
 * how each walk ended and where, what each round cost and earned, and what the towers dealt. It reads nothing a
 * renderer could not.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
final class RunRecorder {

    private static final double PROGRESS_SCALE = 1_000_000.0;

    private final GameEngine engine;
    private final int startingLives;
    // Keyed by identity: a corpse stays in the roster through its fade and must be counted once.
    private final Set<EnemyMob> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<EnemyMob> ended = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Tower, Long> damageCounted = new IdentityHashMap<>();
    private final List<PlayRecord.Round> rounds = new ArrayList<>();
    private final List<PlayRecord.EnemyEnd> ends = new ArrayList<>();
    private final List<PlayRecord.TimedAction> actions = new ArrayList<>();
    private final Map<TowerFactory.Type, Long> roundDamage = new EnumMap<>(TowerFactory.Type.class);

    private boolean roundOpen;
    private int roundStartTick;
    private int creditsAtStart;
    private int livesAtStart;
    private int earned;
    private int spent;
    private long healthIn;
    private int leaked;
    private double closest;
    private int leaks;
    private int cleared;

    RunRecorder(GameEngine engine, int startingLives) {
        this.engine = engine;
        this.startingLives = startingLives;
    }

    /** Call after the tick that started a round: the roster now holds only its enemies. */
    void roundStarted(int tick) {
        this.roundOpen = true;
        this.roundStartTick = tick;
        this.creditsAtStart = this.engine.getGameWorld().economy().getCredits();
        this.livesAtStart = this.engine.getGameWorld().economy().getLives();
        this.earned = 0;
        this.healthIn = 0;
        this.leaked = 0;
        this.closest = 0;
        this.roundDamage.clear();
        this.seen.clear();
        this.ended.clear();
    }

    /** What a player did, and what it cost; a sale costs a negative amount. */
    void acted(int round, int offset, PlayerAction action, int cost) {
        this.actions.add(new PlayRecord.TimedAction(round, offset, action));
        this.spent += cost;
    }

    /** Settles a tower's damage before it leaves the board, so a sale does not lose it. */
    void leaving(Tower tower) {
        this.countDamage(tower);
        this.damageCounted.remove(tower);
    }

    /** The summed full health of every enemy that walked in this round so far. */
    long healthInSoFar() {
        return this.healthIn;
    }

    /** Enemies that have leaked so far. */
    int leaks() {
        return this.leaks;
    }

    void afterTick(int tick) {
        for (EnemyMob mob : this.engine.getGameWorld().enemies().getEnemies()) {
            if (this.ended.contains(mob)) {
                continue;
            }
            PlayRecord.Foe foe = foeOf(mob);
            if (this.seen.add(mob)) {
                this.healthIn += foe.maxHealth();
            }
            EnemyInspection.Fate fate = mob.accept(DefinedEnemyMob::fate);
            if (fate == EnemyInspection.Fate.KILLED || fate == EnemyInspection.Fate.LEAKED) {
                this.end(mob, foe, fate == EnemyInspection.Fate.LEAKED);
            }
        }
        if (this.roundOpen && (this.engine.isWaveReady() || this.engine.outcome().isOver())) {
            this.closeRound(tick);
        }
    }

    private static PlayRecord.Foe foeOf(EnemyMob mob) {
        return mob.accept(defined -> new PlayRecord.Foe(defined.definition().id(), defined.getRank(),
                defined.getMaxHealthPoints(), defined.getBounty()));
    }

    private void end(EnemyMob mob, PlayRecord.Foe foe, boolean leak) {
        this.ended.add(mob);
        double progress = leak ? 1.0 : mob.getProgression() / PROGRESS_SCALE;
        this.ends.add(new PlayRecord.EnemyEnd(foe, this.rounds.size() + 1, leak,
                new PlayRecord.Spot(mob.getX(), mob.getY(), progress)));
        if (leak) {
            this.leaked++;
            this.leaks++;
        } else {
            this.earned += foe.bounty();
        }
        this.closest = Math.max(this.closest, progress);
    }

    private void closeRound(int tick) {
        this.roundOpen = false;
        if (!this.engine.outcome().isOver() || this.engine.outcome() == LevelOutcome.WON) {
            this.cleared++;
        }
        for (Tower tower : this.engine.getTowers()) {
            this.countDamage(tower);
        }
        int livesLost = Math.max(0, this.livesAtStart - this.engine.getGameWorld().economy().getLives());
        PlayRecord.RoundEconomy economy = new PlayRecord.RoundEconomy(this.creditsAtStart, this.earned, this.spent,
                livesLost);
        PlayRecord.RoundPressure pressure = new PlayRecord.RoundPressure(this.healthIn, this.leaked, this.closest,
                this.roundDamage);
        this.rounds.add(new PlayRecord.Round(this.rounds.size() + 1, tick - this.roundStartTick, economy, pressure));
        this.spent = 0;
    }

    private void countDamage(Tower tower) {
        long now = tower.getDamageDealt();
        long before = this.damageCounted.getOrDefault(tower, 0L);
        this.damageCounted.put(tower, now);
        if (this.roundOpen || now != before) {
            this.roundDamage.merge(tower.getType(), now - before, Long::sum);
        }
    }

    PlayRecord finish(PlayRecord.Play play) {
        int scale = this.engine.getGameWorld().getBoard().scale();
        List<PlayRecord.TowerLog> towers = this.engine.getTowers().stream()
                .map(tower -> new PlayRecord.TowerLog(tower.getType(),
                        new Point(tower.getX() / scale, tower.getY() / scale),
                        new PlayRecord.TowerScore(tower.getDamageDealt(), tower.getKillCount(), tower.experience().xp()),
                        tower.upgrades().owned().stream().map(UpgradeNode::id).toList()))
                .toList();
        return new PlayRecord(play, new PlayRecord.Result(this.startingLives, this.leaks, this.cleared,
                this.engine.getWaveCount()), this.rounds, new PlayRecord.Log(this.ends, this.actions, towers));
    }
}
