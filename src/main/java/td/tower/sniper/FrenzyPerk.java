package td.tower.sniper;

/** A crit starts a Frenzy. */
public final class FrenzyPerk implements SniperPerk {

    @Override
    public void react(ShotResult result, ShotActions actions) {
        if (result.critical()) {
            actions.startFrenzy();
        }
    }
}
