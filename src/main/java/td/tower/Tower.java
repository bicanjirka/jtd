package td.tower;

import java.awt.Graphics2D;

public interface Tower {
    TowerFactory.type getType();

    void doTick(int gameTime);

    void paint(Graphics2D g2, int gameTime);

    void paintEffect(Graphics2D g2, int gameTime);

    void setSelected(boolean selected);

    String getInfoString();

    String getStatusString();

    int getSellPrice();

    float getRange();

    float getRangeReal();

    int getX();

    int getY();

    String getName();

    void registerTower(Tower t);

    void unregisterTower(Tower t);

    void doCleanup();

}
