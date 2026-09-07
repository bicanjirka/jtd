package td.cell;

import td.tower.Tower;

public interface Cell {

    void setHighlight(highlightType highlight);

    highlightType getHighlight();

    void setHighlightRange(float range);

    float getHighlightRange();

    int getX();

    int getY();

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
