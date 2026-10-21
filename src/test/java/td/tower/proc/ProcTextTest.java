package td.tower.proc;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.enemy.Rank;
import td.term.TermText;
import td.tower.TowerCapability;
import td.tower.TowerModifiers;
import td.tower.TowerStatus;
import td.tower.TowerTerm;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProcTextTest {

    @Test
    void aConditionNamesTheRanksItCovers() {
        HitBonus headhunter = HitBonus.when(new HitCondition.TargetRankAtLeast(Rank.ELITE), HitAdjustment.damage(0.4f));

        assertThat(headhunter.describe().plain()).isEqualTo("+40% damage against Elite and Boss enemies");
    }

    @Test
    void aScalingSaysWhatItCountsPer() {
        HitBonus toll = HitBonus.per(new HitScaling.PerStackOf(EffectKind.TOLL), HitAdjustment.damage(0.1f));

        assertThat(toll.describe().plain()).isEqualTo("+10% damage per stack of Toll");
        assertThat(toll.describe().mentions()).containsExactly(EffectKind.TOLL);
    }

    @Test
    void aPayloadSaysWhatItApplies() {
        Proc hollowPoint = Proc.onCrit(new ProcPayload.ApplyStacks(EffectKind.VULNERABLE, 1))
                .when(new ProcCondition.HitKindIn(Set.of(HitKind.DIRECT)));

        assertThat(hollowPoint.describe().plain()).isEqualTo("a crit shot applies a stack of Vulnerable");
    }

    @Test
    void aOneShotStatusOnACountedShotIsThatShot() {
        TowerStatus fifth = TowerStatus.nextShots(TowerTerm.DEADEYE_ROUND, 1, TowerModifiers.critMultiplier(0.5f))
                .withCapability(TowerCapability.GUARANTEED_CRIT);

        TermText text = Proc.everyNthShot(5, new ProcPayload.GrantStatus(fifth)).describe();

        assertThat(text.plain()).isEqualTo("every 5th shot is a Deadeye Round");
        assertThat(text.spans()).anySatisfy(span -> assertThat(span).isInstanceOfSatisfying(TermText.Mention.class,
                mention -> assertThat(mention.meaning()).isEqualTo(fifth.describe())));
    }

    @Test
    void aKillTriggerNamesTheHitThatKills() {
        TowerStatus haste = TowerStatus.timed(TowerTerm.HASTE, TowerModifiers.attackSpeed(0.5f), 60);
        Proc proc = Proc.onKill(new ProcPayload.GrantStatus(haste))
                .when(new ProcCondition.HitKindIn(Set.of(HitKind.DIRECT)));

        assertThat(proc.describe().plain()).isEqualTo("a shot that kills grants Haste");
    }

    @Test
    void aTargetConditionSaysWhatTheEnemyCarries() {
        Proc proc = Proc.onHit(new ProcPayload.BreakShield(0.25f))
                .when(new ProcCondition.TargetHasAnyOf(Set.of(EffectKind.SHIELD)));

        assertThat(proc.describe().plain()).isEqualTo("each hit on an enemy with Shield takes 25% of what Shield has left");
    }
}
