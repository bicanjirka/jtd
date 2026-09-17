package td.enemy;

import td.util.GameHost;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The live enemy list for the wave currently in play, and how many of them are still
 * alive - reported to the host as each one dies, which is how the UI learns a wave is
 * cleared (see GameHost.enemyDied). Backed by a {@link CopyOnWriteArrayList} - like
 * {@code TowerRoster}/{@code ProjectileRoster} - since {@link #add}/{@link #replace} are
 * called from an enemy's own {@code doTick} on the {@code game-loop} thread (an ability
 * spawning a reinforcement or hatching an egg). The frame build reads it on that same thread;
 * the array {@link #getEnemies} hands back is a fresh snapshot either way.
 */
public class EnemyRoster implements EnemyRegistry, EnemySpawner {

    private final GameHost host;
    // Decremented on the game-loop thread as mobs die and reset from the EDT on level load, and
    // the value a decrement produces is what decides "wave cleared" and "you won" - so the
    // decrement and the value reported for it have to be one operation, not two.
    private final AtomicInteger count = new AtomicInteger();
    // Volatile rather than final: setEnemies swaps the whole list in one write, so a reader
    // sees the outgoing wave or the incoming one and never a half-filled roster. Still a
    // CopyOnWriteArrayList, because add/replace mutate it in place from an enemy's own doTick.
    private volatile List<EnemyMob> enemies = new CopyOnWriteArrayList<>();

    public EnemyRoster(GameHost host) {
        this.host = host;
    }

    @Override
    public EnemyMob[] getEnemies() {
        return this.enemies.toArray(new EnemyMob[0]);
    }

    /**
     * Replaces the live list wholesale, as one write. Deliberately not {@code clear()} followed
     * by {@code addAll()}: a CopyOnWriteArrayList makes each of those atomic on its own, but
     * between them a reader sees an <em>empty</em> roster - which, mid-level, reads as "the
     * wave is cleared". A concurrent collection makes the collection safe, not the operation
     * (CLAUDE.md 3).
     */
    public void setEnemies(EnemyMob[] enemies) {
        this.enemies = new CopyOnWriteArrayList<>(List.of(enemies));
    }

    public void setCount(int count) {
        this.count.set(count);
    }

    /**
     * How many of the current wave's mobs are still alive - the same count {@link #reportDeath}
     * decrements and reports to the host, exposed for a caller (a {@code BalanceHarness} run
     * loop) that needs to ask "is anything left" without depending on {@link #getEnemies}'
     * length, which is the wave's slot count for as long as any dead mob is still fading and
     * therefore still in the list.
     */
    public int aliveCount() {
        return this.count.get();
    }

    /**
     * A mob has died (by combat kill or by leaking off the path's end) - not a removal despite
     * the name this replaced: the mob stays in {@link #getEnemies}' list for its death fade,
     * this only decrements the alive count and reports it to the host, which is how the UI
     * learns a wave is cleared.
     */
    public void reportDeath() {
        this.host.enemyDied(this.count.decrementAndGet());
    }

    /**
     * Tearing a level down is not a death: unlike {@link #reportDeath}, this does not notify the host.
     */
    public void clear() {
        this.count.set(0);
        this.enemies.clear();
    }

    /**
     * Adds a new, independent enemy - the count grows, since it's one more mob to account for.
     */
    @Override
    public void add(EnemyMob mob) {
        this.enemies.add(mob);
        this.count.incrementAndGet();
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
