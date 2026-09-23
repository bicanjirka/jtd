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
    void describeListsEveryNonZeroBuffAxisWithASignedPercentage() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.HEAD, "Veteran", 30)
                .withBuff(TowerBuff.damage(0.3f).withRange(0.1f).withBounty(0.25f).withCritChance(0.15f))
                .withGate(new KillCountCondition(10));

        assertThat(node.describe()).isEqualTo(
                "Veteran (10 kills): +30% damage, +10% range, +25% bounty, +15% crit chance");
    }

    @Test
    void describeAppendsTheExtraEffectAfterAnyBuffAxes() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.HEAD, "Siege", 35)
                .withBuff(TowerBuff.damage(0.35f))
                .withGate(new DamageDealtCondition(20000))
                .withExtraEffect("+30% splash radius");

        assertThat(node.describe()).isEqualTo("Siege (200.0 damage dealt): +35% damage, +30% splash radius");
    }

    @Test
    void describeWithOnlyAnExtraEffectAndNoBuffAxesOmitsTheLeadingComma() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.HEAD, "Overcharged Array", 35)
                .withGate(new ClusterCondition(2))
                .withExtraEffect("sweeps 40% faster");

        assertThat(node.describe()).isEqualTo("Overcharged Array (2 nearby towers): sweeps 40% faster");
    }

    @Test
    void describeUsesTheGateNotTheRequiresCondition() {
        UpgradeNode node = UpgradeNode.of("id", UpgradeSlot.HEAD, "A2", 15)
                .withRequires(UpgradeCondition.owns("a1"))
                .withGate(new KillCountCondition(5));

        assertThat(node.describe()).startsWith("A2 (5 kills)");
    }
}
