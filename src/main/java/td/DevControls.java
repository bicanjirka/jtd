package td;

import td.damage.Damage;
import td.economy.EconomyDelta;
import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.tower.Tower;
import td.util.GameStartupException;
import td.util.GameWorld;
import td.util.LoadedLevel;
import td.wave.Wave;
import td.wave.WaveContent;
import td.wave.WaveScript;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The dev panel's playtesting cheats, built from the world's public API. Anything that touches
 * enemies or towers is queued and runs on the game-loop thread through {@link #runPending()},
 * which keeps the simulation single-threaded and works while paused. The economy is thread-safe,
 * so credits and lives change at once.
 */
public final class DevControls implements EconomyListener {

    /** Free build holds credits here, so every price is affordable and spending never shows. */
    static final int FREE_BUILD_CREDITS = 1_000_000;

    private final GameWorld world;
    private final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();

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

    /** @return what happened, or why nothing did */
    public String grantXpToSelected(int xp) {
        Optional<Tower> selected = this.selectedTower();
        if (selected.isEmpty()) {
            return "Select a tower first";
        }
        this.pending.add(() -> selected.get().earnXp(xp));
        return "+" + xp + " XP to " + selected.get().getType();
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
