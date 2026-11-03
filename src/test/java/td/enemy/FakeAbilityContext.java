package td.enemy;

import td.effect.EffectTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class FakeAbilityContext implements AbilityContext {

    final List<AppliedEffect> appliedEffects = new ArrayList<>();
    final List<SpawnCall> spawnCalls = new ArrayList<>();
    final List<LeaveZoneAction> zoneCalls = new ArrayList<>();
    final List<ShoveAlliesAction> shoveCalls = new ArrayList<>();
    private final Set<String> kinAlive = new HashSet<>();
    private float healthFraction = 1f;
    private int ticksSinceLastHit = Integer.MAX_VALUE / 2;
    private boolean justDied;
    private boolean justTookCriticalHit;
    private boolean justTookDamage;
    private boolean justDodged;

    void setKinAlive(String definitionId, boolean alive) {
        if (alive) {
            this.kinAlive.add(definitionId);
        } else {
            this.kinAlive.remove(definitionId);
        }
    }

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

    void setJustDodged(boolean justDodged) {
        this.justDodged = justDodged;
    }

    void setJustTookDamage(boolean justTookDamage) {
        this.justTookDamage = justTookDamage;
    }

    @Override
    public float healthFraction() {
        return this.healthFraction;
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
    public boolean justDodged() {
        return this.justDodged;
    }

    @Override
    public boolean justTookDamage() {
        return this.justTookDamage;
    }

    @Override
    public boolean kinAlive(String definitionId) {
        return this.kinAlive.contains(definitionId);
    }

    @Override
    public void applyEffect(EffectTemplate template, EffectTarget target) {
        this.appliedEffects.add(new AppliedEffect(template, target));
    }

    @Override
    public void leaveZone(LeaveZoneAction zone) {
        this.zoneCalls.add(zone);
    }

    @Override
    public void shoveAllies(ShoveAlliesAction shove) {
        this.shoveCalls.add(shove);
    }

    @Override
    public void spawnEnemies(SpawnEnemiesAction spawn) {
        this.spawnCalls.add(new SpawnCall(spawn.definitionId(), spawn.shape(), spawn.consumesSelf()));
    }

    record AppliedEffect(EffectTemplate template, EffectTarget target) {
    }

    record SpawnCall(String definitionId, AbilitySpawnShape shape, boolean consumesSelf) {
    }
}
