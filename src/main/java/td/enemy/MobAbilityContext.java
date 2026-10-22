package td.enemy;

import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectTemplate;
import td.stat.DisruptionPenalty;
import td.util.GameWorld;
import td.zone.Zone;
import td.zone.ZoneOwner;

/** Resolves ability data against one caster's world, position and definition, for one tick. */
final class MobAbilityContext implements AbilityContext {

    /** How long the shield a ward mine gives lasts. */
    private static final int WARD_SHIELD_TICKS = 100;

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
    public int ticksSinceLastHit() {
        return this.caster.ticksSinceLastHit();
    }

    @Override
    public boolean justDied() {
        return this.caster.isDead() && this.caster.fate() != EnemyInspection.Fate.LEAKED
                && this.caster.ticksSinceDeath(this.gameTime) == 0;
    }

    @Override
    public boolean justTookCriticalHit() {
        return this.caster.ticksSinceCriticalHit(this.gameTime) == 0;
    }

    @Override
    public boolean justDodged() {
        return this.caster.ticksSinceDodge(this.gameTime) == 0;
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

    @Override
    public void leaveZone(LeaveZoneAction action) {
        float radius = action.radiusCells() * this.world.getBoard().scale();
        double x = this.caster.getX();
        double y = this.caster.getY();
        int units = DamageUnits.ofPoints(action.power());
        Zone zone = switch (action.kind()) {
            case HEALING_GROUND -> Zone.healing(x, y, radius, action.lifetimeTicks(), units, ZoneOwner.nobody());
            case HASTE_GROUND -> Zone.haste(x, y, radius, action.lifetimeTicks(), ZoneOwner.nobody());
            case WARD_MINE -> Zone.wardMine(x, y, radius, action.lifetimeTicks(), ZoneOwner.nobody(),
                    (mine, by) -> by.applyEffect(Effect.shield(units, WARD_SHIELD_TICKS, DamageSink.none())));
            case BLINDING_FIELD -> Zone.blinding(x, y, radius, action.lifetimeTicks(),
                    DisruptionPenalty.blinding(action.power()), ZoneOwner.nobody());
            case BURNING_GROUND, TAR, FROST_GROUND, FALLOUT, MINE, CURSED_CLOUD -> throw new IllegalArgumentException(
                    action.kind() + " is a tower zone, not an enemy one");
        };
        this.world.zones().add(zone);
    }

    /** Ability effects deal no direct damage, so this sink is never invoked. */
    private static void creditNoOne(Damage damage) {
    }

    @Override
    public void spawnEnemies(SpawnEnemiesAction spawn) {
        EnemySpawner roster = this.world.enemies();
        for (DefinedEnemyMob spawned : AbilitySpawnFactory.build(this.world, this.caster, spawn, this.gameTime)) {
            if (spawn.consumesSelf()) {
                this.caster.markHatched();
                roster.replace(this.caster, spawned);
            } else {
                roster.add(spawned);
            }
        }
    }
}
