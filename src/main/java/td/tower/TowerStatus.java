package td.tower;

import td.stat.StatModifiers;

import java.util.EnumSet;
import java.util.Set;

/**
 * A buff a tower holds for a time or for its next shots. Granting the same id again restarts it, unless
 * it keeps a running copy. A timed status lasts longer with the tower's buff duration; a shot status
 * only when it says so.
 *
 * @param id            what identifies it, so granting it again restarts it rather than stacking
 * @param modifiers     what it does to the tower's stats while it lasts
 * @param durationTicks how long a timed status lasts; 0 for a shot status
 * @param shots         how many shots a shot status lasts; 0 for a timed status
 * @param keepsRunning  whether a grant while a copy runs leaves the running one alone
 * @param lengthened    whether a shot status's shots grow with the tower's buff duration
 * @param capabilities  what it lets the tower do while it lasts
 */
public record TowerStatus(String id, StatModifiers<TowerStat> modifiers, int durationTicks, int shots,
                          boolean keepsRunning, boolean lengthened, Set<TowerCapability> capabilities) {

    public TowerStatus {
        capabilities = capabilities.isEmpty() ? Set.of() : Set.copyOf(capabilities);
    }

    /** Lasts {@code durationTicks}, lengthened by buff duration; restarts when granted again. */
    public static TowerStatus timed(String id, StatModifiers<TowerStat> modifiers, int durationTicks) {
        return new TowerStatus(id, modifiers, durationTicks, 0, false, true, Set.of());
    }

    /** Lasts until {@code shots} shots have begun with it; not lengthened; restarts when granted again. */
    public static TowerStatus nextShots(String id, int shots, StatModifiers<TowerStat> modifiers) {
        return new TowerStatus(id, modifiers, 0, shots, false, false, Set.of());
    }

    /** This status, left alone when granted while a copy runs. */
    public TowerStatus keepingARunningOne() {
        return new TowerStatus(this.id, this.modifiers, this.durationTicks, this.shots, true, this.lengthened,
                this.capabilities);
    }

    /** This shot status, its shots lengthened by buff duration. */
    public TowerStatus lengthenedByBuffs() {
        return new TowerStatus(this.id, this.modifiers, this.durationTicks, this.shots, this.keepsRunning, true,
                this.capabilities);
    }

    /** This status, also granting {@code capability} while it lasts. */
    public TowerStatus withCapability(TowerCapability capability) {
        Set<TowerCapability> all = this.capabilities.isEmpty() ? EnumSet.noneOf(TowerCapability.class)
                : EnumSet.copyOf(this.capabilities);
        all.add(capability);
        return new TowerStatus(this.id, this.modifiers, this.durationTicks, this.shots, this.keepsRunning,
                this.lengthened, all);
    }
}
