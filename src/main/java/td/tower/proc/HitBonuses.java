package td.tower.proc;

import java.util.List;

/** Folds hit bonuses for one hit; pure. */
public final class HitBonuses {

    private HitBonuses() {
    }

    public static HitAdjustment fold(List<HitBonus> bonuses, HitContext context) {
        HitAdjustment total = HitAdjustment.none();
        for (HitBonus bonus : bonuses) {
            total = total.plus(bonus.adjustmentFor(context));
        }
        return total;
    }
}
