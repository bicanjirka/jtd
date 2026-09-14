package td.tower;

/**
 * Notified as towers are built and removed. {@code TowerAura} is the only subscriber - it
 * uses these to pick up towers placed after it and to drop ones that are sold.
 * <p>
 * Fired from {@link TowerRoster} on whichever thread bought or sold the tower (normally the
 * EDT, but level teardown can differ), so an implementor must not assume the EDT.
 */
public interface TowerListener {
    void towerRemoved(Tower t);

    void towerBuild(Tower t);
}
