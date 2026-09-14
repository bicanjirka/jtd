package td.tower;

import td.util.GameWorld;

/**
 * Constructs towers from their {@link type}. That enum is the closed set of buildable towers
 * and the single source of each one's price - the toolbar, the affordability check and the
 * tower itself all read it, so a price lives in exactly one place. The {@code createTower}
 * switch has no {@code default}, so adding a constant without wiring up its class is a
 * compile error rather than a silent gap.
 */
public class TowerFactory {
    public static Tower createTower(type t, GameWorld c, int x, int y) {
        return switch (t) {
            case first -> new TowerOne(c, x, y);
            case second -> new TowerTwo(c, x, y);
            case third -> new TowerThree(c, x, y);
            case fourth -> new TowerFour(c, x, y);
            case aura -> new TowerAura(c, x, y);
            case mortar -> new TowerMortar(c, x, y);
            case seeker -> new TowerSeeker(c, x, y);
            case cinder -> new TowerCinder(c, x, y);
        };
    }

    public enum type {
        first(TowerOne.price),
        second(TowerTwo.price),
        third(TowerThree.price),
        fourth(TowerFour.price),
        aura(TowerAura.price),
        mortar(TowerMortar.price),
        seeker(TowerSeeker.price),
        cinder(TowerCinder.price);

        public final int price;

        type(int price) {
            this.price = price;
        }
    }

}
