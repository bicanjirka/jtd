package td.tower.sniper;

import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Sniper may shoot and whom it picks.
 *
 * @param reach the enemies it may shoot
 * @param aim   how it picks among them
 */
public record SniperSpec(Reach reach, SniperAim aim) {

    private static final SniperAim FURTHEST_ALONG = new SniperAim(new FurthestAlongPathSelector(), "first");

    /** The visible enemies in range, the one furthest along the path first. */
    public static SniperSpec from(Viewpoint view) {
        return new SniperSpec(Reach.visible(view), FURTHEST_ALONG);
    }

    /** Picks as {@code aim} says. */
    public SniperSpec aimingAt(SniperAim aim) {
        return new SniperSpec(this.reach, aim);
    }
}
