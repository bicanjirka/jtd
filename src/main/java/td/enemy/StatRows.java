package td.enemy;

import td.stat.EnemyStat;

/** Which stats the enemy pane shows a row for. */
public final class StatRows {

    private StatRows() {
    }

    /**
     * Speed always; the three effect resistances and stealth never, since traits and buffs show them; any other
     * stat when its base or its current value is off the default.
     */
    public static boolean shows(EnemyStat stat, float base, float current) {
        return switch (stat) {
            case MOVE_SPEED -> true;
            case CHILL_RESIST, BURN_RESIST, FREEZE_RESIST, STEALTH -> false;
            default -> base != stat.defaultBase() || current != stat.defaultBase();
        };
    }
}
