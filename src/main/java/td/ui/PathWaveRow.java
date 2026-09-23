package td.ui;

import td.enemy.EnemyDefinition;
import td.ui.render.PathMarkerBrightness;
import td.ui.render.PathMarkerDraw;
import td.ui.render.PathMarkerShape;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.wave.PathColor;
import td.wave.Wave;

import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.Serial;
import java.util.List;

/**
 * One path's summary within a round: a small chevron marking the path's color, and the
 * {@link PanelEnemy} strip showing what it spawns - each preview mob its own resolved health,
 * bounty and rank badge (the badge lands in a later pass; see {@code td/ui/CLAUDE.md}), since
 * those numbers are no longer wave-uniform the way they were before enemies carried their own
 * per-rank stats. One of these is stacked per path under {@link PanelWaveInfo}'s current/next
 * side, in path order, so a player can see at a glance which enemies come down which lane.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
// Swing components, assigned once by initComponents
final class PathWaveRow extends JPanel {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final int SWATCH_WIDTH = 10;
    private static final int SWATCH_HEIGHT = 16;

    private final Swatch swatch = new Swatch();
    private final PanelEnemy panelEnemy = new PanelEnemy();
    private GameWorld gameWorld;

    PathWaveRow() {
        initComponents();
    }

    void setGameWorld(GameWorld world) {
        this.gameWorld = world;
        this.panelEnemy.setGameWorld(world);
    }

    /**
     * Resolves this row's swatch color from the wave's own {@link Wave#getPathIndex()} against
     * the currently installed level, rather than taking a {@code PathColor} directly - the one
     * source of truth for a path's color is {@code GameWorld.level().paths()}.
     */
    void setWave(Wave wave) {
        if (this.gameWorld != null && wave.getPathIndex() < this.gameWorld.level().pathCount()) {
            this.swatch.color = this.gameWorld.level().paths().get(wave.getPathIndex()).color();
            this.swatch.repaint();
        }
        this.panelEnemy.clearEnemies();
        for (EnemyDefinition e : wave.enemySet()) {
            this.panelEnemy.addEnemy(e, wave.enemyCount(e), wave.rankFor(e));
        }
        this.panelEnemy.recalculateSize();
    }

    void doTick(int gameTime) {
        this.panelEnemy.doTick(gameTime);
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBackground(Color.BLACK);
        setFocusable(false);

        this.swatch.setPreferredSize(new Dimension(SWATCH_WIDTH, SWATCH_HEIGHT));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, 5);
        add(this.swatch, c);

        this.panelEnemy.setMinimumSize(new Dimension(30, 30));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.01;
        c.insets = new Insets(2, 0, 2, 0);
        add(this.panelEnemy, c);
    }

    /**
     * A moving-styled chevron in the row's path color - the same marker shape the board's own
     * path trail uses, reused here through {@link Java2DFrameRenderer#paintPathMarkers} rather
     * than a second, hand-drawn visual language for "which lane is this."
     */
    @ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
    // painted only from the EDT render pulse, like every other Panel* component
    private static final class Swatch extends JPanel {
        @Serial
        private static final long serialVersionUID = 1L;
        private final Java2DFrameRenderer renderer = new Java2DFrameRenderer();
        private PathColor color = PathColor.DEFAULT;

        Swatch() {
            setOpaque(false);
            setFocusable(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float size = Math.min(getWidth(), getHeight()) * 0.6f;
            PathMarkerDraw chevron = new PathMarkerDraw(PathMarkerShape.CHEVRON, PathMarkerBrightness.MOVING, this.color,
                    getWidth() / 2f, getHeight() / 2f, 0.0, size);
            this.renderer.paintPathMarkers(g2, List.of(chevron));
            g2.dispose();
        }
    }
}
