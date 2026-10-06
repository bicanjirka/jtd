package td.tower.sniper;

/** Quick Scope: the first shot at a new target is likelier to crit. */
public final class QuickScopePerk implements SniperPerk {

    private static final float CRIT_BONUS = 0.5f;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        return context.lock().fresh() ? shot.withCritChanceBonus(CRIT_BONUS) : shot;
    }
}
