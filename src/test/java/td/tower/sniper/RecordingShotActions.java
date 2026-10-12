package td.tower.sniper;

import td.enemy.EnemyMob;

import java.util.ArrayList;
import java.util.List;

/** Remembers what the perks asked the Sniper to do. */
final class RecordingShotActions implements ShotActions {

    final List<EnemyMob> executed = new ArrayList<>();
    final List<EnemyMob> ricochets = new ArrayList<>();
    boolean executionKills = true;

    @Override
    public boolean execute(EnemyMob target) {
        this.executed.add(target);
        return this.executionKills;
    }

    @Override
    public void ricochet(EnemyMob from, int bounces, float share, float reachCells) {
        this.ricochets.add(from);
    }
}
