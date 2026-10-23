package td.tower.mortar;

import org.junit.jupiter.api.Test;
import td.fixtures.BoardFixtures;
import td.tower.targeting.Viewpoint;
import td.wave.Path;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class MortarSpecTest {

    private static final int SCALE = BoardFixtures.SCALE;
    private final MortarSpec base = MortarSpec.from(new Viewpoint(100, 100, 4.5f * SCALE, SCALE));

    @Test
    void theBaseSpecDoesNotBracket() {
        assertThat(this.base.bracket().active()).isFalse();
    }

    @Test
    void theBracketTrackerStepsOnlyWhileShellsLandNearTheLastAndCapsAtItsMaximum() {
        BracketTracker tracker = new BracketTracker();
        BracketSpec spec = BracketSpec.of(1.5f, 3, 0.1f);

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

        assertThat(IntStream.rangeClosed(1, 9).mapToObj(plan::typeOf).toList()).containsExactly(
                ShellType.CRYO, ShellType.TAR, ShellType.PLAIN, ShellType.NUKE, ShellType.CRYO, ShellType.TAR,
                ShellType.PLAIN, ShellType.NUKE, ShellType.CRYO);
    }

    @Test
    void withoutSpecialsEveryShellIsPlainAndWithOneEveryThirdIsSpecial() {
        ShellPlan plain = ShellPlan.none();
        ShellPlan one = ShellPlan.none().withSpecial(ShellType.NAPALM);

        assertThat(IntStream.rangeClosed(1, 6).mapToObj(plain::typeOf)).containsOnly(ShellType.PLAIN);
        assertThat(IntStream.rangeClosed(1, 6).mapToObj(one::typeOf).toList()).containsExactly(
                ShellType.PLAIN, ShellType.PLAIN, ShellType.NAPALM, ShellType.PLAIN, ShellType.PLAIN,
                ShellType.NAPALM);
    }

    @Test
    void pathLinePlacesPointsAlongTheNearestPathFromTheImpactBehindAndAhead() {
        Path path = new PathNormal(List.of(new Vec2(0, 100),
                new Vec2(500, 100)));

        List<Vec2> points = PathLine.along(List.of(path), 200, 130, new double[]{-50, 0, 80});

        assertThat(points).extracting(Vec2::x).containsExactly(150.0, 200.0, 280.0);
        assertThat(points).extracting(Vec2::y).containsOnly(100.0);
    }

    @Test
    void pathLineIsEmptyWithoutAPathOfAnyLength() {
        assertThat(PathLine.along(List.of(new PathNormal(List.of())), 0, 0,
                new double[]{10})).isEmpty();
    }

    @Test
    void pathLineChoosesTheNearestOfSeveralPaths() {
        Path far = new PathNormal(List.of(new Vec2(0, 500),
                new Vec2(500, 500)));
        Path near = new PathNormal(List.of(new Vec2(0, 100),
                new Vec2(500, 100)));

        List<Vec2> points = PathLine.along(List.of(far, near), 100, 120,
                new double[]{10});

        assertThat(points).singleElement().satisfies(point -> assertThat(point.y()).isEqualTo(100.0));
    }
}
