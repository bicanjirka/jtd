package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.tower.buff.TowerBuff;

import static org.assertj.core.api.Assertions.assertThat;

class UpgradeNodeTest {

    @Test
    void ofStartsWithNoBuffNoPrerequisiteAndNoExtraEffect() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.HEAD, "Name", 10);

        assertThat(node.statBonus()).isEqualTo(TowerBuff.none());
        assertThat(node.requires().isSatisfied(null, null)).isTrue();
        assertThat(node.extraEffect()).isEmpty();
    }

    @Test
    void bonusesListEveryNonZeroBuffAxisWithASignedPercentage() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.HEAD, "Veteran", 30)
                .withBuff(TowerBuff.damage(0.3f).withRange(0.1f).withBounty(0.25f).withCritChance(0.15f))
                .withXp(150);

        assertThat(node.bonuses()).containsExactly(new UpgradeBonus("Damage", "+30%"), new UpgradeBonus("Range", "+10%"),
                new UpgradeBonus("Bounty", "+25%"), new UpgradeBonus("Crit chance", "+15%"));
    }

    @Test
    void bonusesEndWithTheExtraEffectInWordsAfterAnyBuffAxes() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.HEAD, "Siege", 35)
                .withBuff(TowerBuff.damage(0.35f))
                .withExtraEffect("+30% splash radius");

        assertThat(node.bonuses()).containsExactly(new UpgradeBonus("Damage", "+35%"),
                new UpgradeBonus("+30% splash radius", ""));
    }

    @Test
    void aNegativeAxisIsABonusWithAMinusSign() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.SPECIAL, "Concussive", 35)
                .withBuff(TowerBuff.fireRate(-0.5f))
                .withExtraEffect("blast applies slow");

        assertThat(node.bonuses()).containsExactly(new UpgradeBonus("Fire rate", "-50%"),
                new UpgradeBonus("Blast applies slow", ""));
    }
}
