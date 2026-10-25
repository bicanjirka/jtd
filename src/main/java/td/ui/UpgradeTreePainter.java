package td.ui;

import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeTree;
import td.ui.UpgradeNodeState.Look;
import td.ui.UpgradeTreeLayout.Spot;
import td.ui.render.IconFace;
import td.util.ThreadConfined;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;
import java.util.Optional;

/**
 * Paints one tower's upgrade tree in the design units of {@link UpgradeTreeLayout}: the rook, the halo, the links
 * and forks, every node in its look with its ring, badge, key and price, the XP line and the footer. The caller
 * scales the graphics to the panel.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class UpgradeTreePainter {

    /** What the hovered node marks on other nodes. */
    enum Mark {
        /** A step of the path a click buys. */
        PATH,
        /** A node a downgrade takes away. */
        DROP,
        /** A node the hovered path would lock out. */
        CRACK,
        /** A node the hovered Core step opens. */
        LIT
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
    record XpLine(String text, float share) {
    }

    /** The footer's words; a warning is red, words about the hovered node bold. */
    record Footer(String text, boolean warns, boolean active) {
    }

    private static final Color FOREGROUND = Hud.FOREGROUND;
    private static final Color DIM_TEXT = Hud.TEXT_DIM;
    private static final Color WHITE = Color.WHITE;
    private static final Color DROP = new Color(255, 90, 90);
    private static final Color SHORT = Hud.SHORT;
    private static final Color WARN = new Color(255, 138, 138);
    // The neutrals are the HUD's green-tinted greys, darkest for what is locked out.
    private static final Color LINK_IDLE = new Color(50, 66, 50);
    private static final Color LINK_OUT = new Color(30, 40, 30);
    private static final Color ROOT_IDLE = Hud.BORDER_IDLE;
    private static final Color DIAMOND_IDLE = Hud.CHIP_BORDER;
    private static final Color DIAMOND_OUT = new Color(38, 50, 38);
    private static final Color SEALED_BORDER = new Color(64, 84, 64);
    private static final Color SEALED_FILL = new Color(14, 20, 14);
    private static final Color SEALED_INK = new Color(112, 138, 112);
    private static final Color OUT_BORDER = new Color(38, 50, 38);
    private static final Color OUT_FILL = new Color(10, 14, 10);
    private static final Color OUT_HATCH = new Color(20, 28, 20);
    private static final Color OUT_INK = new Color(64, 82, 64);
    private static final Color BUY_FILL = new Color(12, 22, 12);
    private static final Color WAIT_FILL = new Color(5, 8, 5);
    private static final Color RING_TRACK = new Color(32, 44, 32);
    private static final Color BADGE_FILL = new Color(7, 21, 32);
    private static final Color XP_TRACK = new Color(20, 32, 20);
    private static final Color XP_FILL = new Color(155, 224, 155);
    private static final float HOVER_FILL = 0.12f;
    private static final float HOVER_BORDER = 0.35f;
    private static final Font XP_FONT = new Font(Font.DIALOG, Font.PLAIN, 10);
    private static final Font PRICE_FONT = Hud.PRICE_FONT;
    private static final Font NUMERAL_FONT = new Font(Font.DIALOG, Font.BOLD, 7);
    private static final Font FOOTER_FONT = new Font(Font.DIALOG, Font.PLAIN, 9);
    private static final Font FOOTER_BOLD = FOOTER_FONT.deriveFont(Font.BOLD);
    /** Under the border's title. */
    private static final float XP_BASELINE = 30f;
    private static final float CORNER = 7f;
    private static final float RING_GAP = 4.5f;
    private static final float BADGE = 15f;
    /** How far in from the box's corner the key chip sits; further on a round node, whose edge curves away. */
    private static final float CHIP_INSET = 3f;
    private static final float ROUND_CHIP_INSET = 5f;
    private static final float DIAMOND = 12f;
    private static final float FORK_DIAMOND = 8f;
    private static final Shape HEAD_BAND = path(new float[][]{{54, 100}, {82, 100}, {82, 112}, {106, 112}, {106, 100},
            {134, 100}, {134, 112}, {158, 112}, {158, 100}, {186, 100}, {186, 112}, {210, 112}, {210, 100}, {238, 100},
            {238, 112}, {262, 112}, {262, 100}, {290, 100}, {290, 126}, {274, 136}, {274, 268}, {70, 268}, {70, 136},
            {54, 126}});
    private static final Shape NECK = path(new float[][]{{70, 268}, {274, 268}, {274, 328}, {286, 340}, {58, 340},
            {70, 328}});
    private static final Shape FOUNDATION = path(new float[][]{{58, 340}, {286, 340}, {286, 386}, {296, 386},
            {296, 400}, {46, 400}, {46, 386}, {58, 386}});

    private final UpgradeTreeLayout layout;
    private final UpgradeTree tree;
    private final TowerFactory.Type type;

    UpgradeTreePainter(UpgradeTreeLayout layout, UpgradeTree tree, TowerFactory.Type type) {
        this.layout = layout;
        this.tree = tree;
        this.type = type;
    }

    void paint(Graphics2D g, Map<UpgradeNode, UpgradeNodeState> states, Highlight highlight, XpLine xp, Footer footer) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        this.paintRook(g, states);
        this.paintHalo(g, states);
        this.paintCrownLink(g, states, highlight);
        this.paintRootChoice(g, states, highlight);
        this.layout.links().forEach(link -> this.paintLink(g, link.from(), link.to(), states, highlight));
        this.layout.forks().forEach(fork -> this.paintFork(g, fork, states, highlight));
        this.layout.spots().forEach((node, spot) -> this.paintNode(g, node, spot, states.get(node), highlight));
        this.layout.spots().forEach((node, spot) -> this.paintMarks(g, node, spot, states.get(node)));
        paintXp(g, xp);
        paintFooter(g, footer);
    }

    private void paintRook(Graphics2D g, Map<UpgradeNode, UpgradeNodeState> states) {
        g.setColor(this.tint(states, UpgradeSlot.HEAD));
        g.fill(HEAD_BAND);
        g.setColor(Hud.withAlpha(FOREGROUND, 0.06f));
        g.fill(NECK);
        g.setColor(this.tint(states, UpgradeSlot.BASE));
        g.fill(FOUNDATION);
    }

    /** A band takes its slot's colour once the tower owns a node there. */
    private Color tint(Map<UpgradeNode, UpgradeNodeState> states, UpgradeSlot slot) {
        boolean owns = states.values().stream().anyMatch(s -> s.node().slot() == slot && s.look() == Look.OWNED);
        return owns ? Hud.withAlpha(slotColor(slot), 0.09f) : Hud.withAlpha(FOREGROUND, 0.06f);
    }

    /** A ghost until a special can be reached, lit once one is owned. */
    private void paintHalo(Graphics2D g, Map<UpgradeNode, UpgradeNodeState> states) {
        boolean owned = this.any(states, UpgradeSlot.SPECIAL, Look.OWNED);
        boolean open = states.values().stream().anyMatch(s -> s.node().slot() == UpgradeSlot.SPECIAL
                && s.look() != Look.SEALED && s.look() != Look.LOCKED_OUT);
        Color special = slotColor(UpgradeSlot.SPECIAL);
        Shape halo = new Ellipse2D.Float(UpgradeTreeLayout.HALO_X - UpgradeTreeLayout.HALO_RX,
                UpgradeTreeLayout.HALO_Y - UpgradeTreeLayout.HALO_RY, 2 * UpgradeTreeLayout.HALO_RX,
                2 * UpgradeTreeLayout.HALO_RY);
        float glow = owned ? 0.18f : open ? 0.08f : 0f;
        if (glow > 0f) {
            g.setColor(Hud.withAlpha(special, glow));
            g.setStroke(new BasicStroke(7f));
            g.draw(halo);
        }
        g.setColor(Hud.withAlpha(special, owned ? 0.6f : open ? 0.4f : 0.12f));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(halo);
    }

    /** Transcendent's tie from the crown of the wall into the halo. */
    private void paintCrownLink(Graphics2D g, Map<UpgradeNode, UpgradeNodeState> states, Highlight highlight) {
        Optional<UpgradeNode> crown = this.tree.nodesIn(UpgradeSlot.CORE).stream()
                .max((a, b) -> Integer.compare(a.tier().level(), b.tier().level()))
                .filter(node -> node.tier().level() == 3);
        if (crown.isEmpty()) {
            return;
        }
        Spot spot = this.layout.spots().get(crown.get());
        boolean owned = states.get(crown.get()).look() == Look.OWNED;
        Color core = slotColor(UpgradeSlot.CORE);
        g.setColor(highlight.is(crown.get(), Mark.PATH) ? WHITE : owned ? core : Hud.withAlpha(core, 0.4f));
        g.setStroke(owned ? new BasicStroke(2f) : dashed(2f));
        float top = spot.y() - spot.size() / 2;
        g.draw(new QuadCurve2D.Float(spot.x(), top, spot.x() + 2, UpgradeTreeLayout.HALO_Y + 2,
                UpgradeTreeLayout.HALO_X - UpgradeTreeLayout.HALO_RX, UpgradeTreeLayout.HALO_Y + 1));
    }

    private void paintRootChoice(Graphics2D g, Map<UpgradeNode, UpgradeNodeState> states, Highlight highlight) {
        if (this.layout.rootChoice().isEmpty()) {
            return;
        }
        UpgradeTreeLayout.RootChoice choice = this.layout.rootChoice().get();
        boolean picked = choice.members().stream().anyMatch(m -> states.get(m).look() == Look.OWNED);
        Color head = slotColor(UpgradeSlot.HEAD);
        g.setStroke(new BasicStroke(2f));
        for (UpgradeNode member : choice.members()) {
            Spot spot = this.layout.spots().get(member);
            boolean mine = states.get(member).look() == Look.OWNED;
            g.setColor(highlight.is(member, Mark.PATH) ? WHITE : mine ? head : picked ? LINK_OUT : ROOT_IDLE);
            Path2D.Float route = new Path2D.Float();
            route.moveTo(choice.x(), choice.y());
            route.lineTo(choice.x(), spot.y());
            route.lineTo(spot.x() - spot.size() / 2, spot.y());
            g.draw(route);
        }
        Shape diamond = AffineTransform.getRotateInstance(Math.PI / 4, choice.x(), choice.y())
                .createTransformedShape(new Rectangle2D.Float(choice.x() - DIAMOND / 2, choice.y() - DIAMOND / 2, DIAMOND,
                        DIAMOND));
        g.setColor(Color.BLACK);
        g.fill(diamond);
        g.setColor(picked ? head : DIAMOND_IDLE);
        g.draw(diamond);
    }

    private void paintLink(Graphics2D g, UpgradeNode from, UpgradeNode to, Map<UpgradeNode, UpgradeNodeState> states,
                           Highlight highlight) {
        Spot a = this.layout.spots().get(from);
        Spot b = this.layout.spots().get(to);
        this.strokeLink(g, a.x(), a.y(), a.size() / 2 + 1, b.x(), b.y(), b.size() / 2 + 1,
                linkLook(states.get(from).look(), states.get(to).look(), highlight, from, to, slotColor(to.slot())));
    }

    private void paintFork(Graphics2D g, UpgradeTreeLayout.Fork fork, Map<UpgradeNode, UpgradeNodeState> states,
                           Highlight highlight) {
        Spot from = this.layout.spots().get(fork.from());
        Color head = slotColor(UpgradeSlot.HEAD);
        Look fromLook = states.get(fork.from()).look();
        boolean anyOwned = fork.members().stream().anyMatch(m -> states.get(m).look() == Look.OWNED);
        boolean allOut = fork.members().stream().allMatch(m -> states.get(m).look() == Look.LOCKED_OUT);
        Optional<UpgradeNode> marked = fork.members().stream()
                .filter(m -> highlight.is(m, Mark.PATH) || highlight.is(m, Mark.DROP)).findFirst();
        Look junction = anyOwned ? Look.OWNED : allOut || fromLook == Look.LOCKED_OUT ? Look.LOCKED_OUT : Look.SEALED;
        UpgradeNode junctionMark = marked.orElse(fork.from());
        this.strokeLink(g, from.x(), from.y(), from.size() / 2 + 1, fork.x(), fork.y(), FORK_DIAMOND / 2,
                linkLook(fromLook, junction, highlight, fork.from(), junctionMark, head));
        for (UpgradeNode member : fork.members()) {
            Spot spot = this.layout.spots().get(member);
            this.strokeLink(g, fork.x(), fork.y(), FORK_DIAMOND / 2, spot.x(), spot.y(), spot.size() / 2 + 1,
                    linkLook(junction, states.get(member).look(), highlight, junctionMark, member, head));
        }
        Shape diamond = AffineTransform.getRotateInstance(Math.PI / 4, fork.x(), fork.y())
                .createTransformedShape(new Rectangle2D.Float(fork.x() - FORK_DIAMOND / 2, fork.y() - FORK_DIAMOND / 2,
                        FORK_DIAMOND, FORK_DIAMOND));
        g.setColor(Color.BLACK);
        g.fill(diamond);
        g.setColor(junction == Look.LOCKED_OUT ? DIAMOND_OUT : DIAMOND_IDLE);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(diamond);
    }

    /** A link's colour and stroke: removed, on the path, owned, broken, opening, or idle. */
    private record LinkLook(Color color, Stroke stroke) {
    }

    private static LinkLook linkLook(Look a, Look b, Highlight highlight, UpgradeNode from, UpgradeNode to, Color slot) {
        boolean fromHeld = a == Look.OWNED || highlight.is(from, Mark.DROP);
        if (highlight.is(to, Mark.DROP) && fromHeld) {
            return new LinkLook(DROP, dashed(2f));
        }
        if (highlight.is(to, Mark.PATH) && (a == Look.OWNED || highlight.is(from, Mark.PATH))) {
            return new LinkLook(WHITE, new BasicStroke(2.5f));
        }
        if (a == Look.OWNED && b == Look.OWNED) {
            return new LinkLook(slot, new BasicStroke(2f));
        }
        if (a == Look.LOCKED_OUT || b == Look.LOCKED_OUT) {
            return new LinkLook(LINK_OUT, new BasicStroke(2f));
        }
        if (a == Look.OWNED && b.showsPrice()) {
            return new LinkLook(Hud.withAlpha(slot, 0.73f), dashed(2f));
        }
        return new LinkLook(LINK_IDLE, new BasicStroke(2f));
    }

    private void strokeLink(Graphics2D g, float x1, float y1, float r1, float x2, float y2, float r2, LinkLook look) {
        double length = Math.hypot(x2 - x1, y2 - y1);
        if (length <= r1 + r2) {
            return;
        }
        float ux = (float) ((x2 - x1) / length);
        float uy = (float) ((y2 - y1) / length);
        g.setColor(look.color());
        g.setStroke(look.stroke());
        g.draw(new Line2D.Float(x1 + ux * r1, y1 + uy * r1, x2 - ux * r2, y2 - uy * r2));
    }

    private void paintNode(Graphics2D g, UpgradeNode node, Spot spot, UpgradeNodeState state, Highlight highlight) {
        Color slot = slotColor(node.slot());
        Look look = state.look();
        boolean hovered = highlight.hovered().filter(node::equals).isPresent();
        if (look == Look.WAITING) {
            paintRing(g, spot, slot, state.ring());
        }
        Shape box = shapeOf(spot, 0f);
        if (look == Look.BUYABLE || look == Look.SHORT_OF_CASH) {
            for (int i = 3; i >= 1; i--) {
                g.setColor(Hud.withAlpha(slot, 0.12f));
                g.setStroke(new BasicStroke(2f + 2.5f * i));
                g.draw(box);
            }
        }
        Color fill = hovered ? Hud.mix(fillOf(look, slot), slot, HOVER_FILL) : fillOf(look, slot);
        g.setColor(fill);
        g.fill(box);
        if (look == Look.LOCKED_OUT) {
            paintHatch(g, box, spot);
        }
        Color ink = inkOf(look, slot);
        IconFace face = TermLooks.faceOfLine(this.type, this.tree.artNodeOf(node), node.tier().level());
        Graphics2D symbol = (Graphics2D) g.create();
        try {
            float inset = spot.size() * 0.08f;
            symbol.translate(spot.x() - spot.size() / 2 + inset, spot.y() - spot.size() / 2 + inset);
            float k = (spot.size() - 2 * inset) / TermIconPainter.ROW;
            symbol.scale(k, k);
            TermIconPainter.paintBareSymbol(symbol, face, TermIconPainter.ROW, ink, fill);
        } finally {
            symbol.dispose();
        }
        if (!face.numeral().isEmpty()) {
            g.setFont(NUMERAL_FONT);
            g.setColor(ink);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(face.numeral(), spot.x() + spot.size() / 2 - 3 - metrics.stringWidth(face.numeral()),
                    spot.y() + spot.size() / 2 - 3);
        }
        g.setColor(hovered ? Hud.mix(borderOf(look, slot), WHITE, HOVER_BORDER) : borderOf(look, slot));
        g.setStroke(look == Look.NEEDS_PREVIOUS ? dashed(1.5f) : new BasicStroke(2f));
        g.draw(shapeOf(spot, -1f));
        if (highlight.is(node, Mark.PATH) || highlight.is(node, Mark.DROP)) {
            Color edge = highlight.is(node, Mark.PATH) ? WHITE : DROP;
            g.setColor(Hud.withAlpha(edge, 0.35f));
            g.setStroke(new BasicStroke(5f));
            g.draw(shapeOf(spot, 2.5f));
            g.setColor(edge);
            g.setStroke(new BasicStroke(2f));
            g.draw(shapeOf(spot, 2f));
        }
        if (highlight.is(node, Mark.LIT)) {
            g.setColor(slotColor(UpgradeSlot.CORE));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(shapeOf(spot, 3f));
        }
        if (highlight.is(node, Mark.CRACK)) {
            paintCracks(g, spot);
        }
    }

    /** The badge of a sealed node, the key digit and price of a line's next node. */
    private void paintMarks(Graphics2D g, UpgradeNode node, Spot spot, UpgradeNodeState state) {
        float half = spot.size() / 2;
        Color slot = slotColor(node.slot());
        if (state.sealedBy().isPresent()) {
            Color core = slotColor(UpgradeSlot.CORE);
            Shape badge = new Ellipse2D.Float(spot.x() + half - 10, spot.y() - half - 6, BADGE, BADGE);
            g.setColor(BADGE_FILL);
            g.fill(badge);
            g.setColor(core);
            g.setStroke(new BasicStroke(1f));
            g.draw(badge);
            Graphics2D symbol = (Graphics2D) g.create();
            try {
                symbol.translate(spot.x() + half - 8.5, spot.y() - half - 4.5);
                float k = 12f / TermIconPainter.ROW;
                symbol.scale(k, k);
                TermIconPainter.paintBareSymbol(symbol, TermLooks.faceOfLine(this.type, state.sealedBy().get(), 0),
                        TermIconPainter.ROW, core, BADGE_FILL);
            } finally {
                symbol.dispose();
            }
        }
        if (!state.look().showsPrice()) {
            return;
        }
        if (node.slot() != UpgradeSlot.SPECIAL) {
            float inset = spot.round() ? ROUND_CHIP_INSET : CHIP_INSET;
            Hud.paintChip(g, String.valueOf(this.tree.keyOf(node)), spot.x() - half + inset, spot.y() - half + inset,
                    Hud.withAlpha(slot, 0.6f), slot);
        }
        Color price = switch (state.look()) {
            case WAITING -> Hud.withAlpha(slot, 0.53f);
            case SHORT_OF_CASH -> SHORT;
            default -> slot;
        };
        g.setColor(price);
        centred(g, "$" + node.price(), PRICE_FONT, spot.x(), spot.y() + half + 11);
    }

    private static void paintRing(Graphics2D g, Spot spot, Color slot, float share) {
        float radius = spot.size() / 2 + RING_GAP;
        Ellipse2D.Float track = new Ellipse2D.Float(spot.x() - radius, spot.y() - radius, 2 * radius, 2 * radius);
        g.setStroke(new BasicStroke(3f));
        g.setColor(RING_TRACK);
        g.draw(track);
        g.setColor(slot);
        g.draw(new Arc2D.Float(track.getBounds2D(), 90f, -360f * Math.clamp(share, 0f, 1f), Arc2D.OPEN));
    }

    private static void paintHatch(Graphics2D g, Shape box, Spot spot) {
        Graphics2D hatch = (Graphics2D) g.create();
        try {
            hatch.clip(box);
            hatch.setColor(OUT_HATCH);
            hatch.setStroke(new BasicStroke(3f));
            float left = spot.x() - spot.size() / 2;
            float top = spot.y() - spot.size() / 2;
            for (float d = -spot.size(); d < spot.size(); d += 6f) {
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
        g.setColor(XP_FILL);
        g.fill(new Rectangle2D.Float(barLeft, XP_BASELINE - 5f, (right - barLeft) * Math.clamp(xp.share(), 0f, 1f), 3f));
    }

    private static void paintFooter(Graphics2D g, Footer footer) {
        g.setColor(footer.warns() ? WARN : footer.active() ? WHITE : DIM_TEXT);
        centred(g, footer.text(), footer.active() ? FOOTER_BOLD : FOOTER_FONT, UpgradeTreeLayout.WIDTH / 2f,
                UpgradeTreeLayout.FOOTER_Y + 9);
    }

    private static void centred(Graphics2D g, String text, Font font, float x, float baseline) {
        g.setFont(font);
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2f, baseline);
    }

    private boolean any(Map<UpgradeNode, UpgradeNodeState> states, UpgradeSlot slot, Look look) {
        return states.values().stream().anyMatch(s -> s.node().slot() == slot && s.look() == look);
    }

    /** The node's outline grown by {@code grow} on every side. */
    private static Shape shapeOf(Spot spot, float grow) {
        float size = spot.size() + 2 * grow;
        float left = spot.x() - size / 2;
        float top = spot.y() - size / 2;
        return spot.round() ? new Ellipse2D.Float(left, top, size, size)
                : new RoundRectangle2D.Float(left, top, size, size, 2 * (CORNER + grow), 2 * (CORNER + grow));
    }

    private static Color fillOf(Look look, Color slot) {
        return switch (look) {
            case OWNED -> blend(slot, 0.27f);
            case BUYABLE, SHORT_OF_CASH -> BUY_FILL;
            case WAITING -> WAIT_FILL;
            case NEEDS_PREVIOUS -> Color.BLACK;
            case SEALED -> SEALED_FILL;
            case LOCKED_OUT -> OUT_FILL;
        };
    }

    private static Color borderOf(Look look, Color slot) {
        return switch (look) {
            case OWNED, BUYABLE, SHORT_OF_CASH -> slot;
            case WAITING -> Hud.withAlpha(slot, 0.53f);
            case NEEDS_PREVIOUS -> Hud.withAlpha(slot, 0.6f);
            case SEALED -> SEALED_BORDER;
            case LOCKED_OUT -> OUT_BORDER;
        };
    }

    private static Color inkOf(Look look, Color slot) {
        return switch (look) {
            case OWNED -> WHITE;
            case BUYABLE, SHORT_OF_CASH -> slot;
            case WAITING -> Hud.withAlpha(slot, 0.73f);
            case NEEDS_PREVIOUS -> Hud.withAlpha(slot, 0.6f);
            case SEALED -> SEALED_INK;
            case LOCKED_OUT -> OUT_INK;
        };
    }

    static Color slotColor(UpgradeSlot slot) {
        return Java2DFrameRenderer.colorFor(TowerSpriteFrameBuilder.slotPaletteFor(slot));
    }

    /** {@code color} at {@code share} over black, opaque: a translucent fill that hides what is under it. */
    private static Color blend(Color color, float share) {
        return new Color(Math.round(color.getRed() * share), Math.round(color.getGreen() * share),
                Math.round(color.getBlue() * share));
    }

    private static Stroke dashed(float width) {
        return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{3f, 3f}, 0f);
    }

    private static Shape path(float[][] points) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(points[0][0], points[0][1]);
        for (int i = 1; i < points.length; i++) {
            path.lineTo(points[i][0], points[i][1]);
        }
        path.closePath();
        return path;
    }
}
