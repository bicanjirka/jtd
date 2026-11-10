package td.tower;

import td.util.GameWorld;

/**
 * Builds towers from their {@link Type}. The {@code createTower} switch has no {@code default}, so
 * a new constant without its class does not compile.
 */
public class TowerFactory {

    /**
     * A new tower of {@code t} at cell {@code x}, {@code y}. It records {@link TowerRoster#priceOf} as what it was
     * bought for, and that price grows with each copy on the board: so for a tower that is bought, charge that price
     * first, call this, and only then {@link TowerRoster#add} it.
     */
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

    /** The buildable towers, each with its placement key and name; its price is its tower class's. */
    public enum Type {
        SNIPER(SniperTower.PRICE, 'q', "Sniper"),
        SPLASH(SplashTower.PRICE, 'w', "Burst"),
        MORTAR(MortarTower.PRICE, 'e', "Mortar"),
        CINDER(CinderTower.PRICE, 'r', "Scorcher"),
        SEEKER(SeekerTower.PRICE, 't', "Hive"),
        SONAR(SonarTower.PRICE, 'y', "Radar"),
        PULSE(PulseTower.PRICE, 'u', "Obelisk"),
        AURA(AuraTower.PRICE, 'i', "Beacon");

        private static final double COPY_PRICE_GROWTH = 1.25;

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

        /**
         * The price with {@code copiesOnBoard} of this type already built: each copy multiplies the one before it by
         * the same growth, so the surcharge compounds, rounded half up.
         */
        public int priceFor(int copiesOnBoard) {
            return Math.toIntExact(Math.round(this.price * Math.pow(COPY_PRICE_GROWTH, copiesOnBoard)));
        }
    }

}
