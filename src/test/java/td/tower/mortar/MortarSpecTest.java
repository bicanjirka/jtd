package td.tower.mortar;

import org.junit.jupiter.api.Test;
import td.fixtures.BoardFixtures;
import td.tower.targeting.Viewpoint;

import static org.assertj.core.api.Assertions.assertThat;

class MortarSpecTest {

    private static final int SCALE = BoardFixtures.SCALE;
    private final MortarSpec base = MortarSpec.from(new Viewpoint(100, 100, 4.5f * SCALE, SCALE));

    @Test
    void theBaseMortarHasADeadZoneOfOneAndAHalfCellsAndNoBracketing() {
        assertThat(this.base.reach().deadZone()).isEqualTo(1.5f * SCALE);
        assertThat(this.base.bracket().active()).isFalse();
    }

    @Test
    void longBatteryGrowsTheDeadZoneToTwoAndAHalfCellsWhateverTheRange() {
        MortarSpec far = MortarSpec.from(new Viewpoint(100, 100, 9f * SCALE, SCALE));

        assertThat(new LongBatteryPerk().refineSpec(this.base).reach().deadZone()).isEqualTo(2.5f * SCALE);
        assertThat(new LongBatteryPerk().refineSpec(far).reach().deadZone()).isEqualTo(2.5f * SCALE);
    }

    @Test
    void bracketingThenSiegeRoundsRaisesTheDamageStepButNotTheRadiusStep() {
        MortarSpec spec = new BracketDamagePerk().refineSpec(new BracketingPerk().refineSpec(this.base));

        assertThat(spec.bracket().damageStep()).isEqualTo(0.15f);
        assertThat(spec.bracket().radiusStep()).isEqualTo(0.1f);
    }

    @Test
    void theBracketTrackerStepsOnlyWhileShellsLandNearTheLastAndCapsAtItsMaximum() {
        BracketTracker tracker = new BracketTracker();
        BracketSpec spec = BracketSpec.of(1.5f, 3, 0.1f, 0.1f);

        int first = tracker.land(100, 100, spec, SCALE);
        int second = tracker.land(110, 100, spec, SCALE);
        int third = tracker.land(115, 100, spec, SCALE);
        int fourth = tracker.land(115, 100, spec, SCALE);
        int fifth = tracker.land(115, 100, spec, SCALE);
        int elsewhere = tracker.land(500, 500, spec, SCALE);

        assertThat(new int[]{first, second, third, fourth, fifth, elsewhere}).containsExactly(0, 1, 2, 3, 3, 0);
    }
}
