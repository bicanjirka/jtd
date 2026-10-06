package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import static org.assertj.core.api.Assertions.assertThat;

class WithEffectTargetQueryTest {

    private static FakeEnemyMob marked(double x) {
        FakeEnemyMob mob = FakeEnemyMob.at(x, 0);
        mob.applyEffect(Effect.marked(100, d -> {
        }));
        return mob;
    }

    @Test
    void matchesOnlyEnemiesInReachCarryingOneOfTheEffects() {
        FakeEnemyMob inReach = marked(50);
        FakeEnemyMob tooFar = marked(500);
        FakeEnemyMob plain = FakeEnemyMob.at(50, 0);

        var matches = new WithEffectTargetQuery(0, 0, 100f, EffectKind.MARKED, EffectKind.REVEALED)
                .matching(() -> new EnemyMob[]{inReach, tooFar, plain});

        assertThat(matches).containsExactly(inReach);
    }

    @Test
    void joiningTwoQueriesGivesEveryMatchOnce() {
        FakeEnemyMob both = marked(50);
        FakeEnemyMob onlyNear = FakeEnemyMob.at(60, 0);
        FakeEnemyMob onlyMarked = marked(150);
        TargetQuery near = InRangeTargetQuery.visible(0, 0, 100f);
        TargetQuery withEffect = new WithEffectTargetQuery(0, 0, 200f, EffectKind.MARKED);

        var matches = near.or(withEffect).matching(() -> new EnemyMob[]{both, onlyNear, onlyMarked});

        assertThat(matches).containsExactly(both, onlyNear, onlyMarked);
    }
}
