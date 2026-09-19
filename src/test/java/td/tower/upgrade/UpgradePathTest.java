package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.tower.buff.TowerBuff;

import static org.assertj.core.api.Assertions.assertThat;

class UpgradePathTest {

    @Test
    void theFourArgumentConstructorDefaultsExtraEffectToEmpty() {
        UpgradePath path = new UpgradePath("Veteran", 30, TowerBuff.amplifying(0.2f), UpgradeCondition.always());

        assertThat(path.extraEffect()).isEmpty();
    }

    @Test
    void describeListsEveryNonZeroBuffAxisWithASignedPercentage() {
        UpgradePath path = new UpgradePath("Veteran", 30,
                TowerBuff.none().withDamage(0.3f).withRange(0.1f).withBounty(0.25f).withCritChance(0.15f),
                new KillCountCondition(10));

        assertThat(path.describe()).isEqualTo(
                "Veteran (10 kills): +30% damage, +10% range, +25% bounty, +15% crit chance");
    }

    @Test
    void describeOmitsAnyAxisTheBuffLeavesAtZero() {
        UpgradePath path = new UpgradePath("Overclock", 25, new TowerBuff(-0.2f, 0f, 0.4f, 0f),
                UpgradeCondition.always());

        assertThat(path.describe()).isEqualTo("Overclock (money only): -20% damage, +40% fire rate");
    }

    @Test
    void describeAppendsTheExtraEffectAfterAnyBuffAxes() {
        UpgradePath path = new UpgradePath("Siege", 35, new TowerBuff(0.35f, 0f, 0f, 0f),
                new DamageDealtCondition(20000), "+30% splash radius");

        assertThat(path.describe()).isEqualTo("Siege (200.0 damage dealt): +35% damage, +30% splash radius");
    }

    @Test
    void describeWithOnlyAnExtraEffectAndNoBuffAxesOmitsTheLeadingComma() {
        UpgradePath path = new UpgradePath("Overcharged Array", 35, TowerBuff.none(), new ClusterCondition(2),
                "sweeps 40% faster");

        assertThat(path.describe()).isEqualTo("Overcharged Array (2 nearby towers): sweeps 40% faster");
    }
}
