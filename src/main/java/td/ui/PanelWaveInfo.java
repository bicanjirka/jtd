package td.ui;

import td.util.GameWorld;
import td.util.ThreadConfined;
import td.wave.Wave;

import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Side-by-side summaries of the round in play and the one queued next. Each side stacks one
 * {@link PathWaveRow} per path, in path order - a round starts every path's wave together (see
 * {@code td/wave/CLAUDE.md}'s round model), so this panel shows one row per path rather than
 * one wave. Row count is rebuilt only when the number of paths actually changes (a new level
 * loading), not per frame - see this package's "built once, refreshed in place" convention.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class PanelWaveInfo extends JPanel {
    @Serial
    private static final long serialVersionUID = 1L;

    private final JLabel curRoundLabel = new JLabel("-");
    private final JLabel nextRoundLabel = new JLabel("-");
    private final JPanel curSide = new JPanel();
    private final JPanel nextSide = new JPanel();
    private final JPanel curSection = new JPanel();
    private final JPanel nextSection = new JPanel();
    private final List<PathWaveRow> curRows = new ArrayList<>();
    private final List<PathWaveRow> nextRows = new ArrayList<>();
    private GameWorld gameWorld;

    public PanelWaveInfo() {
        initComponents();
    }

    public void setGameWorld(GameWorld c) {
        this.gameWorld = c;
        for (PathWaveRow row : this.curRows) {
            row.setGameWorld(c);
        }
        for (PathWaveRow row : this.nextRows) {
            row.setGameWorld(c);
        }
    }

    public void clearWaves() {
        this.curRoundLabel.setText("-");
        this.nextRoundLabel.setText("-");
        resize(this.curSide, this.curRows, 0);
        resize(this.nextSide, this.nextRows, 0);
    }

    public void setWaveCur(int round, List<Wave> waves) {
        this.curRoundLabel.setText("" + round);
        setSide(this.curSide, this.curRows, waves);
    }

    public void setWaveNext(int round, List<Wave> waves) {
        this.nextRoundLabel.setText("" + round);
        setSide(this.nextSide, this.nextRows, waves);
    }

    public void doTick(int gameTime) {
        for (PathWaveRow row : this.curRows) {
            row.doTick(gameTime);
        }
        for (PathWaveRow row : this.nextRows) {
            row.doTick(gameTime);
        }
    }

    private void setSide(JPanel container, List<PathWaveRow> rows, List<Wave> waves) {
        resize(container, rows, waves.size());
        for (int i = 0; i < waves.size(); i++) {
            PathWaveRow row = rows.get(i);
            row.setGameWorld(this.gameWorld);
            row.setWave(waves.get(i));
        }
        container.revalidate();
        container.repaint();
    }

    /**
     * Rebuilds a side's rows only when the path count actually changed - a no-op on every
     * ordinary wave start, which just refreshes the existing rows' content in {@link #setSide}.
     */
    private void resize(JPanel container, List<PathWaveRow> rows, int count) {
        if (rows.size() == count) {
            return;
        }
        container.removeAll();
        rows.clear();
        for (int i = 0; i < count; i++) {
            PathWaveRow row = new PathWaveRow();
            row.setGameWorld(this.gameWorld);
            rows.add(row);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = i;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            c.insets = new Insets(i == 0 ? 0 : 4, 0, 0, 0);
            container.add(row, c);
        }
        container.revalidate();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBackground(new Color(0, 0, 0));
        setBorder(Hud.panelBorder("Current & Next Wave"));
        setForeground(new Color(220, 255, 220));

        buildSection(this.curSection, this.curRoundLabel, this.curSide, 0);
        buildSection(this.nextSection, this.nextRoundLabel, this.nextSide, 1);
    }

    /**
     * One bordered section - a round-number heading over its column of {@link PathWaveRow}s -
     * nested inside this panel's outer "Current & Next Wave" border, mirroring the
     * {@code jPanel_gameInfo}/{@code jPanel_gameButtons} nesting {@code PanelGameConsole} already
     * uses. Untitled ({@link Hud#outlineBorder()}) since the round label already headings the
     * section; a second title would just repeat it.
     */
    private void buildSection(JPanel section, JLabel roundLabel, JPanel side, int row) {
        section.setLayout(new GridBagLayout());
        section.setBackground(new Color(0, 0, 0));
        section.setBorder(Hud.outlineBorder());

        roundLabel.setForeground(new Color(220, 255, 220));
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(0, 0, 2, 0);
        section.add(roundLabel, labelConstraints);

        side.setLayout(new GridBagLayout());
        side.setBackground(new Color(0, 0, 0));
        GridBagConstraints sideConstraints = new GridBagConstraints();
        sideConstraints.gridx = 0;
        sideConstraints.gridy = 1;
        sideConstraints.fill = GridBagConstraints.HORIZONTAL;
        sideConstraints.weightx = 1.0;
        section.add(side, sideConstraints);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(row == 0 ? 0 : 6, 0, 0, 0);
        add(section, c);
    }
}
