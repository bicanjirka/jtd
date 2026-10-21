package td.tower;

import td.stat.StatModifiers;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A buff a tower holds for a time or for its next shots. Granting the same term again restarts it, unless
 * it keeps a running copy. A timed status lasts longer with the tower's buff duration; a shot status
 * only when it says so.
 *
 * @param term          what identifies it, so granting it again restarts it rather than stacking
 * @param modifiers     what it does to the tower's stats while it lasts
 * @param durationTicks how long a timed status lasts; 0 for a shot status
 * @param shots         how many shots a shot status lasts; 0 for a timed status
 * @param keepsRunning  whether a grant while a copy runs leaves the running one alone
 * @param lengthened    whether a shot status's shots grow with the tower's buff duration
 * @param capabilities  what it lets the tower do while it lasts
 */
public record TowerStatus(TowerTerm term, StatModifiers<TowerStat> modifiers, int durationTicks, int shots,
                          boolean keepsRunning, boolean lengthened, Set<TowerCapability> capabilities) {

    public TowerStatus {
        capabilities = capabilities.isEmpty() ? Set.of() : Set.copyOf(capabilities);
    }

    /** Lasts {@code durationTicks}, lengthened by buff duration; restarts when granted again. */
    public static TowerStatus timed(TowerTerm term, StatModifiers<TowerStat> modifiers, int durationTicks) {
        return new TowerStatus(term, modifiers, durationTicks, 0, false, true, Set.of());
    }

    /** Lasts until {@code shots} shots have begun with it; not lengthened; restarts when granted again. */
    public static TowerStatus nextShots(TowerTerm term, int shots, StatModifiers<TowerStat> modifiers) {
        return new TowerStatus(term, modifiers, 0, shots, false, false, Set.of());
    }

    /**
     * What it does, every number from its modifiers: "+150% crit damage, guaranteed crit; spent by the next shot",
     * "+100% attack speed for 5 s".
     */
    public TermText describe() {
        List<String> parts = new ArrayList<>();
        for (TowerStat stat : this.modifiers.stats()) {
            parts.add(stat.describe(this.modifiers.get(stat)) + " " + stat.label().toLowerCase(Locale.ROOT));
        }
        for (TowerCapability capability : this.capabilities) {
            parts.add(capability.label().toLowerCase(Locale.ROOT));
        }
        String length = this.shots == 0 ? "for " + TermNumbers.seconds(this.durationTicks / TickRate.TICKS_PER_SECOND)
                : this.shots == 1 ? "spent by the next shot" : "for the next " + this.shots + " shots";
        return TermText.of(String.join(", ", parts) + "; " + length);
    }

    /** This status, left alone when granted while a copy runs. */
    public TowerStatus keepingARunningOne() {
        return new TowerStatus(this.term, this.modifiers, this.durationTicks, this.shots, true, this.lengthened,
                this.capabilities);
    }

    /** This shot status, its shots lengthened by buff duration. */
    public TowerStatus lengthenedByBuffs() {
        return new TowerStatus(this.term, this.modifiers, this.durationTicks, this.shots, this.keepsRunning, true,
                this.capabilities);
    }

    /** This status, also granting {@code capability} while it lasts. */
    public TowerStatus withCapability(TowerCapability capability) {
        Set<TowerCapability> all = this.capabilities.isEmpty() ? EnumSet.noneOf(TowerCapability.class)
                : EnumSet.copyOf(this.capabilities);
        all.add(capability);
        return new TowerStatus(this.term, this.modifiers, this.durationTicks, this.shots, this.keepsRunning,
                this.lengthened, all);
    }
}
