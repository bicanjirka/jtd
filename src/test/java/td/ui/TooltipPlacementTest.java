package td.ui;

import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import static org.assertj.core.api.Assertions.assertThat;

class TooltipPlacementTest {

    private static final Rectangle WINDOW = new Rectangle(0, 0, 800, 600);
    private static final Dimension SIZE = new Dimension(200, 100);

    @Test
    void aTooltipGoesLeftOfItsAnchorWithAGap() {
        Point at = TooltipPlacement.place(new Rectangle(500, 50, 20, 20), SIZE, WINDOW);

        assertThat(at.x).isEqualTo(500 - TooltipPlacement.GAP - 200);
    }

    @Test
    void withNoRoomOnTheLeftItGoesRight() {
        Point at = TooltipPlacement.place(new Rectangle(100, 50, 20, 20), SIZE, WINDOW);

        assertThat(at.x).isEqualTo(120 + TooltipPlacement.GAP);
    }

    @Test
    void itStaysSixPixelsInsideTheWindow() {
        Point at = TooltipPlacement.place(new Rectangle(790, 590, 20, 20), SIZE, WINDOW);

        assertThat(at.y).isEqualTo(600 - TooltipPlacement.MARGIN - 100);
        assertThat(at.x).isLessThanOrEqualTo(800 - TooltipPlacement.MARGIN - 200);
        assertThat(TooltipPlacement.place(new Rectangle(500, 0, 20, 20), SIZE, WINDOW).y)
                .isEqualTo(TooltipPlacement.MARGIN);
    }

    @Test
    void itAlignsWithTheAnchorsTop() {
        Point at = TooltipPlacement.place(new Rectangle(500, 120, 20, 20), SIZE, WINDOW);

        assertThat(at.y).isEqualTo(120);
    }
}
