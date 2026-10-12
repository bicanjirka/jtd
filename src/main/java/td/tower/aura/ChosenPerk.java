package td.tower.aura;

/** Chosen: the aura buffs only its most experienced tower, more strongly. */
public final class ChosenPerk implements AuraPerk {

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.choosing();
    }
}
