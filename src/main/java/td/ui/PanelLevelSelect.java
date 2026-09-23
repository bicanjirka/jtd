package td.ui;

import td.level.LevelDefinition;
import td.wave.PathBuilder;
import td.wave.PathColor;
import td.wave.PathDefinition;
import td.wave.Vec2;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The landing screen shown before any level is loaded: one clickable card per level, with a
 * small vector preview of its path, its stats (waves/credits/lives) and a decorative watermark
 * echoing the game's own flat-neon-on-black shape language. Presentation only - clicking a card
 * just invokes the callback given at construction, leaving it to {@link td.TowerDefense} to
 * actually load and start that level.
 */
public class PanelLevelSelect extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Color BACKGROUND = new Color(2, 3, 2);
    private static final Color FOREGROUND = new Color(220, 255, 220);
    private static final Color MUTED_FOREGROUND = new Color(120, 150, 125);
    private static final Color SUBTITLE_FOREGROUND = new Color(90, 120, 95);
    private static final Color CARD_BACKGROUND = new Color(12, 13, 12);
    private static final Color CARD_HOVER_BACKGROUND = new Color(21, 24, 21);
    private static final Color STAT_BORDER = new Color(42, 46, 42);
    private static final Color STAT_LABEL = new Color(106, 128, 110);
    private static final Color PREVIEW_BACKGROUND = new Color(6, 7, 6);
    private static final Color PREVIEW_GRID_DOT = new Color(22, 24, 22);

    /**
     * Cycled by card index so any number of catalog levels each get a distinct accent.
     */
    private static final Color[] ACCENTS = {Color.GREEN, Color.RED, Color.ORANGE};

    public PanelLevelSelect(List<LevelDefinition> levels, Consumer<LevelDefinition> onLevelSelected) {
        this.setLayout(new BorderLayout());
        this.setBackground(BACKGROUND);
        this.setBorder(new EmptyBorder(32, 32, 28, 32));

        JLabel title = new JLabel("TOWER DEFENSE", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setForeground(FOREGROUND);

        JLabel subtitle = new JLabel("SELECT YOUR LEVEL", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(SUBTITLE_FOREGROUND);
        subtitle.setBorder(new EmptyBorder(6, 0, 24, 0));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(title);
        header.add(subtitle);
        this.add(header, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(0, Math.max(1, Math.min(levels.size(), 3)), 24, 24));
        cards.setOpaque(false);
        for (int i = 0; i < levels.size(); i++) {
            cards.add(buildCard(levels.get(i), i, onLevelSelected));
        }
        this.add(cards, BorderLayout.CENTER);
    }

    private static JPanel buildCard(LevelDefinition level, int index, Consumer<LevelDefinition> onLevelSelected) {
        Color accent = ACCENTS[index % ACCENTS.length];

        JPanel card = new JPanel(new BorderLayout()) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintWatermark((Graphics2D) g, index, accent, this.getWidth(), this.getHeight());
            }
        };
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createLineBorder(accent, 2));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        PathPreview preview = new PathPreview(level, accent);
        card.add(preview, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 20, 16, 20));

        JLabel name = new JLabel(level.name());
        name.setFont(new Font("SansSerif", Font.BOLD, 19));
        name.setForeground(accent);
        name.setBorder(new EmptyBorder(0, 0, 8, 0));
        body.add(name, BorderLayout.NORTH);

        JTextArea description = new JTextArea(level.description());
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setEditable(false);
        description.setFocusable(false);
        description.setOpaque(false);
        description.setForeground(MUTED_FOREGROUND);
        description.setFont(new Font("SansSerif", Font.PLAIN, 13));
        body.add(description, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.setOpaque(false);

        JPanel stats = new JPanel(new GridLayout(1, 3, 10, 0));
        stats.setOpaque(false);
        stats.setBorder(new EmptyBorder(16, 0, 12, 0));
        stats.add(buildStat("WAVES", String.valueOf(level.paths().getFirst().waves().size()), accent));
        stats.add(buildStat("CREDITS", "$" + level.startingCredits(), accent));
        stats.add(buildStat("LIVES", String.valueOf(level.startingLives()), accent));
        south.add(stats);

        JLabel prompt = new JLabel("CLICK TO DEPLOY", SwingConstants.CENTER);
        prompt.setFont(new Font("SansSerif", Font.BOLD, 11));
        prompt.setForeground(SUBTITLE_FOREGROUND);
        prompt.setAlignmentX(Component.CENTER_ALIGNMENT);
        south.add(prompt);

        body.add(south, BorderLayout.SOUTH);
        card.add(body, BorderLayout.CENTER);

        MouseListener interaction = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onLevelSelected.accept(level);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(CARD_HOVER_BACKGROUND);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(CARD_BACKGROUND);
            }
        };
        attachToWholeCard(card, interaction);

        return card;
    }

    /**
     * A card is many nested components (preview, labels, stat boxes); AWT delivers a mouse
     * event to whichever leaf sits under the pointer, not to its ancestors, so a listener on
     * {@code card} alone would miss clicks/hovers landing on any child. Attaching the same
     * listener to every descendant makes the whole card act as one clickable, hoverable unit.
     */
    private static void attachToWholeCard(JComponent component, MouseListener listener) {
        component.addMouseListener(listener);
        for (int i = 0; i < component.getComponentCount(); i++) {
            attachToWholeCard((JComponent) component.getComponent(i), listener);
        }
    }

    private static JPanel buildStat(String label, String value, Color accent) {
        JPanel stat = new JPanel();
        stat.setLayout(new BoxLayout(stat, BoxLayout.Y_AXIS));
        stat.setOpaque(false);
        stat.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(STAT_BORDER, 1),
                new EmptyBorder(8, 4, 8, 4)));

        JLabel labelText = new JLabel(label, SwingConstants.CENTER);
        labelText.setFont(new Font("SansSerif", Font.BOLD, 9));
        labelText.setForeground(STAT_LABEL);
        labelText.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelText.setBorder(new EmptyBorder(0, 0, 6, 0));

        JLabel valueText = new JLabel(value, SwingConstants.CENTER);
        valueText.setFont(new Font("SansSerif", Font.BOLD, 18));
        valueText.setForeground(accent);
        valueText.setAlignmentX(Component.CENTER_ALIGNMENT);

        stat.add(labelText);
        stat.add(valueText);
        return stat;
    }

    private static void paintWatermark(Graphics2D g2, int index, Color accent, int width, int height) {
        Graphics2D watermark = (Graphics2D) g2.create();
        watermark.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float size = Math.min(width, height) * 0.6f;
        AffineTransform transform = AffineTransform.getTranslateInstance(width - size * 0.35, size * 0.1);
        Color faded = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18);
        watermark.setColor(faded);

        switch (index % 3) {
            case 0 -> {
                transform.rotate(Math.toRadians(10));
                watermark.setStroke(new BasicStroke(4f));
                watermark.draw(transform.createTransformedShape(diamondShape(size)));
            }
            case 1 -> watermark.fill(transform.createTransformedShape(ringShape(size)));
            default -> {
                transform.rotate(Math.toRadians(-8));
                watermark.setStroke(new BasicStroke(3.5f));
                watermark.draw(transform.createTransformedShape(starShape(size)));
            }
        }
        watermark.dispose();
    }

    private static Shape diamondShape(float size) {
        float r = size / 2f;
        Path2D.Float diamond = new Path2D.Float();
        diamond.moveTo(r, 0);
        diamond.lineTo(r * 2, r);
        diamond.lineTo(r, r * 2);
        diamond.lineTo(0, r);
        diamond.closePath();
        return diamond;
    }

    private static Shape ringShape(float size) {
        float r = size / 2f;
        float thickness = size * 0.13f;
        Area outer = new Area(new Ellipse2D.Float(0, 0, size, size));
        Area inner = new Area(new Ellipse2D.Float(thickness, thickness, size - thickness * 2, size - thickness * 2));
        outer.subtract(inner);
        return outer;
    }

    private static Shape starShape(float size) {
        float r = size / 2f;
        float outerR = r;
        float innerR = r * 0.4f;
        Path2D.Float star = new Path2D.Float();
        for (int i = 0; i < 10; i++) {
            double angle = Math.toRadians(-90 + i * 36);
            float radius = (i % 2 == 0) ? outerR : innerR;
            float x = r + radius * (float) Math.cos(angle);
            float y = r + radius * (float) Math.sin(angle);
            if (i == 0) {
                star.moveTo(x, y);
            } else {
                star.lineTo(x, y);
            }
        }
        star.closePath();
        return star;
    }

    /**
     * A small, purely decorative sketch of a level's real (possibly smoothed) path.
     */
    private static final class PathPreview extends JComponent {

        @Serial
        private static final long serialVersionUID = 1L;

        private final LevelDefinition level;
        private final Color accent;

        PathPreview(LevelDefinition level, Color accent) {
            this.level = level;
            this.accent = accent;
            this.setOpaque(true);
            this.setPreferredSize(new Dimension(200, 150));
        }

        /**
         * The bounds of every path's points together, not one path in isolation - drawing two
         * differently-routed paths through independently-computed bounds would scale each one
         * into the same box at a different scale, which draws them in two incompatible
         * coordinate systems on top of each other.
         */
        private static double[] sharedBounds(List<List<Vec2>> everyPathsPoints) {
            double minX = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            for (List<Vec2> points : everyPathsPoints) {
                for (Vec2 p : points) {
                    minX = Math.min(minX, p.x());
                    maxX = Math.max(maxX, p.x());
                    minY = Math.min(minY, p.y());
                    maxY = Math.max(maxY, p.y());
                }
            }
            return new double[] {minX, maxX, minY, maxY};
        }

        private static List<Point2D.Float> mapToBounds(List<Vec2> points, double[] bounds, int width, int height, int margin) {
            if (points.isEmpty()) {
                return List.of();
            }
            double minX = bounds[0];
            double spanX = Math.max(1, bounds[1] - bounds[0]);
            double minY = bounds[2];
            double spanY = Math.max(1, bounds[3] - bounds[2]);
            float innerWidth = width - margin * 2f;
            float innerHeight = height - margin * 2f;

            // One uniform scale for both axes, not spanX/spanY mapped independently - an
            // independent mapping stretches the path into the box's aspect ratio instead of
            // preserving its own. The smaller of the two candidate scales is the one that keeps
            // the whole path inside the box; the leftover space on the other axis centers it
            // rather than leaving it pinned to the top-left corner.
            double scale = Math.min(innerWidth / spanX, innerHeight / spanY);
            float offsetX = margin + (float) (innerWidth - spanX * scale) / 2f;
            float offsetY = margin + (float) (innerHeight - spanY * scale) / 2f;

            ArrayList<Point2D.Float> mapped = new ArrayList<>(points.size());
            for (Vec2 p : points) {
                float x = offsetX + (float) ((p.x() - minX) * scale);
                float y = offsetY + (float) ((p.y() - minY) * scale);
                mapped.add(new Point2D.Float(x, y));
            }
            return mapped;
        }

        private static Shape arrowShape(float tipX, float tipY, double angle) {
            AffineTransform transform = AffineTransform.getTranslateInstance(tipX, tipY);
            transform.rotate(angle);
            Path2D.Float arrow = new Path2D.Float();
            arrow.moveTo(6, 0);
            arrow.lineTo(-4, -5);
            arrow.lineTo(-4, 5);
            arrow.closePath();
            return transform.createTransformedShape(arrow);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = this.getWidth();
            int height = this.getHeight();

            g2.setColor(PREVIEW_BACKGROUND);
            g2.fillRect(0, 0, width, height);

            g2.setColor(PREVIEW_GRID_DOT);
            for (int x = 6; x < width; x += 20) {
                for (int y = 6; y < height; y += 20) {
                    g2.fillRect(x, y, 1, 1);
                }
            }

            List<PathDefinition> paths = this.level.paths();
            List<List<Vec2>> everyPathsPoints = paths.stream()
                    .map(path -> PathBuilder.build(path.corners(), path.smoothing(), 1).points())
                    .toList();
            double[] bounds = sharedBounds(everyPathsPoints);

            for (int i = 0; i < paths.size(); i++) {
                List<Point2D.Float> mapped = mapToBounds(everyPathsPoints.get(i), bounds, width, height, 14);
                if (mapped.size() < 2) {
                    continue;
                }
                // A path that never called withColor stays PathColor.DEFAULT (white), which
                // would look flat against every level's own themed accent - falling back to the
                // card's accent there preserves today's single-path look exactly, and only a
                // path with a real, authored color draws in that color instead.
                PathColor pathColor = paths.get(i).color();
                Color strokeColor = pathColor.equals(PathColor.DEFAULT)
                        ? this.accent
                        : new Color(pathColor.r(), pathColor.g(), pathColor.b());

                GeneralPath outline = new GeneralPath();
                outline.moveTo(mapped.getFirst().x, mapped.getFirst().y);
                for (int p = 1; p < mapped.size(); p++) {
                    outline.lineTo(mapped.get(p).x, mapped.get(p).y);
                }
                g2.setColor(new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), 150));
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(outline);

                Point2D.Float start = mapped.getFirst();
                g2.setColor(FOREGROUND);
                g2.fillRect(Math.round(start.x - 4), Math.round(start.y - 4), 8, 8);

                Point2D.Float end = mapped.getLast();
                Point2D.Float beforeEnd = mapped.get(Math.max(0, mapped.size() - 2));
                double angle = Math.atan2(end.y - beforeEnd.y, end.x - beforeEnd.x);
                g2.setColor(strokeColor);
                g2.fill(arrowShape(end.x, end.y, angle));
            }

            g2.dispose();
        }
    }
}
