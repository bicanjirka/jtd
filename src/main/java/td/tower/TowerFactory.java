package td.tower;

import td.util.GameWorld;

/**
 * Builds towers from their {@link Type}. The {@code createTower} switch has no {@code default}, so
 * a new constant without its class does not compile.
 */
public class TowerFactory {
    public static Tower createTower(Type t, GameWorld c, int x, int y) {
        return switch (t) {
            case SNIPER -> new SniperTower(c, x, y);
            case SPLASH -> new SplashTower(c, x, y);
            case SONAR -> new SonarTower(c, x, y);
            case PULSE -> new PulseTower(c, x, y);
            case AURA -> new AuraTower(c, x, y);
            case MORTAR -> new MortarTower(c, x, y);
            case SEEKER -> new SeekerTower(c, x, y);
            case CINDER -> new CinderTower(c, x, y);
        };
    }

    /** The buildable towers, each with its price and placement key - the single source of both. */
    public enum Type {
        SNIPER(SniperTower.PRICE, 'q', "Sniper"),
        SPLASH(SplashTower.PRICE, 'w', "Burst"),
        SONAR(SonarTower.PRICE, 'e', "Radar"),
        PULSE(PulseTower.PRICE, 'r', "Obelisk"),
        AURA(AuraTower.PRICE, 't', "Beacon"),
        MORTAR(MortarTower.PRICE, 'y', "Mortar"),
        SEEKER(SeekerTower.PRICE, 'u', "Hive"),
        CINDER(CinderTower.PRICE, 'i', "Scorcher");

        private static final int COPY_SURCHARGE_PERCENT = 15;

        /** The list price: what the first copy costs and what upgrade prices are multiples of. */
        public final int price;
        public final char placementKey;
        private final String displayName;

        Type(int price, char placementKey, String displayName) {
            this.price = price;
            this.placementKey = placementKey;
            this.displayName = displayName;
        }

        /** What the shop calls it. Display only: the constant, the class and every node id keep the old name. */
        public String displayName() {
            return this.displayName;
        }

        /** The price with {@code copiesOnBoard} of this type already built: +15% each, rounded half up. */
        public int priceFor(int copiesOnBoard) {
            return (this.price * (100 + COPY_SURCHARGE_PERCENT * copiesOnBoard) + 50) / 100;
        }
    }

}
