package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.WorldFixtures;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rules every built-in enemy keeps, a level's own included. What one enemy is - its stats,
 * abilities and numbers - is content: its mechanics are tested on definitions built in their own
 * tests, and its numbers in the balance report.
 */
class BuiltInEnemiesTest {

    private static final int LIFETIME_TICKS = 300;
    private static final Damage LETHAL = Damage.physical(1_000_000_000);

    /** The catalog each built-in level plays with: the built-in enemies and its own. */
    private static List<EnemyCatalog> catalogs() {
        List<EnemyCatalog> catalogs = new ArrayList<>();
        for (LevelDefinition level : new BuiltInLevelCatalog().levels()) {
            EnemyCatalog catalog = EnemyCatalog.builtIn();
            level.customEnemies().forEach(catalog::register);
            level.customRankedEnemies().forEach(catalog::register);
            catalogs.add(catalog);
        }
        return catalogs;
    }

    @Test
    void noEnemyGetsWeakerOrPaysLessAsItsRankRises() {
        List<String> problems = new ArrayList<>();

        for (EnemyCatalog catalog : catalogs()) {
            for (String id : catalog.ids()) {
                for (int i = 1; i < Rank.values().length; i++) {
                    EnemyDefinition lower = catalog.get(id, Rank.values()[i - 1]);
                    EnemyDefinition higher = catalog.get(id, Rank.values()[i]);
                    if (higher.baseHealth() < lower.baseHealth() || higher.price() < lower.price()) {
                        problems.add(id + " at " + Rank.values()[i]);
                    }
                }
            }
        }

        assertThat(problems).isEmpty();
    }

    @Test
    void everyEnemyAnAbilitySpawnsIsInTheCatalog() {
        List<String> problems = new ArrayList<>();

        for (EnemyCatalog catalog : catalogs()) {
            for (String id : catalog.ids()) {
                for (Rank rank : Rank.values()) {
                    for (Ability ability : catalog.get(id, rank).abilities()) {
                        spawnedId(ability.action()).filter(spawned -> !catalog.contains(spawned))
                                .ifPresent(spawned -> problems.add(id + " at " + rank + " spawns " + spawned));
                    }
                }
            }
        }

        assertThat(problems).isEmpty();
    }

    @Test
    void everyEnemyHasOneRoleSentenceAtEveryRank() {
        List<String> problems = new ArrayList<>();

        for (EnemyCatalog catalog : catalogs()) {
            for (String id : catalog.ids()) {
                String first = catalog.get(id, Rank.GRUNT).description();
                for (Rank rank : Rank.values()) {
                    String description = catalog.get(id, rank).description();
                    if (description.isEmpty() || !description.endsWith(".") || !description.equals(first)) {
                        problems.add(id + " at " + rank + ": \"" + description + "\"");
                    }
                }
            }
        }

        assertThat(problems).isEmpty();
    }

    @Test
    void everyEnemyAtEveryRankWalksCastsAndDiesWithoutFailing() {
        for (EnemyCatalog catalog : catalogs()) {
            for (String id : catalog.ids()) {
                for (Rank rank : Rank.values()) {
                    GameWorld world = WorldFixtures.newWorldOnStraightPath();
                    world.setEnemyCatalog(catalog);
                    EnemyDefinition definition = catalog.get(id, rank);
                    EnemyMob mob = catalog.spawn(id, world, 0, definition.baseHealth(), definition.price(), rank);
                    world.enemies().add(mob);

                    live(world, 1, LIFETIME_TICKS);
                    mob.doDamage(LETHAL);
                    live(world, LIFETIME_TICKS + 1, 2 * LIFETIME_TICKS);

                    assertThat(mob.isDead()).as(id + " at " + rank).isTrue();
                }
            }
        }
    }

    private static Optional<String> spawnedId(AbilityAction action) {
        return switch (action) {
            case SpawnEnemiesAction spawn -> Optional.of(spawn.definitionId());
            case ApplyEffectAction ignored -> Optional.empty();
            case LeaveZoneAction ignored -> Optional.empty();
        };
    }

    private static void live(GameWorld world, int fromTick, int toTick) {
        for (int t = fromTick; t <= toTick; t++) {
            for (EnemyMob enemy : world.enemies().getEnemies()) {
                enemy.doTick(t);
            }
            world.zones().doTick(t);
        }
    }
}
