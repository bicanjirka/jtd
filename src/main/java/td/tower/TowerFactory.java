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

    /**
     * The closed set of buildable towers, each carrying its own price and placement shortcut.
     * The key lives here rather than in a parallel array indexed by {@code ordinal()}, where
     * reordering this enum would silently rebind the keyboard.
     */
    public enum Type {
        first(TowerOne.PRICE, 'q'),
        second(TowerTwo.PRICE, 'w'),
        third(TowerThree.PRICE, 'e'),
        fourth(TowerFour.PRICE, 'r'),
        aura(TowerAura.PRICE, 't'),
        mortar(TowerMortar.PRICE, 'y'),
        seeker(TowerSeeker.PRICE, 'u'),
        cinder(TowerCinder.PRICE, 'i');

        public final int price;
        /** The keyboard shortcut that starts placing this tower. */
        public final char placementKey;

        Type(int price, char placementKey) {
            this.price = price;
            this.placementKey = placementKey;
        }
    }

}
