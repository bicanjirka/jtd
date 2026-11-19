package td.ui;

import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.ui.UpgradeNodeState.Look;
import td.ui.UpgradeTreeLayout.Badge;
import td.ui.UpgradeTreeLayout.Band;
import td.ui.UpgradeTreeLayout.End;
import td.ui.UpgradeTreeLayout.Frame;
import td.ui.UpgradeTreeLayout.Link;
import td.ui.UpgradeTreeLayout.Point;
import td.ui.UpgradeTreeLayout.Shared;
import td.ui.UpgradeTreeLayout.Showing;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Paints one tower's upgrade tree in the design units of {@link UpgradeTreeLayout}: each tier's band with its numeral
 * and floor strip (the gate's XP while locked, else the button that buys its singles), the links and badges, the
 * cluster frames, and every node in its look with its price tag, and the footer. A node that changes a number is a
 * circle, one that adds an ability a square. Glyphs take the tower's colour; prices stay neutral. The caller scales
 * the graphics to the panel.
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

    /** The hovered node and what it marks, or the hovered empty slot. */
    record Highlight(Optional<UpgradeNode> hovered, Map<UpgradeNode, Mark> marks, Optional<Shared> slot) {

        Highlight {
            marks = Map.copyOf(marks);
        }

        static Highlight none() {
            return new Highlight(Optional.empty(), Map.of(), Optional.empty());
        }

        boolean is(UpgradeNode node, Mark mark) {
            return this.marks.get(node) == mark;
        }

        /** {@code mark} on any of {@code end}'s nodes. */
        boolean any(End end, Mark mark) {
            return end.nodes().stream().anyMatch(node -> this.is(node, mark));
        }
    }

    /** The footer's words; a warning is red, words about the hovered node bold. */
    record Footer(String text, boolean warns, boolean active) {
    }

    /** What a tier's floor strip shows. */
    sealed interface Floor {

        /** A locked tier: the tower's XP toward the gate's. */
        record Gate(int xp, int needed) implements Floor {
        }

        /** The button that buys the tier's singles, with its key. */
        record Singles(String label, String key, boolean shortOfCash, boolean hovered) implements Floor {
        }

        /** Every single of the tier is owned. */
        record Bought() implements Floor {
        }

        /** Nothing: no single, or none on offer now. */
        record Empty() implements Floor {
        }
    }

    static final Font BUTTON_FONT = new Font(Font.DIALOG, Font.PLAIN, 9);
    private static final float BUTTON_RIGHT = UpgradeTreeLayout.WIDTH - UpgradeTreeLayout.BAND_X - 4f;
    private static final float BUTTON_PAD = 22f;

    private static final Color FOREGROUND = new Color(234, 234, 239);
    private static final Color DIM = new Color(169, 169, 172);
    private static final Color GLOSS = new Color(141, 141, 144);
    private static final Color WHITE = Color.WHITE;
    private static final Color DROP = new Color(255, 90, 90);
    private static final Color SHORT = Hud.SHORT;
    private static final Color BAND_FILL = new Color(24, 24, 26);
    private static final Color BAND_LINE = new Color(42, 42, 44);
    private static final Color LOCKED_STRIP = new Color(21, 21, 23);
    private static final Color TRACK = new Color(42, 42, 44);
    private static final Color NODE_FILL = new Color(22, 22, 24);
    private static final Color TAG_FILL = new Color(16, 16, 18);
    private static final Color LOCKED_BORDER = new Color(58, 58, 61);
    private static final Color LOCKED_INK = new Color(78, 78, 82);
    private static final Color NEED_BORDER = new Color(88, 88, 92);
    private static final Color NEED_INK = new Color(119, 119, 123);
    private static final Color BUY_BORDER = new Color(163, 163, 167);
    private static final Color SHORT_BORDER = new Color(122, 122, 126);
    private static final Color OTHER_BORDER = new Color(74, 74, 77);
    private static final Color OTHER_INK = new Color(102, 102, 106);
    private static final Color HATCH = new Color(35, 35, 37);
    private static final Color HATCH_FILL = new Color(22, 22, 23);
    private static final Color LINK_IDLE = new Color(74, 74, 78);
    private static final Color LINK_LIT = new Color(163, 163, 167);
    private static final Color LINK_OUT = new Color(46, 46, 48);
    private static final Color FRAME_IDLE = new Color(88, 88, 92);
    private static final Color FRAME_TAKEN = new Color(142, 142, 146);
    private static final Color BUTTON_FILL = new Color(41, 41, 43);
    private static final Color BUTTON_BORDER = new Color(103, 103, 105);
    private static final float LOCKED_ALPHA = 0.55f;
    private static final float HOVER_BORDER = 0.35f;
    private static final float OWNED_FILL = 0.28f;
    private static final Font NUMERAL_FONT = new Font(Font.DIALOG, Font.BOLD, 11);
    private static final Font SMALL_NUMERAL_FONT = new Font(Font.DIALOG, Font.BOLD, 9);
    private static final Font GATE_FONT = new Font(Font.DIALOG, Font.PLAIN, 9);
    private static final Font CHIP_FONT = new Font(Font.DIALOG, Font.PLAIN, 7);
    private static final Font FRAME_FONT = new Font(Font.DIALOG, Font.PLAIN, 8);
    private static final Font LINE_NUMERAL_FONT = new Font(Font.DIALOG, Font.BOLD, 7);
    private static final Font FOOTER_FONT = new Font(Font.DIALOG, Font.PLAIN, 9);
    private static final Font FOOTER_BOLD = FOOTER_FONT.deriveFont(Font.BOLD);
    private static final float CASH_BADGE = 10f;

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

    /** Where the button of a strip labelled {@code label} sits in {@code band}, measured with {@code metrics}. */
    static Rectangle2D.Float buttonBox(FontMetrics metrics, String label, Band band) {
        float width = metrics.stringWidth(label) + BUTTON_PAD;
        return new Rectangle2D.Float(BUTTON_RIGHT - width, band.stripTop() + 2f, width, UpgradeTreeLayout.STRIP - 4f);
    }

    /**
     * Paints the whole tree: the tiers up to {@code openTier} are open, {@code floors} says each tier's strip by
     * tier, {@code showing} what the shared slots show.
     */
    void paint(Graphics2D g, Map<UpgradeNode, UpgradeNodeState> states, Highlight highlight, Showing showing,
               int openTier, List<Floor> floors, Footer footer) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        for (Band band : this.layout.bands()) {
            this.faded(g, band.tier() > openTier, () -> this.paintBand(g, band, band.tier() <= openTier,
                    floors.get(band.tier())));
        }
        this.layout.links().stream().filter(link -> showing.shows(link.from()) && showing.shows(link.to()))
                .forEach(link -> this.paintLink(g, link, states, highlight));
        for (Band band : this.layout.bands()) {
            this.faded(g, band.tier() > openTier, () -> this.paintFloor(g, band, floors.get(band.tier())));
        }
        List<Frame> frames = this.layout.frames().stream().filter(frame -> frame.cluster().memberIds().stream()
                .anyMatch(id -> showing.shows(this.tree.node(id).orElseThrow()))).toList();
        for (Frame frame : frames) {
            int tier = this.tree.node(frame.cluster().memberIds().getFirst()).orElseThrow().tier();
            this.faded(g, tier > openTier, () -> this.paintFrame(g, frame, states));
        }
        for (Shared slot : showing.empty()) {
            int tier = slot.bySide().values().iterator().next().tier();
            boolean hovered = highlight.slot().filter(slot::equals).isPresent();
            this.faded(g, tier > openTier, () -> paintEmptySlot(g, slot.spot(), hovered));
        }
        this.layout.spots().forEach((node, spot) -> {
            if (showing.shows(node)) {
                this.paintNode(g, node, spot, states.get(node), highlight);
            }
        });
        for (Frame frame : frames) {
            int tier = this.tree.node(frame.cluster().memberIds().getFirst()).orElseThrow().tier();
            this.faded(g, tier > openTier, () -> paintFrameName(g, frame));
        }
        this.layout.badges().stream().filter(badge -> showing.shows(badge.from()) && showing.shows(badge.to()))
                .forEach(badge -> this.paintBadge(g, badge, states, highlight));
        paintFooter(g, footer, this.layout.height());
    }

    /** {@code paint} at the locked tiers' alpha when {@code locked}. */
    private void faded(Graphics2D g, boolean locked, Runnable paint) {
        if (!locked) {
            paint.run();
            return;
        }
        Composite was = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, LOCKED_ALPHA));
        try {
            paint.run();
        } finally {
            g.setComposite(was);
        }
    }

    /** The band's ground, its numeral strip, the floor strip's line and, while locked, the gate's track. */
    private void paintBand(Graphics2D g, Band band, boolean open, Floor floor) {
        Color tower = this.towerColor();
        float left = UpgradeTreeLayout.BAND_X;
        float right = UpgradeTreeLayout.WIDTH - UpgradeTreeLayout.BAND_X;
        float height = band.bottom() - band.top();
        Color edge = open ? Hud.mix(tower, BAND_FILL, 0.55f) : BAND_LINE;
        Shape ground = new RoundRectangle2D.Float(left, band.top(), right - left, height, 6f, 6f);
        g.setColor(BAND_FILL);
        g.fill(ground);
        g.setColor(open ? Hud.mix(tower, BAND_FILL, 0.82f) : LOCKED_STRIP);
        g.fill(new RoundRectangle2D.Float(left + 0.5f, band.top() + 0.5f, UpgradeTreeLayout.NUMERAL_WIDTH,
                height - 1f, 5f, 5f));
        g.setStroke(new BasicStroke(1f));
        g.setColor(edge);
        g.draw(ground);
        float numeralEdge = left + UpgradeTreeLayout.NUMERAL_WIDTH + 0.5f;
        g.draw(new Line2D.Float(numeralEdge, band.top(), numeralEdge, band.bottom()));
        String numeral = UpgradeTier.numeralOf(band.tier());
        g.setColor(open ? tower : GLOSS);
        centred(g, numeral, numeral.length() > 2 ? SMALL_NUMERAL_FONT : NUMERAL_FONT,
                left + UpgradeTreeLayout.NUMERAL_WIDTH / 2 + 0.5f, band.top() + height / 2 + 4f);
        g.setColor(BAND_LINE);
        g.draw(new Line2D.Float(UpgradeTreeLayout.BODY_X - 2f, band.stripTop() + 0.5f, right, band.stripTop() + 0.5f));
        switch (floor) {
            case Floor.Gate gate -> {
                float trackLeft = UpgradeTreeLayout.BODY_X + 82f;
                float trackWidth = right - 8f - trackLeft;
                float share = gate.needed() == 0 ? 1f : Math.min(1f, gate.xp() / (float) gate.needed());
                g.setColor(TRACK);
                g.fill(new RoundRectangle2D.Float(trackLeft, band.stripTop() + 6f, trackWidth, 3f, 3f, 3f));
                g.setColor(DIM);
                g.fill(new RoundRectangle2D.Float(trackLeft, band.stripTop() + 6f, trackWidth * share, 3f, 3f, 3f));
            }
            case Floor.Singles singles -> {
            }
            case Floor.Bought bought -> {
            }
            case Floor.Empty empty -> {
            }
        }
    }

    /** The strip's words and button, over the links that pass under them. */
    private void paintFloor(Graphics2D g, Band band, Floor floor) {
        float top = band.stripTop();
        switch (floor) {
            case Floor.Gate gate -> {
                float x = UpgradeTreeLayout.BODY_X;
                g.setColor(BAND_FILL);
                g.fill(new Rectangle2D.Float(x + 1f, top + 2f, 76f, UpgradeTreeLayout.STRIP - 3f));
                paintLock(g, x + 4f, top + 3.5f);
                g.setColor(DIM);
                g.setFont(GATE_FONT);
                g.drawString(UpgradeSheetText.gateLabel(gate.xp(), gate.needed()), x + 16f, top + 10.5f);
            }
            case Floor.Singles singles -> {
                g.setFont(BUTTON_FONT);
                Rectangle2D.Float box = buttonBox(g.getFontMetrics(), singles.label(), band);
                g.setColor(BAND_FILL);
                g.fill(new Rectangle2D.Float(box.x - 2f, top + 1f, box.width + 4f, UpgradeTreeLayout.STRIP - 2f));
                Shape button = new RoundRectangle2D.Float(box.x, box.y, box.width, box.height, 4f, 4f);
                g.setColor(singles.hovered() ? Hud.mix(BUTTON_FILL, WHITE, 0.08f) : BUTTON_FILL);
                g.fill(button);
                g.setColor(singles.hovered() ? Hud.mix(BUTTON_BORDER, WHITE, 0.5f) : BUTTON_BORDER);
                g.setStroke(new BasicStroke(1f));
                g.draw(button);
                g.setColor(GLOSS);
                g.setStroke(new BasicStroke(0.8f));
                g.draw(new RoundRectangle2D.Float(box.x + 3f, box.y + 1.5f, 10f, 8f, 3f, 3f));
                g.setColor(DIM);
                centred(g, singles.key(), CHIP_FONT, box.x + 8f, box.y + 7.9f);
                g.setColor(singles.shortOfCash() ? SHORT : FOREGROUND);
                g.setFont(BUTTON_FONT);
                g.drawString(singles.label(), box.x + 16f, box.y + 8.7f);
            }
            case Floor.Bought bought -> {
                g.setFont(BUTTON_FONT);
                g.setColor(GLOSS);
                String label = UpgradeSheetText.SINGLES_BOUGHT;
                g.drawString(label, BUTTON_RIGHT - 2f - g.getFontMetrics().stringWidth(label), top + 10.4f);
            }
            case Floor.Empty empty -> {
            }
        }
    }

    private static void paintLock(Graphics2D g, float x, float y) {
        g.setColor(DIM);
        g.fill(new Rectangle2D.Float(x + 0.5f, y + 3.5f, 7f, 5f));
        g.setStroke(new BasicStroke(1f));
        Path2D.Float shackle = new Path2D.Float();
        shackle.moveTo(x + 1.8f, y + 3.6f);
        shackle.lineTo(x + 1.8f, y + 2.2f);
        shackle.curveTo(x + 1.8f, y - 0.7f, x + 6.2f, y - 0.7f, x + 6.2f, y + 2.2f);
        shackle.lineTo(x + 6.2f, y + 3.6f);
        g.draw(shackle);
    }

    private void paintFrame(Graphics2D g, Frame frame, Map<UpgradeNode, UpgradeNodeState> states) {
        boolean taken = frame.cluster().memberIds().stream().map(id -> this.tree.node(id).orElseThrow())
                .anyMatch(member -> states.get(member).look() == Look.OWNED);
        Shape box = new RoundRectangle2D.Float(frame.box().left(), frame.box().top(), frame.box().right() - frame.box().left(),
                frame.box().bottom() - frame.box().top(), 12f, 12f);
        g.setColor(BAND_FILL);
        g.fill(box);
        g.setColor(taken ? FRAME_TAKEN : FRAME_IDLE);
        g.setStroke(taken ? new BasicStroke(1f) : dashed(1f, 3f, 2f));
        g.draw(box);
    }

    /** The frame's name over its top edge, painted after the nodes so a member's corner mark never hides it. */
    private static void paintFrameName(Graphics2D g, Frame frame) {
        g.setFont(FRAME_FONT);
        String name = frame.cluster().name();
        float width = g.getFontMetrics().stringWidth(name);
        g.setColor(BAND_FILL);
        g.fill(new Rectangle2D.Float(frame.box().left() + 4f, frame.box().top() - 5f, width + 4f, 9f));
        g.setColor(GLOSS);
        g.drawString(name, frame.box().left() + 6f, frame.box().top() + 3f);
    }

    private void paintLink(Graphics2D g, Link link, Map<UpgradeNode, UpgradeNodeState> states, Highlight highlight) {
        Path2D.Float path = new Path2D.Float();
        List<Point> points = link.path();
        path.moveTo(points.getFirst().x(), points.getFirst().y());
        points.subList(1, points.size()).forEach(point -> path.lineTo(point.x(), point.y()));
        g.setColor(BAND_FILL);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.draw(path);
        Color color = LINK_IDLE;
        Stroke stroke = new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
        if (highlight.any(link.to(), Mark.DROP)) {
            color = DROP;
            stroke = dashed(1.6f, 3f, 3f);
        } else if (highlight.any(link.to(), Mark.PATH)) {
            color = WHITE;
            stroke = new BasicStroke(1.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
        } else if (owns(link.from(), states)) {
            color = LINK_LIT;
        } else if (link.to().nodes().stream().allMatch(node -> isOut(states.get(node).look()))) {
            color = LINK_OUT;
        }
        g.setColor(color);
        g.setStroke(stroke);
        g.draw(path);
    }

    /** The need's glyph in a small circle on the needing node's corner, lit as a link would be. */
    private void paintBadge(Graphics2D g, Badge badge, Map<UpgradeNode, UpgradeNodeState> states,
                            Highlight highlight) {
        boolean met = owns(badge.from(), states);
        Color ring = met ? this.towerColor() : NEED_INK;
        if (highlight.any(badge.to(), Mark.DROP) || highlight.any(badge.from(), Mark.DROP)) {
            ring = DROP;
        } else if (highlight.any(badge.to(), Mark.PATH) || highlight.any(badge.from(), Mark.PATH)) {
            ring = WHITE;
        }
        float r = UpgradeTreeLayout.BADGE;
        Shape circle = new Ellipse2D.Float(badge.x() - r, badge.y() - r, 2 * r, 2 * r);
        g.setColor(TAG_FILL);
        g.fill(circle);
        g.setColor(ring);
        g.setStroke(new BasicStroke(ring == WHITE ? 1.4f : 1f));
        g.draw(circle);
        UpgradeNode source = badge.from().nodes().stream().filter(node -> states.get(node).look() == Look.OWNED)
                .findFirst().orElse(badge.from().nodes().getFirst());
        this.paintGlyph(g, source, badge.x() - 4f, badge.y() - 4f, 8f, met ? this.towerColor() : NEED_INK, TAG_FILL);
    }

    private void paintNode(Graphics2D g, UpgradeNode node, Spot spot, UpgradeNodeState state, Highlight highlight) {
        Look look = state.look();
        Color tower = this.towerColor();
        boolean hovered = highlight.hovered().filter(node::equals).isPresent();
        boolean round = !state.ability();
        Shape box = shapeOf(spot, 0f, round);
        Color fill = look == Look.OWNED ? Hud.mix(NODE_FILL, tower, OWNED_FILL) : NODE_FILL;
        g.setColor(fill);
        g.fill(box);
        if (look == Look.SWITCHABLE) {
            paintHatch(g, box, spot);
        }
        float inset = round ? 6.5f : 4.5f;
        float size = spot.size() - 2 * inset;
        Color ink = inkOf(look, tower);
        this.paintGlyph(g, node, spot.x() - size / 2, spot.y() - size / 2, size, ink,
                look == Look.SWITCHABLE ? HATCH_FILL : fill);
        IconFace face = TermLooks.faceOfLine(this.type, this.tree.artNodeOf(node), this.tree.levelOf(node));
        if (!face.numeral().isEmpty()) {
            g.setColor(ink);
            g.setFont(LINE_NUMERAL_FONT);
            float right = spot.x() + spot.size() / 2 - (round ? 6f : 3f);
            g.drawString(face.numeral(), right - g.getFontMetrics().stringWidth(face.numeral()),
                    spot.y() - spot.size() / 2 + 8f);
        }
        Color border = borderOf(look, tower);
        g.setColor(hovered ? Hud.mix(border, WHITE, HOVER_BORDER) : border);
        g.setStroke(new BasicStroke(look == Look.OWNED ? BuyTileLook.BORDER_TAKEN : BuyTileLook.BORDER));
        g.draw(shapeOf(spot, -0.5f, round));
        if (highlight.is(node, Mark.PATH) || highlight.is(node, Mark.DROP)) {
            Color edge = highlight.is(node, Mark.PATH) ? WHITE : DROP;
            g.setColor(Hud.withAlpha(edge, 0.35f));
            g.setStroke(new BasicStroke(4f));
            g.draw(shapeOf(spot, 2.5f, round));
            g.setColor(edge);
            g.setStroke(new BasicStroke(1.6f));
            g.draw(shapeOf(spot, 2f, round));
        }
        if (highlight.is(node, Mark.CRACK)) {
            paintCracks(g, spot);
        }
        if (state.unused()) {
            CashMark.paint(g, spot.x() + spot.size() / 2 - 1f, spot.y() - spot.size() / 2 + 1f, CASH_BADGE);
        }
        this.paintTag(node, g, spot, look);
    }

    /** A shared slot before its cluster's pick: a dashed outline holding a question mark. */
    private static void paintEmptySlot(Graphics2D g, Spot spot, boolean hovered) {
        Shape box = shapeOf(spot, -0.5f, false);
        g.setColor(NODE_FILL);
        g.fill(box);
        g.setColor(hovered ? Hud.mix(NEED_BORDER, WHITE, HOVER_BORDER) : NEED_BORDER);
        g.setStroke(dashed(1.2f, 3f, 2.5f));
        g.draw(box);
        g.setColor(hovered ? NEED_INK.brighter() : NEED_INK);
        centred(g, "?", NUMERAL_FONT, spot.x(), spot.y() + 4f);
    }

    /** The price on the node's bottom edge, or a tick once owned. */
    private void paintTag(UpgradeNode node, Graphics2D g, Spot spot, Look look) {
        BuyTileLook.paintTag(g, spot.x(), spot.y() + spot.size() / 2, node.price(), switch (look) {
            case OWNED -> BuyTileLook.Tone.OWNED;
            case BUYABLE -> BuyTileLook.Tone.BUYABLE;
            case SHORT_OF_CASH -> BuyTileLook.Tone.SHORT_OF_CASH;
            case NEEDS_PREVIOUS, SWITCHABLE, TIER_LOCKED, LOCKED_OUT -> BuyTileLook.Tone.UNAVAILABLE;
        }, this.towerColor());
    }

    private void paintGlyph(Graphics2D g, UpgradeNode node, float x, float y, float size, Color ink, Color fill) {
        IconFace face = TermLooks.faceOfLine(this.type, this.tree.artNodeOf(node), this.tree.levelOf(node));
        Graphics2D symbol = (Graphics2D) g.create();
        try {
            symbol.translate(x, y);
            float k = size / TermIconPainter.ROW;
            symbol.scale(k, k);
            TermIconPainter.paintBareSymbol(symbol, face, TermIconPainter.ROW, ink, fill);
        } finally {
            symbol.dispose();
        }
    }

    private Color towerColor() {
        return TowerLooks.color(this.look);
    }

    private static boolean owns(End end, Map<UpgradeNode, UpgradeNodeState> states) {
        return end.nodes().stream().anyMatch(node -> states.get(node).look() == Look.OWNED);
    }

    private static boolean isOut(Look look) {
        return look == Look.LOCKED_OUT || look == Look.SWITCHABLE;
    }

    private static void paintHatch(Graphics2D g, Shape box, Spot spot) {
        Graphics2D hatch = (Graphics2D) g.create();
        try {
            hatch.clip(box);
            hatch.setColor(HATCH_FILL);
            hatch.fill(box);
            hatch.setColor(HATCH);
            hatch.setStroke(new BasicStroke(2f));
            float left = spot.x() - spot.size() / 2;
            float top = spot.y() - spot.size() / 2;
            for (float d = -spot.size(); d < spot.size(); d += 4f) {
                hatch.draw(new Line2D.Float(left + d, top + spot.size(), left + d + spot.size(), top));
            }
        } finally {
            hatch.dispose();
        }
    }

    private static void paintCracks(Graphics2D g, Spot spot) {
        float left = spot.x() - spot.size() / 2;
        float top = spot.y() - spot.size() / 2;
        float size = spot.size();
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

    private static void paintFooter(Graphics2D g, Footer footer, float height) {
        g.setColor(footer.warns() ? SHORT : footer.active() ? WHITE : DIM);
        centred(g, footer.text(), footer.active() ? FOOTER_BOLD : FOOTER_FONT, UpgradeTreeLayout.WIDTH / 2f,
                height - 5f);
    }

    private static void centred(Graphics2D g, String text, Font font, float x, float baseline) {
        g.setFont(font);
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2f, baseline);
    }

    /** The node's outline grown by {@code grow} on every side: a circle for a stat, a square for an ability. */
    private static Shape shapeOf(Spot spot, float grow, boolean round) {
        float size = spot.size() + 2 * grow;
        float left = spot.x() - size / 2;
        float top = spot.y() - size / 2;
        float corner = BuyTileLook.CORNER + grow;
        return round ? new Ellipse2D.Float(left, top, size, size)
                : new RoundRectangle2D.Float(left, top, size, size, corner, corner);
    }

    private static Color borderOf(Look look, Color tower) {
        return switch (look) {
            case OWNED -> tower;
            case BUYABLE -> BUY_BORDER;
            case SHORT_OF_CASH -> SHORT_BORDER;
            case NEEDS_PREVIOUS -> NEED_BORDER;
            case SWITCHABLE -> OTHER_BORDER;
            case TIER_LOCKED, LOCKED_OUT -> LOCKED_BORDER;
        };
    }

    private static Color inkOf(Look look, Color tower) {
        return switch (look) {
            case OWNED, BUYABLE -> tower;
            case SHORT_OF_CASH -> Hud.mix(tower, NODE_FILL, 0.35f);
            case NEEDS_PREVIOUS -> NEED_INK;
            case SWITCHABLE -> OTHER_INK;
            case TIER_LOCKED, LOCKED_OUT -> LOCKED_INK;
        };
    }

    private static Stroke dashed(float width, float on, float off) {
        return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{on, off}, 0f);
    }
}
