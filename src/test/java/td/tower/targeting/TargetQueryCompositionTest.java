package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TargetQueryCompositionTest {

    @Test
    void allMatchesEveryEnemyAndActsAsTheIdentityForAnd() {
        FakeEnemyMob a = FakeEnemyMob.at(0, 0);
        FakeEnemyMob b = FakeEnemyMob.at(100, 100);
        EnemyRegistry enemies = () -> new EnemyMob[]{a, b};
        TargetQuery inRangeOfA = InRangeTargetQuery.anyType(0, 0, 5);

        List<EnemyMob> combined = TargetQuery.all().and(inRangeOfA).matching(enemies);

        assertThat(combined).containsExactly(a);
        assertThat(combined).isEqualTo(inRangeOfA.matching(enemies));
    }

    @Test
    void noneMatchesNothingAndShortCircuitsWithoutEvaluatingTheOtherSide() {
        EnemyRegistry enemies = () -> new EnemyMob[]{FakeEnemyMob.at(0, 0)};
        TargetQuery throwsIfEvaluated = registry -> {
            throw new AssertionError("none().and(...) must not evaluate the other side");
        };

        List<EnemyMob> combined = TargetQuery.none().and(throwsIfEvaluated).matching(enemies);

        assertThat(combined).isEmpty();
    }

    @Test
    void andComputesTheIntersectionOfTwoQueries() {
        FakeEnemyMob inRangeGhost = FakeEnemyMob.at(0, 0).withType(EnemyMob.type.Invisible);
        FakeEnemyMob inRangeNormal = FakeEnemyMob.at(1, 1);
        FakeEnemyMob outOfRangeGhost = FakeEnemyMob.at(100, 100).withType(EnemyMob.type.Invisible);
        EnemyRegistry enemies = () -> new EnemyMob[]{inRangeGhost, inRangeNormal, outOfRangeGhost};

        TargetQuery inRange = InRangeTargetQuery.anyType(0, 0, 5);
        TargetQuery ghosts = OfTypeTargetQuery.of(EnemyMob.type.Invisible);

        assertThat(inRange.and(ghosts).matching(enemies)).containsExactly(inRangeGhost);
    }
}
