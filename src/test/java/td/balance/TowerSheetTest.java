package td.balance;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** How a stored bench sheet is read back, and what a build's power and worth are made of. */
class TowerSheetTest {

    private static final double PARTNERS = 10;

    /** A sheet of one scenario, fought alone and beside partners that hold {@link #PARTNERS} without a subject. */
    static TowerSheet sheet(List<List<String>> builds) {
        List<List<String>> towers = new ArrayList<>(List.of(List.of("tower", "name", "build", "role", "node", "nodeName",
                "tier", "versus", "cost", "stream", "stream+", "owned")));
        towers.addAll(builds);
        return TowerSheet.of(towers, List.of(List.of("scenario", "partners"), List.of("stream", "-"),
                List.of("stream+", String.valueOf(PARTNERS))));
    }

    /** A build of the tower {@code tower} that adds {@code adds} Walkers to what the partners hold. */
    static List<String> build(String tower, String label, String role, int cost, double adds, String... owned) {
        return List.of(tower, tower, label, role, "", "", "", "", String.valueOf(cost), "0", String.valueOf(PARTNERS + adds),
                String.join(" ", owned));
    }

    /** A build that weighs {@code node} against the build {@code versus}. */
    static List<String> weighing(List<String> build, String node, String versus) {
        List<String> row = new ArrayList<>(build);
        row.set(4, node);
        row.set(5, node);
        row.set(6, "0");
        row.set(7, versus);
        return row;
    }

    @Test
    void aBuildsPowerIsWhatItAddsToWhatThePartnersHoldWithoutIt() {
        TowerSheet sheet = sheet(List.of(build("T", TowerBench.BARE, TowerBench.BARE, 10, 4)));

        double power = sheet.power(sheet.bare("T"));

        assertThat(power).isCloseTo(4, within(1e-9));
    }

    @Test
    void aTeamThatHoldsLessThanThePartnersAloneAddsNothing() {
        TowerSheet sheet = sheet(List.of(build("T", TowerBench.BARE, TowerBench.BARE, 10, -3)));

        double power = sheet.power(sheet.bare("T"));

        assertThat(power).isZero();
    }

    @Test
    void aBuildIsWorthTheCreditsItsPowerCostsAsBareTowers() {
        TowerSheet sheet = sheet(List.of(build("T", TowerBench.BARE, TowerBench.BARE, 10, 2),
                build("U", TowerBench.BARE, TowerBench.BARE, 20, 8),
                build("U", "full:0", TowerBench.FULL, 50, 12, "u.a")));

        double worth = sheet.worth(sheet.build("U", "full:0").orElseThrow());

        double rate = (2.0 / 10 + 8.0 / 20) / 2;
        assertThat(sheet.rate()).isCloseTo(rate, within(1e-9));
        assertThat(worth).isCloseTo(12 / rate, within(1e-9));
    }

    @Test
    void aFullBuildsLabelNamesTheSideOfTheForkTheTierAndTheClusterMemberItTook() {
        TowerSheet sheet = sheet(List.of(build("T", TowerBench.BARE, TowerBench.BARE, 10, 2),
                build("T", "full:1", TowerBench.FULL, 30, 4, "t.a"),
                build("T", "full:t.side:2:t.member", TowerBench.FULL, 60, 8, "t.a", "t.side", "t.member")));

        TowerSheet.Held plain = sheet.build("T", "full:1").orElseThrow();
        TowerSheet.Held forked = sheet.build("T", "full:t.side:2:t.member").orElseThrow();

        assertThat(List.of(plain.side(), String.valueOf(plain.stage()), plain.member())).containsExactly("", "1", "");
        assertThat(List.of(forked.side(), String.valueOf(forked.stage()), forked.member()))
                .containsExactly("t.side", "2", "t.member");
        assertThat(sheet.stage("T", 2)).containsExactly(forked);
        assertThat(sheet.topStage("T")).isEqualTo(2);
    }
}
