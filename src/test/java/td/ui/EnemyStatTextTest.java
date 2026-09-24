package td.ui;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.Effect;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyFactory;
import td.enemy.Rank;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class EnemyStatTextTest {

    private final GameWorld world = WorldFixtures.newWorld();

    private DefinedEnemyMob spawn(String id, Rank rank) {
        return (DefinedEnemyMob) EnemyFactory.getEnemy(id, this.world, 0, 100, 5, rank);
    }

    @Test
    void armorShowsItsValueAndTheShareOfAHitItRemoves() {
        assertThat(EnemyStatText.statLine(EnemyStat.ARMOR, 100f)).isEqualTo("Armor 100 (-50% physical)");
        assertThat(EnemyStatText.statLine(EnemyStat.MAGIC_RESIST, -100f)).isEqualTo("Magic resist -100 (+50% magic)");
    }

    @Test
    void aPlainEnemyShowsOnlyItsSpeed() {
        String text = EnemyStatText.preview(this.spawn("c", Rank.GRUNT).inspect());

        assertThat(text).contains("Speed ").doesNotContain("Armor").doesNotContain("Resilience").doesNotContain("Traits:");
    }

    @Test
    void theArmoredPreviewListsItsResistancesAndTraits() {
        String text = EnemyStatText.preview(this.spawn("s", Rank.GRUNT).inspect());

        assertThat(text).contains("Armor 25 (-20% physical)").contains("Magic resist 25 (-20% magic)")
                .contains("Resilience 100 (immune to crits)")
                .contains("- Resists 20% of all damage").contains("- Immune to critical hits");
    }

    @Test
    void anAdaptiveTraitIsShownAsTheRangeItCanReach() {
        String text = EnemyStatText.preview(this.spawn("c", Rank.ELITE).inspect());

        assertThat(text).contains("- Adaptive: up to 67 armor or magic resist, depending on your damage mix")
                .contains("Freeze diminishing returns");
    }

    @Test
    void theLiveViewShowsCurrentHealthEffectsAndFate() {
        DefinedEnemyMob mob = this.spawn("c", Rank.GRUNT);
        mob.applyEffect(Effect.freeze(30, d -> {
        }));
        mob.doDamage(Damage.physical(4000));

        String alive = EnemyStatText.live(mob.inspect());
        mob.doDamage(Damage.physical(1_000_000));
        String killed = EnemyStatText.live(mob.inspect());

        assertThat(alive).contains("Health: 60 / 100").contains("- Frozen 1.5s").contains("Speed 0 px/s");
        assertThat(killed).contains("Killed");
    }
}
