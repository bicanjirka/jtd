package td.tower;

import td.util.GameWorld;

/**
 * Constructs towers from their {@link Type}. That enum is the closed set of buildable towers
 * and the single source of each one's price - the toolbar, the affordability check and the
 * tower itself all read it, so a price lives in exactly one place. The {@code createTower}
 * switch has no {@code default}, so adding a constant without wiring up its class is a
 * compile error rather than a silent gap.
 */
public class TowerFactory {
    public static Tower createTower(Type t, GameWorld c, int x, int y) {
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

    public enum Type {
        first(TowerOne.PRICE),
        second(TowerTwo.PRICE),
        third(TowerThree.PRICE),
        fourth(TowerFour.PRICE),
        aura(TowerAura.PRICE),
        mortar(TowerMortar.PRICE),
        seeker(TowerSeeker.PRICE),
        cinder(TowerCinder.PRICE);

        public final int price;

        Type(int price) {
            this.price = price;
        }
    }

}
