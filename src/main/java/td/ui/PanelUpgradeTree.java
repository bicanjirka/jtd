package td.ui;

import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.Tower;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.ui.UpgradeTreeLayout.Band;
import td.ui.UpgradeTreeLayout.Shared;
import td.ui.UpgradeTreeLayout.Showing;
import td.ui.UpgradeTreeLayout.Spot;
import td.ui.UpgradeTreePainter.Floor;
import td.ui.UpgradeTreePainter.Footer;
import td.ui.UpgradeTreePainter.Highlight;
import td.ui.UpgradeTreePainter.Mark;
import td.ui.render.InfoSheet;
import td.ui.render.SheetTooltip;
import td.ui.render.TooltipKey;
import td.util.GameWorld;
import td.util.ThreadConfined;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The selected tower's whole upgrade tree, its tiers climbing from the bottom (see {@link UpgradeTreeLayout}), with the
 * tower's XP on the title line, every node in the look of its state. Hovering a node opens its sheet and marks what a
 * click does: a left click buys the node with every node it needs, all or nothing, or takes a cluster's other member
 * in the owned one's place; a right-click on an owned node asks to downgrade it with everything that depends on it. A
 * switch and a downgrade wait for the wave's end. Each open tier's floor strip has a button, and the tier's digit key,
 * that buy its singles. Replaces the wave preview while a tower is selected, and never scrolls: the tree scales to the
 * room it has.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelUpgradeTree extends JPanel implements EconomyListener {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final float MAX_SCALE = BuyTileLook.SCALE;
    /** What the Info panel above keeps at least when the window is short. */
    private static final int INFO_FLOOR = 180;
    /** The least height the tree is drawn at, a little over half size. */
    private static final int MIN_HEIGHT = 240;
    private static final int HIT_SLACK = 2;
    /** Where the title line's XP ends, about as far in from the right as the title starts from the left. */
    private static final int TITLE_RIGHT = 10;

    private transient GameWorld context;
    private transient Tower tower;
    private boolean levelEnded = false;
    private transient TooltipLayer tooltips;
    private transient Map<UpgradeNode, UpgradeNodeState> states = Map.of();
    private transient Optional<UpgradeTreeLayout> layout = Optional.empty();
    private transient Optional<UpgradeNode> hovered = Optional.empty();
    private OptionalInt hoveredTier = OptionalInt.empty();
    private transient Optional<Shared> hoveredSlot = Optional.empty();
    private transient Optional<UpgradeHover> hover = Optional.empty();
    private transient Optional<ConfirmPopup> confirmation = Optional.empty();

    public PanelUpgradeTree() {
        this.setBackground(Hud.BACKGROUND);
        this.setBorder(Hud.panelBorder("Upgrades"));
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                PanelUpgradeTree.this.pointerAt(event.getPoint());
            }

            @Override
            public void mouseExited(MouseEvent event) {
                PanelUpgradeTree.this.pointerAt(new Point(-1, -1));
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                PanelUpgradeTree.this.clicked(event);
            }
        };
        this.addMouseListener(mouse);
        this.addMouseMotionListener(mouse);
    }

    /** The selected tower's tree at full size, so a short tree leaves the Info panel above the room it doesn't use. */
    @Override
    public Dimension getPreferredSize() {
        Insets insets = this.getInsets();
        float height = this.layout.map(UpgradeTreeLayout::height).orElse((float) UpgradeTreeLayout.HEIGHT);
        return new Dimension((int) Math.ceil(UpgradeTreeLayout.WIDTH * MAX_SCALE) + insets.left + insets.right,
                (int) Math.ceil(height * MAX_SCALE) + insets.top + insets.bottom);
    }

    /**
     * Its full height while the console has room for it above {@link #INFO_FLOOR}, else what is left, down to
     * {@link #MIN_HEIGHT}: the console is shorter than its panels' preferred heights, so its layout gives each
     * panel its minimum, and the tree scales to whatever it gets.
     */
    @Override
    public Dimension getMinimumSize() {
        int full = this.getPreferredSize().height;
        int room = this.getParent() == null ? full : this.getParent().getHeight() - INFO_FLOOR;
        return new Dimension(this.getPreferredSize().width, Math.clamp(room, Math.min(MIN_HEIGHT, full), full));
    }

    /** Each node's hover is a tooltip of what it is and what a click on it does now. */
    public void setTooltips(TooltipLayer tooltips) {
        this.tooltips = tooltips;
        tooltips.watchSpots(this, point -> this.nodeAt(point).filter(node -> this.tower != null)
                .map(node -> new TooltipSpot(new SheetTooltip(TooltipKey.of("upgrade", node.id()), this.sheetOf(node)),
                        this.anchorOf(node))));
    }

    public void setGameWorld(GameWorld context) {
        this.context = context;
        this.context.economy().addEconomyListener(this);
    }

    public void setTower(Tower tower) {
        this.confirmation.ifPresent(ConfirmPopup::cancel);
        this.tower = tower;
        this.hovered = Optional.empty();
        this.hoveredTier = OptionalInt.empty();
        this.hoveredSlot = Optional.empty();
        this.layout = Optional.ofNullable(tower)
                .map(chosen -> UpgradeTreeLayout.of(chosen.upgradeTree(), UpgradeTreePlans.of(chosen.getType())));
        this.revalidate();
        this.refresh();
    }

    public void setLevelEnded(boolean ended) {
        this.levelEnded = ended;
        this.refresh();
    }

    /** May run on the game-loop thread. */
    @Override
    public void economyChanged(EconomyState state) {
        SwingUtilities.invokeLater(this::refresh);
    }

    /**
     * Buys {@code tier}'s singles ({@link UpgradeTree#singlesIn}) left to right as the panel shows them, each it is
     * offered and can pay for; a no-op without a tower, after the level's end, and for a tier the tree lacks.
     */
    public void buySingles(int tier) {
        if (this.tower == null || this.levelEnded || tier < 0 || tier >= this.tower.upgradeTree().tiers().size()) {
            return;
        }
        List<UpgradeNode> singles = this.tower.upgradeTree().singlesIn(tier);
        this.tower.buyEach(this.layout.map(laid -> laid.leftToRight(singles)).orElse(singles));
        this.refresh();
    }

    /**
     * Reads every node's state and the hovered node's path again. Also called from the render pulse, since XP and
     * gate progress change without economy events; an open node sheet follows.
     */
    public void refresh() {
        if (this.tower == null) {
            this.states = Map.of();
            this.hover = Optional.empty();
            this.repaint();
            return;
        }
        Map<UpgradeNode, UpgradeNodeState> next = new LinkedHashMap<>();
        for (UpgradeNode node : this.tower.upgradeTree().nodes()) {
            next.put(node, UpgradeNodeState.of(node, this.tower, this.context));
        }
        this.states = next;
        this.hover = this.hovered.filter(node -> !this.levelEnded).map(node -> UpgradeHover.of(node, this.tower, this.context));
        this.repaint();
        if (this.tooltips != null) {
            this.tooltips.refreshButtons();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (this.tower == null || this.states.isEmpty() || this.layout.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            float scale = this.scale();
            g2.translate(this.originX(scale), this.getInsets().top);
            g2.scale(scale, scale);
            UpgradeTree tree = this.tower.upgradeTree();
            new UpgradeTreePainter(this.layout.get(), tree, TowerSpriteFrameBuilder.bodyLookOf(this.tower),
                    this.tower.getType()).paint(g2, this.states, this.highlight(), this.showing(),
                    tree.highestOpenTier(this.tower, this.context), this.floors(), this.footer());
        } finally {
            g2.dispose();
        }
    }

    /** The border, then the tower's XP on its title line, at the right. */
    @Override
    protected void paintBorder(Graphics g) {
        super.paintBorder(g);
        if (this.tower == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(Hud.LABEL_FONT);
            FontMetrics metrics = g2.getFontMetrics();
            String xp = UpgradeSheetText.titleXp(this.tower.experience().xp());
            int width = metrics.stringWidth(xp);
            int x = this.getWidth() - TITLE_RIGHT - width;
            g2.setColor(Hud.BACKGROUND);
            g2.fillRect(x - 4, 0, width + 8, metrics.getHeight());
            g2.setColor(Hud.TEXT_DIM);
            g2.drawString(xp, x, metrics.getAscent());
        } finally {
            g2.dispose();
        }
    }

    /** Each tier's floor strip: its gate while locked, else its singles' button. */
    private List<Floor> floors() {
        UpgradeTree tree = this.tower.upgradeTree();
        int xp = this.tower.experience().xp();
        List<Floor> floors = new ArrayList<>();
        for (int tier = 0; tier < tree.tiers().size(); tier++) {
            if (!tree.isOpen(tier, this.tower, this.context)) {
                floors.add(new Floor.Gate(xp, tree.tiers().get(tier).xp()));
                continue;
            }
            List<UpgradeNode> offered = this.offeredSingles(tier);
            if (!offered.isEmpty()) {
                int total = offered.stream().mapToInt(UpgradeNode::price).sum();
                floors.add(new Floor.Singles(UpgradeSheetText.singlesButton(total),
                        String.valueOf(GameKey.singlesKey(tier)), !this.anyAffordable(offered),
                        this.hoveredTier.isPresent() && this.hoveredTier.getAsInt() == tier));
            } else if (this.allOwned(tree.singlesIn(tier))) {
                floors.add(new Floor.Bought());
            } else {
                floors.add(new Floor.Empty());
            }
        }
        return floors;
    }

    /** Whether the button would buy anything now: red only when the credits cover none of what it offers. */
    private boolean anyAffordable(List<UpgradeNode> offered) {
        return offered.stream().anyMatch(node -> this.context.economy().canPay(node.price()));
    }

    private boolean allOwned(List<UpgradeNode> singles) {
        return !singles.isEmpty()
                && singles.stream().allMatch(node -> this.states.get(node).look() == UpgradeNodeState.Look.OWNED);
    }

    /** {@code tier}'s singles on offer now, money aside, left to right. */
    private List<UpgradeNode> offeredSingles(int tier) {
        UpgradeTree tree = this.tower.upgradeTree();
        List<UpgradeNode> offered = tree.singlesIn(tier).stream()
                .filter(node -> tree.offers(node, this.tower, this.context)).toList();
        return this.layout.map(laid -> laid.leftToRight(offered)).orElse(offered);
    }

    private Highlight highlight() {
        if (this.hoveredTier.isPresent() && !this.levelEnded) {
            Map<UpgradeNode, Mark> marks = new HashMap<>();
            this.offeredSingles(this.hoveredTier.getAsInt()).forEach(node -> marks.put(node, Mark.PATH));
            return new Highlight(Optional.empty(), marks, Optional.empty());
        }
        if (this.hovered.isEmpty() || this.hover.isEmpty()) {
            return new Highlight(Optional.empty(), Map.of(), this.hoveredSlot);
        }
        Map<UpgradeNode, Mark> marks = new HashMap<>();
        switch (this.hover.get()) {
            case UpgradeHover.Buying buying -> {
                buying.path().locksOut().forEach(locked -> marks.put(locked, Mark.CRACK));
                buying.path().steps().forEach(step -> marks.put(step, Mark.PATH));
            }
            case UpgradeHover.Switching switching -> {
                marks.put(switching.change().from(), Mark.DROP);
                switching.change().sold().forEach(sold -> marks.put(sold, Mark.DROP));
                marks.put(switching.change().to(), Mark.PATH);
            }
            case UpgradeHover.Downgrading downgrading ->
                    downgrading.removal().removed().forEach(removed -> marks.put(removed, Mark.DROP));
        }
        return new Highlight(this.hovered, marks, Optional.empty());
    }

    /** What the shared slots show: the side of the cluster member hovered, else of the one owned. */
    private Showing showing() {
        return this.layout.map(laid -> laid.showing(node -> this.states.containsKey(node)
                && this.states.get(node).look() == UpgradeNodeState.Look.OWNED, this.hovered)).orElse(Showing.all());
    }

    private Footer footer() {
        if (this.levelEnded) {
            return new Footer(UpgradeSheetText.ENDED_FOOTER, false, false);
        }
        if (this.hoveredSlot.isPresent()) {
            return new Footer(UpgradeSheetText.pickFirst(this.hoveredSlot.get().bySide().keySet().stream()
                    .sorted(Comparator.comparingDouble(member -> this.layout.flatMap(laid -> laid.spotOf(member))
                            .map(Spot::x).orElse(0f))).map(UpgradeNode::displayName).toList()), false, true);
        }
        if (this.hoveredTier.isPresent()) {
            List<UpgradeNode> offered = this.offeredSingles(this.hoveredTier.getAsInt());
            int total = offered.stream().mapToInt(UpgradeNode::price).sum();
            return new Footer(UpgradeSheetText.singlesFooter(offered.size(), total), !this.anyAffordable(offered),
                    true);
        }
        return new Footer(UpgradeSheetText.footer(this.hover), UpgradeSheetText.footerWarns(this.hover),
                this.hover.isPresent());
    }

    private void pointerAt(Point point) {
        Optional<UpgradeNode> now = this.nodeAt(point);
        OptionalInt tier = now.isPresent() ? OptionalInt.empty() : this.buttonAt(point);
        Optional<Shared> slot = now.isPresent() ? Optional.empty() : this.emptySlotAt(point);
        if (now.equals(this.hovered) && tier.equals(this.hoveredTier) && slot.equals(this.hoveredSlot)) {
            return;
        }
        this.hovered = now;
        this.hoveredTier = tier;
        this.hoveredSlot = slot;
        this.hover = now.filter(node -> this.tower != null && !this.levelEnded)
                .map(node -> UpgradeHover.of(node, this.tower, this.context));
        this.repaint();
    }

    private void clicked(MouseEvent event) {
        OptionalInt tier = this.buttonAt(event.getPoint());
        if (tier.isPresent() && SwingUtilities.isLeftMouseButton(event)) {
            this.buySingles(tier.getAsInt());
            return;
        }
        Optional<UpgradeNode> node = this.nodeAt(event.getPoint());
        if (node.isEmpty() || this.tower == null || this.levelEnded) {
            return;
        }
        UpgradeNodeState.Look look = this.states.get(node.get()).look();
        boolean allowed = UpgradeTree.changesAllowed(this.context);
        boolean settled = look == UpgradeNodeState.Look.OWNED || look == UpgradeNodeState.Look.SWITCHABLE;
        if (SwingUtilities.isRightMouseButton(event) && look == UpgradeNodeState.Look.OWNED && allowed) {
            this.askToDowngrade(node.get());
        } else if (SwingUtilities.isLeftMouseButton(event) && look == UpgradeNodeState.Look.SWITCHABLE && allowed) {
            this.askToSwitch(node.get());
        } else if (SwingUtilities.isLeftMouseButton(event) && !settled) {
            this.tower.buyPath(node.get());
            this.refresh();
        }
    }

    /**
     * The confirmation beside the node; the list and the refund are worked out again when it is confirmed. What all
     * refunds in full is nothing to ask about.
     */
    private void askToDowngrade(UpgradeNode node) {
        switch (UpgradeHover.of(node, this.tower, this.context)) {
            case UpgradeHover.Downgrading downgrading when downgrading.refund().isFull() -> {
                this.tower.downgrade(node);
                this.refresh();
            }
            case UpgradeHover.Downgrading downgrading -> this.askToDowngrade(node, downgrading);
            case UpgradeHover.Buying buying -> {
            }
            case UpgradeHover.Switching switching -> {
            }
        }
    }

    /** A switch that sells nothing back is nothing to ask about; one that does names what it sells. */
    private void askToSwitch(UpgradeNode member) {
        switch (UpgradeHover.of(member, this.tower, this.context)) {
            case UpgradeHover.Switching switching when switching.change().sold().isEmpty() -> {
                this.tower.switchTo(member);
                this.refresh();
            }
            case UpgradeHover.Switching switching -> this.askToSwitch(member, switching);
            case UpgradeHover.Buying buying -> {
            }
            case UpgradeHover.Downgrading downgrading -> {
            }
        }
    }

    private void askToSwitch(UpgradeNode member, UpgradeHover.Switching switching) {
        if (this.tooltips != null) {
            this.tooltips.closeAll();
        }
        this.confirmation.ifPresent(ConfirmPopup::cancel);
        Tower switched = this.tower;
        Rectangle anchor = SwingUtilities.convertRectangle(this, this.anchorOf(member),
                SwingUtilities.getRootPane(this).getLayeredPane());
        this.confirmation = Optional.of(ConfirmPopup.open(SwingUtilities.getRootPane(this).getLayeredPane(), anchor,
                UpgradeSheetText.switchConfirmation(switching), UpgradeSheetText.switchButton(switching),
                ConfirmPopup.IRREVERSIBLE,
                () -> {
                    this.confirmation = Optional.empty();
                    if (!this.levelEnded) {
                        switched.switchTo(member);
                    }
                    this.refresh();
                },
                () -> this.confirmation = Optional.empty()));
    }

    private void askToDowngrade(UpgradeNode node, UpgradeHover.Downgrading downgrading) {
        if (this.tooltips != null) {
            this.tooltips.closeAll();
        }
        this.confirmation.ifPresent(ConfirmPopup::cancel);
        Tower downgraded = this.tower;
        Rectangle anchor = SwingUtilities.convertRectangle(this, this.anchorOf(node),
                SwingUtilities.getRootPane(this).getLayeredPane());
        this.confirmation = Optional.of(ConfirmPopup.open(SwingUtilities.getRootPane(this).getLayeredPane(), anchor,
                UpgradeSheetText.downgradeConfirmation(downgrading, downgraded.upgradeTree()),
                UpgradeSheetText.downgradeButton(downgrading), ConfirmPopup.IRREVERSIBLE,
                () -> {
                    this.confirmation = Optional.empty();
                    if (!this.levelEnded) {
                        downgraded.downgrade(node);
                    }
                    this.refresh();
                },
                () -> this.confirmation = Optional.empty()));
    }

    private InfoSheet sheetOf(UpgradeNode node) {
        UpgradeOffer offer = UpgradeOffer.of(node, this.tower, this.context);
        if (this.levelEnded) {
            return UpgradeSheetText.hover(offer);
        }
        return UpgradeSheetText.hover(offer, UpgradeHover.of(node, this.tower, this.context), this.tower.upgradeTree());
    }

    private Optional<UpgradeNode> nodeAt(Point point) {
        if (this.tower == null || this.layout.isEmpty()) {
            return Optional.empty();
        }
        float scale = this.scale();
        if (scale <= 0f) {
            return Optional.empty();
        }
        float x = (point.x - this.originX(scale)) / scale;
        float y = (point.y - this.getInsets().top) / scale;
        Showing showing = this.showing();
        return this.layout.flatMap(laid -> laid.nodeAt(x, y, HIT_SLACK, showing));
    }

    /** The shared slot under {@code point} that stands empty, its cluster's pick not made yet. */
    private Optional<Shared> emptySlotAt(Point point) {
        if (this.tower == null || this.layout.isEmpty()) {
            return Optional.empty();
        }
        float scale = this.scale();
        if (scale <= 0f) {
            return Optional.empty();
        }
        float x = (point.x - this.originX(scale)) / scale;
        float y = (point.y - this.getInsets().top) / scale;
        Showing showing = this.showing();
        return this.layout.flatMap(laid -> laid.emptySlotAt(x, y, HIT_SLACK, showing));
    }

    /** The tier whose singles' button is under {@code point}, while it shows. */
    private OptionalInt buttonAt(Point point) {
        if (this.tower == null || this.layout.isEmpty() || this.levelEnded) {
            return OptionalInt.empty();
        }
        float scale = this.scale();
        if (scale <= 0f) {
            return OptionalInt.empty();
        }
        float x = (point.x - this.originX(scale)) / scale;
        float y = (point.y - this.getInsets().top) / scale;
        FontMetrics metrics = this.getFontMetrics(UpgradeTreePainter.BUTTON_FONT);
        for (Band band : this.layout.get().bands()) {
            if (!this.tower.upgradeTree().isOpen(band.tier(), this.tower, this.context)) {
                continue;
            }
            List<UpgradeNode> offered = this.offeredSingles(band.tier());
            if (offered.isEmpty()) {
                continue;
            }
            String label = UpgradeSheetText.singlesButton(offered.stream().mapToInt(UpgradeNode::price).sum());
            Rectangle2D.Float box = UpgradeTreePainter.buttonBox(metrics, label, band);
            if (box.contains(x, y)) {
                return OptionalInt.of(band.tier());
            }
        }
        return OptionalInt.empty();
    }

    /**
     * The panel's left edge, level with {@code node}: what opens from a node opens over the board, never over the
     * tree, where it would sit on the nodes the pointer goes to next.
     */
    private Rectangle anchorOf(UpgradeNode node) {
        Rectangle bounds = this.boundsOf(node);
        return new Rectangle(0, bounds.y, 0, bounds.height);
    }

    private Rectangle boundsOf(UpgradeNode node) {
        float scale = this.scale();
        Optional<Spot> spot = this.layout.flatMap(laid -> laid.spotOf(node));
        if (spot.isEmpty()) {
            return new Rectangle(0, this.getHeight() / 2, 0, 0);
        }
        float size = spot.get().size() * scale;
        return new Rectangle(Math.round(this.originX(scale) + (spot.get().x() * scale) - size / 2),
                Math.round(this.getInsets().top + spot.get().y() * scale - size / 2), Math.round(size),
                Math.round(size));
    }

    /** The whole design fits inside the border, never growing past {@link #MAX_SCALE}, at its top. */
    private float scale() {
        Insets insets = this.getInsets();
        float height = this.layout.map(UpgradeTreeLayout::height).orElse((float) UpgradeTreeLayout.HEIGHT);
        float fit = Math.min((this.getWidth() - insets.left - insets.right) / (float) UpgradeTreeLayout.WIDTH,
                (this.getHeight() - insets.top - insets.bottom) / height);
        return Math.min(MAX_SCALE, fit);
    }

    private float originX(float scale) {
        return (this.getWidth() - UpgradeTreeLayout.WIDTH * scale) / 2f;
    }
}
