package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import td.ui.render.InfoSheet;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class TooltipPanelTest {

    private static TooltipPanel showing(Row row) {
        TooltipPanel panel = new TooltipPanel();
        panel.show(new InfoSheet(List.of(row)));
        return panel;
    }

    @Test
    void aValueAlignedRightKeepsAGapFromItsLabelInATooltipCutToItsText() {
        TooltipPanel withValue = showing(Row.plain(Glyph.DOT, "Range", "4"));
        TooltipPanel withoutValue = showing(Row.plain(Glyph.DOT, "Range", ""));

        assertThat(withValue.getWidth() - withoutValue.getWidth()).isGreaterThanOrEqualTo(TooltipPanel.VALUE_GAP);
    }
}
