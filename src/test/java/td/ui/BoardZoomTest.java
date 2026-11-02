package td.ui;

import org.junit.jupiter.api.Test;

import java.awt.Dimension;

import static org.assertj.core.api.Assertions.assertThat;

class BoardZoomTest {

    @Test
    void theTighterSideOfTheRoomSetsTheZoom() {
        BoardZoom zoom = BoardZoom.fit(new Dimension(400, 300), new Dimension(1000, 450));

        assertThat(zoom.factor()).isEqualTo(450 / 300.0);
    }

    @Test
    void aRoomTooSmallForTheSmallestZoomClipsTheBoardInstead() {
        BoardZoom zoom = BoardZoom.fit(new Dimension(400, 300), new Dimension(40, 30));

        assertThat(zoom).isEqualTo(BoardZoom.smallest());
    }

    @Test
    void aBoardWithNoCellsDrawsAtItsNaturalZoom() {
        BoardZoom zoom = BoardZoom.fit(new Dimension(0, 0), new Dimension(1000, 450));

        assertThat(zoom).isEqualTo(BoardZoom.natural());
    }

    @Test
    void theLastScreenPixelOfAZoomedBoardIsStillOnTheBoard() {
        BoardZoom zoom = BoardZoom.fit(new Dimension(400, 300), new Dimension(700, 700));

        assertThat(zoom.toBoard(zoom.toView(400) - 1)).isEqualTo(399);
        assertThat(zoom.toBoard(zoom.toView(400))).isEqualTo(400);
    }

    @Test
    void aPointLeftOfTheBoardStaysOffIt() {
        BoardZoom zoom = BoardZoom.fit(new Dimension(400, 300), new Dimension(800, 600));

        assertThat(zoom.toBoard(-1)).isNegative();
    }
}
