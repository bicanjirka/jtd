package td.enemy;

import td.util.GameHost;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The live enemy list for the wave currently in play, and how many of them are still
 * alive - reported to the host as each one dies, which is how the UI learns a wave is
 * cleared (see GameHost.enemyDied). Backed by a {@link CopyOnWriteArrayList} - like
 * {@code TowerRoster}/{@code ProjectileRoster} - since {@link #add}/{@link #replace} are
 * called from an enemy's own {@code doTick} on the {@code game-loop} thread (an ability
 * spawning a reinforcement or hatching an egg) while {@link #getEnemies} is read from the EDT
 * for rendering.
 */
public class EnemyRoster implements EnemyRegistry, EnemySpawner {

    private final GameHost host;
    private final List<EnemyMob> enemies = new CopyOnWriteArrayList<>();
    private int count = 0;

    public EnemyRoster(GameHost host) {
        this.host = host;
    }

    @Override
    public EnemyMob[] getEnemies() {
        return this.enemies.toArray(new EnemyMob[0]);
    }

    public void setEnemies(EnemyMob[] enemies) {
        this.enemies.clear();
        this.enemies.addAll(List.of(enemies));
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void remove() {
        this.count--;
        this.host.enemyDied(this.count);
    }

    /** Tearing a level down is not a death: unlike {@link #remove()}, this does not notify the host. */
    public void clear() {
        this.count = 0;
        this.enemies.clear();
    }

    /** Adds a new, independent enemy - the count grows, since it's one more mob to account for. */
    @Override
    public void add(EnemyMob mob) {
        this.enemies.add(mob);
        this.count++;
    }

    /**
     * Removes {@code outgoing} and adds {@code incoming} as one step - the count is unchanged
     * (one out, one in), and {@code outgoing} is not reported to the host: a hatch is a
     * transformation, not a kill, so it earns no bounty/score/kill-count credit.
     */
    @Override
    public void replace(EnemyMob outgoing, EnemyMob incoming) {
        this.enemies.remove(outgoing);
        this.enemies.add(incoming);
    }
}
