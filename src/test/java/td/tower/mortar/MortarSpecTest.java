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

    @Test
    void aNukeIsEveryFourthShellAndTheSpecialsRunOnTheOthers() {
        ShellPlan plan = ShellPlan.none().withSpecial(ShellType.CRYO).withSpecial(ShellType.TAR).withNukeEvery(4);

        assertThat(java.util.stream.IntStream.rangeClosed(1, 9).mapToObj(plan::typeOf).toList()).containsExactly(
                ShellType.CRYO, ShellType.TAR, ShellType.PLAIN, ShellType.NUKE, ShellType.CRYO, ShellType.TAR,
                ShellType.PLAIN, ShellType.NUKE, ShellType.CRYO);
    }

    @Test
    void withoutSpecialsEveryShellIsPlainAndWithOneEveryThirdIsSpecial() {
        ShellPlan plain = ShellPlan.none();
        ShellPlan one = ShellPlan.none().withSpecial(ShellType.NAPALM);

        assertThat(java.util.stream.IntStream.rangeClosed(1, 6).mapToObj(plain::typeOf)).containsOnly(ShellType.PLAIN);
        assertThat(java.util.stream.IntStream.rangeClosed(1, 6).mapToObj(one::typeOf).toList()).containsExactly(
                ShellType.PLAIN, ShellType.PLAIN, ShellType.NAPALM, ShellType.PLAIN, ShellType.PLAIN,
                ShellType.NAPALM);
    }

    @Test
    void pathLinePlacesPointsAlongTheNearestPathFromTheImpactBehindAndAhead() {
        td.wave.Path path = new td.wave.PathNormal(java.util.List.of(new td.wave.Vec2(0, 100),
                new td.wave.Vec2(500, 100)));

        java.util.List<td.wave.Vec2> points = PathLine.along(java.util.List.of(path), 200, 130, new double[]{-50, 0, 80});

        assertThat(points).extracting(td.wave.Vec2::x).containsExactly(150.0, 200.0, 280.0);
        assertThat(points).extracting(td.wave.Vec2::y).containsOnly(100.0);
    }

    @Test
    void pathLineIsEmptyWithoutAPathOfAnyLength() {
        assertThat(PathLine.along(java.util.List.of(new td.wave.PathNormal(java.util.List.of())), 0, 0,
                new double[]{10})).isEmpty();
    }

    @Test
    void pathLineChoosesTheNearestOfSeveralPaths() {
        td.wave.Path far = new td.wave.PathNormal(java.util.List.of(new td.wave.Vec2(0, 500),
                new td.wave.Vec2(500, 500)));
        td.wave.Path near = new td.wave.PathNormal(java.util.List.of(new td.wave.Vec2(0, 100),
                new td.wave.Vec2(500, 100)));

        java.util.List<td.wave.Vec2> points = PathLine.along(java.util.List.of(far, near), 100, 120,
                new double[]{10});

        assertThat(points).singleElement().satisfies(point -> assertThat(point.y()).isEqualTo(100.0));
    }
}
