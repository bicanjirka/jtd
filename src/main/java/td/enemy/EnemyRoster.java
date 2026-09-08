package td.enemy;

import td.util.GameHost;

/**
 * The live enemy array for the wave currently in play, and how many of them are still
 * alive - reported to the host as each one dies, which is how the UI learns a wave is
 * cleared (see GameHost.enemyDied).
 */
public class EnemyRoster implements EnemyRegistry {

    private final GameHost host;
    private EnemyMob[] enemies = new EnemyMob[0];
    private int count = 0;

    public EnemyRoster(GameHost host) {
        this.host = host;
    }

    @Override
    public EnemyMob[] getEnemies() {
        return this.enemies;
    }

    public void setEnemies(EnemyMob[] enemies) {
        this.enemies = enemies;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void remove() {
        this.count--;
        this.host.enemyDied(this.count);
    }

    public void removeAll() {
        this.count = 0;
        this.enemies = new EnemyMob[0];
        this.host.enemyDied(this.count);
    }
}
