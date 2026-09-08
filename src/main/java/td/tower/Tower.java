package td.tower;

public interface Tower {
    TowerFactory.type getType();

    void doTick(int gameTime);

    <R> R accept(TowerVisitor<R> visitor);

    void setSelected(boolean selected);

    boolean isSelected();

    String getInfoString();

    String getStatusString();

    int getSellPrice();

    float getRange();

    float getRangeReal();

    int getX();

    int getY();

    int getBoardX();

    int getBoardY();

    void registerTower(Tower t);

    void unregisterTower(Tower t);

    void doCleanup();

}
