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
}
