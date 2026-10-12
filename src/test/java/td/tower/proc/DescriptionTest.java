package td.tower.proc;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.enemy.Rank;
import td.tower.TowerCapability;
import td.tower.TowerModifiers;
import td.tower.TowerStatus;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DescriptionTest {

    @Test
    void aConditionNamesTheRanksItCovers() {
        HitBonus headhunter = HitBonus.when(new HitCondition.TargetRankAtLeast(Rank.ELITE), HitAdjustment.damage(0.4f));

        assertThat(headhunter.describe()).isEqualTo(new Description("Vs Elite and Boss", "Damage +40%"));
    }

    @Test
    void aScalingSaysWhatItCountsPer() {
        HitBonus toll = HitBonus.per(new HitScaling.PerStackOf(EffectKind.TOLL), HitAdjustment.damage(0.1f));

        assertThat(toll.describe()).isEqualTo(new Description("Per Toll stack", "Damage +10%"));
    }

    @Test
    void aPayloadSaysWhatItApplies() {
        Proc hollowPoint = Proc.onCrit(new ProcPayload.ApplyStacks(EffectKind.VULNERABLE, 1))
                .when(new ProcCondition.HitKindIn(Set.of(HitKind.DIRECT)));

        assertThat(hollowPoint.describe()).isEqualTo(new Description("On crit, shots", "Vulnerable +1"));
    }

    @Test
    void aStatusSaysHowLongItLastsAndWhatItGives() {
        TowerStatus fifth = TowerStatus.nextShots("fifth", 1, TowerModifiers.critMultiplier(0.5f))
                .withCapability(TowerCapability.GUARANTEED_CRIT);

        Proc proc = Proc.everyNthShot(5, new ProcPayload.GrantStatus(fifth));

        assertThat(proc.describe()).isEqualTo(new Description("Every 5th shot", "next shot: Crit damage +50%, Guaranteed crit"));
    }
}
