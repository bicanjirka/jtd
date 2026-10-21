package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.stat.StatModifiers;
import td.term.TermText;
import td.tower.TowerModifiers;

import static org.assertj.core.api.Assertions.assertThat;

class UpgradeNodeTest {

    @Test
    void ofStartsWithNoBuffNoPrerequisiteAndNoExtraEffect() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.HEAD, "Name", 10);

        assertThat(node.effects().modifiers()).isEqualTo(StatModifiers.none());
        assertThat(node.requires().isSatisfied(null, null)).isTrue();
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

    private static UpgradeNode sniperNode(String name) {
        return new td.tower.SniperTower(td.fixtures.WorldFixtures.newWorld(), 0, 0).upgradeTree().nodes().stream()
                .filter(node -> node.displayName().equals(name)).findFirst().orElseThrow();
    }

    private static java.util.List<String> sentences(UpgradeNode node) {
        return node.effects().sentences().stream().map(td.term.TermText::plain).toList();
    }

    @Test
    void momentumSaysWhomItAimsAtAndWhatACritAndAKillGrant() {
        UpgradeNode momentum = sniperNode("Momentum");

        assertThat(momentum.effects().statRows()).containsExactly(new UpgradeBonus("Targets", "highest rank"));
        assertThat(sentences(momentum)).containsExactly("a crit shot grants Charged Shot",
                "a shot that kills grants Haste");
    }

    @Test
    void headhunterSaysWhomItAimsAtAndWhomItHitsHarder() {
        UpgradeNode headhunter = sniperNode("Headhunter");

        assertThat(headhunter.effects().statRows()).containsExactly(new UpgradeBonus("Targets", "highest rank"));
        assertThat(sentences(headhunter)).containsExactly("+75% damage against Elite and Boss enemies");
    }

    @Test
    void theMarksmansEyeSpecialsSayWhatTheyDoWithoutChangingWhomTheSniperAimsAt() {
        assertThat(sentences(sniperNode("Hollow Point"))).containsExactly("each shot applies a stack of Vulnerable");
        assertThat(sentences(sniperNode("Fourth Shot"))).containsExactly("every 4th shot is a Deadeye Round");
        assertThat(sentences(sniperNode("Shatter Shot"))).containsExactly(
                "+100% crit chance, +100% crit damage against an enemy with Freeze or Daze");
    }
}
