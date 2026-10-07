package td.tower.splash;

import org.junit.jupiter.api.Test;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.FakeEnemyMob;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SympathyCopierTest {

    private static void give(FakeEnemyMob enemy, Effect effect) {
        enemy.applyEffect(effect);
    }

    @Test
    void anEnemyThatLacksADebuffGetsItAsTheCarrierHasItWithTheTimeItHasLeft() {
        FakeEnemyMob carrier = FakeEnemyMob.at(0, 0);
        FakeEnemyMob other = FakeEnemyMob.at(10, 0);
        give(carrier, Effect.sundered(3, 70, d -> {
        }));
        give(carrier, Effect.vulnerable(2, 50, d -> {
        }));
        give(carrier, Effect.exposed(30, d -> {
        }));

        List<Effect> copies = SympathyCopier.missingFrom(carrier, other);

        assertThat(copies).extracting(Effect::kind)
                .containsExactlyInAnyOrder(EffectKind.SUNDERED, EffectKind.VULNERABLE, EffectKind.EXPOSED);
        assertThat(copies).filteredOn(copy -> copy.kind() == EffectKind.SUNDERED).singleElement()
                .satisfies(copy -> {
                    assertThat(copy.stacks()).isEqualTo(3);
                    assertThat(copy.remainingTicks()).isEqualTo(70);
                });
    }

    @Test
    void anEnemyOnlyGetsTheStacksItLacksSoSharingBackAndForthNeverClimbs() {
        FakeEnemyMob carrier = FakeEnemyMob.at(0, 0);
        FakeEnemyMob other = FakeEnemyMob.at(10, 0);
        give(carrier, Effect.sundered(3, 70, d -> {
        }));
        give(other, Effect.sundered(1, 40, d -> {
        }));

        SympathyCopier.missingFrom(carrier, other).forEach(other::applyEffect);

        assertThat(other.effectStacks(EffectKind.SUNDERED)).isEqualTo(3);
        assertThat(SympathyCopier.missingFrom(carrier, other)).isEmpty();
        assertThat(SympathyCopier.missingFrom(other, carrier)).isEmpty();
    }

    @Test
    void aChillIsToppedUpToTheCarriersLevelAndNoFurtherAndAnExposedOneIsNotRefreshed() {
        FakeEnemyMob carrier = FakeEnemyMob.at(0, 0);
        FakeEnemyMob other = FakeEnemyMob.at(10, 0);
        give(carrier, Effect.chill(0.5f, 100, d -> {
        }));
        give(other, Effect.chill(0.2f, 100, d -> {
        }));
        give(carrier, Effect.exposed(60, d -> {
        }));
        give(other, Effect.exposed(10, d -> {
        }));

        List<Effect> copies = SympathyCopier.missingFrom(carrier, other);

        assertThat(copies).singleElement().satisfies(copy -> {
            assertThat(copy.kind()).isEqualTo(EffectKind.CHILL);
            assertThat(copy.fuelLevel()).isCloseTo(0.3f, within(0.001f));
            assertThat(copy.ticksToFade()).isEqualTo(100);
        });
    }

    @Test
    void whatTheCarrierSuffersButSympathyDoesNotShareStaysWithIt() {
        FakeEnemyMob carrier = FakeEnemyMob.at(0, 0);
        FakeEnemyMob other = FakeEnemyMob.at(10, 0);
        give(carrier, Effect.freeze(40, d -> {
        }));
        give(carrier, Effect.hex(EffectKind.DOOM, 80, d -> {
        }));

        assertThat(SympathyCopier.missingFrom(carrier, other)).isEmpty();
    }
}
