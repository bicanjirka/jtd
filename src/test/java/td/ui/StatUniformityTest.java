package td.ui;

import org.junit.jupiter.api.Test;
import td.stat.StatKey;
import td.stat.StatReading;
import td.stat.StatTerm;
import td.stat.StatUnit;
import td.stat.StatWording;
import td.tower.TowerStat;
import td.tower.TowerStatRow;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Rules every stat keeps, so each reads the same way on every sheet. */
class StatUniformityTest {

    private static final int SHORT_NAME_MOST = 12;
    /** The narrowest the Info pane gets, less the edge a value keeps from it. */
    private static final int PANE_TEXT_WIDTH = 166;
    private static final int MARK_BOX = 13;
    private static final int LABEL_TO_VALUE = 8;
    /** The widest number a row shows before abbreviating, which only a stat with no meaning reaches. */
    private static final String WIDEST_NUMBER = "9999.9";
    /** Stored values that reach every meaning a stat writes, each held to its stat's range. */
    private static final float[] SAMPLES = {-50f, 0f, 0.5f, 1f, 25f, 99f, 100f};

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

    @Test
    void theWidestRowOfEveryStatFitsTheNarrowestPane() {
        Font font = new Font(Font.DIALOG, Font.PLAIN, 12);
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        FontMetrics label = image.createGraphics().getFontMetrics(font);
        FontMetrics gloss = image.createGraphics().getFontMetrics(font.deriveFont(11f));
        StatWording wording = new StatWording(40f);
        List<String> tooWide = new ArrayList<>();

        for (StatTerm stat : stats()) {
            int widestValue = label.stringWidth(WIDEST_NUMBER) + gloss.stringWidth(" " + stat.unit().word());
            for (float sample : SAMPLES) {
                float value = stat instanceof StatKey key ? key.clamp(sample) : sample;
                StatReading reading = wording.read(stat, value);
                widestValue = Math.max(widestValue, label.stringWidth(reading.number())
                        + gloss.stringWidth(" " + reading.unit() + " " + reading.meaning().orElse("")));
            }
            int width = MARK_BOX + label.stringWidth(" " + stat.shortName()) + LABEL_TO_VALUE + widestValue;
            if (width > PANE_TEXT_WIDTH) {
                tooWide.add(stat.key() + " " + width);
            }
        }

        assertThat(tooWide).isEmpty();
    }
}
