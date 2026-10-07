package td.tower.splash;

import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Splash may aim at, what its blast is like and what the blast carries: the base rules
 * reshaped by each perk it owns. Each {@code withX} is a copy.
 *
 * @param reach the enemies it may aim at
 * @param blast what the blast is like
 * @param arcs  how arcs run past the blast; {@link ArcSpec#none()} off the Arc chain
 * @param hexes the hexes it casts; {@link HexSpec#none()} off the Hex chain
 */
public record SplashSpec(Reach reach, BlastSpec blast, ArcSpec arcs, HexSpec hexes) {

    /** The visible enemies in range, a plain blast and nothing it carries. */
    public static SplashSpec from(Viewpoint view) {
        return new SplashSpec(Reach.visible(view), BlastSpec.base(), ArcSpec.none(), HexSpec.none());
    }

    public SplashSpec withBlast(BlastSpec blast) {
        return new SplashSpec(this.reach, blast, this.arcs, this.hexes);
    }

    public SplashSpec withArcs(ArcSpec arcs) {
        return new SplashSpec(this.reach, this.blast, arcs, this.hexes);
    }

    public SplashSpec withHexes(HexSpec hexes) {
        return new SplashSpec(this.reach, this.blast, this.arcs, hexes);
    }

    public Viewpoint view() {
        return this.reach.view();
    }
}
