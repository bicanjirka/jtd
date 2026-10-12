package td.tower.aura;

/** Tutelage: buffed towers earn more XP. */
public final class TutelagePerk implements AuraPerk {

    private static final float XP = 0.1f;

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.withTutelageXp(XP);
    }
}
