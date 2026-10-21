package td.enemy;

import td.util.GameWorld;
import td.wave.Vec2;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Builds the mobs a spawn ability creates: at the caster's position, on its path and at its rank unless the ability
 * fixes one, each member delayed by the shape's spacing so they don't stack.
 */
final class AbilitySpawnFactory {

    private AbilitySpawnFactory() {
    }

    static List<DefinedEnemyMob> build(GameWorld world, DefinedEnemyMob caster, SpawnEnemiesAction spawn, int gameTime) {
        RankedEnemy ranked = world.getEnemyCatalog().ranked(spawn.definitionId());
        Rank rank = ranked.spawnRank(spawn.rank().orElse(caster.getRank()));
        AbilitySpawnShape shape = spawn.shape();
        EnemyDefinition baseDefinition = ranked.definitionFor(rank);
        EnemyDefinition definition = shape.traitOverride()
                .map(trait -> baseDefinition.withAdditionalTraits(List.of(trait)))
                .orElse(baseDefinition);
        int health = Math.round(definition.baseHealth() * shape.healthMultiplier());
        int price = Math.round(definition.price() * shape.bountyMultiplier());
        return IntStream.range(0, shape.members())
                .mapToObj(i -> {
                    SpawnParameters spawnParameters = SpawnParameters.of(i * shape.delaySpacingSlots(), definition.baseSpeed(),
                            health, price, shape.sizeMultiplier(), 1f, new Vec2(0, 0), caster.pathIndex());
                    DefinedEnemyMob spawned = new DefinedEnemyMob(definition, world, spawnParameters, rank);
                    spawned.spawnAtSamePositionAs(caster);
                    spawned.recordAbilitySpawn(gameTime);
                    return spawned;
                })
                .toList();
    }
}
