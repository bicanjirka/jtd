package td.tower.mortar;

/** Siege Rounds: each Bracketing step adds more damage. */
public final class BracketDamagePerk implements MortarPerk {

    private static final float DAMAGE_STEP = 0.15f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withBracket(spec.bracket().withDamageStep(DAMAGE_STEP));
    }
}
