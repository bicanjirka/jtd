package td.tower.seeker;

import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Aerie may hunt, what its freezes set off, how its nest draws and what its ice does around it: the base rules
 * reshaped by each mechanic it owns. Each {@code withX} is a copy.
 *
 * @param reach   the enemies it may launch at
 * @param release what every so many freezes set off
 * @param feeds   whether each freeze takes time off the next load
 * @param brood   how the nest draws each new missile
 * @param ice     what its ice does to the enemies around it
 */
public record SeekerSpec(Reach reach, Release release, boolean feeds, BroodSpec brood, IceRider ice) {

    /** The visible enemies in range, Frost missiles only and nothing more. */
    public static SeekerSpec from(Viewpoint view) {
        return new SeekerSpec(Reach.visible(view), Release.NONE, false, BroodSpec.frostOnly(), IceRider.NONE);
    }

    public SeekerSpec withReach(Reach reach) {
        return new SeekerSpec(reach, this.release, this.feeds, this.brood, this.ice);
    }

    public SeekerSpec withRelease(Release release) {
        return new SeekerSpec(this.reach, release, this.feeds, this.brood, this.ice);
    }

    public SeekerSpec feeding() {
        return new SeekerSpec(this.reach, this.release, true, this.brood, this.ice);
    }

    public SeekerSpec withBrood(BroodSpec brood) {
        return new SeekerSpec(this.reach, this.release, this.feeds, brood, this.ice);
    }

    public SeekerSpec withIce(IceRider ice) {
        return new SeekerSpec(this.reach, this.release, this.feeds, this.brood, ice);
    }
}
