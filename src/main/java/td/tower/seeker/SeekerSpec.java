package td.tower.seeker;

import td.tower.targeting.FastestSelector;
import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Seeker may hit, whom it picks, and what its nest holds: the base rules reshaped by each
 * perk it owns. Each {@code withX} is a copy.
 *
 * @param reach the enemies it may fire at
 * @param aim   whom it picks among them
 * @param nest  what it banks
 */
public record SeekerSpec(Reach reach, SeekerAim aim, NestSpec nest) {

    private static final SeekerAim FASTEST = new SeekerAim(new FastestSelector(), "fastest");

    /** The visible enemies in range, the fastest first, and no nest. */
    public static SeekerSpec from(Viewpoint view) {
        return new SeekerSpec(Reach.visible(view), FASTEST, NestSpec.none());
    }

    public SeekerSpec withReach(Reach reach) {
        return new SeekerSpec(reach, this.aim, this.nest);
    }

    public SeekerSpec withAim(SeekerAim aim) {
        return new SeekerSpec(this.reach, aim, this.nest);
    }

    public SeekerSpec withNest(NestSpec nest) {
        return new SeekerSpec(this.reach, this.aim, nest);
    }
}
