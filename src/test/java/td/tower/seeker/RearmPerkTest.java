package td.tower.seeker;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RearmPerkTest {

    private static final class CountingActions implements SeekerActions {

        private int rearms;

        @Override
        public List<EnemyMob> burst(EnemyMob center, float share, float radiusCells) {
            return List.of();
        }

        @Override
        public void silence(EnemyMob target, int ticks) {
        }

        @Override
        public void applyStacks(EnemyMob target, EffectKind kind, int stacks) {
        }

        @Override
        public void rearm() {
            this.rearms++;
        }

        @Override
        public void freeze(EnemyMob target, float strength) {
        }

        @Override
        public void dispel(EnemyMob target) {
        }

        @Override
        public void spot(EnemyMob target, int ticks) {
        }
    }

    private static Impact impact(boolean froze, boolean rearmed) {
        return new Impact(FakeEnemyMob.at(0, 0), Impact.Before.NOTHING, froze, rearmed);
    }

    @Test
    void aMissileThatFreezesItsTargetRearms() {
        CountingActions actions = new CountingActions();

        new RearmPerk().react(impact(true, false), actions);

        assertThat(actions.rearms).isEqualTo(1);
    }

    @Test
    void aRearmedMissilesFreezeRearmsNothing() {
        CountingActions actions = new CountingActions();

        new RearmPerk().react(impact(true, true), actions);

        assertThat(actions.rearms).isZero();
    }

    @Test
    void aMissileThatFreezesNothingRearmsNothing() {
        CountingActions actions = new CountingActions();

        new RearmPerk().react(impact(false, false), actions);

        assertThat(actions.rearms).isZero();
    }
}
