package td.wave;

import td.enemy.EnemyFactory;

import java.util.List;
import java.util.Set;

/**
 * The parsed result of a wave's token string (see the wave mini-language table in
 * CLAUDE.md) - one {@link EnemyFactory.Enemy} per spawn slot, in spawn order, with repeat
 * counts already flattened out. An Empty token keeps its own slot since it counts toward
 * spawn timing, not toward {@link #enemyCount()}. Deliberately GameWorld-free, unlike
 * {@link Wave} itself, which turns this into live {@code EnemyMob}s bound to a world.
 */
public record WaveContent(List<EnemyFactory.Enemy> spawnSequence) {

    public WaveContent {
        spawnSequence = List.copyOf(spawnSequence);
    }

    public Set<EnemyFactory.Enemy> enemySet() {
        return Set.copyOf(this.spawnSequence);
    }

    public int enemyCount(EnemyFactory.Enemy enemy) {
        return (int) this.spawnSequence.stream().filter(e -> e == enemy).count();
    }

    /** The real enemy count - every spawn slot except the Empty spacer. */
    public int enemyCount() {
        return this.spawnSequence.size() - this.enemyCount(EnemyFactory.Enemy.Empty);
    }
}
