package td.tower.proc;

import org.junit.jupiter.api.Test;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.Rank;
import td.fixtures.FakeEnemyMob;
import td.stat.DisruptionPenalty;
import td.stat.StatModifiers;
import td.tower.TowerBaseStats;
import td.tower.TowerStats;
import td.tower.TowerTerm;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HitConditionTest {

    private static final TowerStats SHOT = TowerStats.of(new TowerBaseStats(10, 3f, 20), StatModifiers.none(),
            StatModifiers.none(), DisruptionPenalty.none(), 10, Set.of());

    private static HitContext hitOn(FakeEnemyMob target) {
        return HitContext.of(target, HitKind.DIRECT, 0.5f, SHOT);
    }

    private static HitContext anyHit() {
        return hitOn(FakeEnemyMob.at(0, 0));
    }

    @Test
    void aFreshTargetConditionHoldsOnlyOnTheFirstAttackAtATarget() {
        HitCondition fresh = new HitCondition.FreshTarget();

        assertThat(fresh.holds(anyHit().withFocus(0, true))).isTrue();
        assertThat(fresh.holds(anyHit().withFocus(0, false))).isFalse();
    }

    @Test
    void aFocusConditionHoldsFromItsFocusOn() {
        HitCondition focused = new HitCondition.FocusAtLeast(TowerTerm.STEADY_AIM, 2);

        assertThat(focused.holds(anyHit().withFocus(1, false))).isFalse();
        assertThat(focused.holds(anyHit().withFocus(2, false))).isTrue();
    }

    @Test
    void aHealthConditionHoldsOnlyBelowItsShare() {
        HitCondition wounded = new HitCondition.TargetHealthBelow(0.25f);

        assertThat(wounded.holds(hitOn(FakeEnemyMob.at(0, 0).atHealthFraction(0.2f)))).isTrue();
        assertThat(wounded.holds(hitOn(FakeEnemyMob.at(0, 0).atHealthFraction(0.25f)))).isFalse();
    }

    @Test
    void aStoppedConditionHoldsOnAnEnemyThatCannotMove() {
        FakeEnemyMob frozen = FakeEnemyMob.at(0, 0);
        frozen.reportFrozen();

        assertThat(new HitCondition.TargetStopped().holds(hitOn(frozen))).isTrue();
        assertThat(new HitCondition.TargetStopped().holds(anyHit())).isFalse();
    }

    @Test
    void anEffectConditionHoldsWhenTheTargetCarriesAnyOfItsKinds() {
        HitCondition marked = new HitCondition.TargetHasAnyOf(Set.of(EffectKind.MARKED, EffectKind.EXPOSED));
        FakeEnemyMob exposed = FakeEnemyMob.at(0, 0);
        exposed.applyEffect(Effect.exposed(100, DamageSink.none()));

        assertThat(marked.holds(hitOn(exposed))).isTrue();
        assertThat(marked.holds(anyHit())).isFalse();
    }

    @Test
    void aRangeConditionHoldsOnlyPastItsShareOfTheRange() {
        HitCondition far = new HitCondition.BeyondRangeShare(0.5f);

        assertThat(far.holds(HitContext.of(FakeEnemyMob.at(0, 0), HitKind.DIRECT, 0.6f, SHOT))).isTrue();
        assertThat(far.holds(HitContext.of(FakeEnemyMob.at(0, 0), HitKind.DIRECT, 0.5f, SHOT))).isFalse();
    }

    @Test
    void aHitKindConditionHoldsOnlyForItsKinds() {
        HitCondition direct = new HitCondition.HitKindIn(Set.of(HitKind.DIRECT));

        assertThat(direct.holds(anyHit())).isTrue();
        assertThat(direct.holds(HitContext.of(FakeEnemyMob.at(0, 0), HitKind.BLAST, 0.5f, SHOT))).isFalse();
    }

    @Test
    void anAllConditionHoldsOnlyWhenEveryPartDoes() {
        HitCondition woundedBoss = new HitCondition.All(List.of(new HitCondition.TargetRankAtLeast(Rank.BOSS),
                new HitCondition.TargetHealthBelow(0.5f)));

        assertThat(woundedBoss.holds(hitOn(FakeEnemyMob.at(0, 0).ranked(Rank.BOSS).atHealthFraction(0.2f)))).isTrue();
        assertThat(woundedBoss.holds(hitOn(FakeEnemyMob.at(0, 0).ranked(Rank.BOSS)))).isFalse();
        assertThat(woundedBoss.holds(hitOn(FakeEnemyMob.at(0, 0).atHealthFraction(0.2f)))).isFalse();
    }
}
