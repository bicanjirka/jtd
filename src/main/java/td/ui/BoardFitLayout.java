package td.ui;

import javax.swing.JComponent;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * Sizes the bordered board as large as its container allows at the board's own proportions, and
 * centres it. The board's preferred size, less its border, is the board at its natural zoom.
 */
public final class BoardFitLayout implements LayoutManager {

    private final JComponent board;

    public BoardFitLayout(JComponent board) {
        this.board = board;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return this.sizeAt(BoardZoom.natural(), parent.getInsets());
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return this.sizeAt(BoardZoom.smallest(), parent.getInsets());
    }

    @Override
    public void layoutContainer(Container parent) {
        Insets room = parent.getInsets();
        Insets chrome = this.board.getInsets();
        int roomWidth = parent.getWidth() - room.left - room.right;
        int roomHeight = parent.getHeight() - room.top - room.bottom;
        BoardZoom zoom = BoardZoom.fit(this.naturalBoard(),
                new Dimension(roomWidth - chrome.left - chrome.right, roomHeight - chrome.top - chrome.bottom));
        Dimension size = this.sizeAt(zoom, new Insets(0, 0, 0, 0));
        // Too small a room clips the board's far edges, never the corner its cells count from.
        int x = room.left + Math.max(0, (roomWidth - size.width) / 2);
        int y = room.top + Math.max(0, (roomHeight - size.height) / 2);
        this.board.setBounds(x, y, size.width, size.height);
    }

    private Dimension sizeAt(BoardZoom zoom, Insets room) {
        Insets chrome = this.board.getInsets();
        Dimension natural = this.naturalBoard();
        return new Dimension(zoom.toView(natural.width) + chrome.left + chrome.right + room.left + room.right,
                zoom.toView(natural.height) + chrome.top + chrome.bottom + room.top + room.bottom);
    }

    private Dimension naturalBoard() {
        Insets chrome = this.board.getInsets();
        Dimension preferred = this.board.getPreferredSize();
        return new Dimension(preferred.width - chrome.left - chrome.right, preferred.height - chrome.top - chrome.bottom);
    }
}
