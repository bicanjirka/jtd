package td.util;

import td.tower.Tower;

public interface TowerListener {
    void towerRemoved(Tower t);

    void towerBuild(Tower t);
}
