package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HurtAgilityTraitTest {

    private static DefinedEnemyMob evasive() {
        GameWorld world = WorldFixtures.newWorldOnBoard(1, 1001, 1001);
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.register(EnemyDefinition.of("evasive", "Evasive", 100, 1, 0f, BodyArchetype.CIRCLE)
                .withTraits(List.of(new HurtAgilityTrait(60f))));
        return (DefinedEnemyMob) catalog.spawn("evasive", world, 0, 100, 1, Rank.GRUNT);
    }

    @Test
    void agilityGrowsAsHealthDrops() {
        DefinedEnemyMob enemy = evasive();
        float unhurt = enemy.inspect().stat(EnemyStat.AGILITY);

        enemy.doDamage(Damage.physical(enemy.getHealth() / 2));

        assertThat(unhurt).isZero();
        assertThat(enemy.inspect().stat(EnemyStat.AGILITY)).isEqualTo(30f);
    }
}
