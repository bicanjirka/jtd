package td.tower;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveTowerStatusesTest {

    private static final TowerStatus HASTE = TowerStatus.timed(TowerTerm.HASTE, TowerModifiers.attackSpeed(1f), 100);
    private static final TowerStatus FRENZY = TowerStatus.nextShots(TowerTerm.FRENZY, 3, TowerModifiers.attackSpeed(1f));

    private final ActiveTowerStatuses statuses = new ActiveTowerStatuses();

    @Test
    void aSameIdGrantRestartsTheClock() {
        this.statuses.grant(HASTE, 0, 1f);
        this.statuses.grant(HASTE, 50, 1f);

        boolean expiredAtFirstEnd = this.statuses.expire(100);

        assertThat(expiredAtFirstEnd).isFalse();
        assertThat(this.statuses.expire(150)).isTrue();
        assertThat(this.statuses.modifiers().isNone()).isTrue();
    }

    @Test
    void aStatusThatKeepsARunningCopyIsNotRestarted() {
        TowerStatus keeping = FRENZY.keepingARunningOne();
        this.statuses.grant(keeping, 0, 1f);
        this.statuses.spendShot();
        this.statuses.spendShot();

        this.statuses.grant(keeping, 10, 1f);
        this.statuses.spendShot();

        assertThat(this.statuses.modifiers().isNone()).isTrue();
    }

    @Test
    void aShotStatusIsSpentOneShotAtATime() {
        this.statuses.grant(FRENZY, 0, 1f);

        this.statuses.spendShot();
        this.statuses.spendShot();
        boolean stillThere = !this.statuses.modifiers().isNone();
        this.statuses.spendShot();

        assertThat(stillThere).isTrue();
        assertThat(this.statuses.modifiers().isNone()).isTrue();
    }

    @Test
    void aShotStatusIsLengthenedOnlyWhenItSaysSo() {
        ActiveTowerStatuses lengthened = new ActiveTowerStatuses();
        lengthened.grant(FRENZY.lengthenedByBuffs(), 0, 2f);
        this.statuses.grant(FRENZY, 0, 2f);

        for (int shot = 0; shot < 3; shot++) {
            lengthened.spendShot();
            this.statuses.spendShot();
        }

        assertThat(lengthened.modifiers().isNone()).isFalse();
        assertThat(this.statuses.modifiers().isNone()).isTrue();
    }

    @Test
    void aTimedStatusExpires() {
        this.statuses.grant(HASTE, 0, 2f);

        assertThat(this.statuses.expire(199)).isFalse();
        assertThat(this.statuses.expire(200)).isTrue();
    }

    @Test
    void twoDifferentStatusesStack() {
        this.statuses.grant(HASTE, 0, 1f);
        this.statuses.grant(FRENZY, 0, 1f);

        assertThat(this.statuses.modifiers().flat(TowerStat.ATTACK_SPEED)).isEqualTo(2f);
    }
}
