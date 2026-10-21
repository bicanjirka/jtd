package td.tower.proc;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.TowerStatus;

import java.util.ArrayList;
import java.util.List;

/** Records what procs asked of a tower, in order, so a test can read it back. */
final class RecordingProcActions implements ProcActions {

    private final List<String> calls = new ArrayList<>();

    @Override
    public void applyStacks(EnemyMob target, EffectKind kind, int stacks) {
        this.calls.add("stacks " + kind + " " + stacks);
    }

    @Override
    public void charge(EnemyMob target, int ticks) {
        this.calls.add("charge " + ticks);
    }

    @Override
    public void grantStatus(TowerStatus status) {
        this.calls.add("status " + status.term().title());
    }

    List<String> calls() {
        return List.copyOf(this.calls);
    }
}
