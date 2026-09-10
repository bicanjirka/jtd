package td.ui;

import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.util.GameWorld;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.Serial;

/**
 * The right-hand console: title, wave/lives/score/credits status, the four speed buttons,
 * and the tower-info/wave-preview panels. Registers itself as an {@link EconomyListener} on
 * {@link #setGameWorld}, so credits/lives/score stay current without TowerDefense pushing
 * them - only wave progress (not an economy value) still needs an explicit
 * {@link #setWaveProgress} push. Speed-button *behavior* stays in TowerDefense (tick speed
 * is a UI/EDT-only concern per CLAUDE.md, but deciding what a click does is still
 * TowerDefense's call) - this panel only owns the buttons existing and their play/pause
 * visibility.
 */
public class PanelGameConsole extends JPanel implements EconomyListener {
    @Serial
    private static final long serialVersionUID = 1L;

    private final PanelTowerInfo panelTowerInfo = new PanelTowerInfo();
    private final PanelWaveInfo panelWaveInfo = new PanelWaveInfo();

    private HudButton jButton_play;
    private HudButton jButton_pause;
    private HudButton jButton_fast;
    private HudButton jButton_superFast;
    private JLabel jLabel_wave;
    private JLabel jLabel_lives;
    private JLabel jLabel_score;
    private JLabel jLabel_credits;

    private Runnable onPlay = () -> {
    };
    private Runnable onPause = () -> {
    };
    private Runnable onFast = () -> {
    };
    private Runnable onSuperFast = () -> {
    };

    public PanelGameConsole(String titleText) {
        initComponents(titleText);
    }

    public PanelTowerInfo getTowerInfo() {
        return this.panelTowerInfo;
    }

    public PanelWaveInfo getWaveInfo() {
        return this.panelWaveInfo;
    }

    public void setGameWorld(GameWorld world) {
        world.addEconomyListener(this);
        this.panelTowerInfo.setGameWorld(world);
        this.panelWaveInfo.setGameWorld(world);
    }

    public void setWaveProgress(int current, int total) {
        this.jLabel_wave.setText("  " + current + "/" + total);
    }

    public void setPlaying(boolean playing) {
        this.jButton_play.setVisible(!playing);
        this.jButton_pause.setVisible(playing);
    }

    public void onPlay(Runnable r) {
        this.onPlay = r;
    }

    public void onPause(Runnable r) {
        this.onPause = r;
    }

    public void onFast(Runnable r) {
        this.onFast = r;
    }

    public void onSuperFast(Runnable r) {
        this.onSuperFast = r;
    }

    /** Also reachable from the game-loop thread - see GameWorld.apply()'s callers. */
    @Override
    public void economyChanged(EconomyState state) {
        SwingUtilities.invokeLater(() -> {
            this.jLabel_credits.setText("$" + state.credits());
            this.jLabel_lives.setText("" + state.lives());
            this.jLabel_score.setText("" + state.score());
        });
    }

    private static HudButton glyphButton(String glyph) {
        HudButton button = new HudButton(glyph);
        button.setFont(Hud.GLYPH_FONT);
        return button;
    }

    private void initComponents(String titleText) {
        GridBagConstraints gridBagConstraints;

        JLabel jLabel_name = new JLabel();
        JPanel jPanel_gameInfo = new JPanel();
        JLabel jLabel_waveText = new JLabel();
        JLabel jLabel_livesText = new JLabel();
        JLabel jLabel_scoreText = new JLabel();
        JLabel jLabel_creditsText = new JLabel();
        JPanel jPanel_gameButtons = new JPanel();
        // Glyphs rather than drawn icons: one character each, so they scale with the font and
        // look the same everywhere, and there is no artwork to keep in step with anything.
        this.jButton_play = glyphButton("►");        // BLACK RIGHT-POINTING POINTER
        this.jButton_pause = glyphButton("▮▮"); // two BLACK VERTICAL RECTANGLEs
        this.jButton_fast = glyphButton("►►");
        this.jButton_superFast = glyphButton("►►►");
        this.jLabel_wave = new JLabel();
        this.jLabel_lives = new JLabel();
        this.jLabel_score = new JLabel();
        this.jLabel_credits = new JLabel();

        this.setLayout(new GridBagLayout());
        this.setBackground(new Color(0, 0, 0));
        this.setFocusable(false);
        this.setMaximumSize(new Dimension(200, 2147483647));
        this.setMinimumSize(new Dimension(200, 263));
        this.setPreferredSize(new Dimension(200, 402));

        jLabel_name.setBackground(new Color(0, 0, 0));
        jLabel_name.setFont(new Font("Dialog", Font.BOLD, 16));
        jLabel_name.setForeground(new Color(220, 255, 220));
        jLabel_name.setText(titleText);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new Insets(4, 10, 4, 10);
        this.add(jLabel_name, gridBagConstraints);

        jPanel_gameInfo.setLayout(new GridBagLayout());

        jPanel_gameInfo.setBackground(new Color(0, 0, 0));
        jPanel_gameInfo.setBorder(Hud.panelBorder("Status"));
        jPanel_gameInfo.setForeground(new Color(220, 255, 220));
        jPanel_gameInfo.setFocusable(false);
        jLabel_waveText.setBackground(new Color(0, 0, 0));
        jLabel_waveText.setForeground(new Color(220, 255, 220));
        jLabel_waveText.setText("Wave:");
        jLabel_waveText.setFocusable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        jPanel_gameInfo.add(jLabel_waveText, gridBagConstraints);

        this.jLabel_wave.setBackground(new Color(0, 0, 0));
        this.jLabel_wave.setForeground(new Color(220, 255, 220));
        this.jLabel_wave.setHorizontalAlignment(SwingConstants.RIGHT);
        this.jLabel_wave.setText("xx/xx");
        this.jLabel_wave.setFocusable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        jPanel_gameInfo.add(this.jLabel_wave, gridBagConstraints);

        jLabel_livesText.setBackground(new Color(0, 0, 0));
        jLabel_livesText.setForeground(new Color(220, 255, 220));
        jLabel_livesText.setText("Lives:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new Insets(0, 2, 0, 2);
        jPanel_gameInfo.add(jLabel_livesText, gridBagConstraints);

        this.jLabel_lives.setBackground(new Color(0, 0, 0));
        this.jLabel_lives.setForeground(new Color(220, 255, 220));
        this.jLabel_lives.setHorizontalAlignment(SwingConstants.RIGHT);
        this.jLabel_lives.setText("xx");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new Insets(0, 2, 0, 2);
        jPanel_gameInfo.add(this.jLabel_lives, gridBagConstraints);

        jLabel_scoreText.setBackground(new Color(0, 0, 0));
        jLabel_scoreText.setForeground(new Color(220, 255, 220));
        jLabel_scoreText.setText("Score:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new Insets(0, 2, 0, 2);
        jPanel_gameInfo.add(jLabel_scoreText, gridBagConstraints);

        this.jLabel_score.setBackground(new Color(0, 0, 0));
        this.jLabel_score.setForeground(new Color(220, 255, 220));
        this.jLabel_score.setHorizontalAlignment(SwingConstants.RIGHT);
        this.jLabel_score.setText("0000000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new Insets(0, 2, 0, 0);
        jPanel_gameInfo.add(this.jLabel_score, gridBagConstraints);

        jLabel_creditsText.setBackground(new Color(0, 0, 0));
        jLabel_creditsText.setForeground(new Color(220, 255, 220));
        jLabel_creditsText.setText("Cash:");
        jLabel_creditsText.setFocusable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new Insets(0, 2, 0, 2);
        jPanel_gameInfo.add(jLabel_creditsText, gridBagConstraints);

        this.jLabel_credits.setBackground(new Color(0, 0, 0));
        this.jLabel_credits.setForeground(new Color(220, 255, 220));
        this.jLabel_credits.setHorizontalAlignment(SwingConstants.RIGHT);
        this.jLabel_credits.setText("000000");
        this.jLabel_credits.setFocusable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new Insets(0, 2, 0, 0);
        jPanel_gameInfo.add(this.jLabel_credits, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.insets = new Insets(0, 2, 0, 0);
        this.add(jPanel_gameInfo, gridBagConstraints);

        jPanel_gameButtons.setLayout(new GridBagLayout());

        jPanel_gameButtons.setBackground(new Color(0, 0, 0));
        jPanel_gameButtons.setBorder(Hud.panelBorder("Speed"));
        jPanel_gameButtons.setFocusable(false);
        this.jButton_play.setToolTipText("Play / start the next wave");
        this.jButton_play.addActionListener(this::jButton_playActionPerformed);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new Insets(5, 0, 0, 0);
        jPanel_gameButtons.add(this.jButton_play, gridBagConstraints);

        this.jButton_pause.setToolTipText("Pause");
        this.jButton_pause.setVisible(false);
        this.jButton_pause.addActionListener(this::jButton_pauseActionPerformed);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new Insets(5, 0, 0, 0);
        jPanel_gameButtons.add(this.jButton_pause, gridBagConstraints);

        this.jButton_fast.setToolTipText("Fast (single-steps one tick while paused)");
        this.jButton_fast.addActionListener(this::jButton_fastActionPerformed);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new Insets(5, 2, 0, 0);
        jPanel_gameButtons.add(this.jButton_fast, gridBagConstraints);

        this.jButton_superFast.setToolTipText("Super fast");
        this.jButton_superFast.addActionListener(this::jButton_superFastActionPerformed);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new Insets(5, 2, 0, 0);
        jPanel_gameButtons.add(this.jButton_superFast, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.01;
        this.add(jPanel_gameButtons, gridBagConstraints);

        this.panelTowerInfo.setFocusable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new Insets(0, 2, 0, 0);
        this.add(this.panelTowerInfo, gridBagConstraints);

        this.panelWaveInfo.setMinimumSize(null);
        this.panelWaveInfo.setName("Waving :)");
        this.panelWaveInfo.setPreferredSize(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new Insets(0, 2, 0, 0);
        this.add(this.panelWaveInfo, gridBagConstraints);
    }

    private void jButton_playActionPerformed(ActionEvent evt) {
        this.onPlay.run();
    }

    private void jButton_pauseActionPerformed(ActionEvent evt) {
        this.onPause.run();
    }

    private void jButton_fastActionPerformed(ActionEvent evt) {
        this.onFast.run();
    }

    private void jButton_superFastActionPerformed(ActionEvent evt) {
        this.onSuperFast.run();
    }
}
