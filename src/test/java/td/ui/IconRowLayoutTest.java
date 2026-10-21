package td.ui;

import org.junit.jupiter.api.Test;

import java.awt.Point;

import static org.assertj.core.api.Assertions.assertThat;

class IconRowLayoutTest {

    private static final int INFO_PANE_WIDTH = 184;

    @Test
    void sixFacesFitBesideTheLabelInTheInfoPane() {
        IconRowLayout layout = IconRowLayout.of(6, false, INFO_PANE_WIDTH);

        assertThat(layout.perLine()).isEqualTo(6);
        assertThat(layout.lines()).isEqualTo(1);
    }

    @Test
    void aSeventhFaceWrapsToASecondLine() {
        IconRowLayout layout = IconRowLayout.of(7, false, INFO_PANE_WIDTH);

        assertThat(layout.lines()).isEqualTo(2);
        assertThat(layout.cell(6)).isEqualTo(new Point(IconRowLayout.LABEL_WIDTH, layout.lineHeight()));
    }

    @Test
    void aTimeLabelAddsItsHeightToEveryLine() {
        IconRowLayout plain = IconRowLayout.of(7, false, INFO_PANE_WIDTH);
        IconRowLayout labelled = IconRowLayout.of(7, true, INFO_PANE_WIDTH);

        assertThat(labelled.lineHeight() - plain.lineHeight()).isEqualTo(TermIconPainter.LABEL);
        assertThat(labelled.height() - plain.height()).isEqualTo(2 * TermIconPainter.LABEL);
    }

    @Test
    void aPointBetweenFacesHitsNone() {
        IconRowLayout layout = IconRowLayout.of(3, false, INFO_PANE_WIDTH);
        int between = IconRowLayout.LABEL_WIDTH + TermIconPainter.ROW + 1;

        assertThat(layout.iconAt(between, 5)).isEmpty();
        assertThat(layout.iconAt(IconRowLayout.LABEL_WIDTH - 1, 5)).isEmpty();
    }

    @Test
    void aPointOnTheThirdFaceHitsIt() {
        IconRowLayout layout = IconRowLayout.of(3, false, INFO_PANE_WIDTH);
        Point third = layout.cell(2);

        assertThat(layout.iconAt(third.x + 3, third.y + 3)).hasValue(2);
    }
}
