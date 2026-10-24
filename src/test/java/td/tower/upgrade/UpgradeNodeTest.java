package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.enemy.Rank;
import td.stat.StatModifiers;
import td.term.TermText;
import td.tower.TowerModifiers;
import td.tower.TowerStatus;
import td.tower.TowerTerm;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitBonus;
import td.tower.proc.HitCondition;
import td.tower.proc.HitKind;
import td.tower.proc.Proc;
import td.tower.proc.ProcCondition;
import td.tower.proc.ProcPayload;
import td.tower.targeting.TargetPriority;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UpgradeNodeTest {

    @Test
    void ofStartsWithNoBuffNoPrerequisiteAndNoExtraEffect() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.HEAD, "Name", 10);

        assertThat(node.effects().modifiers()).isEqualTo(StatModifiers.none());
        assertThat(node.requires()).isEqualTo(UpgradeCondition.always());
        assertThat(node.effects().note()).isEmpty();
    }

    @Test
    void bonusesListEveryNonZeroBuffAxisWithASignedPercentage() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.HEAD, "Veteran", 30)
                .withModifiers(TowerModifiers.damagePercent(0.3f).plus(TowerModifiers.rangePercent(0.1f)).plus(TowerModifiers.bounty(0.25f)).plus(TowerModifiers.critChance(0.15f)))
                .withXp(150);

        assertThat(node.effects().statRows()).containsExactly(new UpgradeBonus("Damage", "+30%"), new UpgradeBonus("Range", "+10%"),
                new UpgradeBonus("Crit chance", "+15%"), new UpgradeBonus("Bounty", "+25%"));
    }

    @Test
    void theExtraEffectIsASentenceApartFromTheBuffAxes() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.HEAD, "Siege", 35)
                .withModifiers(TowerModifiers.damagePercent(0.35f))
                .withNote("+30% splash radius");

        assertThat(node.effects().statRows()).containsExactly(new UpgradeBonus("Damage", "+35%"));
        assertThat(node.effects().sentences()).extracting(TermText::plain).containsExactly("+30% splash radius");
    }

    @Test
    void aNegativeAxisIsABonusWithAMinusSign() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.SPECIAL, "Concussive", 35)
                .withModifiers(TowerModifiers.attackSpeed(-0.5f))
                .withNote("blast applies slow");

        assertThat(node.effects().statRows()).containsExactly(new UpgradeBonus("Attack speed", "-50%"));
        assertThat(node.effects().sentences()).extracting(TermText::plain).containsExactly("blast applies slow");
    }

    @Test
    void aRangeNodeReadsInCells() {
        UpgradeNode range = StandardBaseSlot.nodes(10, 4.0f).getFirst();

        assertThat(range.effects().statRows()).containsExactly(new UpgradeBonus("Range", "+0.6"));
    }

    @Test
    void anEmissionRowSaysWhatNearbyTowersGet() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.HEAD, "Keen Edge", 10)
                .withEmission(TowerModifiers.critMultiplier(0.5f));

        assertThat(node.effects().statRows()).containsExactly(new UpgradeBonus("Nearby crit damage", "+50%"));
    }

    private static List<String> sentences(UpgradeNode node) {
        return node.effects().sentences().stream().map(TermText::plain).toList();
    }

    @Test
    void aNodeThatSetsTheTargetSaysWhomItAimsAt() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.SPECIAL, "Hunter", 10).withTargeting(TargetPriority.HIGHEST_RANK);

        assertThat(node.effects().statRows()).containsExactly(new UpgradeBonus("Targets", "highest rank"));
    }

    @Test
    void aNodesHitBonusesAndProcsAreItsSentences() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.SPECIAL, "Hunter", 10)
                .withHitBonus(HitBonus.when(new HitCondition.TargetRankAtLeast(Rank.ELITE), HitAdjustment.damage(0.75f)))
                .withProc(Proc.onKill(new ProcPayload.GrantStatus(TowerStatus.timed(TowerTerm.HASTE,
                        TowerModifiers.attackSpeed(1f), 100))).when(new ProcCondition.HitKindIn(Set.of(HitKind.DIRECT))));

        assertThat(sentences(node)).containsExactlyInAnyOrder("+75% damage against Elite and Boss enemies",
                "a shot that kills grants Haste");
    }
}
