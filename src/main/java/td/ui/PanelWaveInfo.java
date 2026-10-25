package td.ui;

import td.level.LevelOutcome;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.wave.Wave;
import td.wave.WaveProgress;

import javax.swing.JComponent;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The round in play over the next one, each a heading and one {@link PathWaveRow} per path. Between rounds the
 * round in play gives way to how it ended and the prompt to send the next. Rows are rebuilt only when the path count
 * changes.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelWaveInfo extends JPanel {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final int ROW_GAP = 3;
    private static final int RULE_GAP = 5;

    private final HeadingLine nowHeading = new HeadingLine("NOW");
    private final JPanel nowSide = new JPanel();
    private final SendLine sendLine = new SendLine();
    private final JPanel nextSection = new JPanel();
    private final HeadingLine nextHeading = new HeadingLine("NEXT");
    private final JPanel nextSide = new JPanel();
    private final List<PathWaveRow> nowRows = new ArrayList<>();
    private final List<PathWaveRow> nextRows = new ArrayList<>();
    private GameWorld gameWorld;
    private TooltipLayer tooltips;

    public PanelWaveInfo() {
        initComponents();
        clearWaves();
    }

    /** The layer the preview enemies' tooltips open on, for the rows there are and those still to come. */
    public void setTooltips(TooltipLayer layer) {
        this.tooltips = layer;
        this.nowRows.forEach(row -> row.setTooltips(layer));
        this.nextRows.forEach(row -> row.setTooltips(layer));
    }

    public void setGameWorld(GameWorld c) {
        this.gameWorld = c;
        this.nowRows.forEach(row -> row.setGameWorld(c));
        this.nextRows.forEach(row -> row.setGameWorld(c));
    }

    public void clearWaves() {
        show(WaveProgress.none(), true, LevelOutcome.PLAYING);
    }

    public void show(WaveProgress progress, boolean waveReady, LevelOutcome outcome) {
        WavesReadout readout = WavesReadout.of(progress, waveReady, outcome);
        this.nowHeading.show(readout.now());
        setSide(this.nowSide, this.nowRows, readout.now().waves());
        this.sendLine.show(readout.send());
        this.nextSection.setVisible(readout.next().isPresent());
        readout.next().ifPresent(this.nextHeading::show);
        setSide(this.nextSide, this.nextRows, readout.next().map(WavesReadout.Heading::waves).orElse(List.of()));
        revalidate();
        repaint();
    }

    public void doTick(int gameTime) {
        this.nowRows.forEach(row -> row.doTick(gameTime));
        this.nextRows.forEach(row -> row.doTick(gameTime));
    }

    private void setSide(JPanel container, List<PathWaveRow> rows, List<Wave> waves) {
        resize(container, rows, waves.size());
        for (int i = 0; i < waves.size(); i++) {
            PathWaveRow row = rows.get(i);
            row.setGameWorld(this.gameWorld);
            row.setWave(waves.get(i));
        }
        container.setVisible(!waves.isEmpty());
    }

    /** Rebuilds a side's rows only when the path count changed. */
    private void resize(JPanel container, List<PathWaveRow> rows, int count) {
        if (rows.size() == count) {
            return;
        }
        container.removeAll();
        rows.clear();
        for (int i = 0; i < count; i++) {
            PathWaveRow row = new PathWaveRow();
            row.setGameWorld(this.gameWorld);
            if (this.tooltips != null) {
                row.setTooltips(this.tooltips);
            }
            rows.add(row);
            container.add(row, stacked(i, ROW_GAP));
        }
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBackground(Hud.BACKGROUND);
        setBorder(Hud.panelBorder("Waves"));

        this.nowSide.setLayout(new GridBagLayout());
        this.nowSide.setBackground(Hud.BACKGROUND);
        this.nextSide.setLayout(new GridBagLayout());
        this.nextSide.setBackground(Hud.BACKGROUND);

        this.nextSection.setLayout(new GridBagLayout());
        this.nextSection.setBackground(Hud.BACKGROUND);
        this.nextSection.setBorder(Hud.ruleAboveBorder(RULE_GAP));
        this.nextSection.add(this.nextHeading, stacked(0, 0));
        this.nextSection.add(this.nextSide, stacked(1, 0));

        add(this.nowHeading, stacked(0, 0));
        add(this.nowSide, stacked(1, 0));
        add(this.sendLine, stacked(2, 0));
        add(this.nextSection, stacked(3, RULE_GAP));
    }

    /** Row {@code y} of a column, full width, {@code gap} below the row above it. */
    private static GridBagConstraints stacked(int y, int gap) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(y == 0 ? 0 : gap, 0, 0, 0);
        return c;
    }

    private static int baseline(FontMetrics metrics, int height) {
        return (height - metrics.getAscent() - metrics.getDescent()) / 2 + metrics.getAscent();
    }

    /** A round's heading: its skull and number, or what the panel waits for, with a caption at the right. */
    @ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
    private static final class HeadingLine extends JComponent {
        @Serial
        private static final long serialVersionUID = 1L;
        private static final int HEIGHT = 16;
        private static final int SKULL_SIZE = 13;
        private static final int GAP = 4;
        private static final Font TITLE_FONT = Hud.LABEL_FONT.deriveFont(Font.BOLD);
        private static final Font CAPTION_FONT = Hud.CHIP_FONT.deriveFont(Map.of(TextAttribute.TRACKING, 0.12f));

        private final String caption;
        private WavesReadout.Heading heading = new WavesReadout.Heading(Optional.empty(), "", "", List.of());

        HeadingLine(String caption) {
            this.caption = caption;
        }

        void show(WavesReadout.Heading shown) {
            this.heading = shown;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(0, HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Hud.BACKGROUND);
            g2.fillRect(0, 0, getWidth(), getHeight());
            int x = 0;
            Optional<WavesReadout.Skull> skull = this.heading.skull();
            if (skull.isPresent()) {
                StatusIcon.paintStillSkull(g2, skull.get().kind(), skull.get().state(), 0,
                        (HEIGHT - SKULL_SIZE) / 2f, SKULL_SIZE);
                x = SKULL_SIZE + GAP;
            }
            g2.setFont(skull.isPresent() ? TITLE_FONT : Hud.LABEL_FONT);
            g2.setColor(skull.isPresent() ? Hud.FOREGROUND : Hud.TEXT_DIM);
            g2.drawString(this.heading.title(), x, baseline(g2.getFontMetrics(), HEIGHT));
            x += g2.getFontMetrics().stringWidth(this.heading.title()) + GAP;
            g2.setFont(Hud.LABEL_FONT);
            g2.setColor(suffixColor(skull));
            g2.drawString(this.heading.suffix(), x, baseline(g2.getFontMetrics(), HEIGHT));
            g2.setFont(CAPTION_FONT);
            g2.setColor(Hud.TEXT_DIM);
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(this.caption, getWidth() - metrics.stringWidth(this.caption), baseline(metrics, HEIGHT));
            g2.dispose();
        }

        /** How a round ended reads in the tone its cracked skull wears; a round count is dim. */
        private static Color suffixColor(Optional<WavesReadout.Skull> skull) {
            return skull.filter(s -> s.state().isOver())
                    .map(s -> Java2DFrameRenderer.colorFor(RoundSkulls.tone(s.kind(), s.state())))
                    .orElse(Hud.TEXT_DIM);
        }
    }

    /** The prompt to send the next round, with the chip of the key that sends it; not a control. */
    @ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
    private static final class SendLine extends JComponent {
        @Serial
        private static final long serialVersionUID = 1L;
        private static final int HEIGHT = 20;
        private static final int INDENT = HeadingLine.SKULL_SIZE + HeadingLine.GAP;
        private static final int GAP = 4;

        private String text = "";

        void show(Optional<String> prompt) {
            this.text = prompt.orElse("");
            setVisible(prompt.isPresent());
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(0, HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Hud.BACKGROUND);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(Hud.LABEL_FONT);
            g2.setColor(Hud.TEXT_DIM);
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(this.text, INDENT, baseline(metrics, HEIGHT));
            Hud.paintChip(g2, GameKey.PAUSE.chip(), INDENT + metrics.stringWidth(this.text) + GAP,
                    (HEIGHT - Hud.CHIP_SIZE) / 2f, Hud.CHIP_BORDER, Hud.FOREGROUND);
            g2.dispose();
        }
    }
}
