package td.tower.seeker;

import td.tower.targeting.FastestSelector;
import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Seeker may hit, whom it picks, what its nest holds and how many missiles a launch sends:
 * the base rules reshaped by each perk it owns. Each {@code withX} is a copy.
 *
 * @param reach           the enemies it may fire at
 * @param aim             whom it picks among them
 * @param nest            what it banks
 * @param missilesPerShot how many missiles one launch sends, each at a different target when it can
 * @param spreadsSalvo    whether the launches of one salvo go to different targets
 */
public record SeekerSpec(Reach reach, SeekerAim aim, NestSpec nest, int missilesPerShot, boolean spreadsSalvo) {

    private static final SeekerAim FASTEST = new SeekerAim(new FastestSelector(), "fastest");

    /** The visible enemies in range, the fastest first, no nest and one missile a launch. */
    public static SeekerSpec from(Viewpoint view) {
        return new SeekerSpec(Reach.visible(view), FASTEST, NestSpec.none(), 1, false);
    }

    public SeekerSpec withReach(Reach reach) {
        return new SeekerSpec(reach, this.aim, this.nest, this.missilesPerShot, this.spreadsSalvo);
    }

    public SeekerSpec withAim(SeekerAim aim) {
        return new SeekerSpec(this.reach, aim, this.nest, this.missilesPerShot, this.spreadsSalvo);
    }

    public SeekerSpec withNest(NestSpec nest) {
        return new SeekerSpec(this.reach, this.aim, nest, this.missilesPerShot, this.spreadsSalvo);
    }

    public SeekerSpec withMissilesPerShot(int missilesPerShot) {
        return new SeekerSpec(this.reach, this.aim, this.nest, missilesPerShot, this.spreadsSalvo);
    }

    public SeekerSpec withSpreadSalvo() {
        return new SeekerSpec(this.reach, this.aim, this.nest, this.missilesPerShot, true);
    }

    public Viewpoint view() {
        return this.reach.view();
    }
}
