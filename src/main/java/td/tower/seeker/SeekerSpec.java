package td.tower.seeker;

import td.tower.targeting.FastestSelector;
import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Seeker may hit, whom it picks, what its nest holds, how many missiles a launch sends and
 * what a missile does to the enemy it freezes: the base rules reshaped by each perk it owns. Each
 * {@code withX} is a copy.
 *
 * @param reach           the enemies it may fire at
 * @param aim             whom it picks among them
 * @param nest            what it banks
 * @param missilesPerShot how many missiles one launch sends, each at a different target when it can
 * @param spreadsSalvo    whether the launches of one salvo go to different targets
 * @param freeze          how a missile freezes
 * @param shatter         when a frozen enemy shatters
 * @param frostbite       whether its hits on a frozen enemy, or one whose freezes are diminished, always crit
 */
public record SeekerSpec(Reach reach, SeekerAim aim, NestSpec nest, int missilesPerShot, boolean spreadsSalvo,
                         FreezeSpec freeze, ShatterSpec shatter, boolean frostbite) {

    private static final SeekerAim FASTEST = new SeekerAim(new FastestSelector(), "fastest");

    /** The visible enemies in range, the fastest first, no nest, one missile a launch and the base freeze. */
    public static SeekerSpec from(Viewpoint view) {
        return new SeekerSpec(Reach.visible(view), FASTEST, NestSpec.none(), 1, false, FreezeSpec.base(),
                ShatterSpec.none(), false);
    }

    public SeekerSpec withReach(Reach reach) {
        return new SeekerSpec(reach, this.aim, this.nest, this.missilesPerShot, this.spreadsSalvo, this.freeze,
                this.shatter, this.frostbite);
    }

    public SeekerSpec withAim(SeekerAim aim) {
        return new SeekerSpec(this.reach, aim, this.nest, this.missilesPerShot, this.spreadsSalvo, this.freeze,
                this.shatter, this.frostbite);
    }

    public SeekerSpec withNest(NestSpec nest) {
        return new SeekerSpec(this.reach, this.aim, nest, this.missilesPerShot, this.spreadsSalvo, this.freeze,
                this.shatter, this.frostbite);
    }

    public SeekerSpec withMissilesPerShot(int missilesPerShot) {
        return new SeekerSpec(this.reach, this.aim, this.nest, missilesPerShot, this.spreadsSalvo, this.freeze,
                this.shatter, this.frostbite);
    }

    public SeekerSpec withSpreadSalvo() {
        return new SeekerSpec(this.reach, this.aim, this.nest, this.missilesPerShot, true, this.freeze,
                this.shatter, this.frostbite);
    }

    public SeekerSpec withFreeze(FreezeSpec freeze) {
        return new SeekerSpec(this.reach, this.aim, this.nest, this.missilesPerShot, this.spreadsSalvo, freeze,
                this.shatter, this.frostbite);
    }

    public SeekerSpec withShatter(ShatterSpec shatter) {
        return new SeekerSpec(this.reach, this.aim, this.nest, this.missilesPerShot, this.spreadsSalvo, this.freeze,
                shatter, this.frostbite);
    }

    public SeekerSpec withFrostbite() {
        return new SeekerSpec(this.reach, this.aim, this.nest, this.missilesPerShot, this.spreadsSalvo, this.freeze,
                this.shatter, true);
    }

    public Viewpoint view() {
        return this.reach.view();
    }
}
