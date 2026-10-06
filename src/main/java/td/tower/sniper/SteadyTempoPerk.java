package td.tower.sniper;

/** Steady Aim also speeds the Sniper up: every shot after the first at one target comes sooner. */
public final class SteadyTempoPerk implements SniperPerk {

    private static final float FIRE_RATE_BONUS = 0.25f;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        return context.lock().stacks() > 0 ? shot.withFireRateBonus(FIRE_RATE_BONUS) : shot;
    }
}
