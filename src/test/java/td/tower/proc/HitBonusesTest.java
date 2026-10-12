package td.tower.proc;

import org.junit.jupiter.api.Test;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.Rank;
import td.fixtures.FakeEnemyMob;
import td.stat.DisruptionPenalty;
import td.stat.StatModifiers;
import td.tower.TowerBaseStats;
import td.tower.TowerStats;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HitBonusesTest {

    private static final TowerStats SHOT = TowerStats.of(new TowerBaseStats(10, 3f, 20), StatModifiers.none(),
            StatModifiers.none(), DisruptionPenalty.none(), 10, Set.of());

    private static HitContext hit(FakeEnemyMob target) {
        return HitContext.of(target, HitKind.DIRECT, 0.5f, SHOT);
    }

    @Test
    void aConditionalBonusAppliesOnlyWhenItHolds() {
        HitBonus headhunter = HitBonus.when(new HitCondition.TargetRankAtLeast(Rank.ELITE), HitAdjustment.damage(0.4f));

        HitAdjustment onBoss = HitBonuses.fold(List.of(headhunter), hit(FakeEnemyMob.at(0, 0).ranked(Rank.BOSS)));
        HitAdjustment onGrunt = HitBonuses.fold(List.of(headhunter), hit(FakeEnemyMob.at(0, 0).ranked(Rank.GRUNT)));

        assertThat(onBoss.damageFactor()).isCloseTo(1.4f, within(1e-6f));
        assertThat(onGrunt).isEqualTo(HitAdjustment.none());
    }

    @Test
    void bonusesAdd() {
        List<HitBonus> bonuses = List.of(HitBonus.when(new HitCondition.Always(), HitAdjustment.damage(0.4f)),
                HitBonus.when(new HitCondition.Always(), HitAdjustment.damage(0.5f)),
                HitBonus.when(new HitCondition.Always(), HitAdjustment.critChance(0.1f)));

        HitAdjustment total = HitBonuses.fold(bonuses, hit(FakeEnemyMob.at(0, 0)));

        assertThat(total.damageFactor()).isCloseTo(1.9f, within(1e-6f));
        assertThat(total.critChance()).isCloseTo(0.1f, within(1e-6f));
    }

    @Test
    void perFocusScalesWithTheFocusUpToItsMax() {
        HitBonus huntersMark = HitBonus.per(new HitScaling.PerFocus(5), HitAdjustment.damage(0.2f));
        HitContext third = hit(FakeEnemyMob.at(0, 0)).withFocus(3, false);
        HitContext ninth = hit(FakeEnemyMob.at(0, 0)).withFocus(9, false);

        assertThat(huntersMark.adjustmentFor(third).damage()).isCloseTo(0.6f, within(1e-6f));
        assertThat(huntersMark.adjustmentFor(ninth).damage()).isCloseTo(1f, within(1e-6f));
    }

    @Test
    void perRangeShareScalesWithDistance() {
        HitBonus far = HitBonus.per(new HitScaling.PerRangeShare(), HitAdjustment.damage(1f));

        assertThat(far.adjustmentFor(hit(FakeEnemyMob.at(0, 0))).damage()).isCloseTo(0.5f, within(1e-6f));
    }

    @Test
    void perStackOfScalesWithTheTargetsStacks() {
        HitBonus toll = HitBonus.per(new HitScaling.PerStackOf(EffectKind.VULNERABLE), HitAdjustment.damage(0.1f));
        FakeEnemyMob target = FakeEnemyMob.at(0, 0);
        target.applyEffect(Effect.vulnerable(3, 100, d -> {
        }));

        assertThat(toll.adjustmentFor(hit(target)).damage()).isCloseTo(0.3f, within(1e-6f));
    }
}
