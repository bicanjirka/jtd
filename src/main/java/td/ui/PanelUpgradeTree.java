package td.ui;

import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.Tower;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.ui.UpgradeTreeLayout.Spot;
import td.ui.UpgradeTreePainter.Footer;
import td.ui.UpgradeTreePainter.Gates;
import td.ui.UpgradeTreePainter.Highlight;
import td.ui.UpgradeTreePainter.Mark;
import td.ui.UpgradeTreePainter.XpLine;
import td.ui.render.InfoSheet;
import td.ui.render.SheetTooltip;
import td.ui.render.TooltipKey;
import td.util.GameWorld;
import td.util.ThreadConfined;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The selected tower's whole upgrade tree, its tiers climbing from the bottom (see {@link UpgradeTreeLayout}), under a
 * slim XP line, every node in the look of its state. Hovering a node opens its sheet and marks what a click does: a
 * left click buys the node with every node it needs, all or nothing, or takes a cluster's other member in the owned
 * one's place; a right-click on an owned node asks to downgrade it with everything that depends on it. A switch and a
 * downgrade wait for the wave's end. Replaces the wave preview while a tower is selected, and never scrolls: the tree
 * scales to the room it has.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelUpgradeTree extends JPanel implements EconomyListener {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final float MAX_SCALE = 1.15f;
    /** What the Info panel above keeps at least when the window is short. */
    private static final int INFO_FLOOR = 180;
    /** The least height the tree is drawn at, a little over half size. */
    private static final int MIN_HEIGHT = 240;
    private static final int HIT_SLACK = 2;

    private Color xpFill = Hud.FOREGROUND;
    private transient GameWorld context;
    private transient Tower tower;
    private boolean levelEnded = false;
    private transient TooltipLayer tooltips;
    private transient Map<UpgradeNode, UpgradeNodeState> states = Map.of();
    private transient Optional<UpgradeTreeLayout> layout = Optional.empty();
    private transient Optional<UpgradeNode> hovered = Optional.empty();
    private transient Optional<UpgradeHover> hover = Optional.empty();
    private transient Optional<ConfirmPopup> confirmation = Optional.empty();

    public PanelUpgradeTree() {
        this.setBackground(Hud.BACKGROUND);
        this.setBorder(Hud.panelBorder("Upgrades"));
        this.setPreferredSize(new Dimension(UpgradeTreeLayout.WIDTH, UpgradeTreeLayout.HEIGHT));
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

    /**
     * Its full height while the console has room for it above {@link #INFO_FLOOR}, else what is left, down to
     * {@link #MIN_HEIGHT}: the console is shorter than its panels' preferred heights, so its layout gives each
     * panel its minimum, and the tree scales to whatever it gets.
     */
    @Override
    public Dimension getMinimumSize() {
        int room = this.getParent() == null ? UpgradeTreeLayout.HEIGHT : this.getParent().getHeight() - INFO_FLOOR;
        return new Dimension(UpgradeTreeLayout.WIDTH, Math.clamp(room, MIN_HEIGHT, UpgradeTreeLayout.HEIGHT));
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
        this.refresh();
    }

    public void setXpFill(Color fill) {
        this.xpFill = fill;
        this.repaint();
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
            this.layout = Optional.empty();
            this.hover = Optional.empty();
            this.repaint();
            return;
        }
        UpgradeTree tree = this.tower.upgradeTree();
        Map<UpgradeNode, UpgradeNodeState> next = new LinkedHashMap<>();
        for (int tier = 0; tier < tree.tiers().size(); tier++) {
            tree.unlockOf(tier).ifPresent(unlock -> next.put(unlock, UpgradeNodeState.of(unlock, this.tower, this.context)));
        }
        for (UpgradeNode node : tree.nodes()) {
            next.put(node, UpgradeNodeState.of(node, this.tower, this.context));
        }
        this.states = next;
        this.relayout();
        this.hover = this.hovered.filter(node -> !this.levelEnded).map(node -> UpgradeHover.of(node, this.tower, this.context));
        this.repaint();
        if (this.tooltips != null) {
            this.tooltips.refreshButtons();
        }
    }

    /**
     * Lays the tree out for what the tower owns and the hovered member's branches, then again with every open tier
     * whose shown nodes are all settled folded thin.
     */
    private void relayout() {
        UpgradeTree tree = this.tower.upgradeTree();
        Optional<UpgradeNode> preview = this.hovered.filter(node -> tree.clusterOf(node).isPresent());
        UpgradeTreeLayout full = UpgradeTreeLayout.of(tree, this.tower.upgrades(), preview, Set.of());
        int open = tree.highestOpenTier(this.tower, this.context);
        Set<Integer> folded = new LinkedHashSet<>();
        for (int tier = 0; tier <= open; tier++) {
            if (this.settled(tier, full)) {
                folded.add(tier);
            }
        }
        this.layout = Optional.of(folded.isEmpty() ? full
                : UpgradeTreeLayout.of(tree, this.tower.upgrades(), preview, folded));
    }

    /** Whether every node {@code tier}'s row shows is owned or a cluster's other pick, and no branch hides in it. */
    private boolean settled(int tier, UpgradeTreeLayout full) {
        List<UpgradeNode> shown = full.shownIn(tier);
        boolean unlocked = this.tower.upgradeTree().isPaidOpen(tier, this.tower.upgrades());
        boolean hides = full.rows().stream().filter(row -> row.tier() == tier).anyMatch(row -> full.hidden().stream()
                .anyMatch(spot -> spot.y() >= row.top() && spot.y() <= row.bottom()));
        return !shown.isEmpty() && unlocked && !hides && shown.stream().allMatch(node -> !full.previewed().contains(node)
                && (this.states.get(node).look() == UpgradeNodeState.Look.OWNED
                || this.states.get(node).look() == UpgradeNodeState.Look.SWITCHABLE));
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
            g2.translate(this.originX(scale), 0f);
            g2.scale(scale, scale);
            UpgradeTree tree = this.tower.upgradeTree();
            new UpgradeTreePainter(this.layout.get(), tree, TowerSpriteFrameBuilder.bodyLookOf(this.tower),
                    this.tower.getType()).paint(g2, this.states, this.highlight(), this.gates(), this.xpLine(),
                    this.footer());
        } finally {
            g2.dispose();
        }
    }

    private Gates gates() {
        UpgradeTree tree = this.tower.upgradeTree();
        int xp = this.tower.experience().xp();
        Map<Integer, String> labels = new HashMap<>();
        for (int tier = 1; tier < tree.tiers().size(); tier++) {
            labels.put(tier, UpgradeSheetText.bandLabel(tree, tier, xp, tree.isOpen(tier, this.tower, this.context)));
        }
        return new Gates(tree.highestOpenTier(this.tower, this.context), labels);
    }

    private Highlight highlight() {
        if (this.hovered.isEmpty() || this.hover.isEmpty()) {
            return Highlight.none();
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
        return new Highlight(this.hovered, marks);
    }

    private XpLine xpLine() {
        int xp = this.tower.experience().xp();
        UpgradeTree tree = this.tower.upgradeTree();
        OptionalInt next = UpgradeSheetText.nextTier(tree, xp);
        // With no tier waiting on XP, the bar stays full.
        int toward = next.isPresent() ? tree.tiers().get(next.getAsInt()).xp() : xp;
        return new XpLine(UpgradeSheetText.xpLabel(xp, tree), toward == 0 ? 1f : xp / (float) toward, this.xpFill);
    }

    private Footer footer() {
        if (this.levelEnded) {
            return new Footer(UpgradeSheetText.ENDED_FOOTER, false, false);
        }
        return new Footer(UpgradeSheetText.footer(this.hover), UpgradeSheetText.footerWarns(this.hover),
                this.hover.isPresent());
    }

    private void pointerAt(Point point) {
        Optional<UpgradeNode> now = this.nodeAt(point);
        if (now.equals(this.hovered)) {
            return;
        }
        this.hovered = now;
        if (this.tower != null) {
            this.relayout();
        }
        this.hover = now.filter(node -> this.tower != null && !this.levelEnded)
                .map(node -> UpgradeHover.of(node, this.tower, this.context));
        this.repaint();
    }

    private void clicked(MouseEvent event) {
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
        float y = point.y / scale;
        return this.layout.flatMap(laid -> laid.nodeAt(x, y, HIT_SLACK));
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
        float width = spot.get().width() * scale;
        float height = spot.get().height() * scale;
        return new Rectangle(Math.round(this.originX(scale) + (spot.get().x() * scale) - width / 2),
                Math.round(spot.get().y() * scale - height / 2), Math.round(width), Math.round(height));
    }

    /**
     * The whole design fits the panel, never growing past {@link #MAX_SCALE}. It sits at the top, under the border's
     * title, so a tier folding moves only what is below it.
     */
    private float scale() {
        float height = this.layout.map(UpgradeTreeLayout::height).orElse((float) UpgradeTreeLayout.HEIGHT);
        float fit = Math.min(this.getWidth() / (float) UpgradeTreeLayout.WIDTH, this.getHeight() / height);
        return Math.min(MAX_SCALE, fit);
    }

    private float originX(float scale) {
        return (this.getWidth() - UpgradeTreeLayout.WIDTH * scale) / 2f;
    }
}
