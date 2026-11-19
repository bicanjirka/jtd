package td.tower.aura;

import td.enemy.EnemyMob;
import td.tower.targeting.MostNeighboursSelector;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.SoonestOutSelector;
import td.tower.targeting.TargetSelector;

import java.util.Comparator;
import java.util.Optional;

/** Which enemy in the light the lantern's beam holds: the Tier I Spotlight pick. */
public enum Spotlight {
    /** No beam. */
    NONE,
    /** The highest rank, then the most health. */
    SEARCHLIGHT,
    /** The one nearest its exit. */
    LAST_LIGHT,
    /** One that gives its allies an aura, else the one with the most allies near it. */
    RINGLEADER;

    /** How near, in cells, an ally counts for the Ringleader. */
    public static final float PACK_CELLS = 1f;

    /** Whether the light holds an enemy at all. */
    public boolean isActive() {
        return this != NONE;
    }

    /** What picks the held enemy among those in the light, on a board of {@code cellPixels}. */
    public TargetSelector selector(float cellPixels) {
        return switch (this) {
            case NONE -> _ -> Optional.empty();
            case SEARCHLIGHT -> candidates -> candidates.stream()
                    .max(Comparator.comparing(EnemyMob::getRank).thenComparingInt(EnemyMob::getHealth));
            case LAST_LIGHT -> new SoonestOutSelector();
            case RINGLEADER -> PreferringSelector.leadersFirst(new MostNeighboursSelector(PACK_CELLS * cellPixels));
        };
    }
}
