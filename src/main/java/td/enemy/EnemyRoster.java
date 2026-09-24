package td.enemy;

import td.util.GameHost;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The current wave's live enemies and how many are still alive; each death is reported to the host,
 * which is how a cleared wave is noticed. Mobs are added and replaced from an enemy's own
 * {@code doTick}, and {@link #getEnemies} returns a fresh copy.
 */
public class EnemyRoster implements EnemyRegistry, EnemySpawner {

    private final GameHost host;
    // The decrement and the value it reports must be one operation: that value decides
    // "wave cleared" and "won".
    private final AtomicInteger count = new AtomicInteger();
    // Swapped whole in one write, so a reader never sees a half-filled roster.
    private volatile List<EnemyMob> enemies = new CopyOnWriteArrayList<>();

    public EnemyRoster(GameHost host) {
        this.host = host;
    }

    @Override
    public EnemyMob[] getEnemies() {
        return this.enemies.toArray(new EnemyMob[0]);
    }

    /**
     * Replaces the list in one write. Not {@code clear()} then {@code addAll()}: between them a
     * reader sees an empty roster, which reads as a cleared wave.
     */
    public void setEnemies(EnemyMob[] enemies) {
        this.enemies = new CopyOnWriteArrayList<>(List.of(enemies));
    }

    public void setCount(int count) {
        this.count.set(count);
    }

    /** Mobs still alive. Unlike {@link #getEnemies}' length, excludes dead mobs still fading. */
    public int aliveCount() {
        return this.count.get();
    }

    /**
     * Decrements the alive count and reports it to the host. The mob stays listed for its death
     * fade.
     */
    public void reportDeath() {
        this.host.enemyDied(this.count.decrementAndGet());
    }

    /** Tearing a level down is not a death, so the host is not notified. */
    public void clear() {
        this.count.set(0);
        this.enemies.clear();
    }

    @Override
    public void add(EnemyMob mob) {
        this.enemies.add(mob);
        this.count.incrementAndGet();
    }

    /**
     * Swaps {@code outgoing} for {@code incoming}. The count is unchanged and nothing is reported:
     * a hatch is not a kill.
     */
    @Override
    public void replace(EnemyMob outgoing, EnemyMob incoming) {
        this.enemies.remove(outgoing);
        this.enemies.add(incoming);
    }
}
