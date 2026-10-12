package td.tower.aura;

/** Rally: an enemy on the path spot nearest the aura rallies the towers it buffs. */
public final class RallyPerk implements AuraPerk {

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.rallying();
    }
}
