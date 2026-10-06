package td.tower.sniper;

import td.tower.targeting.HighestHealthSelector;

import java.util.Optional;

/** Hollow Point: a crit leaves the enemy vulnerable; aims at the most health. */
public final class HollowPointPerk implements SniperPerk {

    @Override
    public Optional<SniperAim> aim(Viewpoint view) {
        return Optional.of(new SniperAim(new HighestHealthSelector(), "most health"));
    }

    @Override
    public void react(ShotResult result, ShotActions actions) {
        if (result.critical()) {
            actions.applyVulnerable(result.target(), 1);
        }
    }
}
