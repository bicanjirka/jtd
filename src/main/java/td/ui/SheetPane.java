package td.ui;

import td.ui.render.InfoSheet;
import td.ui.render.SheetIcon;
import td.util.ThreadConfined;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.io.Serial;
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
            Rectangle bounds = target.bounds();
            g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, HIGHLIGHT_ARC, HIGHLIGHT_ARC);
            g2.dispose();
        });
    }

    private Optional<HoverTarget> mentionAt(Point point, Element element, HoverRun.MentionRun run)
            throws BadLocationException {
        Element paragraph = element.getParentElement();
        Rectangle2D union = null;
        for (int i = 0; i < paragraph.getElementCount(); i++) {
            Element sibling = paragraph.getElement(i);
            AttributeSet attributes = sibling.getAttributes();
            if (attributes.getAttribute(HoverKey.INSTANCE) != run) {
                continue;
            }
            Rectangle2D start = this.modelToView2D(sibling.getStartOffset());
            Rectangle2D end = this.modelToView2D(sibling.getEndOffset() - 1);
            Rectangle2D span = start.createUnion(end);
            union = union == null ? span : union.createUnion(span);
        }
        if (union == null) {
            return Optional.empty();
        }
        Rectangle bounds = union.getBounds();
        bounds.width = Math.max(bounds.width, 1);
        return bounds.contains(point)
                ? Optional.of(new HoverTarget.Mention(run.mention(), bounds))
                : Optional.empty();
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
