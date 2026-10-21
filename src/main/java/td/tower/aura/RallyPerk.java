package td.tower.aura;

import td.term.Term;
import td.tower.TowerTerm;
import td.tower.upgrade.PerkText;
import td.util.TickRate;

import java.util.Set;

/** Rally: an enemy on the path spot nearest the aura rallies the towers it buffs. */
public final class RallyPerk implements AuraPerk {

    private static final RallyRule RALLY = new RallyRule(1f, 0.3f, Math.round(5f * TickRate.TICKS_PER_SECOND),
            Math.round(10f * TickRate.TICKS_PER_SECOND));

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.rallying(RALLY);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(TowerTerm.RALLY);
    }

    @Override
    public String describe() {
        return "marks the path spot nearest the aura: when an enemy steps on it, every buffed tower gets +"
                + PerkText.percent(RALLY.attackSpeed()) + " attack speed for " + PerkText.seconds(RALLY.ticks()) + ", then a "
                + PerkText.seconds(RALLY.cooldownTicks()) + " cooldown";
    }
}
