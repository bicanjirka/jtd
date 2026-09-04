package td.enemy;

import td.util.Context;

import java.awt.*;

/**
 * Interface pro nepratele
 *
 * @author Jirka
 *
 */
public interface EnemyMob {
    /**
     * Provede s nepritelem vse, co se ma behem jednoho herniho tiknuti
     * stat, napriklad ho posune po herni plose
     *
     * @param gameTime - herni cas
     */
    void doTick(int gameTime);

    /**
     * Vykresli nepritele
     *
     * @param g2       - grafika komponenty
     * @param gameTime - herni cas
     */
    void paint(Graphics2D g2, int gameTime);

    /**
     * Aktualni souradnice X nepritele na herni plose
     *
     * @return - x
     */
    int getX();

    /**
     * Aktualni souradnice Y nepritele na herni plose
     *
     * @return - y
     */
    int getY();

    int getProgression();

    /**
     * Jestli je nepritel validni target, tzn. zameritelny vezema
     *
     * @return - validni
     */
    boolean validTarget();
//    public boolean visible();

    /**
     * Jestli je nepritel validni target daneho typu, tzn. zameritelny vezema
     *
     * @param
     * @return - validni
     */
    boolean validTarget(type type);

    /**
     * Jestli je nepritel validni target a je jednim z danych typu, tzn. zameritelny vezema
     *
     * @param
     * @return - validni
     */
    boolean validTarget(type type0, type type1);

    /**
     * Kolik ma nepritel zivotu
     *
     * @return - hp
     */
    long getHealth();

    /**
     * Odebere nepriteli patricny pocet zivotu
     *
     * @param damage - zraneni
     */
    void doDamage(int damage);

    /**
     * Zjisti aktualni rychlost
     *
     * @return - rychlost
     */
    int getSpeed();

    /**
     * Informace o nepriteli
     *
     * @return - info
     */
    String getInfoString();

    /**
     * Vytvori kopii nepritele (klon)
     *
     * @param context - herni kontext
     * @param delay   - zpozdeni
     * @param health  - zivoty
     * @param price   - hodnota
     * @return - klon
     */
    EnemyMob create(Context context, int delay, int health, int price, int level);

    enum type {
        Normal,
        Flying,
        Invisible
    }
}
