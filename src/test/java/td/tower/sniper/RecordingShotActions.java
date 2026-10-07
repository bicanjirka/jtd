package td.tower.sniper;

import td.effect.EffectKind;
import td.enemy.EnemyMob;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Remembers what the perks asked the Sniper to do. */
final class RecordingShotActions implements ShotActions {

    int frenzies;
    int bursts;
    final List<EnemyMob> executed = new ArrayList<>();
    private final Map<EffectKind, List<EnemyMob>> stacked = new EnumMap<>(EffectKind.class);
    final List<EnemyMob> ricochets = new ArrayList<>();
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
    public void ricochet(EnemyMob from, int bounces, float share, float reachCells) {
        this.ricochets.add(from);
    }

    @Override
    public void applyStacks(EnemyMob target, EffectKind kind, int stacks) {
        this.stacked.computeIfAbsent(kind, k -> new ArrayList<>()).add(target);
    }

    /** The enemies given stacks of {@code kind}, in order. */
    List<EnemyMob> stackedWith(EffectKind kind) {
        return this.stacked.getOrDefault(kind, List.of());
    }
}
