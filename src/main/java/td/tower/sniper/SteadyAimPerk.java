package td.tower.sniper;

/** Steady Aim: every shot after the first at one target has more crit chance. */
public final class SteadyAimPerk implements SniperPerk {

    private static final float CRIT_PER_STACK = 0.1f;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        return shot.withCritChanceBonus(CRIT_PER_STACK * context.lock().stacks());
    }
}
