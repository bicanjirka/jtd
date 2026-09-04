package td.util;

import td.tower.Tower;

public interface TowerListener {
    public void towerRemoved(Tower t);
    public void towerBuild(Tower t);
}
