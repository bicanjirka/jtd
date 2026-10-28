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

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

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
                && isNow(this.caster.ticksSinceDeath(this.gameTime));
    }

    @Override
    public boolean justTookCriticalHit() {
        return isNow(this.caster.ticksSinceCriticalHit(this.gameTime));
    }

    @Override
    public boolean justDodged() {
        return isNow(this.caster.ticksSinceDodge(this.gameTime));
    }

    @Override
    public boolean justTookDamage() {
        return isNow(this.caster.ticksSinceDamageTaken(this.gameTime));
    }

    private static boolean isNow(OptionalInt ticksSince) {
        return ticksSince.equals(OptionalInt.of(0));
    }

    @Override
    public void applyEffect(EffectTemplate template, EffectTarget target) {
        int targets = switch (target) {
            case SelfTarget ignored -> {
                this.caster.applyEffect(template.toEffect(MobAbilityContext::creditNoOne));
                yield 1;
            }
            case RadiusTarget radiusTarget -> this.applyToOthersInRadius(template, radiusTarget.radius());
        };
        float castRadius = switch (target) {
            case SelfTarget ignored -> 0f;
            case RadiusTarget radiusTarget -> radiusTarget.radius();
        };
        this.caster.recordAbilityCast(template.kind(), castRadius, this.gameTime);
        MobObserver observer = this.caster.observer();
        if (observer.isOn()) {
            observer.report(new MobEvent.Cast(template.kind(), castRadius / this.world.getBoard().scale(), targets));
        }
    }

    /** @return how many it reached */
    private int applyToOthersInRadius(EffectTemplate template, float radius) {
        float radius2 = radius * radius;
        double selfX = this.caster.getX();
        double selfY = this.caster.getY();
        int reached = 0;
        for (EnemyMob other : this.world.enemies().getEnemies()) {
            if (other == this.caster || !other.validTarget()) {
                continue;
            }
            double dx = other.getX() - selfX;
            double dy = other.getY() - selfY;
            if (dx * dx + dy * dy <= radius2) {
                other.applyEffect(template.toEffect(MobAbilityContext::creditNoOne));
                reached++;
            }
        }
        return reached;
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
        MobObserver observer = this.caster.observer();
        if (observer.isOn()) {
            observer.report(new MobEvent.ZoneLeft(action.kind(), action.radiusCells()));
        }
    }

    /** Ability effects deal no direct damage, so this sink is never invoked. */
    private static void creditNoOne(Damage damage) {
    }

    @Override
    public void spawnEnemies(SpawnEnemiesAction spawn) {
        EnemySpawner roster = this.world.enemies();
        List<DefinedEnemyMob> built = AbilitySpawnFactory.build(this.world, this.caster, spawn, this.gameTime);
        for (DefinedEnemyMob spawned : built) {
            if (spawn.consumesSelf()) {
                this.caster.markHatched();
                roster.replace(this.caster, spawned);
            } else {
                roster.add(spawned);
            }
        }
        if (this.caster.isObserved()) {
            this.passObservationOn(built, spawn.consumesSelf());
        }
    }

    /** What an observed enemy spawns is observed too; one that hatches ends its own trace in its hatchling. */
    private void passObservationOn(List<DefinedEnemyMob> built, boolean hatched) {
        List<Long> ids = built.stream().map(DefinedEnemyMob::id).toList();
        this.caster.observer().report(new MobEvent.Spawned(ids, hatched));
        String via = (hatched ? "hatched-from:#" : "spawned-by:#") + this.caster.id();
        built.forEach(spawned -> this.world.enemyObservation().observe(spawned, via));
        if (hatched) {
            this.caster.endObservation("hatched-into:" + ids.stream().map(id -> "#" + id).collect(Collectors.joining(",")));
        }
    }
}
