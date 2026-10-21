package td.ui;

import td.term.TermText;
import td.ui.render.IconFace;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.SheetIcon;
import td.ui.render.SheetLine;
import td.util.ThreadConfined;

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
import java.awt.font.FontRenderContext;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Optional;

/**
 * Lays an {@link InfoSheet} out as a styled document: one paragraph per line, each row's glyph
 * painted by {@link Java2DFrameRenderer} and its label in that glyph's colour, values on a
 * right-aligned tab stop at the pane's edge.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
final class InfoSheetDocument {

    private static final Color VALUE_COLOR = new Color(170, 205, 170);
    private static final Color PROSE_COLOR = new Color(150, 170, 150);
    private static final Color GONE_COLOR = new Color(255, 120, 120);
    private static final Color BETTER_COLOR = new Color(140, 255, 140);
    private static final Color WORSE_COLOR = GONE_COLOR;
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
    private static final int DIVIDER_HEIGHT = 7;
    private static final int ICON_LABEL_FONT_SIZE = 10;
    private static final int TITLE_GAP = 6;
    /** A value ending exactly at the pane's edge wraps on rounding, so the column stops short. */
    private static final int EDGE_MARGIN = 4;

    private final Java2DFrameRenderer renderer;
    private final DefaultStyledDocument doc = new DefaultStyledDocument();
    private final int width;
    private final FontMetrics metrics;
    private HoverRun hover;

    private InfoSheetDocument(Java2DFrameRenderer renderer, int width, FontMetrics metrics) {
        this.renderer = renderer;
        this.width = width - EDGE_MARGIN;
        this.metrics = metrics;
    }

    /**
     * @param width   the text width, where values align
     * @param metrics the pane's font, to size the health bar beside its value
     */
    static StyledDocument of(InfoSheet sheet, Java2DFrameRenderer renderer, int width, FontMetrics metrics) {
        InfoSheetDocument writer = new InfoSheetDocument(renderer, width, metrics);
        sheet.lines().forEach(writer::append);
        writer.finish();
        return writer.doc;
    }

    private void append(SheetLine line) {
        switch (line) {
            case SheetLine.EnemyHeader header -> this.header(header);
            case SheetLine.Title title -> this.title(title);
            case SheetLine.HealthBar bar -> this.healthBar(bar);
            case SheetLine.Row row -> this.row(row);
            case SheetLine.Prose prose -> {
                SimpleAttributeSet dim = colored(PROSE_COLOR);
                StyleConstants.setFontSize(dim, PROSE_FONT_SIZE);
                this.spans(prose.text(), dim);
            }
            case SheetLine.Sentence sentence -> this.sentence(sentence);
            case SheetLine.IconTitle title -> this.iconTitle(title);
            case SheetLine.IconRow row -> this.iconRow(row);
            case SheetLine.Divider divider -> {
                this.icon(new PaintedIcon(this.width, DIVIDER_HEIGHT, g2 -> {
                    g2.setColor(Hud.BORDER_IDLE);
                    g2.draw(new Line2D.Float(-this.width / 2f, 0, this.width / 2f, 0));
                }), smallFont());
                this.insert("\n", smallFont());
                return;
            }
            case SheetLine.Gap gap -> {
                this.insert("\n", smallFont());
                return;
            }
        }
        this.insert("\n", new SimpleAttributeSet());
    }

    private void header(SheetLine.EnemyHeader header) {
        this.icon(new PaintedIcon(BODY_BOX, BODY_BOX, g2 -> this.renderer.paintEnemyGlyph(g2, header.body(), BODY_SIZE)));
        SimpleAttributeSet name = new SimpleAttributeSet();
        StyleConstants.setBold(name, true);
        this.insert(" " + header.name() + "\t", name);
        Optional<Color> badgeColor = Java2DFrameRenderer.rankBadgePalette(header.badge()).map(Java2DFrameRenderer::colorFor);
        if (badgeColor.isPresent()) {
            this.icon(new PaintedIcon(GLYPH_BOX, GLYPH_BOX,
                    g2 -> this.renderer.paintRankBadgeGlyph(g2, header.badge(), BADGE_SIZE)));
        }
        SimpleAttributeSet rank = colored(badgeColor.orElse(PROSE_COLOR));
        StyleConstants.setFontSize(rank, RANK_FONT_SIZE);
        this.insert(" " + header.rank(), rank);
    }

    private void title(SheetLine.Title title) {
        this.icon(new PaintedIcon(BODY_BOX, BODY_BOX,
                g2 -> this.renderer.paintRowGlyph(g2, title.glyph(), Optional.of(title.tone()), BODY_SIZE)));
        SimpleAttributeSet name = new SimpleAttributeSet();
        StyleConstants.setBold(name, true);
        this.insert(" " + title.name(), name);
        if (!title.value().isEmpty()) {
            this.insert("\t" + title.value(), colored(VALUE_COLOR));
        }
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
        this.hover = row.tooltip().isPresent() ? new HoverRun.RowRun(row) : null;
        this.icon(new PaintedIcon(GLYPH_BOX, GLYPH_BOX, g2 -> this.renderer.paintRowGlyph(g2, row.glyph(), row.tone(), GLYPH_SIZE)));
        SimpleAttributeSet label = row.tone().map(Java2DFrameRenderer::colorFor).map(InfoSheetDocument::colored).orElseGet(SimpleAttributeSet::new);
        this.insert(" " + row.label(), label);
        if (!row.value().isEmpty()) {
            this.insert("\t" + row.value(), colored(switch (row.trend()) {
                case NONE -> VALUE_COLOR;
                case BETTER -> BETTER_COLOR;
                case WORSE -> WORSE_COLOR;
            }));
        }
        this.hover = null;
    }

    private void sentence(SheetLine.Sentence sentence) {
        if (sentence.glyph().isPresent()) {
            SheetLine.Glyph glyph = sentence.glyph().get();
            Optional<Palette> tone = sentence.tone();
            this.icon(new PaintedIcon(GLYPH_BOX, GLYPH_BOX, g2 -> this.renderer.paintRowGlyph(g2, glyph, tone, GLYPH_SIZE)));
            this.insert(" ", new SimpleAttributeSet());
        }
        this.spans(sentence.text(), colored(Hud.FOREGROUND));
    }

    /** Plain runs in {@code plain}'s style, each mention as its face then its title in the face's ink. */
    private void spans(TermText text, SimpleAttributeSet plain) {
        for (TermText.Span span : text.spans()) {
            switch (span) {
                case TermText.Plain run -> this.insert(run.text(), plain);
                case TermText.Mention mention -> {
                    IconFace face = TermLooks.faceOf(mention.term());
                    this.hover = new HoverRun.MentionRun(mention);
                    this.icon(new PaintedIcon(TermIconPainter.MENTION, TermIconPainter.MENTION,
                            g2 -> paintFace(g2, face, TermIconPainter.MENTION)));
                    SimpleAttributeSet title = new SimpleAttributeSet(plain);
                    StyleConstants.setForeground(title, TermIconPainter.ink(face.tone()));
                    this.insert(" " + mention.term().title(), title);
                    this.hover = null;
                }
            }
        }
    }

    /**
     * The face and name as one icon, and the value as another after a tab: a text line sits on the baseline, so
     * painting them whole is what centres the name on its face.
     */
    private void iconTitle(SheetLine.IconTitle title) {
        Font nameFont = this.metrics.getFont().deriveFont(Font.BOLD);
        int nameWidth = (int) Math.ceil(textWidth(nameFont, title.name()));
        this.icon(new PaintedIcon(TermIconPainter.ROW + TITLE_GAP + nameWidth, TermIconPainter.ROW,
                g2 -> this.paintTitle(g2, title, nameFont, nameWidth)));
        if (!title.value().isEmpty()) {
            Font valueFont = this.metrics.getFont();
            int valueWidth = (int) Math.ceil(textWidth(valueFont, title.value()));
            this.insert("\t", new SimpleAttributeSet());
            this.icon(new PaintedIcon(valueWidth, TermIconPainter.ROW, g2 -> {
                g2.translate(-valueWidth / 2.0, -TermIconPainter.ROW / 2.0);
                paintCentredText(g2, title.value(), valueFont, VALUE_COLOR, 0);
            }));
        }
    }

    private void paintTitle(Graphics2D g2, SheetLine.IconTitle title, Font nameFont, int nameWidth) {
        int width = TermIconPainter.ROW + TITLE_GAP + nameWidth;
        g2.translate(-width / 2.0, -TermIconPainter.ROW / 2.0);
        Graphics2D face = (Graphics2D) g2.create();
        TermIconPainter.paint(face, title.face(), TermIconPainter.ROW);
        face.dispose();
        paintCentredText(g2, title.name(), nameFont, TermIconPainter.ink(title.tone()), TermIconPainter.ROW + TITLE_GAP);
    }

    /** {@code text} at {@code x}, its middle on the middle of a row-high box whose top left is the origin. */
    private static void paintCentredText(Graphics2D g2, String text, Font font, Color color, int x) {
        g2.setFont(font);
        FontMetrics textMetrics = g2.getFontMetrics();
        g2.setColor(color);
        g2.drawString(text, x, (TermIconPainter.ROW + textMetrics.getAscent() - textMetrics.getDescent()) / 2f);
    }

    private static double textWidth(Font font, String text) {
        return font.getStringBounds(text, new FontRenderContext(null, true, true)).getWidth();
    }

    /** One embedded icon as wide as the pane: the label, then each face where {@link IconRowLayout} puts it. */
    private void iconRow(SheetLine.IconRow row) {
        boolean anyBelow = row.icons().stream().anyMatch(icon -> !icon.face().below().isEmpty());
        IconRowLayout layout = IconRowLayout.of(row.icons().size(), anyBelow, this.width);
        this.hover = new HoverRun.IconRowRun(row, layout);
        this.icon(new PaintedIcon(this.width, layout.height(), g2 -> this.paintIconRow(g2, row, layout)));
        this.hover = null;
    }

    private void paintIconRow(Graphics2D g2, SheetLine.IconRow row, IconRowLayout layout) {
        g2.translate(-this.width / 2.0, -layout.height() / 2.0);
        g2.setFont(new Font(Font.DIALOG, Font.PLAIN, ICON_LABEL_FONT_SIZE));
        FontMetrics labelMetrics = g2.getFontMetrics();
        g2.setColor(PROSE_COLOR);
        g2.drawString(row.label(), 0,
                (TermIconPainter.ROW + labelMetrics.getAscent() - labelMetrics.getDescent()) / 2f);
        for (int i = 0; i < row.icons().size(); i++) {
            SheetIcon icon = row.icons().get(i);
            Graphics2D cell = (Graphics2D) g2.create();
            cell.translate(layout.cell(i).x, layout.cell(i).y);
            TermIconPainter.paint(cell, icon.face(), TermIconPainter.ROW);
            cell.dispose();
        }
    }

    /** A face painted about the centre of the icon that holds it. */
    private static void paintFace(Graphics2D g2, IconFace face, int size) {
        g2.translate(-size / 2.0, -size / 2.0);
        TermIconPainter.paint(g2, face, size);
    }

    private static SimpleAttributeSet smallFont() {
        SimpleAttributeSet small = new SimpleAttributeSet();
        StyleConstants.setFontSize(small, GAP_FONT_SIZE);
        return small;
    }

    private static SimpleAttributeSet colored(Color color) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color);
        return attributes;
    }

    private void icon(PaintedIcon icon) {
        this.icon(icon, new SimpleAttributeSet());
    }

    private void icon(PaintedIcon icon, AttributeSet base) {
        SimpleAttributeSet attributes = new SimpleAttributeSet(base);
        StyleConstants.setIcon(attributes, icon);
        this.insert(" ", attributes);
    }

    /** In the pane's font unless {@code attributes} say otherwise, so text measures as it draws. */
    private void insert(String text, AttributeSet attributes) {
        SimpleAttributeSet withFont = new SimpleAttributeSet(attributes);
        if (this.hover != null) {
            withFont.addAttribute(HoverKey.INSTANCE, this.hover);
        }
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
