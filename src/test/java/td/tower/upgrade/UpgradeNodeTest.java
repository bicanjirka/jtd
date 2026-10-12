package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.stat.StatModifiers;
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

        assertThat(node.bonuses()).containsExactly(new UpgradeBonus("Damage", "+30%"), new UpgradeBonus("Range", "+10%"),
                new UpgradeBonus("Crit chance", "+15%"), new UpgradeBonus("Bounty", "+25%"));
    }

    @Test
    void bonusesEndWithTheExtraEffectInWordsAfterAnyBuffAxes() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.HEAD, "Siege", 35)
                .withModifiers(TowerModifiers.damagePercent(0.35f))
                .withNote("+30% splash radius");

        assertThat(node.bonuses()).containsExactly(new UpgradeBonus("Damage", "+35%"),
                new UpgradeBonus("+30% splash radius", ""));
    }

    @Test
    void aNegativeAxisIsABonusWithAMinusSign() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.SPECIAL, "Concussive", 35)
                .withModifiers(TowerModifiers.attackSpeed(-0.5f))
                .withNote("blast applies slow");

        assertThat(node.bonuses()).containsExactly(new UpgradeBonus("Attack speed", "-50%"),
                new UpgradeBonus("Blast applies slow", ""));
    }

    @Test
    void aRangeNodeReadsInCells() {
        UpgradeNode range = StandardBaseSlot.nodes(10, 4.0f).getFirst();

        assertThat(range.bonuses()).containsExactly(new UpgradeBonus("Range", "+0.6"));
    }

    @Test
    void anEmissionRowSaysWhatNearbyTowersGet() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.HEAD, "Keen Edge", 10)
                .withEmission(TowerModifiers.critMultiplier(0.5f));

        assertThat(node.bonuses()).containsExactly(new UpgradeBonus("Nearby crit damage", "+50%"));
    }

    private static UpgradeNode sniperNode(String name) {
        return new td.tower.SniperTower(td.fixtures.WorldFixtures.newWorld(), 0, 0).upgradeTree().nodes().stream()
                .filter(node -> node.displayName().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void momentumSaysWhomItAimsAtAndWhatACritAndAKillGrant() {
        assertThat(sniperNode("Momentum").bonuses()).containsExactly(new UpgradeBonus("Targets", "highest rank"),
                new UpgradeBonus("On crit, shots", "next shot: Damage +400%, Armor penetration +100%, Plating penetration +100%"),
                new UpgradeBonus("On kill, shots", "5 s: Attack speed +100%"));
    }

    @Test
    void headhunterSaysWhomItAimsAtAndWhomItHitsHarder() {
        assertThat(sniperNode("Headhunter").bonuses()).containsExactly(new UpgradeBonus("Targets", "highest rank"),
                new UpgradeBonus("Vs Elite and Boss", "Damage +40%"));
    }
}
