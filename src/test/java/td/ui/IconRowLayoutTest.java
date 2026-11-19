package td.ui;

import org.junit.jupiter.api.Test;

import java.awt.Point;

import static org.assertj.core.api.Assertions.assertThat;

class IconRowLayoutTest {

    /** Just wide enough for the label and six faces. */
    private static final int SIX_WIDE = IconRowLayout.LABEL_WIDTH + 6 * (TermIconPainter.ROW + IconRowLayout.GAP)
            - IconRowLayout.GAP;

    @Test
    void anUnlabelledRowStartsAtTheEdgeAndFitsMoreOnALine() {
        IconRowLayout labelled = IconRowLayout.of(20, false, 228);
        IconRowLayout unlabelled = IconRowLayout.unlabelled(20, false, 228);

        assertThat(unlabelled.cell(0).x).isZero();
        assertThat(unlabelled.perLine()).isGreaterThan(labelled.perLine());
    }

    @Test
    void sixFacesFitBesideTheLabelInARowWideEnoughForThem() {
        IconRowLayout layout = IconRowLayout.of(6, false, SIX_WIDE);

        assertThat(layout.perLine()).isEqualTo(6);
        assertThat(layout.lines()).isEqualTo(1);
    }

    @Test
    void aSeventhFaceWrapsToASecondLine() {
        IconRowLayout layout = IconRowLayout.of(7, false, SIX_WIDE);

        assertThat(layout.lines()).isEqualTo(2);
        assertThat(layout.cell(6)).isEqualTo(new Point(IconRowLayout.LABEL_WIDTH, layout.lineHeight()));
    }

    @Test
    void aTimeLabelAddsItsHeightToEveryLine() {
        IconRowLayout plain = IconRowLayout.of(7, false, SIX_WIDE);
        IconRowLayout labelled = IconRowLayout.of(7, true, SIX_WIDE);

        assertThat(labelled.lineHeight() - plain.lineHeight()).isEqualTo(TermIconPainter.LABEL);
        assertThat(labelled.height() - plain.height()).isEqualTo(2 * TermIconPainter.LABEL);
    }

    @Test
    void aPointBetweenFacesHitsNone() {
        IconRowLayout layout = IconRowLayout.of(3, false, SIX_WIDE);
        int between = IconRowLayout.LABEL_WIDTH + TermIconPainter.ROW + 1;

        assertThat(layout.iconAt(between, 5)).isEmpty();
        assertThat(layout.iconAt(IconRowLayout.LABEL_WIDTH - 1, 5)).isEmpty();
    }

    @Test
    void aPointOnTheThirdFaceHitsIt() {
        IconRowLayout layout = IconRowLayout.of(3, false, SIX_WIDE);
        Point third = layout.cell(2);

        assertThat(layout.iconAt(third.x + 3, third.y + 3)).hasValue(2);
    }
}
