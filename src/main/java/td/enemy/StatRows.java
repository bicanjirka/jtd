package td.enemy;

import td.stat.EnemyStat;

/** Which stats the enemy pane shows a row for. */
public final class StatRows {

    private StatRows() {
    }

    /**
     * Speed always; stealth never, since its buff icon shows it; any other stat when its base or its current value
     * is off the default.
     */
    public static boolean shows(EnemyStat stat, float base, float current) {
        boolean offDefault = base != stat.defaultBase() || current != stat.defaultBase();
        return switch (stat) {
            case MOVE_SPEED -> true;
            case STEALTH -> false;
            case ARMOR, MAGIC_RESIST, PLATING, INSULATION, PHYSICAL_DAMAGE_TAKEN, MAGIC_DAMAGE_TAKEN, RESILIENCE,
                 AGILITY, PERIODIC_DAMAGE_TAKEN, SPIRIT, REGENERATION, CHILL_RESIST, BURN_RESIST,
                 FREEZE_RESIST -> offDefault;
        };
    }
}
