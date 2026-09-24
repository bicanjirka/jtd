package td.ui;

import td.board.BoardGeometry;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.enemy.SpawnParameters;
import td.util.GameHost;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.wave.PathNormal;
import td.wave.Vec2;

import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The wave preview: each enemy type with its count, drawn as real mobs. They live in a throwaway
 * {@link GameWorld} with a {@link GameHost#noOp()} host, so they can never affect the game.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelEnemy extends JPanel {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<EnemyMob> enemies = new CopyOnWriteArrayList<>();
    private final List<EnemyDefinition> definitions = new ArrayList<>();
    private final List<Rank> ranks = new ArrayList<>();
    private final Java2DFrameRenderer frameRenderer = new Java2DFrameRenderer();
    private final GameWorld contextLocal;
    private List<Integer> enemiesCount = new ArrayList<>();
    private Font font;
    private GameWorld contextFull;
    private int pHeight = 0;
    private int pWidth = 0;
    private int scale = 32;
    private int gameTime = 0;

    public PanelEnemy() {
        initComponents();
        this.contextLocal = new GameWorld(GameHost.noOp());
        this.contextLocal.setPath(new PathNormal(List.of()));
    }

    public void setGameWorld(GameWorld c) {
        this.contextFull = c;
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

    public Dimension getPreferredSize() {
        return new Dimension(195, 40);
    }

    public void recalculateSize() {
        this.pWidth = this.getWidth();
        if (!this.definitions.isEmpty()) {
            this.pHeight = Math.min(this.pWidth / this.definitions.size(), this.getHeight());
        } else {
            this.pHeight = this.getHeight();
        }
        this.scale = this.pHeight;
        this.contextLocal.setBoard(BoardGeometry.of(this.scale, 0, 0));
        this.font = new Font(Font.DIALOG, Font.PLAIN, (int) (0.30 * this.scale));
        rebuildEnemies();
    }

    /**
     * Builds the preview mobs once the final scale is known, since a mob's position is fixed at
     * construction.
     */
    private void rebuildEnemies() {
        this.enemies.clear();
        for (int nr = 0; nr < this.definitions.size(); nr++) {
            this.contextLocal.setPath(new PathNormal(List.of(new Vec2(this.scale / 2 + this.scale * nr, this.pHeight / 2))));
            EnemyDefinition definition = this.definitions.get(nr);
            SpawnParameters spawnParameters = SpawnParameters.atSlot(0, definition.baseSpeed(), 0, 0);
            EnemyMob enemy = new DefinedEnemyMob(definition, this.contextLocal, spawnParameters, this.ranks.get(nr));
            enemy.doTick(0);
            this.enemies.add(enemy);
        }
    }

    public void doTick(int gameTime) {
        this.gameTime = gameTime;
        this.repaint();
    }

    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, this.pWidth, this.pHeight);

        EnemyFrameBuilder frameBuilder = new EnemyFrameBuilder(this.gameTime, 1.0);
        for (EnemyMob e : this.enemies) {
            e.accept(frameBuilder);
        }
        this.frameRenderer.paintEnemies(g2, frameBuilder.build());

        g2.setColor(Color.PINK);
        g2.setFont(this.font);
        int nr = 0;
        for (EnemyMob ignored : this.enemies) {
            g2.drawString("" + this.enemiesCount.get(nr), this.scale * nr, this.pHeight);
            nr++;
        }
    }

    private void mouseOver(int x) {
        int nr = x / this.scale;
        if (nr < this.enemies.size()) {
            EnemyMob e = this.enemies.get(nr);
            this.contextFull.setInfoText(e.getInfoString());
        }
    }

    private void initComponents() {

        setLayout(null);

        setBackground(new Color(0, 0, 0));
        setForeground(new Color(255, 255, 255));
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent evt) {
                formMouseMoved(evt);
            }
        });

    }

    private void formMouseMoved(MouseEvent evt) {
        this.mouseOver(evt.getX());
    }

    private void formComponentResized(ComponentEvent evt) {
        this.recalculateSize();
    }

}
