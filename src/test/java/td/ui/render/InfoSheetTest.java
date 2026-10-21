package td.ui.render;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InfoSheetTest {

    private static final IconFace FACE = IconFace.of("effect.chill", "Ch", Palette.STATUS_MARKER_CHILL,
            Palette.TERM_FRAME_CONTROL);
    private static final TooltipKey ICON_KEY = TooltipKey.ofTerm("effect", EffectKind.CHILL);
    private static final TooltipKey ROW_KEY = TooltipKey.of("stat", "ARMOR");
    private static final InfoSheet ICON_SHEET = new InfoSheet(List.of(new SheetLine.Gap()));
    private static final InfoSheet ROW_SHEET = new InfoSheet(List.of(new SheetLine.Divider()));

    private final InfoSheet sheet = new InfoSheet(List.of(
            new SheetLine.IconRow("Debuffs", List.of(new SheetIcon(FACE, ICON_KEY, ICON_SHEET))),
            SheetLine.Row.plain(SheetLine.Glyph.DOT, "Armor", "25").withTooltip(ROW_KEY, ROW_SHEET)));

    @Test
    void tooltipForFindsAnIconsSheetByKey() {
        assertThat(this.sheet.tooltipFor(ICON_KEY)).hasValue(ICON_SHEET);
    }

    @Test
    void tooltipForFindsARowsSheet() {
        assertThat(this.sheet.tooltipFor(ROW_KEY)).hasValue(ROW_SHEET);
    }

    @Test
    void anUnknownKeyHasNoTooltip() {
        assertThat(this.sheet.tooltipFor(TooltipKey.of("stat", "SPEED"))).isEmpty();
    }
}
