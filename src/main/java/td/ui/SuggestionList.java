package td.ui;

import td.util.ThreadConfined;
import td.wave.WaveScriptSuggestions.Candidate;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.Serial;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

/**
 * A list of candidates hung on the window's popup layer by the control it serves: a scrolling column of a few rows,
 * or every row at once, in as many columns as the window's height needs. The pointer highlights a row, the wheel
 * moves the highlight and a click takes the row. A row with a look shows its icon and colours, and rows whose hints
 * differ are ruled apart. A row that can't be taken is never highlighted.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class SuggestionList extends JComponent {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int ROW_HEIGHT = 16;
    private static final int PADDING = 6;
    private static final Color SELECTED_FILL = new Color(46, 66, 46);
    private static final Color HINT = new Color(150, 170, 150);
    private static final int ICON_GAP = 4;

    /** Each row's look, by its place in the list. */
    private final transient IntFunction<Optional<RowLook>> looks;
    /** Whether a row can be taken; the pointer, the wheel and the keys pass over one that can't. */
    private final transient IntPredicate usable;

    private List<Candidate> rows = List.of();
    private int selected;
    private int top;
    private int perColumn = 1;
    private int columns = 1;

    /** Every row can be taken. */
    SuggestionList(IntFunction<Optional<RowLook>> looks, Runnable onTake) {
        this(looks, index -> true, onTake);
    }

    /** The control it serves keeps the focus while it is clicked, unless it is made focusable. */
    SuggestionList(IntFunction<Optional<RowLook>> looks, IntPredicate usable, Runnable onTake) {
        this.looks = looks;
        this.usable = usable;
        setFocusable(false);
        MouseAdapter pointer = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                SuggestionList.this.highlightAt(e.getX(), e.getY());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SuggestionList.this.highlightAt(e.getX(), e.getY())) {
                    onTake.run();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                SuggestionList.this.move(e.getWheelRotation());
            }
        };
        addMouseListener(pointer);
        addMouseMotionListener(pointer);
        addMouseWheelListener(pointer);
    }

    void show(List<Candidate> candidates) {
        this.rows = candidates;
        this.selected = 0;
        this.top = 0;
    }

    /** Moves the highlight {@code delta} rows that can be taken on, wrapping round. */
    void move(int delta) {
        int step = Integer.signum(delta);
        for (int moved = 0; moved < Math.abs(delta); moved++) {
            int tried = 1;
            while (tried < this.rows.size() && !this.usable.test(Math.floorMod(this.selected + step * tried, this.rows.size()))) {
                tried++;
            }
            this.selectIfUsable(Math.floorMod(this.selected + step * tried, this.rows.size()));
        }
    }

    /** Moves the highlight to the same row of a column to either side, if that column has one. */
    void moveAcross(int delta) {
        int index = this.selected + delta * this.perColumn;
        if (index >= 0 && index < this.rows.size()) {
            this.selectNear(index);
        }
    }

    /** Highlights row {@code index}, or the nearest after or before it that can be taken. */
    void selectNear(int index) {
        for (int distance = 0; distance < this.rows.size(); distance++) {
            if (this.selectIfUsable(index + distance) || this.selectIfUsable(index - distance)) {
                return;
            }
        }
    }

    private boolean selectIfUsable(int index) {
        if (index < 0 || index >= this.rows.size() || !this.usable.test(index)) {
            return false;
        }
        this.select(index);
        return true;
    }

    /** Highlights row {@code index}, scrolling it into view. */
    void select(int index) {
        this.selected = index;
        int shown = this.perColumn * this.columns;
        if (this.selected < this.top) {
            this.top = this.selected;
        } else if (this.selected >= this.top + shown) {
            this.top = this.selected - shown + 1;
        }
        repaint();
    }

    int selectedIndex() {
        return this.selected;
    }

    int rowCount() {
        return this.rows.size();
    }

    /** How many rows one column shows. */
    int visibleRows() {
        return this.perColumn;
    }

    Candidate selected() {
        return this.rows.get(this.selected);
    }

    boolean isHung() {
        return getParent() != null;
    }

    /** Hangs one column of up to {@code maxRows} rows under {@code anchor}, scrolling the rest. */
    void hangUnder(JComponent anchor, int maxRows) {
        this.perColumn = Math.max(1, Math.min(this.rows.size(), maxRows));
        this.columns = 1;
        this.hang(anchor);
    }

    /**
     * Hangs every row by {@code anchor}, on whichever side has more room, in as few columns as that side's height
     * allows, the rows shared evenly between them.
     */
    void hangInColumnsBy(JComponent anchor) {
        JLayeredPane layers = SwingUtilities.getRootPane(anchor).getLayeredPane();
        Point below = SwingUtilities.convertPoint(anchor, 0, anchor.getHeight(), layers);
        int room = Math.max(layers.getHeight() - below.y, below.y - anchor.getHeight()) - 2;
        int count = Math.max(1, this.rows.size());
        this.columns = Math.ceilDiv(count, Math.clamp(room / ROW_HEIGHT, 1, count));
        this.perColumn = Math.ceilDiv(count, this.columns);
        this.top = 0;
        this.hang(anchor);
    }

    void takeDown() {
        if (getParent() != null) {
            JComponent parent = (JComponent) getParent();
            parent.remove(this);
            parent.repaint();
        }
    }

    /** Under {@code anchor}, or above it where the window has no room below. */
    private void hang(JComponent anchor) {
        JLayeredPane layers = SwingUtilities.getRootPane(anchor).getLayeredPane();
        Point below = SwingUtilities.convertPoint(anchor, 0, anchor.getHeight(), layers);
        int width = Math.max(this.columns * this.columnWidth(), anchor.getWidth());
        int height = this.perColumn * ROW_HEIGHT + 2;
        int x = Math.max(0, Math.min(below.x, layers.getWidth() - width));
        int y = below.y + height > layers.getHeight() ? below.y - anchor.getHeight() - height : below.y;
        setBounds(x, y, width, height);
        setPreferredSize(getSize());
        if (getParent() == null) {
            layers.add(this, JLayeredPane.POPUP_LAYER);
        }
        layers.repaint();
    }

    /** Highlights the row at ({@code x}, {@code y}), and answers whether one is there. */
    private boolean highlightAt(int x, int y) {
        int column = x / Math.max(1, getWidth() / this.columns);
        int line = Math.floorDiv(y - 1, ROW_HEIGHT);
        int index = this.top + column * this.perColumn + line;
        if (y < 1 || line >= this.perColumn || column >= this.columns || index >= this.rows.size()
                || !this.usable.test(index)) {
            return false;
        }
        if (index != this.selected) {
            this.selected = index;
            repaint();
        }
        return true;
    }

    private int columnWidth() {
        FontMetrics metrics = getFontMetrics(Hud.LABEL_FONT);
        int widest = 0;
        for (Candidate row : this.rows) {
            widest = Math.max(widest, metrics.stringWidth(row.text()) + metrics.stringWidth(row.hint()));
        }
        return widest + 5 * PADDING + this.iconRoom();
    }

    /** The widest icon and its gap, so every row's text lines up. */
    private int iconRoom() {
        return IntStream.range(0, this.rows.size()).mapToObj(this.looks).flatMap(Optional::stream)
                .mapToInt(look -> look.icon().getIconWidth() + ICON_GAP).max().orElse(0);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Hud.BACKGROUND);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(Hud.BORDER_IDLE);
        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        g2.setFont(Hud.LABEL_FONT);
        FontMetrics metrics = g2.getFontMetrics();
        int columnWidth = getWidth() / this.columns;
        int iconRoom = this.iconRoom();
        int shown = Math.min(this.rows.size() - this.top, this.perColumn * this.columns);
        for (int i = 0; i < shown; i++) {
            Candidate row = this.rows.get(this.top + i);
            Optional<RowLook> look = this.looks.apply(this.top + i);
            int left = (i / this.perColumn) * columnWidth;
            int line = i % this.perColumn;
            int y = 1 + line * ROW_HEIGHT;
            if (line == 0 && left > 0) {
                g2.setColor(Hud.BORDER_IDLE);
                g2.drawLine(left, 1, left, getHeight() - 2);
            }
            if (this.top + i == this.selected) {
                g2.setColor(SELECTED_FILL);
                g2.fillRect(left + 1, y, columnWidth - 2, ROW_HEIGHT);
            }
            if (line > 0 && !row.hint().equals(this.rows.get(this.top + i - 1).hint())) {
                g2.setColor(Hud.BORDER_IDLE);
                g2.drawLine(left + PADDING, y, left + columnWidth - PADDING, y);
            }
            look.ifPresent(l -> l.icon().paintIcon(this, g2, left + PADDING,
                    y + (ROW_HEIGHT - l.icon().getIconHeight()) / 2));
            int baseline = y + (ROW_HEIGHT + metrics.getAscent() - metrics.getDescent()) / 2;
            g2.setColor(look.map(RowLook::text).orElse(Hud.FOREGROUND));
            g2.drawString(row.text(), left + PADDING + iconRoom, baseline);
            g2.setColor(look.map(RowLook::hint).orElse(HINT));
            g2.drawString(row.hint(), left + columnWidth - PADDING - metrics.stringWidth(row.hint()), baseline);
        }
        g2.dispose();
    }
}
