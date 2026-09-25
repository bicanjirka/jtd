package td.ui;

import td.ui.render.InfoSheet;
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
import java.util.function.Consumer;

/**
 * The round in play and the next one side by side, each with one {@link PathWaveRow} per path. Rows
 * are rebuilt only when the path count changes.
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
    private Consumer<InfoSheet> onEnemyHover = sheet -> {
    };

    public PanelWaveInfo() {
        initComponents();
    }

    /** Called with the sheet of a preview enemy under the pointer. */
    public void onEnemyHover(Consumer<InfoSheet> listener) {
        this.onEnemyHover = listener;
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

    /** Rebuilds a side's rows only when the path count changed. */
    private void resize(JPanel container, List<PathWaveRow> rows, int count) {
        if (rows.size() == count) {
            return;
        }
        container.removeAll();
        rows.clear();
        for (int i = 0; i < count; i++) {
            PathWaveRow row = new PathWaveRow(sheet -> this.onEnemyHover.accept(sheet));
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
     * One bordered section: a round heading over its rows. Untitled, since the heading already
     * names it.
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
