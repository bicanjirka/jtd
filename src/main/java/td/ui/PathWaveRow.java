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
 * One path's part of a round: a swatch in the path's colour and a {@link PanelEnemy} strip of what
 * it spawns.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
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
     * Takes the colour from the installed level by the wave's path index, the one source of a
     * path's colour. With a single path the swatch distinguishes nothing, so it is hidden.
     */
    void setWave(Wave wave) {
        if (this.gameWorld != null && wave.getPathIndex() < this.gameWorld.level().pathCount()) {
            this.swatch.color = this.gameWorld.level().paths().get(wave.getPathIndex()).color();
            this.swatch.setVisible(this.gameWorld.level().pathCount() > 1);
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

    /** A chevron in the path's colour, drawn like the board's path markers. */
    @ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
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
