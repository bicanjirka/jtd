package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import td.stat.StatBreakdown;
import td.stat.StatKey;
import td.stat.StatTerm;
import td.stat.StatWording;
import td.ui.render.InfoSheet;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class TooltipPanelTest {

    /** Stored values that reach every note a stat writes and its widest number, each held to its stat's range. */
    private static final float[] SAMPLES = {-100f, -50f, 0.5f, 1f, 25f, 99f, 100f, 9999.9f, 999_990f};
    private static final StatWording WORDING = new StatWording(40f);

    private static TooltipPanel showing(Row row) {
        TooltipPanel panel = new TooltipPanel();
        panel.show(new InfoSheet(List.of(row)));
        return panel;
    }

    private static int heightOfRow(StatTerm stat, float value) {
        StatBreakdown breakdown = new StatBreakdown(stat, value, List.of(), value, List.of(), value);
        TooltipPanel panel = new TooltipPanel();
        panel.show(new InfoSheet(StatRowText.catalog(List.of(breakdown), WORDING)));
        return panel.getHeight();
    }

    @Test
    void anOpenTooltipTakesTheHeightOfWhatItShowsNext() {
        Row line = Row.plain(Glyph.DOT, "Range", "4");
        InfoSheet longer = new InfoSheet(List.of(line, line, line, line));
        TooltipPanel opened = showing(line);
        TooltipPanel fresh = new TooltipPanel();
        fresh.show(longer);

        opened.show(longer);

        assertThat(opened.getHeight()).isEqualTo(fresh.getHeight());
    }

    @Test
    void theExpiredNoteMakesRoomForItself() {
        TooltipPanel panel = showing(Row.plain(Glyph.DOT, "Range", "4"));
        int before = panel.getHeight();

        panel.showExpired();

        assertThat(panel.getHeight()).isGreaterThan(before);
    }

    @Test
    void everyStatRowStaysOnOneLine() {
        List<String> wrapped = new ArrayList<>();

        StatLooks.all().forEach(stat -> {
            int oneLine = heightOfRow(stat, stat instanceof StatKey key ? key.clamp(0f) : 0f);
            for (float sample : SAMPLES) {
                float value = stat instanceof StatKey key ? key.clamp(sample) : sample;
                if (heightOfRow(stat, value) > oneLine) {
                    wrapped.add(stat.key() + " at " + value);
                }
            }
        });

        assertThat(wrapped).isEmpty();
    }

    @Test
    void aValueAlignedRightKeepsAGapFromItsLabelInATooltipCutToItsText() {
        TooltipPanel withValue = showing(Row.plain(Glyph.DOT, "Range", "4"));
        TooltipPanel withoutValue = showing(Row.plain(Glyph.DOT, "Range", ""));

        assertThat(withValue.getWidth() - withoutValue.getWidth()).isGreaterThanOrEqualTo(TooltipPanel.VALUE_GAP);
    }
}
