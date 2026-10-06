package td.tower.sniper;

import td.enemy.EnemyMob;

import java.util.ArrayList;
import java.util.List;

/** Remembers what the perks asked the Sniper to do. */
final class RecordingShotActions implements ShotActions {

    int frenzies;
    int bursts;
    final List<EnemyMob> executed = new ArrayList<>();
    final List<EnemyMob> vulnerable = new ArrayList<>();
    boolean executionKills = true;

    @Override
    public void startFrenzy() {
        this.frenzies++;
    }

    @Override
    public void startBurst() {
        this.bursts++;
    }

    @Override
    public boolean execute(EnemyMob target) {
        this.executed.add(target);
        return this.executionKills;
    }

    @Override
    public void applyVulnerable(EnemyMob target, int stacks) {
        this.vulnerable.add(target);
    }
}
