package td.ui;

import td.term.TermNumbers;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.ui.UpgradeNodeState.Look;
import td.ui.UpgradeTreeLayout.Spot;
import td.ui.render.IconFace;
import td.ui.render.TowerLook;
import td.util.ThreadConfined;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;
import java.util.Optional;

/**
 * Paints one tower's upgrade tree in the design units of {@link UpgradeTreeLayout}: each tier's row with the tower's
 * picture at its left, the gate bands, the links and cluster frames, every node in its look with its key and price,
 * the XP line and the footer. An open tier takes the tower's colour for it, a locked one is grey. A node that changes
 * a number is a rounded square, one that adds an ability a circle. The caller scales the graphics to the panel.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class UpgradeTreePainter {

    /** What the hovered node marks on other nodes. */
    enum Mark {
        /** A step of the path a click buys, or the member a switch takes. */
        PATH,
        /** A node a downgrade or a switch takes away. */
        DROP,
        /** A node the hovered path would lock out. */
        CRACK
    }

    /** The hovered node and what it marks. */
    record Highlight(Optional<UpgradeNode> hovered, Map<UpgradeNode, Mark> marks) {

        Highlight {
            marks = Map.copyOf(marks);
        }

        static Highlight none() {
            return new Highlight(Optional.empty(), Map.of());
        }

        boolean is(UpgradeNode node, Mark mark) {
            return this.marks.get(node) == mark;
        }
    }

    /** The XP line: its words and how full its bar is. */
    record XpLine(String text, float share, Color fill) {
    }

    /** The footer's words; a warning is red, words about the hovered node bold. */
    record Footer(String text, boolean warns, boolean active) {
    }

    /** How far the tower's tiers are open, and each gate band's words by tier. */
    record Gates(int openTier, Map<Integer, String> labels) {

        Gates {
            labels = Map.copyOf(labels);
        }
    }

    private static final Color FOREGROUND = Hud.FOREGROUND;
    private static final Color DIM_TEXT = Hud.TEXT_DIM;
    private static final Color WHITE = Color.WHITE;
    private static final Color DROP = new Color(255, 90, 90);
    private static final Color SHORT = Hud.SHORT;
    private static final Color WARN = new Color(255, 138, 138);
    // The neutrals are the HUD's greys, darkest for what is locked out.
    private static final Color LOCKED = new Color(110, 110, 113);
    private static final Color LINK_IDLE = new Color(67, 67, 68);
    private static final Color LINK_OUT = new Color(44, 44, 45);
    private static final Color LOCKED_BORDER = new Color(84, 84, 86);
    private static final Color LOCKED_FILL = new Color(24, 24, 25);
    private static final Color OUT_BORDER = new Color(53, 53, 54);
    private static final Color OUT_FILL = new Color(23, 23, 23);
    private static final Color OUT_HATCH = new Color(33, 33, 34);
    private static final Color OUT_INK = new Color(83, 83, 85);
    private static final Color BUY_FILL = new Color(28, 28, 29);
    private static final Color XP_TRACK = new Color(36, 36, 37);
    private static final float HOVER_FILL = 0.12f;
    private static final float HOVER_BORDER = 0.35f;
    private static final float ABILITY_BORDER = 0.4f;
    private static final float PREVIEW_ALPHA = 0.4f;
    private static final Font XP_FONT = new Font(Font.DIALOG, Font.PLAIN, 10);
    private static final Font PRICE_FONT = Hud.PRICE_FONT;
    private static final Font NUMERAL_FONT = new Font(Font.DIALOG, Font.BOLD, 7);
    private static final Font BAND_FONT = new Font(Font.DIALOG, Font.PLAIN, 9);
    private static final Font FRAME_FONT = new Font(Font.DIALOG, Font.PLAIN, 7);
    private static final Font PILL_FONT = new Font(Font.DIALOG, Font.BOLD, 9);
    private static final Font FOOTER_FONT = new Font(Font.DIALOG, Font.PLAIN, 9);
    private static final Font FOOTER_BOLD = FOOTER_FONT.deriveFont(Font.BOLD);
    /** Under the border's title. */
    private static final float XP_BASELINE = 30f;
    private static final float CORNER = 7f;
    private static final float CHIP_INSET = 3f;
    private static final float ROUND_CHIP_INSET = 5f;
    private static final float BADGE = 15f;
    private static final float THUMB = 18f;
    private static final float SMALL_THUMB = 11f;

    private final UpgradeTreeLayout layout;
    private final UpgradeTree tree;
    private final TowerLook look;
    private final TowerFactory.Type type;

    UpgradeTreePainter(UpgradeTreeLayout layout, UpgradeTree tree, TowerLook look, TowerFactory.Type type) {
        this.layout = layout;
        this.tree = tree;
        this.look = look;
        this.type = type;
    }

    void paint(Graphics2D g, Map<UpgradeNode, UpgradeNodeState> states, Highlight highlight, Gates gates, XpLine xp,
               Footer footer) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        this.layout.rows().forEach(row -> this.paintRow(g, row, gates));
        this.paintSplit(g, gates);
        this.layout.frames().forEach(frame -> this.paintFrame(g, frame, gates));
        this.layout.links().forEach(link -> this.paintLink(g, link, states, highlight, gates));
        this.layout.hidden().forEach(spot -> paintHidden(g, spot));
        this.layout.spots().forEach((node, spot) -> this.dimmed(g, node, () -> {
            UpgradeNodeState state = states.get(node);
            if (this.tree.isUnlock(node)) {
                this.paintPill(g, spot, state, highlight, gates);
            } else {
                this.paintNode(g, node, spot, state, highlight, gates);
                this.paintMarks(g, node, spot, state, gates);
            }
        }));
        paintXp(g, xp);
        paintFooter(g, footer, this.layout.height());
    }

    /** {@code paint} at the preview's alpha when {@code node} only shows for the hovered member. */
    private void dimmed(Graphics2D g, UpgradeNode node, Runnable paint) {
        if (!this.layout.previewed().contains(node)) {
            paint.run();
            return;
        }
        Composite was = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, PREVIEW_ALPHA));
        try {
            paint.run();
        } finally {
            g.setComposite(was);
        }
    }

    /** The tier's faint ground, the tower's picture at its left, and the words of the gate under it. */
    private void paintRow(Graphics2D g, UpgradeTreeLayout.Row row, Gates gates) {
        Color tier = this.tierColor(row.tier());
        boolean open = row.tier() <= gates.openTier();
        g.setColor(Hud.withAlpha(open ? tier : FOREGROUND, open ? 0.07f : 0.03f));
        g.fill(new RoundRectangle2D.Float(4f, row.top() + 1f, UpgradeTreeLayout.WIDTH - 8f, row.bottom() - row.top() - 2f,
                8f, 8f));
        Graphics2D thumb = (Graphics2D) g.create();
        try {
            thumb.translate(UpgradeTreeLayout.THUMB_X, row.centre());
            thumb.setColor(open ? tier : LOCKED);
            thumb.fill(TowerLooks.body(this.look, row.folded() ? SMALL_THUMB : THUMB));
        } finally {
            thumb.dispose();
        }
        if (row.bandBottom() <= row.bottom()) {
            return;
        }
        float y = (row.bottom() + row.bandBottom()) / 2;
        boolean bandOpen = row.tier() <= gates.openTier();
        String label = gates.labels().getOrDefault(row.tier(), UpgradeTier.nameOf(row.tier()));
        float x = this.tree.unlockOf(row.tier()).isPresent()
                ? this.layout.spotOf(this.tree.unlockOf(row.tier()).get()).map(s -> s.x() + s.width() / 2 + 6f)
                        .orElse(46f)
                : 46f;
        g.setFont(BAND_FONT);
        g.setColor(bandOpen ? tier : DIM_TEXT);
        g.drawString(label, x, y + 3f);
        float end = x + g.getFontMetrics().stringWidth(label) + 6f;
        float stop = this.layout.split().filter(split -> row.tier() == this.forkTier()).map(split -> split - 4f)
                .orElse(UpgradeTreeLayout.WIDTH - 8f);
        if (end < stop) {
            g.setColor(Hud.withAlpha(bandOpen ? tier : FOREGROUND, 0.18f));
            g.setStroke(new BasicStroke(1f));
            g.draw(new Line2D.Float(end, y, stop, y));
        }
    }

    /** The line between the shared column and the side's, from the fork's band to the top. */
    private void paintSplit(Graphics2D g, Gates gates) {
        if (this.layout.split().isEmpty()) {
            return;
        }
        float x = this.layout.split().get();
        int fork = this.forkTier();
        float top = this.layout.rows().stream().filter(row -> row.tier() >= fork)
                .map(UpgradeTreeLayout.Row::top).reduce(Float::min).orElse(0f);
        float bottom = this.layout.rows().stream().filter(row -> row.tier() == fork)
                .map(UpgradeTreeLayout.Row::bandBottom).findFirst().orElse(top);
        g.setColor(Hud.withAlpha(fork <= gates.openTier() ? this.tierColor(fork) : FOREGROUND, 0.25f));
        g.setStroke(dashed(1f));
        g.draw(new Line2D.Float(x, top + 3f, x, bottom - 3f));
    }

    private int forkTier() {
        return this.tree.fork().flatMap(fork -> this.tree.node(fork.memberIds().getFirst())).map(UpgradeNode::tier)
                .orElse(Integer.MAX_VALUE);
    }

    private void paintFrame(Graphics2D g, UpgradeTreeLayout.Frame frame, Gates gates) {
        int tier = this.tree.node(frame.cluster().memberIds().getFirst()).orElseThrow().tier();
        Color color = tier <= gates.openTier() ? this.tierColor(tier) : LOCKED;
        Shape box = new RoundRectangle2D.Float(frame.left(), frame.top(), frame.right() - frame.left(),
                frame.bottom() - frame.top(), 2 * CORNER + 4f, 2 * CORNER + 4f);
        g.setColor(Hud.withAlpha(color, 0.5f));
        g.setStroke(new BasicStroke(1f));
        g.draw(box);
        if (frame.bottom() - frame.top() < UpgradeTreeLayout.NODE) {
            return;
        }
        g.setFont(FRAME_FONT);
        FontMetrics metrics = g.getFontMetrics();
        String name = frame.cluster().name();
        float width = metrics.stringWidth(name);
        float x = frame.left() + 8f;
        g.setColor(Hud.BACKGROUND);
        g.fill(new Rectangle2D.Float(x - 2f, frame.top() - 4f, width + 4f, 7f));
        g.setColor(Hud.withAlpha(color, 0.85f));
        g.drawString(name, x, frame.top() + 2.5f);
    }

    private void paintLink(Graphics2D g, UpgradeTreeLayout.Link link, Map<UpgradeNode, UpgradeNodeState> states,
                           Highlight highlight, Gates gates) {
        Spot a = this.layout.spots().get(link.from());
        Spot b = this.layout.spots().get(link.to());
        Look from = states.get(link.from()).look();
        Look to = states.get(link.to()).look();
        Color tier = this.tierColor(link.to().tier());
        Color color;
        Stroke stroke = new BasicStroke(2f);
        if (highlight.is(link.to(), Mark.DROP)) {
            color = DROP;
            stroke = dashed(2f);
        } else if (highlight.is(link.to(), Mark.PATH)) {
            color = WHITE;
        } else if (from == Look.OWNED && to == Look.OWNED) {
            color = tier;
        } else if (from == Look.LOCKED_OUT || to == Look.LOCKED_OUT) {
            color = LINK_OUT;
        } else if (from == Look.OWNED && (to == Look.BUYABLE || to == Look.SHORT_OF_CASH)) {
            color = Hud.withAlpha(tier, 0.7f);
            stroke = dashed(2f);
        } else {
            color = LINK_IDLE;
        }
        double length = Math.hypot(b.x() - a.x(), b.y() - a.y());
        float r1 = Math.max(a.width(), a.height()) / 2 + 1f;
        float r2 = Math.max(b.width(), b.height()) / 2 + 1f;
        if (length <= r1 + r2) {
            return;
        }
        float ux = (float) ((b.x() - a.x()) / length);
        float uy = (float) ((b.y() - a.y()) / length);
        g.setColor(color);
        g.setStroke(stroke);
        g.draw(new Line2D.Float(a.x() + ux * r1, a.y() + uy * r1, b.x() - ux * r2, b.y() - uy * r2));
    }

    /** Where the branch of a member the tower didn't pick hides: a dashed box with an ellipsis. */
    private static void paintHidden(Graphics2D g, Spot spot) {
        g.setColor(LINK_IDLE);
        g.setStroke(dashed(1f));
        g.draw(new RoundRectangle2D.Float(spot.x() - spot.width() / 2, spot.y() - spot.height() / 2, spot.width(),
                spot.height(), 5f, 5f));
        for (int i = -1; i <= 1; i++) {
            g.fill(new Ellipse2D.Float(spot.x() + i * 3.5f - 1f, spot.y() - 1f, 2f, 2f));
        }
    }

    /** A tier's unlock: its price until bought, a check after. */
    private void paintPill(Graphics2D g, Spot spot, UpgradeNodeState state, Highlight highlight, Gates gates) {
        Look look = state.look();
        Color tier = this.tierColor(state.node().tier());
        boolean lit = look == Look.OWNED || look == Look.BUYABLE || look == Look.SHORT_OF_CASH;
        Color color = lit ? tier : LOCKED;
        boolean hovered = highlight.hovered().filter(state.node()::equals).isPresent();
        Shape pill = new RoundRectangle2D.Float(spot.x() - spot.width() / 2, spot.y() - spot.height() / 2, spot.width(),
                spot.height(), spot.height(), spot.height());
        g.setColor(look == Look.OWNED ? blend(tier, 0.27f) : hovered ? Hud.mix(BUY_FILL, color, HOVER_FILL) : BUY_FILL);
        g.fill(pill);
        g.setColor(highlight.is(state.node(), Mark.PATH) ? WHITE : hovered ? Hud.mix(color, WHITE, HOVER_BORDER) : color);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(pill);
        String text = look == Look.OWNED ? "✔" : TermNumbers.cash(state.node().price());
        g.setColor(look == Look.SHORT_OF_CASH ? SHORT : look == Look.OWNED ? WHITE : color);
        centred(g, text, PILL_FONT, spot.x(), spot.y() + 3.5f);
        state.key().ifPresent(key -> Hud.paintChip(g, String.valueOf(key), spot.x() - spot.width() / 2 - 6f,
                spot.y() - spot.height() / 2 - 2f, Hud.withAlpha(color, 0.6f), color));
    }

    private void paintNode(Graphics2D g, UpgradeNode node, Spot spot, UpgradeNodeState state, Highlight highlight,
                           Gates gates) {
        Color tier = this.tierColor(node.tier());
        Look look = state.look();
        boolean hovered = highlight.hovered().filter(node::equals).isPresent();
        boolean round = state.ability();
        Shape box = shapeOf(spot, 0f, round);
        if (look == Look.BUYABLE || look == Look.SHORT_OF_CASH) {
            for (int i = 3; i >= 1; i--) {
                g.setColor(Hud.withAlpha(tier, 0.12f));
                g.setStroke(new BasicStroke(1.5f + 2f * i));
                g.draw(box);
            }
        }
        Color fill = hovered ? Hud.mix(fillOf(look, tier), tier, HOVER_FILL) : fillOf(look, tier);
        g.setColor(fill);
        g.fill(box);
        if (look == Look.LOCKED_OUT) {
            paintHatch(g, box, spot);
        }
        Color ink = inkOf(look, tier);
        IconFace face = TermLooks.faceOfLine(this.type, this.tree.artNodeOf(node), this.tree.levelOf(node));
        Graphics2D symbol = (Graphics2D) g.create();
        try {
            float inset = spot.width() * (round ? 0.16f : 0.08f);
            symbol.translate(spot.x() - spot.width() / 2 + inset, spot.y() - spot.height() / 2 + inset);
            float k = (spot.width() - 2 * inset) / TermIconPainter.ROW;
            symbol.scale(k, k);
            TermIconPainter.paintBareSymbol(symbol, face, TermIconPainter.ROW, ink, fill);
        } finally {
            symbol.dispose();
        }
        if (!face.numeral().isEmpty() && spot.width() >= UpgradeTreeLayout.NODE - 1f) {
            g.setFont(NUMERAL_FONT);
            g.setColor(ink);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(face.numeral(), spot.x() + spot.width() / 2 - 3 - metrics.stringWidth(face.numeral()),
                    spot.y() + spot.height() / 2 - 3);
        }
        Color border = borderOf(look, tier);
        if (round && look != Look.LOCKED_OUT) {
            border = Hud.mix(border, WHITE, ABILITY_BORDER);
        }
        g.setColor(hovered ? Hud.mix(border, WHITE, HOVER_BORDER) : border);
        g.setStroke(look == Look.NEEDS_PREVIOUS || look == Look.SWITCHABLE ? dashed(1.5f) : new BasicStroke(2f));
        g.draw(shapeOf(spot, -1f, round));
        if (highlight.is(node, Mark.PATH) || highlight.is(node, Mark.DROP)) {
            Color edge = highlight.is(node, Mark.PATH) ? WHITE : DROP;
            g.setColor(Hud.withAlpha(edge, 0.35f));
            g.setStroke(new BasicStroke(5f));
            g.draw(shapeOf(spot, 2.5f, round));
            g.setColor(edge);
            g.setStroke(new BasicStroke(2f));
            g.draw(shapeOf(spot, 2f, round));
        }
        if (highlight.is(node, Mark.CRACK)) {
            paintCracks(g, spot);
        }
    }

    /** The cash mark of an owned node no wave has used, the key chip and the price. */
    private void paintMarks(Graphics2D g, UpgradeNode node, Spot spot, UpgradeNodeState state, Gates gates) {
        float half = spot.width() / 2;
        if (spot.width() < UpgradeTreeLayout.NODE - 1f) {
            return;
        }
        if (state.unused()) {
            CashMark.paint(g, spot.x() + half - 10 + BADGE / 2f, spot.y() - half - 6 + BADGE / 2f, BADGE);
        }
        Color tier = this.tierColor(node.tier());
        if (state.key().isPresent() && this.showsKey(node)) {
            float inset = state.ability() ? ROUND_CHIP_INSET : CHIP_INSET;
            Hud.paintChip(g, String.valueOf(state.key().getAsInt()), spot.x() - half + inset, spot.y() - half + inset,
                    Hud.withAlpha(tier, 0.6f), tier);
        }
        if (!state.look().showsPrice()) {
            return;
        }
        g.setColor(switch (state.look()) {
            case TIER_LOCKED -> LOCKED;
            case NEEDS_PREVIOUS -> Hud.withAlpha(tier, 0.6f);
            case SHORT_OF_CASH -> SHORT;
            default -> tier;
        });
        centred(g, TermNumbers.cash(node.price()), PRICE_FONT, spot.x(), spot.y() + half + 11);
    }

    /** A cluster's key shows on its first member only, which the key takes. */
    private boolean showsKey(UpgradeNode node) {
        return this.tree.isForkSide(node) || this.tree.clusterOf(node)
                .map(cluster -> cluster.memberIds().getFirst().equals(node.id())).orElse(true);
    }

    private Color tierColor(int tier) {
        return TowerLooks.tierColor(this.look, tier);
    }

    private static void paintHatch(Graphics2D g, Shape box, Spot spot) {
        Graphics2D hatch = (Graphics2D) g.create();
        try {
            hatch.clip(box);
            hatch.setColor(OUT_HATCH);
            hatch.setStroke(new BasicStroke(3f));
            float left = spot.x() - spot.width() / 2;
            float top = spot.y() - spot.height() / 2;
            for (float d = -spot.width(); d < spot.width(); d += 6f) {
                hatch.draw(new Line2D.Float(left + d, top + spot.height(), left + d + spot.width(), top));
            }
        } finally {
            hatch.dispose();
        }
    }

    private static void paintCracks(Graphics2D g, Spot spot) {
        float left = spot.x() - spot.width() / 2;
        float top = spot.y() - spot.height() / 2;
        float size = spot.width();
        g.setColor(DROP);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Float crack = new Path2D.Float();
        crack.moveTo(left + size * 0.15f, top + size * 0.05f);
        crack.lineTo(left + size * 0.45f, top + size * 0.45f);
        crack.lineTo(left + size * 0.35f, top + size * 0.6f);
        crack.lineTo(left + size * 0.7f, top + size * 0.95f);
        crack.moveTo(left + size * 0.45f, top + size * 0.45f);
        crack.lineTo(left + size * 0.85f, top + size * 0.3f);
        g.draw(crack);
    }

    private static void paintXp(Graphics2D g, XpLine xp) {
        g.setFont(XP_FONT);
        FontMetrics metrics = g.getFontMetrics();
        float left = 10f;
        float right = UpgradeTreeLayout.WIDTH - 10f;
        g.setColor(FOREGROUND);
        g.drawString(xp.text(), left, XP_BASELINE);
        float barLeft = Math.min(left + metrics.stringWidth(xp.text()) + 8f, right - 30f);
        g.setColor(XP_TRACK);
        g.fill(new Rectangle2D.Float(barLeft, XP_BASELINE - 5f, right - barLeft, 3f));
        g.setColor(xp.fill());
        g.fill(new Rectangle2D.Float(barLeft, XP_BASELINE - 5f, (right - barLeft) * Math.clamp(xp.share(), 0f, 1f), 3f));
    }

    private static void paintFooter(Graphics2D g, Footer footer, float height) {
        g.setColor(footer.warns() ? WARN : footer.active() ? WHITE : DIM_TEXT);
        centred(g, footer.text(), footer.active() ? FOOTER_BOLD : FOOTER_FONT, UpgradeTreeLayout.WIDTH / 2f,
                height - UpgradeTreeLayout.FOOTER + 13f);
    }

    private static void centred(Graphics2D g, String text, Font font, float x, float baseline) {
        g.setFont(font);
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2f, baseline);
    }

    /** The node's outline grown by {@code grow} on every side. */
    private static Shape shapeOf(Spot spot, float grow, boolean round) {
        float size = spot.width() + 2 * grow;
        float left = spot.x() - size / 2;
        float top = spot.y() - size / 2;
        float corner = Math.min(CORNER, size / 4);
        return round ? new Ellipse2D.Float(left, top, size, size)
                : new RoundRectangle2D.Float(left, top, size, size, 2 * (corner + grow), 2 * (corner + grow));
    }

    private static Color fillOf(Look look, Color tier) {
        return switch (look) {
            case OWNED -> blend(tier, 0.27f);
            case BUYABLE, SHORT_OF_CASH, SWITCHABLE -> BUY_FILL;
            case NEEDS_PREVIOUS -> Hud.BACKGROUND;
            case TIER_LOCKED -> LOCKED_FILL;
            case LOCKED_OUT -> OUT_FILL;
        };
    }

    private static Color borderOf(Look look, Color tier) {
        return switch (look) {
            case OWNED, BUYABLE, SHORT_OF_CASH -> tier;
            case SWITCHABLE -> Hud.withAlpha(tier, 0.7f);
            case NEEDS_PREVIOUS -> Hud.withAlpha(tier, 0.55f);
            case TIER_LOCKED -> LOCKED_BORDER;
            case LOCKED_OUT -> OUT_BORDER;
        };
    }

    private static Color inkOf(Look look, Color tier) {
        return switch (look) {
            case OWNED -> WHITE;
            case BUYABLE, SHORT_OF_CASH -> tier;
            case SWITCHABLE -> Hud.withAlpha(tier, 0.8f);
            case NEEDS_PREVIOUS -> Hud.withAlpha(tier, 0.6f);
            case TIER_LOCKED -> LOCKED;
            case LOCKED_OUT -> OUT_INK;
        };
    }

    /** {@code color} at {@code share} over black, opaque: a translucent fill that hides what is under it. */
    private static Color blend(Color color, float share) {
        return new Color(Math.round(color.getRed() * share), Math.round(color.getGreen() * share),
                Math.round(color.getBlue() * share));
    }

    private static Stroke dashed(float width) {
        return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{3f, 3f}, 0f);
    }
}
