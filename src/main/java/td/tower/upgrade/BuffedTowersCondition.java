package td.tower.upgrade;

import td.tower.Tower;
import td.tower.buff.TowerBuff;
import td.util.GameWorld;

/** Satisfied once the tower buffs at least {@code required} other towers, as it stands on the board now. */
public record BuffedTowersCondition(int required) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return buffed(tower, context) >= this.required;
    }

    @Override
    public String describe() {
        return "buffing " + this.required + " towers";
    }

    @Override
    public String progress(Tower tower, GameWorld context) {
        return "buffing " + Math.min(buffed(tower, context), this.required) + "/" + this.required + " towers";
    }

    private static long buffed(Tower tower, GameWorld context) {
        return context.towers().all().stream()
                .filter(other -> other != tower && !tower.buffFor(other).equals(TowerBuff.none()))
                .count();
    }
}
