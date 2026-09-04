package td.cell;

import td.tower.Tower;

import java.awt.*;

/**
 * Interface pro herni policka
 *
 * @author Juras
 *
 */
public interface Cell {

    /**
     * Vykresli zvyrazneni v zavislosti na jeho typu
     *
     * @param g2 - grafika
     */
    void paintEffect(Graphics2D g2);

    /**
     * Nastavi zvyrazneni policka
     *
     * @param highlight - zvyrazneni
     */
    void setHighlight(highlightType highlight);

    /**
     * Nastavi polomer kruznici znazornujici dostrel veze
     *
     * @param range - dostrel veze
     */
    void setHighlightRange(float range);

    /**
     * Nastavi jestli je policko po vuli staveni vezi
     */
    void enable(boolean b);

    /**
     * Jestli se sa na policku stavet<br>
     * Neda pokud lezi na ceste a nebo obsahuje vez
     */
    boolean buildable();

    /**
     * Jestli policko obsahuje vez
     */
    boolean hasTower();

    /**
     * Ucini policko prazdnym
     */
    void unSetTower();

    /**
     * Vrati vez, kterou obsahuje policko
     */
    Tower getTower();

    /**
     * Postavi na policku vez
     *
     * @param tower - vez co se ma postavit
     */
    void setTower(Tower tower);

    /**
     * Moznosti zvyrazneni herniho policka<br>
     * <ul>
     * 	<li>none - zadne</li>
     * 	<li>select - oznaceni veze</li>
     * 	<li>place - pokladani veze</li>
     * </ul>
     *
     * @author Juras
     *
     */
    enum highlightType {
        none,
        select,
        place
    }

}
