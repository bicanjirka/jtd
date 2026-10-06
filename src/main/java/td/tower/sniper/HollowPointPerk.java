package td.tower.sniper;

/** Hollow Point: a crit leaves the enemy vulnerable. */
public final class HollowPointPerk implements SniperPerk {

    @Override
    public void react(ShotResult result, ShotActions actions) {
        if (result.critical()) {
            actions.applyVulnerable(result.target(), 1);
        }
    }
}
