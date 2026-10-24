package td.ui;

import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.tower.Tower;
import td.tower.TowerRank;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.ui.UpgradeTreeLayout.Spot;
import td.ui.UpgradeTreePainter.Footer;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The selected tower's whole upgrade tree as a talent tree over a faint rook (see {@link UpgradeTreeLayout}),
 * under a slim XP line, every node in the look of its state. Hovering a node opens its sheet and marks what a
 * click does: a left click buys the node with every node it needs, all or nothing; a right-click on an owned
 * node asks to downgrade it with everything that depends on it. Replaces the wave preview while a tower is
 * selected, and never scrolls: the tree scales to the room it has.
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

    private final transient Map<UpgradeTree, UpgradeTreeLayout> layouts = new IdentityHashMap<>();
    private transient GameWorld context;
    private transient Tower tower;
    private boolean levelEnded = false;
    private transient TooltipLayer tooltips;
    private transient Map<UpgradeNode, UpgradeNodeState> states = Map.of();
    private transient Optional<UpgradeNode> hovered = Optional.empty();
    private transient Optional<UpgradeHover> hover = Optional.empty();
    private transient Optional<ConfirmPopup> confirmation = Optional.empty();

    public PanelUpgradeTree() {
        this.setBackground(Color.BLACK);
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
        if (this.tower == null || this.states.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            float scale = this.scale();
            g2.translate(this.originX(scale), this.originY(scale));
            g2.scale(scale, scale);
            UpgradeTree tree = this.tower.upgradeTree();
            new UpgradeTreePainter(this.layoutOf(tree), tree, this.tower.getType())
                    .paint(g2, this.states, this.highlight(), this.xpLine(), this.footer());
        } finally {
            g2.dispose();
        }
    }

    private Highlight highlight() {
        if (this.hovered.isEmpty() || this.hover.isEmpty()) {
            return Highlight.none();
        }
        Map<UpgradeNode, Mark> marks = new HashMap<>();
        UpgradeNode node = this.hovered.get();
        switch (this.hover.get()) {
            case UpgradeHover.Buying buying -> {
                buying.path().locksOut().forEach(locked -> marks.put(locked, Mark.CRACK));
                buying.path().steps().forEach(step -> marks.put(step, Mark.PATH));
            }
            case UpgradeHover.Downgrading downgrading ->
                    downgrading.removal().removed().forEach(removed -> marks.put(removed, Mark.DROP));
        }
        this.states.values().stream().filter(state -> state.sealedBy().filter(node::equals).isPresent())
                .forEach(state -> marks.putIfAbsent(state.node(), Mark.LIT));
        return new Highlight(this.hovered, marks);
    }

    private XpLine xpLine() {
        int xp = this.tower.experience().xp();
        Optional<UpgradeNode> next = UpgradeSheetText.nextXpGate(this.tower.offeredUpgrades(this.context), xp);
        // With no node waiting on XP, the bar fills toward the next rank.
        int toward = next.map(UpgradeNode::xp).orElseGet(() -> Arrays.stream(TowerRank.values())
                .mapToInt(TowerRank::xp).filter(rank -> rank > xp).findFirst().orElse(xp));
        return new XpLine(UpgradeSheetText.xpLabel(xp, next), toward == 0 ? 1f : xp / (float) toward);
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
        this.hover = now.filter(node -> this.tower != null && !this.levelEnded)
                .map(node -> UpgradeHover.of(node, this.tower, this.context));
        this.repaint();
    }

    private void clicked(MouseEvent event) {
        Optional<UpgradeNode> node = this.nodeAt(event.getPoint());
        if (node.isEmpty() || this.tower == null || this.levelEnded) {
            return;
        }
        boolean owned = this.states.get(node.get()).look() == UpgradeNodeState.Look.OWNED;
        if (SwingUtilities.isRightMouseButton(event) && owned) {
            this.askToDowngrade(node.get());
        } else if (SwingUtilities.isLeftMouseButton(event) && !owned) {
            this.tower.buyPath(node.get());
            this.refresh();
        }
    }

    /** The confirmation beside the node; the list and the refund are worked out again when it is confirmed. */
    private void askToDowngrade(UpgradeNode node) {
        switch (UpgradeHover.of(node, this.tower, this.context)) {
            case UpgradeHover.Downgrading downgrading -> this.askToDowngrade(node, downgrading);
            case UpgradeHover.Buying buying -> {
            }
        }
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
        if (this.tower == null) {
            return Optional.empty();
        }
        float scale = this.scale();
        if (scale <= 0f) {
            return Optional.empty();
        }
        float x = (point.x - this.originX(scale)) / scale;
        float y = (point.y - this.originY(scale)) / scale;
        return this.layoutOf(this.tower.upgradeTree()).spots().entrySet().stream()
                .filter(entry -> Math.abs(entry.getValue().x() - x) <= entry.getValue().size() / 2 + HIT_SLACK
                        && Math.abs(entry.getValue().y() - y) <= entry.getValue().size() / 2 + HIT_SLACK)
                .map(Map.Entry::getKey)
                .findFirst();
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
        Spot spot = this.layoutOf(this.tower.upgradeTree()).spots().get(node);
        float size = spot.size() * scale;
        return new Rectangle(Math.round(this.originX(scale) + (spot.x() * scale) - size / 2),
                Math.round(this.originY(scale) + (spot.y() * scale) - size / 2), Math.round(size), Math.round(size));
    }

    private UpgradeTreeLayout layoutOf(UpgradeTree tree) {
        return this.layouts.computeIfAbsent(tree, UpgradeTreeLayout::of);
    }

    /** The whole design fits the panel, never growing past {@link #MAX_SCALE}. */
    private float scale() {
        float fit = Math.min(this.getWidth() / (float) UpgradeTreeLayout.WIDTH,
                this.getHeight() / (float) UpgradeTreeLayout.HEIGHT);
        return Math.min(MAX_SCALE, fit);
    }

    private float originX(float scale) {
        return (this.getWidth() - UpgradeTreeLayout.WIDTH * scale) / 2f;
    }

    private float originY(float scale) {
        return (this.getHeight() - UpgradeTreeLayout.HEIGHT * scale) / 2f;
    }
}
