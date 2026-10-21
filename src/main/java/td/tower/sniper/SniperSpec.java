package td.tower.sniper;

import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

import java.util.Optional;

/**
 * Whom the Sniper may shoot and whom it picks, reshaped by each mechanic it owns. Each {@code withX} is a
 * copy.
 *
 * @param reach the enemies it may shoot
 * @param aim   whom a special makes it pick; empty for the enemy furthest along the path
 */
public record SniperSpec(Reach reach, Optional<SniperAim> aim) {

    private static final SniperAim FURTHEST_ALONG = new SniperAim(new FurthestAlongPathSelector(), "first");

    /** The visible enemies in range, the one furthest along the path first. */
    public static SniperSpec from(Viewpoint view) {
        return new SniperSpec(Reach.visible(view), Optional.empty());
    }

    public SniperSpec withReach(Reach reach) {
        return new SniperSpec(reach, this.aim);
    }

    /** Picks as {@code aim} says, unless a mechanic bought earlier already chose: the first special decides. */
    public SniperSpec aimingAt(SniperAim aim) {
        return this.aim.isPresent() ? this : new SniperSpec(this.reach, Optional.of(aim));
    }

    public Viewpoint view() {
        return this.reach.view();
    }

    /** How it picks among the enemies in its reach. */
    public SniperAim chosenAim() {
        return this.aim.orElse(FURTHEST_ALONG);
    }
}
