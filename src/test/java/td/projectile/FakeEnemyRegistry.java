package td.projectile;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

final class FakeEnemyRegistry implements EnemyRegistry {

    private final EnemyMob[] enemies;

    FakeEnemyRegistry(EnemyMob... enemies) {
        this.enemies = enemies;
    }

    @Override
    public EnemyMob[] getEnemies() {
        return this.enemies;
    }
}
