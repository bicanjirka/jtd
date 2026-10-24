package td;

import td.damage.AttackOrigin;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.economy.EconomyDelta;
import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.effect.DamageSink;
import td.effect.EffectKind;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyMob;
import td.enemy.EnemyMobVisitor;
import td.enemy.Rank;
import td.tower.Tower;
import td.tower.TowerTerm;
import td.util.GameStartupException;
import td.util.GameWorld;
import td.util.LoadedLevel;
import td.wave.Wave;
import td.wave.WaveContent;
import td.wave.WaveScript;
import td.wave.WaveScriptSuggestions;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * The dev panel's playtesting cheats, built from the world's public API. Anything that touches
 * enemies or towers is queued and runs on the game-loop thread through {@link #runPending()},
 * which keeps the simulation single-threaded and works while paused. The economy is thread-safe,
 * so credits and lives change at once.
 */
public final class DevControls implements EconomyListener {

    /** Free build holds credits here, so every price is affordable and spending never shows. */
    static final int FREE_BUILD_CREDITS = 1_000_000;

    private static final EnemyMobVisitor<DefinedEnemyMob> AS_DEFINED = mob -> mob;

    private final GameWorld world;
    /** The panel as an attacker: what its effects deal is periodic, so it never crits or spends a mark. */
    private final AttackOrigin origin = AttackOrigin.fresh();
    private final AttackProfile attack = AttackProfile.none().asPeriodic().withOrigin(this.origin);
    private final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();

    private volatile DevSelection selection = DevSelection.none();
    private volatile Report report = new Report(0L, "");
    private volatile boolean infiniteLives;
    private volatile int livesFloor;
    private volatile boolean freeBuild;
    private volatile long spawnSeed;

    public DevControls(GameWorld world) {
        this.world = world;
    }

    public void setCredits(int credits) {
        this.world.economy().apply(EconomyDelta.credits(credits - this.world.economy().getCredits()));
    }

    /** With infinite lives on, this also becomes the count they are topped back up to. */
    public void setLives(int lives) {
        this.livesFloor = lives;
        this.world.economy().apply(EconomyDelta.lives(lives - this.world.economy().getLives()));
    }

    /** Lives lost while this is on come straight back, so the level never ends in a loss. */
    public void setInfiniteLives(boolean on) {
        this.livesFloor = Math.max(1, this.world.economy().getLives());
        this.infiniteLives = on;
    }

    public void setFreeBuild(boolean on) {
        this.freeBuild = on;
        if (on) {
            this.topUp(this.world.economy().state());
        }
    }

    public void setUpgradeGatesIgnored(boolean ignored) {
        this.world.playtestRules().setUpgradeGatesIgnored(ignored);
    }

    /**
     * Enemies that walk in from now on get {@code percent} of their authored health; those on the
     * board keep theirs.
     *
     * @return what happened, or why nothing did
     */
    public String setEnemyHealthPercent(int percent) {
        if (percent <= 0) {
            return "Enemy health must be above 0%";
        }
        this.world.playtestRules().setEnemyHealthPercent(percent);
        return "New enemies get " + percent + "% health";
    }

    /**
     * Runs on whichever thread changed the economy, so a leak is refunded before the tick that
     * caused it checks for a loss.
     */
    @Override
    public void economyChanged(EconomyState state) {
        this.topUp(state);
    }

    private void topUp(EconomyState state) {
        if (this.infiniteLives && state.lives() < this.livesFloor) {
            this.world.economy().apply(EconomyDelta.lives(this.livesFloor - state.lives()));
        }
        if (this.freeBuild && state.credits() < FREE_BUILD_CREDITS) {
            this.world.economy().apply(EconomyDelta.credits(FREE_BUILD_CREDITS - state.credits()));
        }
    }

    /**
     * Parses {@code script} in the level's wave language for path {@code pathIndex}, without
     * spawning anything: the dev panel previews it as it is typed.
     */
    public WaveScriptCheck checkWaveScript(String script, Rank defaultRank, int pathIndex) {
        LoadedLevel level = this.world.level();
        if (!level.isLoaded()) {
            return WaveScriptCheck.problem("Load a level first");
        }
        WaveContent content;
        try {
            content = WaveScript.parse(script, defaultRank, level.catalog());
        } catch (GameStartupException e) {
            // Typed input, so a script that doesn't parse is reported, not fatal.
            return WaveScriptCheck.problem(e.getMessage());
        }
        if (content.enemyCount() == 0) {
            return WaveScriptCheck.problem("Nothing to spawn");
        }
        int path = Math.floorMod(pathIndex, level.pathCount());
        return WaveScriptCheck.of(new Wave(this.world, content, ++this.spawnSeed, path, 1f));
    }

    /** What could be typed at {@code caret} of a wave script, in the installed level's catalog. */
    public WaveScriptSuggestions.Result suggestWaveScript(String script, int caret) {
        LoadedLevel level = this.world.level();
        if (!level.isLoaded()) {
            return WaveScriptSuggestions.Result.none();
        }
        return WaveScriptSuggestions.suggest(script, caret, level.catalog());
    }

    /**
     * Queues {@code script}'s enemies on path {@code pathIndex}, alongside whatever wave is
     * running.
     *
     * @return what happened, or why nothing did
     */
    public String spawnWave(String script, Rank defaultRank, int pathIndex) {
        WaveScriptCheck check = this.checkWaveScript(script, defaultRank, pathIndex);
        check.wave().ifPresent(wave -> this.pending.add(() -> {
            for (EnemyMob mob : wave.spawn()) {
                this.world.enemies().add(mob);
            }
        }));
        return check.wave().map(wave -> "Spawning " + wave.enemyCount() + " on path " + (wave.getPathIndex() + 1))
                .orElse(check.problem());
    }

    /** Kills every enemy on the board through the normal hit, so bounty and XP are paid. */
    public void killAll() {
        this.pending.add(() -> {
            for (EnemyMob mob : this.world.enemies().getEnemies()) {
                if (!mob.isDead()) {
                    mob.doDamage(Damage.physical(Integer.MAX_VALUE / 2));
                }
            }
        });
    }

    /** Removes every enemy, waiting ones included, paying nothing. A running wave counts as cleared. */
    public void clearAll() {
        this.pending.add(() -> this.world.enemies().clear());
    }

    /** What the selection row works on, as of the last {@link #runPending}. */
    public DevSelection selection() {
        return this.selection;
    }

    /**
     * {@code values} fill the kind's {@link DevEffects#enemyFields fields}, in order. Whether the effect took hold is
     * known only once it runs, so that comes as a {@link #report()}.
     *
     * @return what was queued, or why nothing was
     */
    public String applyEffectToEnemy(EffectKind kind, List<Float> values) {
        Optional<String> unusable = DevEffects.whyUnusable(kind);
        if (unusable.isPresent()) {
            return kind.title() + " " + unusable.get();
        }
        Optional<String> problem = problemWith(DevEffects.enemyFields(kind), values);
        if (problem.isPresent()) {
            return problem.get();
        }
        return this.onSelectedEnemy("Applying " + kind.title(), mob -> {
            DamageSink sink = damage -> mob.doDamage(damage, this.attack);
            DevEffects.enemyEffects(kind, values, sink, this.origin).forEach(mob::applyEffect);
            this.report(mob.hasEffect(kind) ? "Applied " + kind.title()
                    : kind.title() + " didn't take hold: resisted, immune or kept out");
        });
    }

    /** @return what happened, or why nothing did */
    public String clearEnemyEffects() {
        return this.onSelectedEnemy("Cleared its effects", DefinedEnemyMob::clearEffects);
    }

    /** Through the normal hit, so bounty and XP are paid. */
    public String killSelectedEnemy() {
        return this.onSelectedEnemy("Killed it", mob -> mob.doDamage(Damage.physical(Integer.MAX_VALUE / 2)));
    }

    public String healSelectedEnemy() {
        return this.onSelectedEnemy("Healed it to full", DefinedEnemyMob::restoreHealth);
    }

    /** Takes it off the board and pays nothing. */
    public String removeSelectedEnemy() {
        return this.onSelectedEnemy("Removed it", this.world.enemies()::remove);
    }

    /**
     * {@code values} fill the buff's {@link DevEffects#buffFields fields}, in order.
     *
     * @return what happened, or why nothing did
     */
    public String grantBuffToSelectedTower(TowerTerm buff, List<Float> values) {
        if (this.selection.kind() != DevSelection.Kind.TOWER) {
            return "Select a tower first";
        }
        Optional<String> problem = problemWith(DevEffects.buffFields(buff), values);
        if (problem.isPresent()) {
            return problem.get();
        }
        this.pending.add(() -> this.selectedTower()
                .ifPresent(tower -> tower.receiveStatus(DevEffects.towerBuff(buff, values))));
        return "Granted " + buff.title() + " to " + this.selection.description();
    }

    /** The first value that doesn't fit its field, or a count that doesn't match. */
    private static Optional<String> problemWith(List<DevField> fields, List<Float> values) {
        if (values.size() != fields.size()) {
            return Optional.of("Expected " + fields.size() + " values, got " + values.size());
        }
        return IntStream.range(0, fields.size()).mapToObj(i -> fields.get(i).problemWith(values.get(i)))
                .flatMap(Optional::stream).findFirst();
    }

    private String onSelectedEnemy(String done, Consumer<DefinedEnemyMob> change) {
        if (this.selection.kind() != DevSelection.Kind.ENEMY) {
            return "Select an enemy first";
        }
        this.pending.add(() -> this.selectedEnemy().ifPresent(change));
        return done;
    }

    /** Game-loop thread only: the enemy selection belongs to it. */
    private Optional<DefinedEnemyMob> selectedEnemy() {
        return this.world.enemySelection().selectedAlive().map(mob -> mob.accept(AS_DEFINED));
    }

    private DevSelection currentSelection() {
        Optional<Tower> tower = this.selectedTower();
        if (tower.isPresent()) {
            return DevSelection.tower(tower.get().displayName());
        }
        return this.selectedEnemy().map(mob -> DevSelection.enemy(mob.definition().displayName()))
                .orElse(DevSelection.none());
    }

    /** @return what happened, or why nothing did */
    public String grantXpToSelected(int xp) {
        if (this.selection.kind() != DevSelection.Kind.TOWER) {
            return "Select a tower first";
        }
        this.pending.add(() -> this.selectedTower().ifPresent(tower -> tower.earnXp(xp)));
        return "+" + xp + " XP to " + this.selection.description();
    }

    /** What the last queued change turned out to do, once the game-loop thread ran it. */
    public Report report() {
        return this.report;
    }

    /** Game-loop thread only, the one writer. */
    private void report(String text) {
        this.report = new Report(this.report.serial() + 1, text);
    }

    /** The installed level's paths, at least one: the spawn box picks among them. */
    public int pathCount() {
        return Math.max(1, this.world.level().pathCount());
    }

    public Optional<Tower> selectedTower() {
        return this.world.towers().all().stream().filter(Tower::isSelected).findFirst();
    }

    /** Runs the queued changes. Call on the game-loop thread only. */
    public void runPending() {
        for (Runnable change = this.pending.poll(); change != null; change = this.pending.poll()) {
            change.run();
        }
        this.selection = this.currentSelection();
    }

    /**
     * One line saying what a queued change did; {@code serial} grows with every report, so the same words twice
     * still read as news.
     */
    public record Report(long serial, String text) {
    }

    /** A wave script's parse: the wave it would spawn, or the problem that stops it. */
    public record WaveScriptCheck(Optional<Wave> wave, String problem) {

        static WaveScriptCheck of(Wave wave) {
            return new WaveScriptCheck(Optional.of(wave), "");
        }

        static WaveScriptCheck problem(String problem) {
            return new WaveScriptCheck(Optional.empty(), problem);
        }
    }
}
