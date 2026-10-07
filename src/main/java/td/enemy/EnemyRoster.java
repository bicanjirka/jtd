package td.enemy;

import td.damage.Damage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The current wave's live enemies and how many are still alive, which is how a cleared wave is
 * noticed. Mobs are added and replaced from an enemy's own {@code doTick}, and
 * {@link #getEnemies} returns a fresh copy.
 */
public class EnemyRoster implements EnemyRegistry, EnemySpawner {

    private final AtomicInteger count = new AtomicInteger();
    private final AtomicLong entries = new AtomicLong();
    private final List<WalkEndListener> walkEndListeners = new CopyOnWriteArrayList<>();
    private final List<ShieldListener> shieldListeners = new CopyOnWriteArrayList<>();
    // Swapped whole in one write, so a reader never sees a half-filled roster.
    private volatile List<EnemyMob> enemies = new CopyOnWriteArrayList<>();

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

    /** The mob stays listed for its death fade. Its walk is over, killed or leaked. */
    public void reportDeath(EnemyWalk walk) {
        this.count.decrementAndGet();
        for (WalkEndListener listener : this.walkEndListeners) {
            listener.walkEnded(walk);
        }
    }

    public void addWalkEndListener(WalkEndListener listener) {
        this.walkEndListeners.add(listener);
    }

    /** A shield on {@code enemy} took {@code absorbed} of a hit. */
    public void reportShielded(EnemyMob enemy, Damage absorbed) {
        for (ShieldListener listener : this.shieldListeners) {
            listener.shieldTook(enemy, absorbed);
        }
    }

    public void addShieldListener(ShieldListener listener) {
        this.shieldListeners.add(listener);
    }

    public void removeShieldListener(ShieldListener listener) {
        this.shieldListeners.remove(listener);
    }

    /** A mob going live takes the next place in the entry order. */
    public long recordEntry() {
        return this.entries.incrementAndGet();
    }

    /** How many mobs have gone live so far: a mob whose ordinal is higher entered after this call. */
    public long entries() {
        return this.entries.get();
    }

    public void clear() {
        this.count.set(0);
        this.enemies.clear();
    }

    @Override
    public void add(EnemyMob mob) {
        this.enemies.add(mob);
        this.count.incrementAndGet();
    }

    /** Swaps {@code outgoing} for {@code incoming}. The count is unchanged: a hatch is not a kill. */
    @Override
    public void replace(EnemyMob outgoing, EnemyMob incoming) {
        this.enemies.remove(outgoing);
        this.enemies.add(incoming);
    }
}
