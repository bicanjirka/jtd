package td.tower.upgrade;

import td.stat.StatModifiers;
import td.tower.TowerCapability;
import td.tower.TowerStat;
import td.tower.proc.Description;
import td.tower.proc.HitBonus;
import td.tower.proc.Proc;
import td.tower.targeting.TargetPriority;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * What an upgrade node does when bought, as data, and the one row of words for what data can't say.
 * Each {@code withX} adds to it.
 *
 * @param modifiers    what it does to its own tower's stats
 * @param emission     what an aura gives the towers it buffs
 * @param capabilities what it lets the tower do
 * @param targeting    whom it makes the tower aim at, if anyone
 * @param hitBonuses   how it changes some hits
 * @param procs        what it does on some attacks
 * @param note         words for what a perk does
 */
public record NodeEffects(StatModifiers<TowerStat> modifiers, StatModifiers<TowerStat> emission,
                          Set<TowerCapability> capabilities, Optional<TargetPriority> targeting, List<HitBonus> hitBonuses,
                          List<Proc> procs, String note) {

    private static final NodeEffects NONE = new NodeEffects(StatModifiers.none(), StatModifiers.none(), Set.of(),
            Optional.empty(), List.of(), List.of(), "");

    public NodeEffects {
        capabilities = capabilities.isEmpty() ? Set.of() : Set.copyOf(capabilities);
        hitBonuses = List.copyOf(hitBonuses);
        procs = List.copyOf(procs);
    }

    public static NodeEffects none() {
        return NONE;
    }

    /** This, plus {@code more}: modifiers add up. */
    public NodeEffects withModifiers(StatModifiers<TowerStat> more) {
        return new NodeEffects(this.modifiers.plus(more), this.emission, this.capabilities, this.targeting,
                this.hitBonuses, this.procs, this.note);
    }

    /** This, plus {@code more} given to the towers an aura buffs: emissions add up. */
    public NodeEffects withEmission(StatModifiers<TowerStat> more) {
        return new NodeEffects(this.modifiers, this.emission.plus(more), this.capabilities, this.targeting,
                this.hitBonuses, this.procs, this.note);
    }

    public NodeEffects withCapability(TowerCapability capability) {
        Set<TowerCapability> all = this.capabilities.isEmpty() ? EnumSet.noneOf(TowerCapability.class)
                : EnumSet.copyOf(this.capabilities);
        all.add(capability);
        return new NodeEffects(this.modifiers, this.emission, all, this.targeting, this.hitBonuses, this.procs,
                this.note);
    }

    public NodeEffects withTargeting(TargetPriority priority) {
        return new NodeEffects(this.modifiers, this.emission, this.capabilities, Optional.of(priority),
                this.hitBonuses, this.procs, this.note);
    }

    public NodeEffects withHitBonus(HitBonus bonus) {
        List<HitBonus> all = new ArrayList<>(this.hitBonuses);
        all.add(bonus);
        return new NodeEffects(this.modifiers, this.emission, this.capabilities, this.targeting, all, this.procs,
                this.note);
    }

    public NodeEffects withProc(Proc proc) {
        List<Proc> all = new ArrayList<>(this.procs);
        all.add(proc);
        return new NodeEffects(this.modifiers, this.emission, this.capabilities, this.targeting, this.hitBonuses,
                all, this.note);
    }

    public NodeEffects withNote(String note) {
        return new NodeEffects(this.modifiers, this.emission, this.capabilities, this.targeting, this.hitBonuses,
                this.procs, note);
    }

    /**
     * Every row this node shows in the Upgrades panel: one per stat changed, one per stat emitted, one
     * per capability, whom it targets, one per hit bonus and proc, then the note.
     */
    public List<UpgradeBonus> bonuses() {
        List<UpgradeBonus> bonuses = new ArrayList<>();
        for (TowerStat stat : this.modifiers.stats()) {
            bonuses.add(new UpgradeBonus(stat.label(), stat.describe(this.modifiers.get(stat))));
        }
        for (TowerStat stat : this.emission.stats()) {
            String label = "Nearby " + Character.toLowerCase(stat.label().charAt(0)) + stat.label().substring(1);
            bonuses.add(new UpgradeBonus(label, stat.describe(this.emission.get(stat))));
        }
        this.capabilities.stream().sorted().forEach(capability -> bonuses.add(new UpgradeBonus(capability.label(), "")));
        this.targeting.ifPresent(priority -> bonuses.add(new UpgradeBonus("Targets", priority.label())));
        this.hitBonuses.forEach(bonus -> bonuses.add(row(bonus.describe())));
        this.procs.forEach(proc -> bonuses.add(row(proc.describe())));
        if (!this.note.isEmpty()) {
            bonuses.add(new UpgradeBonus(Character.toUpperCase(this.note.charAt(0)) + this.note.substring(1), ""));
        }
        return bonuses;
    }

    private static UpgradeBonus row(Description description) {
        return new UpgradeBonus(description.label(), description.value());
    }
}
