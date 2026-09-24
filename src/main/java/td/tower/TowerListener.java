package td.tower;

/**
 * Notified as towers are built and removed, on whichever thread bought or sold them - not
 * necessarily the EDT.
 */
public interface TowerListener {
    void towerRemoved(Tower t);

    void towerBuild(Tower t);
}
