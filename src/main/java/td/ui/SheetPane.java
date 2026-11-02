package td.ui;

import td.ui.render.InfoSheet;
import td.ui.render.SheetIcon;
import td.util.ThreadConfined;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Shows an {@link InfoSheet} and finds what its pointer rests on. Mentions, icon rows and rows that open a
 * tooltip are marked in the laid-out document; this pane turns a point into the {@link HoverTarget} there and
 * brightens it.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class SheetPane extends JTextPane {

    private static final Color HIGHLIGHT = new Color(255, 255, 255, 36);
    private static final int HIGHLIGHT_ARC = 6;

    @Serial
    private static final long serialVersionUID = 1L;

    private Optional<Point> lastPoint = Optional.empty();
    private Optional<HoverTarget> highlighted = Optional.empty();

    SheetPane() {
        this.setEditable(false);
        this.setBackground(Color.BLACK);
        this.setForeground(Hud.FOREGROUND);
        this.setBorder(null);
        this.setFocusable(false);
    }

    /** Lays {@code sheet} out for {@code width}; a pointer at rest keeps its highlight on the new sheet. */
    void show(InfoSheet sheet, Java2DFrameRenderer renderer, int width) {
        this.setStyledDocument(InfoSheetDocument.of(sheet, renderer, width, this.getFontMetrics(this.getFont())));
        this.setCaretPosition(0);
        this.highlight(this.lastPoint.flatMap(this::hoverAt));
    }

    /** Forgets where the pointer was, as it has left the pane. */
    void clearPointer() {
        this.lastPoint = Optional.empty();
        this.highlight(Optional.empty());
    }

    /** What {@code point} rests on; remembers it for the next {@link #show}. */
    Optional<HoverTarget> hoverAt(Point point) {
        this.lastPoint = Optional.of(point);
        int position = this.viewToModel2D(point);
        if (position < 0) {
            return Optional.empty();
        }
        Element element = this.getStyledDocument().getCharacterElement(position);
        Object run = element.getAttributes().getAttribute(HoverKey.INSTANCE);
        if (run == null && position > 0) {
            // Past the middle of an icon row's one wide icon the position is the one after it, the line's end.
            Element before = this.getStyledDocument().getCharacterElement(position - 1);
            Object prior = before.getAttributes().getAttribute(HoverKey.INSTANCE);
            if (switch (prior) {
                case HoverRun.IconRowRun row -> true;
                case null, default -> false;
            }) {
                element = before;
                run = prior;
            }
        }
        try {
            return switch (run) {
                case HoverRun.MentionRun mention -> this.mentionAt(point, element, mention);
                case HoverRun.IconRowRun row -> this.iconAt(point, element, row);
                case HoverRun.RowRun row -> Optional.of(this.rowTarget(element, row));
                case null, default -> Optional.empty();
            };
        } catch (BadLocationException e) {
            return Optional.empty();
        }
    }

    /** Brightens {@code target}, or nothing. */
    void highlight(Optional<HoverTarget> target) {
        if (!target.equals(this.highlighted)) {
            this.highlighted = target;
            this.repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.highlighted.ifPresent(target -> {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(HIGHLIGHT);
            target.areas().forEach(area -> g2.fillRoundRect(area.x, area.y, area.width, area.height, HIGHLIGHT_ARC,
                    HIGHLIGHT_ARC));
            g2.dispose();
        });
    }

    private Optional<HoverTarget> mentionAt(Point point, Element element, HoverRun.MentionRun run)
            throws BadLocationException {
        Element paragraph = element.getParentElement();
        int start = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;
        for (int i = 0; i < paragraph.getElementCount(); i++) {
            Element sibling = paragraph.getElement(i);
            if (sibling.getAttributes().getAttribute(HoverKey.INSTANCE) == run) {
                start = Math.min(start, sibling.getStartOffset());
                end = Math.max(end, sibling.getEndOffset());
            }
        }
        if (start >= end) {
            return Optional.empty();
        }
        List<Rectangle> lines = this.lineBoxes(start, end);
        return lines.stream().filter(line -> line.contains(point)).findFirst()
                .map(line -> new HoverTarget.Mention(run.mention(), line, lines));
    }

    /**
     * One box per line the text from {@code start} to {@code end} covers, each only as wide as that text: a wrapped
     * mention's two halves, not the block both lines span.
     */
    private List<Rectangle> lineBoxes(int start, int end) throws BadLocationException {
        List<Rectangle2D> boxes = new ArrayList<>();
        for (int at = start; at < end; at++) {
            Rectangle2D left = this.getUI().modelToView2D(this, at, Position.Bias.Forward);
            // Backward bias keeps the edge after the last character of a line on that line, not the next one's start.
            Rectangle2D right = this.getUI().modelToView2D(this, at + 1, Position.Bias.Backward);
            Rectangle2D character = new Rectangle2D.Double(left.getX(), left.getY(),
                    Math.max(1, right.getX() - left.getX()), left.getHeight());
            Rectangle2D last = boxes.isEmpty() ? null : boxes.getLast();
            if (last != null && last.getY() < character.getMaxY() && character.getY() < last.getMaxY()) {
                boxes.set(boxes.size() - 1, last.createUnion(character));
            } else {
                boxes.add(character);
            }
        }
        return boxes.stream().map(Rectangle2D::getBounds).toList();
    }

    private Optional<HoverTarget> iconAt(Point point, Element element, HoverRun.IconRowRun run)
            throws BadLocationException {
        Rectangle box = this.modelToView2D(element.getStartOffset()).getBounds();
        OptionalInt index = run.layout().iconAt(point.x - box.x, point.y - box.y);
        if (index.isEmpty()) {
            return Optional.empty();
        }
        SheetIcon icon = run.row().icons().get(index.getAsInt());
        Point cell = run.layout().cell(index.getAsInt());
        Rectangle bounds = new Rectangle(box.x + cell.x, box.y + cell.y, TermIconPainter.ROW, TermIconPainter.ROW);
        return Optional.of(new HoverTarget.Icon(icon, bounds));
    }

    private HoverTarget rowTarget(Element element, HoverRun.RowRun run) {
        Element paragraph = element.getParentElement();
        try {
            Rectangle2D start = this.modelToView2D(paragraph.getStartOffset());
            Rectangle2D end = this.modelToView2D(Math.max(paragraph.getStartOffset(), paragraph.getEndOffset() - 1));
            Rectangle bounds = start.createUnion(end).getBounds();
            bounds.width = Math.max(bounds.width, this.getWidth() - bounds.x);
            return new HoverTarget.Row(run.row(), bounds);
        } catch (BadLocationException e) {
            throw new IllegalStateException("A run of the sheet lies outside its document", e);
        }
    }
}
