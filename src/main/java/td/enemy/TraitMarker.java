package td.enemy;

import td.stat.EnemyStat;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The glyph drawn for a {@link Trait} in the marker row below a mob, and the stats whose own
 * display already says what such a trait does, so a trait and its stats show as one entry.
 */
public enum TraitMarker {
    PERCENT_RESIST(EnemyStat.ARMOR, EnemyStat.MAGIC_RESIST),
    PHYSICAL_RESIST(EnemyStat.ARMOR),
    MAGIC_RESIST(EnemyStat.MAGIC_RESIST),
    FLAT_RESIST(EnemyStat.PHYSICAL_PLATING, EnemyStat.MAGIC_PLATING),
    CRITICAL_IMMUNE(EnemyStat.RESILIENCE),
    HURT_SPEED,
    BURN_IMMUNE(EnemyStat.BURN_RESIST),
    FREEZE_IMMUNE(EnemyStat.FREEZE_RESIST),
    EFFECT_RESIST(EnemyStat.CHILL_RESIST, EnemyStat.BURN_RESIST, EnemyStat.FREEZE_RESIST);

    private final Set<EnemyStat> shownAs;

    TraitMarker(EnemyStat... shownAs) {
        this.shownAs = shownAs.length == 0 ? EnumSet.noneOf(EnemyStat.class) : EnumSet.copyOf(List.of(shownAs));
    }

    public boolean isShownAs(EnemyStat stat) {
        return this.shownAs.contains(stat);
    }
}
