package td.tower;

import java.awt.*;

/**
 * Interface vsech vezi
 *
 * @author Jirka
 *
 */
public interface Tower {
    /**
     * Zjisti typ veze
     *
     * @return
     */
    TowerFactory.type getType();

    /**
     * Provede na vezi tiknuti
     *
     * @param gameTime - herni cas
     */
    void doTick(int gameTime);

    /**
     * Vykresli vez
     *
     * @param g2       - grafika komponenty kam se ma vykreslit
     * @param gameTime - herni cas
     */
    void paint(Graphics2D g2, int gameTime);

    /**
     * Vykresli efekt strelby
     *
     * @param g2       - grafika komponenty
     * @param gameTime - herni cas
     */
    void paintEffect(Graphics2D g2, int gameTime);

    /**
     * Oznaci vez
     *
     * @param selected - oznacena
     */
    void setSelected(boolean selected);

    /**
     * Informace o typu veze (cena, damage, ...)
     *
     * @return - info
     */
    String getInfoString();

    /**
     * Informace o konkretni vezi
     *
     * @return - info
     */
    String getStatusString();

    /**
     * Cena, za kolik se vez prodava
     *
     * @return - cena
     */
    int getSellPrice();

    /**
     * Dostrel veze v jednotkach sirky policka
     *
     * @return - dostrel
     */
    float getRange();

    /**
     * Dostrel veze v px
     *
     * @return - dostrel
     */
    float getRangeReal();

    /**
     * @return - souradnice X veze
     */
    int getX();

    /**
     * @return - souradnice Y veze
     */
    int getY();

    /**
     * Zjisti jmeno veze
     *
     * @return - jmeno
     */
    String getName();

    void registerTower(Tower t);

    void unregisterTower(Tower t);

    void doCleanup();

}
