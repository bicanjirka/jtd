package td.tower.targeting;

import td.enemy.EnemyMob;
import td.util.Context;
import td.util.GameHost;

/** Builds a {@link Context} carrying exactly the enemies a targeting test wants to scan. */
final class TestContexts {

    private TestContexts() {
    }

    static Context withEnemies(EnemyMob... enemies) {
        Context context = new Context(new GameHost() {
            @Override
            public void enemyDied(int enemiesLeft) {
            }

            @Override
            public void setInfoText(String s) {
            }

            @Override
            public void clearCell(int x, int y) {
            }
        });
        context.enemies = enemies;
        return context;
    }
}
