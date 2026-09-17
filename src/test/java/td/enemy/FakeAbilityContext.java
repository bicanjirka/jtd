package td.enemy;

import td.effect.EffectTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * A configurable, recording {@link AbilityContext} double for headless ability tests.
 */
final class FakeAbilityContext implements AbilityContext {

    final List<AppliedEffect> appliedEffects = new ArrayList<>();
    final List<SpawnCall> spawnCalls = new ArrayList<>();
    private float healthFraction = 1f;
    private int ticksSinceLastHit = Integer.MAX_VALUE / 2;
    private boolean justDied;
    private boolean justTookCriticalHit;

    void setHealthFraction(float healthFraction) {
        this.healthFraction = healthFraction;
    }

    void setTicksSinceLastHit(int ticksSinceLastHit) {
        this.ticksSinceLastHit = ticksSinceLastHit;
    }

    void setJustDied(boolean justDied) {
        this.justDied = justDied;
    }

    void setJustTookCriticalHit(boolean justTookCriticalHit) {
        this.justTookCriticalHit = justTookCriticalHit;
    }

    @Override
    public float healthFraction() {
        return this.healthFraction;
    }

    @Override
    public int ticksSinceSpawn() {
        return 0;
    }

    @Override
    public int ticksSinceLastHit() {
        return this.ticksSinceLastHit;
    }

    @Override
    public boolean justDied() {
        return this.justDied;
    }

    @Override
    public boolean justTookCriticalHit() {
        return this.justTookCriticalHit;
    }

    @Override
    public void applyEffect(EffectTemplate template, EffectTarget target) {
        this.appliedEffects.add(new AppliedEffect(template, target));
    }

    @Override
    public void spawnEnemies(String definitionId, int count, boolean consumesSelf) {
        this.spawnCalls.add(new SpawnCall(definitionId, count, consumesSelf));
    }

    record AppliedEffect(EffectTemplate template, EffectTarget target) {
    }

    record SpawnCall(String definitionId, int count, boolean consumesSelf) {
    }
}
