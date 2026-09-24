package td.ui;

import td.ui.render.EnemySheet;
import td.ui.render.SheetLine;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Optional;

/**
 * Lays an {@link EnemySheet} out as a styled document: one paragraph per line, each row's glyph
 * painted by {@link Java2DFrameRenderer} and its label in that glyph's colour, values on a
 * right-aligned tab stop at the pane's edge.
 */
final class EnemySheetDocument {

    private static final Color VALUE_COLOR = new Color(170, 205, 170);
    private static final Color PROSE_COLOR = new Color(150, 170, 150);
    private static final Color GONE_COLOR = new Color(255, 120, 120);
    private static final Color BAR_BACKGROUND = new Color(12, 22, 12);
    private static final Color BAR_FILL = new Color(52, 122, 52);
    private static final Color BAR_FILL_GONE = new Color(60, 66, 60);
    private static final Font BAR_FONT = new Font(Font.DIALOG, Font.PLAIN, 10);
    private static final int GLYPH_BOX = 11;
    private static final float GLYPH_SIZE = 4.2f;
    private static final int BODY_BOX = 13;
    private static final float BODY_SIZE = 5f;
    private static final float BADGE_SIZE = 4.5f;
    private static final int BAR_HEIGHT = 12;
    private static final int BAR_VALUE_GAP = 8;
    private static final int RANK_FONT_SIZE = 11;
    private static final int PROSE_FONT_SIZE = 11;
    private static final int GAP_FONT_SIZE = 4;
    /** A value ending exactly at the pane's edge wraps on rounding, so the column stops short. */
    private static final int EDGE_MARGIN = 4;

    private final Java2DFrameRenderer renderer;
    private final DefaultStyledDocument doc = new DefaultStyledDocument();
    private final int width;
    private final FontMetrics metrics;

    private EnemySheetDocument(Java2DFrameRenderer renderer, int width, FontMetrics metrics) {
        this.renderer = renderer;
        this.width = width - EDGE_MARGIN;
        this.metrics = metrics;
    }

    /**
     * @param width   the text width, where values align
     * @param metrics the pane's font, to size the health bar beside its value
     */
    static StyledDocument of(EnemySheet sheet, Java2DFrameRenderer renderer, int width, FontMetrics metrics) {
        EnemySheetDocument writer = new EnemySheetDocument(renderer, width, metrics);
        sheet.lines().forEach(writer::append);
        writer.finish();
        return writer.doc;
    }

    private void append(SheetLine line) {
        switch (line) {
            case SheetLine.Header header -> this.header(header);
            case SheetLine.HealthBar bar -> this.healthBar(bar);
            case SheetLine.Row row -> this.row(row);
            case SheetLine.Prose prose -> {
                SimpleAttributeSet dim = colored(PROSE_COLOR);
                StyleConstants.setFontSize(dim, PROSE_FONT_SIZE);
                this.insert(prose.text(), dim);
            }
            case SheetLine.Gap gap -> {
                SimpleAttributeSet small = new SimpleAttributeSet();
                StyleConstants.setFontSize(small, GAP_FONT_SIZE);
                this.insert("\n", small);
                return;
            }
        }
        this.insert("\n", new SimpleAttributeSet());
    }

    private void header(SheetLine.Header header) {
        this.icon(new PaintedIcon(BODY_BOX, BODY_BOX, g2 -> this.renderer.paintEnemyGlyph(g2, header.body(), BODY_SIZE)));
        SimpleAttributeSet name = new SimpleAttributeSet();
        StyleConstants.setBold(name, true);
        this.insert(" " + header.name() + "\t", name);
        Optional<Color> badgeColor = Java2DFrameRenderer.rankBadgePalette(header.badge()).map(Java2DFrameRenderer::colorFor);
        badgeColor.ifPresent(color -> this.icon(new PaintedIcon(GLYPH_BOX, GLYPH_BOX,
                g2 -> this.renderer.paintRankBadgeGlyph(g2, header.badge(), BADGE_SIZE))));
        SimpleAttributeSet rank = colored(badgeColor.orElse(PROSE_COLOR));
        StyleConstants.setFontSize(rank, RANK_FONT_SIZE);
        this.insert(" " + header.rank(), rank);
    }

    /** The bar fills what its value leaves, with health written across it. */
    private void healthBar(SheetLine.HealthBar bar) {
        int barWidth = this.width - this.metrics.stringWidth(bar.value()) - BAR_VALUE_GAP;
        float fraction = bar.maxHealth() > 0 ? Math.clamp((float) bar.health() / bar.maxHealth(), 0f, 1f) : 0f;
        String health = bar.health() + " / " + bar.maxHealth();
        this.icon(new PaintedIcon(barWidth, BAR_HEIGHT, g2 -> paintBar(g2, barWidth, fraction, health, bar.gone())));
        this.insert("\t" + bar.value(), colored(bar.gone() ? GONE_COLOR : VALUE_COLOR));
    }

    private static void paintBar(Graphics2D g2, int barWidth, float fraction, String text, boolean gone) {
        float left = -barWidth / 2f;
        float top = -BAR_HEIGHT / 2f;
        g2.setColor(BAR_BACKGROUND);
        g2.fill(new Rectangle2D.Float(left, top, barWidth, BAR_HEIGHT));
        g2.setColor(gone ? BAR_FILL_GONE : BAR_FILL);
        g2.fill(new Rectangle2D.Float(left, top, barWidth * fraction, BAR_HEIGHT));
        g2.setColor(Hud.BORDER_IDLE);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new Rectangle2D.Float(left + 0.5f, top + 0.5f, barWidth - 1, BAR_HEIGHT - 1));
        g2.setFont(BAR_FONT);
        FontMetrics barMetrics = g2.getFontMetrics();
        g2.setColor(gone ? PROSE_COLOR : Hud.FOREGROUND);
        g2.drawString(text, -barMetrics.stringWidth(text) / 2f, (barMetrics.getAscent() - barMetrics.getDescent()) / 2f);
    }

    private void row(SheetLine.Row row) {
        this.icon(new PaintedIcon(GLYPH_BOX, GLYPH_BOX, g2 -> this.renderer.paintRowGlyph(g2, row.glyph(), row.tone(), GLYPH_SIZE)));
        SimpleAttributeSet label = row.tone().map(Java2DFrameRenderer::colorFor).map(EnemySheetDocument::colored).orElseGet(SimpleAttributeSet::new);
        this.insert(" " + row.label(), label);
        if (!row.value().isEmpty()) {
            this.insert("\t" + row.value(), colored(VALUE_COLOR));
        }
    }

    private static SimpleAttributeSet colored(Color color) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color);
        return attributes;
    }

    private void icon(PaintedIcon icon) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setIcon(attributes, icon);
        this.insert(" ", attributes);
    }

    /** In the pane's font unless {@code attributes} say otherwise, so text measures as it draws. */
    private void insert(String text, AttributeSet attributes) {
        SimpleAttributeSet withFont = new SimpleAttributeSet(attributes);
        if (!withFont.isDefined(StyleConstants.FontFamily)) {
            StyleConstants.setFontFamily(withFont, this.metrics.getFont().getFamily());
        }
        if (!withFont.isDefined(StyleConstants.FontSize)) {
            StyleConstants.setFontSize(withFont, this.metrics.getFont().getSize());
        }
        try {
            this.doc.insertString(this.doc.getLength(), text, withFont);
        } catch (BadLocationException e) {
            throw new IllegalStateException("Appending to the end of the document", e);
        }
    }

    /** Drops the last line's newline and sets the value tab stop on every paragraph. */
    private void finish() {
        try {
            if (this.doc.getLength() > 0) {
                this.doc.remove(this.doc.getLength() - 1, 1);
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException("Removing the document's last character", e);
        }
        SimpleAttributeSet paragraph = new SimpleAttributeSet();
        StyleConstants.setTabSet(paragraph, new TabSet(new TabStop[]{new TabStop(this.width, TabStop.ALIGN_RIGHT, TabStop.LEAD_NONE)}));
        this.doc.setParagraphAttributes(0, this.doc.getLength(), paragraph, false);
    }
}
