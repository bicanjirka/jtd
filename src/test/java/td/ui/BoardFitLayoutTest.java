package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class BoardFitLayoutTest {

    private static final int BORDER = 2;

    @Test
    void theBoardGrowsToTheRoomAtItsOwnProportionsAndIsCentred() {
        JPanel room = roomFor(board(400, 300), 1000, 300 * 2 + 2 * BORDER);

        room.doLayout();

        assertThat(room.getComponent(0).getBounds()).isEqualTo(new Rectangle(
                (1000 - (800 + 2 * BORDER)) / 2, 0, 800 + 2 * BORDER, 600 + 2 * BORDER));
    }

    @Test
    void aRoomTooSmallKeepsTheBoardsTopLeftCornerInView() {
        JPanel room = roomFor(board(400, 300), 50, 50);

        room.doLayout();

        assertThat(room.getComponent(0).getLocation()).isEqualTo(new Point(0, 0));
    }

    @Test
    void theRoomAsksForTheBoardAtItsNaturalSize() {
        JPanel room = roomFor(board(400, 300), 50, 50);

        assertThat(room.getPreferredSize()).isEqualTo(new Dimension(400 + 2 * BORDER, 300 + 2 * BORDER));
    }

    private static JPanel board(int width, int height) {
        JPanel board = new JPanel();
        board.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
        board.setPreferredSize(new Dimension(width + 2 * BORDER, height + 2 * BORDER));
        return board;
    }

    private static JPanel roomFor(JPanel board, int width, int height) {
        JPanel room = new JPanel(new BoardFitLayout(board));
        room.add(board);
        room.setSize(width, height);
        return room;
    }
}
