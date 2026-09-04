package td.cell;

import td.tower.Tower;

import java.awt.*;

public interface Cell {

    void paintEffect(Graphics2D g2);

    void setHighlight(highlightType highlight);

    void setHighlightRange(float range);

    void enable(boolean b);

    boolean buildable();

    boolean hasTower();

    void unSetTower();

    Tower getTower();

    void setTower(Tower tower);

    enum highlightType {
        none,
        select,
        place
    }

}
