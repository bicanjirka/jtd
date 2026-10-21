package td.tower.aura;

import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
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
    public TermText describe() {
        return TermText.of("picks the path spot nearest this tower: when an enemy steps on it, every buffed tower gains ")
                .plus(TermText.of(TowerTerm.RALLY, TermText.of(TermNumbers.signedPercent(RALLY.attackSpeed())
                        + " attack speed for " + TermNumbers.seconds(RALLY.ticks() / (float) TickRate.TICKS_PER_SECOND))))
                .plus(", then the spot rests " + TermNumbers.seconds(RALLY.cooldownTicks() / (float) TickRate.TICKS_PER_SECOND));
    }
}
