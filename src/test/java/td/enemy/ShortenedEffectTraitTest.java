package td.enemy;

import org.junit.jupiter.api.Test;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShortenedEffectTraitTest {

    private static DefinedEnemyMob mobWith(Trait trait) {
        return EnemyFixtures.liveMob(WorldFixtures.newWorld(), EnemyFixtures.simpleDefinition("shortened")
                .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(trait))));
    }

    private static int ticksLeft(DefinedEnemyMob mob, EffectKind kind) {
        return mob.activeEffects().stream().filter(effect -> effect.kind() == kind).findFirst().orElseThrow()
                .remainingTicks();
    }

    @Test
    void itShortensItsOwnKindByItsShare() {
        DefinedEnemyMob mob = mobWith(new ShortenedEffectTrait(EffectKind.DAZED, 0.25f));

        mob.applyEffect(Effect.dazed(100, DamageSink.none()));

        assertThat(ticksLeft(mob, EffectKind.DAZED)).isEqualTo(25);
    }

    @Test
    void itLeavesEveryOtherKindAlone() {
        DefinedEnemyMob mob = mobWith(new ShortenedEffectTrait(EffectKind.DAZED, 0.25f));

        mob.applyEffect(Effect.exposed(100, DamageSink.none()));

        assertThat(ticksLeft(mob, EffectKind.EXPOSED)).isEqualTo(100);
    }

    @Test
    void itMultipliesWithTenacity() {
        DefinedEnemyMob mob = EnemyFixtures.liveMob(WorldFixtures.newWorld(), EnemyFixtures.simpleDefinition("both")
                .withTenacity(100f)
                .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(new ShortenedEffectTrait(EffectKind.DAZED, 0.5f)))));

        mob.applyEffect(Effect.dazed(100, DamageSink.none()));

        assertThat(ticksLeft(mob, EffectKind.DAZED)).isEqualTo(25);
    }
}
