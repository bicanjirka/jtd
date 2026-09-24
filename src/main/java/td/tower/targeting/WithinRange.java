package td.tower.targeting;

import td.enemy.EnemyMob;

final class WithinRange {

    private WithinRange() {
    }

    static boolean of(EnemyMob e, int x, int y, float range2) {
        double dx = e.getX() - x;
        double dy = e.getY() - y;
        return dx * dx + dy * dy < range2;
    }
}
