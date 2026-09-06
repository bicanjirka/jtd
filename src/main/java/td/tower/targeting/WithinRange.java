package td.tower.targeting;

import td.enemy.EnemyMob;

/** The one range check every {@link TargetQuery}/{@link NextTargetQuery} implementation needs. */
final class WithinRange {

    private WithinRange() {
    }

    static boolean of(EnemyMob e, int x, int y, float range2) {
        int dx = e.getX() - x;
        int dy = e.getY() - y;
        return dx * dx + dy * dy < range2;
    }
}
