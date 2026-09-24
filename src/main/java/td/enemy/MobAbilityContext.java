package td.enemy;

import td.damage.Damage;
import td.effect.EffectTemplate;
import td.util.GameWorld;

/** Resolves ability data against one caster's world, position and definition, for one tick. */
final class MobAbilityContext implements AbilityContext {

    private final DefinedEnemyMob caster;
    private final GameWorld world;
    private final int gameTime;

    MobAbilityContext(DefinedEnemyMob caster, GameWorld world, int gameTime) {
        this.caster = caster;
        this.world = world;
        this.gameTime = gameTime;
    }

    @Override
    public float healthFraction() {
        return this.caster.getHealthFraction();
    }

    @Override
    public int ticksSinceSpawn() {
        return this.caster.ticksSinceSpawn();
    }

    @Override
    public int ticksSinceLastHit() {
        return this.caster.ticksSinceLastHit();
    }

    @Override
    public boolean justDied() {
        return this.caster.isDead() && this.caster.ticksSinceDeath(this.gameTime) == 0;
    }

    @Override
    public boolean justTookCriticalHit() {
        return this.caster.ticksSinceCriticalHit(this.gameTime) == 0;
    }

    @Override
    public boolean justTookDamage() {
        return this.caster.ticksSinceDamageTaken(this.gameTime) == 0;
    }

    @Override
    public void applyEffect(EffectTemplate template, EffectTarget target) {
        switch (target) {
            case SelfTarget ignored -> this.caster.applyEffect(template.toEffect(MobAbilityContext::creditNoOne));
            case RadiusTarget radiusTarget -> this.applyToOthersInRadius(template, radiusTarget.radius());
        }
        float castRadius = switch (target) {
            case SelfTarget ignored -> 0f;
            case RadiusTarget radiusTarget -> radiusTarget.radius();
        };
        this.caster.recordAbilityCast(template.kind(), castRadius, this.gameTime);
    }

    private void applyToOthersInRadius(EffectTemplate template, float radius) {
        float radius2 = radius * radius;
        double selfX = this.caster.getX();
        double selfY = this.caster.getY();
        for (EnemyMob other : this.world.enemies().getEnemies()) {
            if (other == this.caster || !other.validTarget()) {
                continue;
            }
            double dx = other.getX() - selfX;
            double dy = other.getY() - selfY;
            if (dx * dx + dy * dy <= radius2) {
                other.applyEffect(template.toEffect(MobAbilityContext::creditNoOne));
            }
        }
    }

    /** Ability effects deal no direct damage, so this sink is never invoked. */
    private static void creditNoOne(Damage damage) {
    }

    @Override
    public void spawnEnemies(String definitionId, AbilitySpawnShape shape, boolean consumesSelf) {
        EnemySpawner roster = this.world.enemies();
        for (DefinedEnemyMob spawned : AbilitySpawnFactory.build(this.world, this.caster, definitionId, shape, this.gameTime)) {
            if (consumesSelf) {
                roster.replace(this.caster, spawned);
            } else {
                roster.add(spawned);
            }
        }
    }
}
