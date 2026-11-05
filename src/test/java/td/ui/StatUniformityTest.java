package td.ui;

import org.junit.jupiter.api.Test;
import td.stat.StatTerm;
import td.stat.StatUnit;
import td.tower.TowerStat;
import td.tower.TowerStatRow;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Rules every stat keeps, so each reads the same way on every sheet. */
class StatUniformityTest {

    private static final int SHORT_NAME_MOST = 10;

    private static List<StatTerm> stats() {
        return StatLooks.all().toList();
    }

    @Test
    void everyStatHasAShortAndAFullNameAndSaysWhatItDoes() {
        assertThat(stats()).isNotEmpty().allSatisfy(stat -> {
            assertThat(stat.shortName()).isNotBlank().doesNotContain(".");
            assertThat(stat.title()).isNotBlank();
            if (stat.title().length() <= SHORT_NAME_MOST) {
                assertThat(stat.shortName()).as(stat.key()).isEqualTo(stat.title());
            }
            String meaning = stat.describe().plain();
            assertThat(meaning).as(stat.key()).isNotBlank().doesNotEndWith(".");
            assertThat(Character.isLowerCase(meaning.charAt(0))).as(stat.key()).isTrue();
        });
    }

    @Test
    void everyStatHasAUnitADirectionAGroupAndAMark() {
        assertThat(stats()).allSatisfy(stat -> {
            assertThat(stat.unit()).isNotNull();
            assertThat(stat.direction()).isNotNull();
            assertThat(stat.group()).isNotNull();
            assertThat(StatLooks.of(stat)).isNotNull();
        });
    }

    @Test
    void everyStatIsFoundByItsKey() {
        assertThat(stats()).allSatisfy(stat -> assertThat(StatLooks.statOf(stat)).contains(stat));
        assertThat(stats()).extracting(StatTerm::key).doesNotHaveDuplicates();
    }

    @Test
    void noTowerRowReadsInPixelsSoTowerWordsNeedNoBoard() {
        assertThat(Stream.concat(Arrays.stream(TowerStat.values()), Arrays.stream(TowerStatRow.values())))
                .extracting(StatTerm::unit).doesNotContain(StatUnit.PIXELS_PER_TICK);
    }
}
