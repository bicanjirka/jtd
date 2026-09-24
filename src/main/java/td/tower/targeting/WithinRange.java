package td.tower.targeting;

import td.enemy.EnemyTarget;

final class WithinRange {

    private WithinRange() {
    }

    static boolean of(EnemyTarget e, int x, int y, float range2) {
        double dx = e.getX() - x;
        double dy = e.getY() - y;
        return dx * dx + dy * dy < range2;
    }
}
