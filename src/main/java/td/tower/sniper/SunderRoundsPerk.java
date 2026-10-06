package td.tower.sniper;

/** Sunder Rounds: every crit sunders the enemy. */
public final class SunderRoundsPerk implements SniperPerk {

    @Override
    public void react(ShotResult result, ShotActions actions) {
        if (result.critical()) {
            actions.applySundered(result.target(), 1);
        }
    }
}
