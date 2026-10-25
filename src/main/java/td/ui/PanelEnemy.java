package td.ui;

import td.board.BoardGeometry;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.enemy.SpawnParameters;
import td.ui.render.SheetTooltip;
import td.ui.render.TooltipKey;
import td.util.GameHost;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.wave.PathNormal;
import td.wave.Vec2;

import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The wave preview: each enemy type with its count, drawn as real mobs. They live in a throwaway
 * {@link GameWorld} with a {@link GameHost#noOp()} host, so they can never affect the game.
 *
 * <p>Each type has a slot: the mob over its count. Slots keep their full width while they fit and all
 * shrink evenly once they don't, so the strip's height never depends on how many types it shows.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelEnemy extends JPanel {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final int SLOT_WIDTH = 32;
    private static final int MOB_SIZE = 24;
    /** The board cell a full slot previews its mob on: a body is a fraction of a cell, so this sets the art's size. */
    private static final int CELL_SIZE = 40;
    /** How many body scales a mob spans from its feet to the top of its rank badge. */
    private static final float BADGE_REACH = 3f;
    private static final int BASELINE_GAP = 1;
    private static final int COUNT_HEIGHT = 10;
    static final int HEIGHT = MOB_SIZE + COUNT_HEIGHT;

    private final List<EnemyMob> enemies = new CopyOnWriteArrayList<>();
    private final List<EnemyDefinition> definitions = new ArrayList<>();
    private final List<Rank> ranks = new ArrayList<>();
    private final Java2DFrameRenderer frameRenderer = new Java2DFrameRenderer();
    private final GameWorld contextLocal;
    private List<Integer> enemiesCount = new ArrayList<>();
    private int slotWidth = 0;
    private int gameTime = 0;

    public PanelEnemy() {
        initComponents();
        this.contextLocal = new GameWorld(GameHost.noOp());
        this.contextLocal.setPath(new PathNormal(List.of()));
    }

    public void clearEnemies() {
        this.definitions.clear();
        this.ranks.clear();
        this.enemies.clear();
        this.enemiesCount = new ArrayList<>();
        this.contextLocal.setPath(new PathNormal(List.of()));
    }

    public void addEnemy(EnemyDefinition definition, int count, Rank rank) {
        this.definitions.add(definition);
        this.ranks.add(rank);
        this.enemiesCount.add(count);
    }

    /** One slot wide: the strip takes the width its row gives it, so a long wave never widens the console. */
    public Dimension getPreferredSize() {
        return new Dimension(SLOT_WIDTH, HEIGHT);
    }

    public Dimension getMinimumSize() {
        return new Dimension(0, HEIGHT);
    }

    public void recalculateSize() {
        int types = Math.max(1, this.definitions.size());
        this.slotWidth = Math.min(SLOT_WIDTH, this.getWidth() / types);
        rebuildEnemies();
    }

    /**
     * Builds the preview mobs once the final scale is known, since a mob's position and body size are fixed at
     * construction.
     */
    private void rebuildEnemies() {
        this.enemies.clear();
        int cell = this.slotWidth * CELL_SIZE / SLOT_WIDTH;
        float room = this.slotWidth * MOB_SIZE / (float) (SLOT_WIDTH * BADGE_REACH);
        for (int nr = 0; nr < this.definitions.size(); nr++) {
            EnemyDefinition definition = this.definitions.get(nr);
            Rank rank = this.ranks.get(nr);
            this.contextLocal.setBoard(BoardGeometry.of(cell, 0, 0));
            float body = previewMob(definition, rank, this.contextLocal).getBodyScale();
            // A body grows with rank and wears its badge above it; past the mob line the badge is cut off, so it stops there.
            if (body > room) {
                this.contextLocal.setBoard(BoardGeometry.of(Math.max(1, Math.round(cell * room / body)), 0, 0));
                body = room;
            }
            this.contextLocal.setPath(new PathNormal(List.of(new Vec2(this.slotWidth * nr + this.slotWidth / 2f,
                    MOB_SIZE - BASELINE_GAP - body))));
            EnemyMob enemy = previewMob(definition, rank, this.contextLocal);
            enemy.doTick(0);
            this.enemies.add(enemy);
        }
    }

    /**
     * A preview mob with the health and bounty a spawn of {@code definition} starts with, so its
     * hover text reads like the real mob's; health-dependent stats such as a hurt-speed factor
     * need a non-zero maximum.
     */
    static DefinedEnemyMob previewMob(EnemyDefinition definition, Rank rank, GameWorld world) {
        SpawnParameters spawnParameters = SpawnParameters.atSlot(0, definition.baseSpeed(), definition.baseHealth(),
                definition.price());
        return new DefinedEnemyMob(definition, world, spawnParameters, rank);
    }

    public void doTick(int gameTime) {
        this.gameTime = gameTime;
        this.repaint();
    }

    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Hud.BACKGROUND);
        g2.fillRect(0, 0, this.getWidth(), this.getHeight());

        EnemyFrameBuilder frameBuilder = new EnemyFrameBuilder(this.gameTime, 1.0);
        for (EnemyMob e : this.enemies) {
            e.accept(frameBuilder);
        }
        this.frameRenderer.paintEnemies(g2, frameBuilder.build());

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Hud.FOREGROUND);
        g2.setFont(Hud.PRICE_FONT);
        FontMetrics metrics = g2.getFontMetrics();
        int baseline = MOB_SIZE + (COUNT_HEIGHT - metrics.getAscent() - metrics.getDescent()) / 2 + metrics.getAscent();
        for (int nr = 0; nr < this.enemies.size(); nr++) {
            String count = "×" + this.enemiesCount.get(nr);
            g2.drawString(count, this.slotWidth * nr + (this.slotWidth - metrics.stringWidth(count)) / 2f, baseline);
        }
    }

    /** The enemy's slot under {@code point} and the tooltip it opens; empty past the last one. */
    Optional<TooltipSpot> spotAt(Point point) {
        // Zero until the first layout gives the strip a width.
        if (this.slotWidth == 0 || point.x < 0) {
            return Optional.empty();
        }
        int nr = point.x / this.slotWidth;
        if (nr >= this.enemies.size()) {
            return Optional.empty();
        }
        TooltipKey key = TooltipKey.of("preview", nr + ":" + this.definitions.get(nr).id());
        return Optional.of(new TooltipSpot(new SheetTooltip(key, this.enemies.get(nr).accept(new EnemyInfoText())),
                new Rectangle(this.slotWidth * nr, 0, this.slotWidth, this.getHeight())));
    }

    private void initComponents() {

        setLayout(null);

        setBackground(Hud.BACKGROUND);
        setForeground(Hud.FOREGROUND);
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

    }

    private void formComponentResized(ComponentEvent evt) {
        this.recalculateSize();
    }

}
