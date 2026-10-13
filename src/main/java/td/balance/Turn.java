package td.balance;

import td.GameEngine;
import td.cell.Cell;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.util.RandomSource;
import td.util.ThreadConfined;
import td.wave.Point;
import td.wave.Wave;
import td.wave.WaveProgress;

import java.util.List;
import java.util.Optional;

/**
 * What a player sees and does at one moment of a play: the board, the credits, the round coming or running, and
 * the three things a player can do, each through the same input path a click takes. Every action that goes
 * through is logged with when it happened.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class Turn {

    private final GameEngine engine;
    private final RunRecorder recorder;
    private final BoardSurvey survey;
    private final RandomSource random;
    private final Rehearsal rehearsal;

    private int round;
    private int offset;

    Turn(GameEngine engine, RunRecorder recorder, BoardSurvey survey, RandomSource random, Rehearsal rehearsal) {
        this.engine = engine;
        this.recorder = recorder;
        this.survey = survey;
        this.random = random;
        this.rehearsal = rehearsal;
    }

    /** Plays the same level again from its first tick, as far as a round, with another player deciding. */
    @FunctionalInterface
    interface Rehearsal {

        PlayRecord play(Player player, int throughRound);
    }

    /**
     * What would happen if the play so far were played again by {@code alternative}, through round
     * {@code throughRound}: the same seed and rules, so a player that replays its own actions first and then
     * changes course sees exactly what the change does. Costs a play of its own.
     */
    public PlayRecord rehearse(Player alternative, int throughRound) {
        return this.rehearsal.play(alternative, throughRound);
    }

    /** Everything done so far, in order: what a rehearsal replays first. */
    public List<PlayRecord.TimedAction> actionsSoFar() {
        return this.recorder.actionsSoFar();
    }

    void at(int round, int offset) {
        this.round = round;
        this.offset = offset;
    }

    /** The round coming (between rounds) or running, 1-based. */
    public int round() {
        return this.round;
    }

    public int roundCount() {
        return this.engine.getWaveCount();
    }

    /** Whether the round has not started yet. */
    public boolean betweenRounds() {
        return this.offset == PlayRecord.TimedAction.BEFORE_ROUND;
    }

    /** Ticks since the round started; -1 before it. */
    public int offset() {
        return this.offset;
    }

    /** Every path's wave in the round coming or running: what the wave preview shows. */
    public List<Wave> waves() {
        WaveProgress progress = this.engine.waveProgress();
        return this.betweenRounds() ? progress.next() : progress.current();
    }

    /** The waves of round {@code number} (1-based), or none past the last. */
    public List<Wave> wavesOf(int number) {
        if (number < 1 || number > this.roundCount()) {
            return List.of();
        }
        return this.engine.getGameWorld().level().wavesAt(number - 1);
    }

    /** What the preview would show of enemy health: 1 unless the rules make enemies tougher or weaker. */
    public float enemyHealthScale() {
        return this.engine.getGameWorld().playtestRules().enemyHealthScale();
    }

    public int credits() {
        return this.engine.getGameWorld().economy().getCredits();
    }

    public int lives() {
        return this.engine.getGameWorld().economy().getLives();
    }

    public BoardSurvey survey() {
        return this.survey;
    }

    /** The player's own randomness, apart from the game's, so a choice never shifts what the game rolls. */
    public RandomSource random() {
        return this.random;
    }

    public List<Tower> towers() {
        return this.engine.getTowers();
    }

    /** What the next {@code type} costs, with the copies already built. */
    public int priceOf(TowerFactory.Type type) {
        return this.engine.getGameWorld().towers().priceOf(type);
    }

    /** Whether a tower can go on {@code cell} now. */
    public boolean isFree(Point cell) {
        Cell at = this.engine.cells().at(cell.x(), cell.y());
        return at.buildable() && !at.hasTower();
    }

    /** The tower on {@code cell}, if any. */
    public Optional<Tower> towerAt(Point cell) {
        Cell at = this.engine.cells().at(cell.x(), cell.y());
        return at.hasTower() ? Optional.of(at.getTower()) : Optional.empty();
    }

    /** Offered nodes {@code tower} could buy right now: XP and gate met, price aside. */
    public List<UpgradeNode> buyable(Tower tower) {
        return tower.offeredUpgrades(this.engine.getGameWorld()).stream()
                .filter(node -> node.gateMet(tower, this.engine.getGameWorld()))
                .toList();
    }

    /** Whether {@code tower} has the XP and meets the gate for {@code node}, whether or not it is offered yet. */
    public boolean gateMet(Tower tower, UpgradeNode node) {
        return node.gateMet(tower, this.engine.getGameWorld());
    }

    /** Builds {@code type} on {@code cell} if it is free and affordable. */
    public Optional<Tower> build(TowerFactory.Type type, Point cell) {
        int before = this.credits();
        Optional<Tower> built = place(this.engine, type, cell);
        built.ifPresent(tower -> this.recorder.acted(this.round, this.offset, new PlayerAction.Build(type, cell),
                before - this.credits()));
        return built;
    }

    /** Buys {@code node} on {@code tower} if it is offered, its gate met and affordable. */
    public boolean upgrade(Tower tower, UpgradeNode node) {
        int before = this.credits();
        if (!tower.buyUpgrade(node)) {
            return false;
        }
        this.recorder.acted(this.round, this.offset, new PlayerAction.Upgrade(this.cellOf(tower), node.id()),
                before - this.credits());
        return true;
    }

    public void sell(Tower tower) {
        int before = this.credits();
        Point cell = this.cellOf(tower);
        this.recorder.leaving(tower);
        this.engine.getGameWorld().towers().sell(tower);
        this.recorder.acted(this.round, this.offset, new PlayerAction.Sell(cell), before - this.credits());
    }

    /** Does {@code action}; whether it went through. */
    public boolean perform(PlayerAction action) {
        return switch (action) {
            case PlayerAction.Build build -> this.build(build.type(), build.cell()).isPresent();
            case PlayerAction.Upgrade upgrade -> this.towerAt(upgrade.cell())
                    .flatMap(tower -> tower.upgradeTree().nodes().stream()
                            .filter(node -> node.id().equals(upgrade.nodeId())).findFirst()
                            .filter(node -> this.upgrade(tower, node)))
                    .isPresent();
            case PlayerAction.Sell sell -> {
                Optional<Tower> tower = this.towerAt(sell.cell());
                tower.ifPresent(this::sell);
                yield tower.isPresent();
            }
        };
    }

    public Point cellOf(Tower tower) {
        return new Point(tower.getX() / this.survey.scale(), tower.getY() / this.survey.scale());
    }

    /**
     * Builds through the engine's own click handling, so the price rules apply. A click always leaves placement
     * mode, so success is read off the cell.
     */
    static Optional<Tower> place(GameEngine engine, TowerFactory.Type type, Point cell) {
        int scale = engine.getGameWorld().getBoard().scale();
        Cell at = engine.cells().at(cell.x(), cell.y());
        if (!at.buildable() || at.hasTower()) {
            return Optional.empty();
        }
        engine.startPlacing(type, 0f);
        engine.mouseClicked(cell.x() * scale + scale / 2, cell.y() * scale + scale / 2);
        return at.hasTower() ? Optional.of(at.getTower()) : Optional.empty();
    }
}
