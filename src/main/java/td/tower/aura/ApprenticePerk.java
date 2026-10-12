package td.tower.aura;

/** Apprentice: the buffed tower with the least XP earns double. */
public final class ApprenticePerk implements AuraPerk {

    private static final float XP = 1f;

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.withApprenticeXp(XP);
    }
}
