package td.tower.sniper;

import td.enemy.EnemyMob;

import java.util.ArrayList;
import java.util.List;

/** Remembers what the mechanics asked the Sniper to do. */
final class RecordingShotActions implements ShotActions {

    final List<EnemyMob> executed = new ArrayList<>();
    final List<EnemyMob> ricochets = new ArrayList<>();
    final List<EnemyMob> bled = new ArrayList<>();
    final List<EnemyMob> exposed = new ArrayList<>();
    boolean executionKills = true;

    @Override
    public boolean execute(EnemyMob target) {
        this.executed.add(target);
        return this.executionKills;
    }

    @Override
    public void bleed(EnemyMob target, float sharePerCell, int ticks) {
        this.bled.add(target);
    }

    @Override
    public void expose(EnemyMob target, int ticks) {
        this.exposed.add(target);
    }

    @Override
    public void ricochet(EnemyMob from, float reachCells) {
        this.ricochets.add(from);
    }
}
