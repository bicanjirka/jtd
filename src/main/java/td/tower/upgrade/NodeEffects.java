package td.tower.upgrade;

import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.tower.TowerStat;

import java.util.ArrayList;
import java.util.List;

/** What an upgrade node does when bought, as data, and the one row of words for what data can't say. */
public record NodeEffects(StatModifiers<TowerStat> modifiers, String note) {

    private static final NodeEffects NONE = new NodeEffects(StatModifiers.none(), "");

    public static NodeEffects none() {
        return NONE;
    }

    /** This, plus {@code more}: modifiers add up. */
    public NodeEffects withModifiers(StatModifiers<TowerStat> more) {
        return new NodeEffects(this.modifiers.plus(more), this.note);
    }

    public NodeEffects withNote(String note) {
        return new NodeEffects(this.modifiers, note);
    }

    /** Every row this node shows in the Upgrades panel: one per stat changed, then the note. */
    public List<UpgradeBonus> bonuses() {
        List<UpgradeBonus> bonuses = new ArrayList<>();
        for (TowerStat stat : this.modifiers.stats()) {
            bonuses.add(new UpgradeBonus(stat.label(), valueOf(stat, this.modifiers.get(stat))));
        }
        if (!this.note.isEmpty()) {
            bonuses.add(new UpgradeBonus(Character.toUpperCase(this.note.charAt(0)) + this.note.substring(1), ""));
        }
        return bonuses;
    }

    private static String valueOf(TowerStat stat, StatModifier modifier) {
        if (modifier.hasSetValue()) {
            return stat.unit().plain(modifier.setTo());
        }
        List<String> parts = new ArrayList<>();
        if (modifier.flat() != 0f) {
            parts.add(stat.unit().signed(modifier.flat()));
        }
        if (modifier.percentAdd() != 0f) {
            parts.add(signedPercent(modifier.percentAdd()));
        }
        if (modifier.multiply() != 1f) {
            parts.add(signedPercent(modifier.multiply() - 1f));
        }
        return String.join(", ", parts);
    }

    private static String signedPercent(float fraction) {
        return (fraction < 0f ? "-" : "+") + Math.round(Math.abs(fraction) * 100) + "%";
    }
}
