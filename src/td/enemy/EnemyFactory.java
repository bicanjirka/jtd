package td.enemy;

import java.util.HashMap;
import java.util.Map;

import td.util.Context;

/**
 * Trida na vyrobu a identifikaci nepratel
 * @author Jirka
 *
 */
public class EnemyFactory {
	
	/**
	 * Staticky seznam typu nepratel
	 * @author Jirka
	 *
	 */
	public static enum Enemy {
		Circle		("c", new EnemyMobCircle()),
		Square		("s", new EnemyMobSquare()),
		Triangle	("t", new EnemyMobTriangle()),
		Ghost		("g", new EnemyMobGhost()),
		Empty		("e", new EnemyMobEmpty());
		
		private final String name;
		private final EnemyMob instance;
		Enemy(String name, EnemyMob instance) {
			this.name = name;
			this.instance = instance;
		}
		/**
		 * Zkratka jmena nepritele
		 * @return - zkratku
		 */
		public String getName() {
			return this.name;
		}
		/**
		 * Udela kopii nepritele se vsemi jeho parametry
		 * @param context - herni kontext
		 * @param delay - zpozdeni (kdy ma ozit)
		 * @param health - zivoty
		 * @param price - hodnota, kolik dostanu za jeho smrt
		 * @return - kopie nepritele
		 */
		public EnemyMob getCopy(Context context, int delay, int health, int price, int level) {
			return instance.create(context, delay, health, price, level);
		}
	}
	
	private static Map<String, Enemy> table = new HashMap<String, Enemy>();
    static {
        for (Enemy enemy : Enemy.values()) {
            table.put(enemy.getName(), enemy);
        }
    }
	/**
	 * Jestli je zadane heslo na seznamu nepratel
	 * @param name - zkratka nepritele
	 * @return - je na seznamu
	 */
	public static boolean isEnemy(String name) {
        return table.containsKey(name);
    }
    /**
     * Identifikuje nepritele podle jmena
     * @param name - zkratka nepritele
     * @return - trida nepritele
     */
    public static Enemy identifyEnemy(String name) {
        return table.get(name);
    }
    /**
     * Zkopiruje nepritele ze seznamu a vrati onu kopii
     * @param name - zkratka
     * @param context - herni kontext
     * @param delay - zpozdeni ve hre
     * @param health - zivoty
     * @param price - hodnota
     * @return - kopie
     */
    public static EnemyMob getEnemy(String name, Context context, int delay, int health, int price, int level) {
        return table.get(name).getCopy(context, delay, health, price, level);
    }
}
