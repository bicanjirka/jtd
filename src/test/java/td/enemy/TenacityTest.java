package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.stat.StatCurves;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TenacityTest {

    private static DefinedEnemyMob mobWithTenacity(float tenacity) {
        return EnemyFixtures.liveMob(WorldFixtures.newWorld(),
                EnemyFixtures.simpleDefinition("tenacious").withTenacity(tenacity));
    }

    private static int ticksLeft(DefinedEnemyMob mob, EffectKind kind) {
        return mob.activeEffects().stream().filter(effect -> effect.kind() == kind).findFirst().orElseThrow()
                .remainingTicks();
    }

    @Test
    void theFactorShrinksAboveZeroAndGrowsBelow() {
        assertThat(StatCurves.durationFactor(0f)).isEqualTo(1f);
        assertThat(StatCurves.durationFactor(100f)).isEqualTo(0.5f);
        assertThat(StatCurves.durationFactor(-100f)).isEqualTo(2f);
    }

    @Test
    void itScalesWhatDoesNotHelpTheEnemyEitherWay() {
        DefinedEnemyMob tough = mobWithTenacity(100f);
        DefinedEnemyMob frail = mobWithTenacity(-100f);

        tough.applyEffect(Effect.exposed(100, DamageSink.none()));
        frail.applyEffect(Effect.exposed(100, DamageSink.none()));

        assertThat(ticksLeft(tough, EffectKind.EXPOSED)).isEqualTo(50);
        assertThat(ticksLeft(frail, EffectKind.EXPOSED)).isEqualTo(200);
    }

    @Test
    void itLeavesWhatHelpsTheEnemyAlone() {
        DefinedEnemyMob tough = mobWithTenacity(100f);

        tough.applyEffect(Effect.hastened(100, DamageSink.none()));

        assertThat(ticksLeft(tough, EffectKind.HASTENED)).isEqualTo(100);
    }

    @Test
    void itNeverBringsAnEffectBelowATick() {
        DefinedEnemyMob tough = mobWithTenacity(10_000f);

        tough.applyEffect(Effect.exposed(1, DamageSink.none()));

        assertThat(ticksLeft(tough, EffectKind.EXPOSED)).isEqualTo(1);
    }

    @Test
    void itCutsAPoolsTotalWithItsTime() {
        DefinedEnemyMob plain = mobWithTenacity(0f);
        DefinedEnemyMob tough = mobWithTenacity(100f);

        plain.applyEffect(Effect.burn(Damage.magic(1000), 100, DamageSink.none()));
        tough.applyEffect(Effect.burn(Damage.magic(1000), 100, DamageSink.none()));

        assertThat(tough.activeEffects().getFirst().remainingPoolDamage()
                / plain.activeEffects().getFirst().remainingPoolDamage()).isCloseTo(0.5f, within(0.05f));
    }
}
