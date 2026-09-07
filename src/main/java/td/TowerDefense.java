package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.cell.Cell;
import td.enemy.EnemyMob;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.ui.EnemyPainter;
import td.ui.GameBoard;
import td.ui.PanelTowerInfo;
import td.ui.PanelTowerSelector;
import td.ui.PanelWaveInfo;
import td.ui.TowerEffectPainter;
import td.ui.TowerSpritePainter;
import td.util.Cache;
import td.util.Context;
import td.util.ContextListener;
import td.util.GameHost;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.List;

// Inspired by HexTD
public class TowerDefense extends JFrame implements ContextListener, GameHost {

    private static final Logger LOG = LoggerFactory.getLogger(TowerDefense.class);

    @Serial
    private static final long serialVersionUID = 1L;
    private static final String NAME = "Tower Defense";
    static final String VERSION = "1.4";

    private static final int[] PATH_X = {-1, 0, 1, 2, 3, 4, 5, 5, 6, 7, 7, 7, 7, 7, 7, 7, 6, 5, 4, 4, 3, 3, 3, 3, 4, 5, 6, 6, 7, 8, 9, 10, 11, 11, 11, 12, 13, 14, 14, 14, 15, 16, 17, 17, 17, 17, 16, 15, 15, 15, 15, 14, 13, 12, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 20};
    private static final int[] PATH_Y = {11, 11, 11, 11, 11, 11, 11, 12, 12, 12, 11, 10, 9, 8, 7, 6, 6, 6, 6, 5, 5, 4, 3, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 5, 5, 5, 5, 4, 3, 3, 3, 3, 4, 5, 6, 6, 6, 7, 8, 9, 9, 9, 9, 10, 11, 12, 12, 12, 12, 12, 12, 12, 12, 12};
    private static final List<GameEngine.WaveDefinition> DEFAULT_WAVES = List.of(
            new GameEngine.WaveDefinition("c e c e c e c e c", 251, 2, 1),
            new GameEngine.WaveDefinition("c e 2 c e 3 c e 4 c", 377, 3, 1),
            new GameEngine.WaveDefinition("c e c", 812, 10, 2),
            new GameEngine.WaveDefinition("4 c 2 e 2 s", 747, 5, 1),
            new GameEngine.WaveDefinition("c c e s", 1109, 15, 3),
            new GameEngine.WaveDefinition("10 c", 953, 2, 1),
            new GameEngine.WaveDefinition("3 s e 4 c t e s t", 1117, 4, 2),
            new GameEngine.WaveDefinition("2 c e e t", 2193, 15, 4),
            new GameEngine.WaveDefinition("g 2 e 2 s", 1493, 10, 2),
            new GameEngine.WaveDefinition("s t s c g c t c s g t c s g c t s g t c", 1476, 2, 2),
            new GameEngine.WaveDefinition("g c g", 3789, 15, 4),
            new GameEngine.WaveDefinition("6 g 2 e 4 t", 3088, 7, 3),
            new GameEngine.WaveDefinition("c e c e c e c e c", 2912, 1, 2),
            new GameEngine.WaveDefinition("2 s 3 t 2 g 4 e c", 3242, 10, 3),
            new GameEngine.WaveDefinition("s 4 e t", 4014, 50, 6),
            new GameEngine.WaveDefinition("c 5 e 3 g 3 e 3 s 3 t", 4016, 4, 4),
            new GameEngine.WaveDefinition("s", 4751, 0, 8)
    );

    private final GameEngine engine;
    private final Context context;
    private final GameBoard gameBoard;
    private BufferedImage backGround;
    private final String statusMessage = """
            Welcome to TowerDefence
            Shortcuts:
            
            q - build triangle
            w - build circle
            e - build spiral
            r - build star
            t - build jing-jang
            p - pause
            f - cycle speed
            s - star wave""";

    private final GameLoop gameLoop = new GameLoop(this::doGameTick, this::requestRender);
    private final Object gameTimeLock = new Object();
    private int gameTime;
    // requestRender() and paintBoard() are both always invoked on the EDT (the former via
    // SwingUtilities.invokeLater, the latter via Swing's own paint dispatch), so this needs
    // no synchronization of its own - it's just in-flight bookkeeping on a single thread.
    private boolean painting = false;
    // Single source of truth for both the tick speed and pause state (TickSpeed.PAUSED);
    // EDT-only, since it's only ever touched from button/key listeners.
    private TickSpeed currentSpeed = TickSpeed.NORMAL;
    private boolean gameStopped = false;

    private JButton jButton_play;
    private JButton jButton_pause;
    private JButton jButton_fast;
    private JButton jButton_superFast;
    private JLabel jLabel_waveText;
    private JLabel jLabel_gameLostText;
    private JLabel jLabel_gameWonText;
    private JLabel jLabel_creditsText;
    private JLabel jLabel_livesText;
    private JLabel jLabel_name;
    private JLabel jLabel_scoreText;
    private JLabel jLabel_credits;
    private JLabel jLabel_lives;
    private JLabel jLabel_score;
    private JLabel jLabel_wave;
    private PanelTowerSelector panelTowerSelector;
    private JPanel jPanel_gameLost;
    private JPanel jPanel_gameWon;
    private JPanel jPanel_board;
    private JPanel jPanel_console;
    private JPanel jPanel_gameButtons;
    private JPanel jPanel_gameInfo;
    private PanelTowerInfo panelTowerInfo;
    private PanelWaveInfo panelWaveInfo;

    {
        this.setLayout(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle(NAME);
        this.setFocusable(true);
        this.setVisible(true);
    }

    public TowerDefense() {
        this.engine = new GameEngine(this);
        this.context = this.engine.getContext();
        this.context.addContextListener(this);
        Cache cache = this.context.getCache();
        if (cache.hasBufImg("bg")) {
            this.backGround = cache.getBufImg("bg");
        }
        this.gameBoard = new GameBoard(this, this.context);
        initComponents();
        this.panelWaveInfo.setContext(this.context);
        this.panelTowerSelector.doInit(this.context, this);
        this.panelTowerInfo.setContext(this.context);

        this.jPanel_board.add(this.gameBoard);

        loadTestLevel();
        this.gameTime = 0;
        this.setSpeed(TickSpeed.NORMAL);
        this.gameLoop.start();
    }

    /**
     * Runs one logic tick, called from the game loop thread. GameLoop itself
     * already skips calling this while TickSpeed.PAUSED is selected, so the
     * only remaining guard here is for after the game has ended.
     */
    private void doGameTick() {
        if (this.gameStopped) {
            return;
        }
        int time;
        synchronized (this.gameTimeLock) {
            time = ++this.gameTime;
        }
        this.doTick(time);
    }

    /**
     * Requests a repaint, called from the game loop thread via
     * SwingUtilities.invokeLater once per batch of ticks. Skips requesting
     * one while a previous paint is still in flight, or once the game has
     * ended.
     */
    private void requestRender() {
        if (this.gameStopped) {
            return;
        }
        if (!this.painting) {
            this.gameBoard.repaint();
        }
    }

    // Levels are hardcoded here for now, but built the same way a file-based level loader would build them.
    private void loadTestLevel() {
        int width = 20;
        int height = 15;
        this.engine.loadLevel(width, height, PATH_X, PATH_Y, DEFAULT_WAVES, 50);
        this.gameBoard.recalculateBoard(width, height);
        this.jPanel_gameLost.setVisible(false);
        this.jPanel_gameWon.setVisible(false);
        this.startLevel();
    }

    public void setInfoText(String s) {
        this.unSelectTower();
        this.panelTowerInfo.setExternalText(s);
    }

    public void enemyDied(int enemiesLeft) {
        if (enemiesLeft == 0 && this.engine.getCurrentWaveIndex() < this.engine.getWaveCount()) {
            LOG.info("Wave {} cleared, ready for the next one", this.engine.getCurrentWaveIndex());
            this.engine.setWaveReady(true);
            this.jButton_play.setVisible(true);
            this.jButton_pause.setVisible(false);
        } else if (enemiesLeft == 0) {
            this.gameWon();
        }
    }

    private void setWavePreview() {
        this.panelWaveInfo.clearWaves();
        int wave = this.engine.getCurrentWaveIndex();
        if (wave - 1 >= 0) {
            this.panelWaveInfo.setWaveCur(wave, this.engine.getWaveAt(wave - 1));
        }
        if (wave < this.engine.getWaveCount()) {
            this.panelWaveInfo.setWaveNext(wave + 1, this.engine.getWaveAt(wave));
        }
    }

    private void nextWave() {
        if (this.engine.nextWave()) {
            this.setWavePreview();
            this.updateInfo();
        }
    }

    private void updateInfo() {
        this.jLabel_wave.setText("  " + this.engine.getCurrentWaveIndex() + "/" + this.engine.getWaveCount());
        this.jLabel_credits.setText("$" + this.context.getCredits());
        this.jLabel_lives.setText("" + this.context.getLives());
        this.jLabel_score.setText("" + this.context.getScore());
    }

    public void moneyChanged() {
        this.updateInfo();
    }

    public void livesChanged() {
        this.updateInfo();
        if (this.context.getLives() <= 0) {
            this.gameLost();
        }
    }

    private void gameLost() {
        LOG.info("Game over - lost, score={}", this.context.getScore());
        this.gameStopped = true;
        this.jPanel_gameLost.setVisible(true);
    }

    private void gameWon() {
        LOG.info("Game won, score={}", this.context.getScore());
        this.gameStopped = true;
        this.jPanel_gameWon.setVisible(true);
    }

    public void startLevel() {
        this.engine.startLevel();
        this.setWavePreview();
    }

    public void doTick(int time) {
        boolean waveStarted = this.engine.doTick(time);
        if (waveStarted) {
            this.setWavePreview();
            this.updateInfo();
        }
        this.panelWaveInfo.doTick(time);
    }

    /**
     * The single place that changes tick speed - this owns keeping the
     * play/pause button visibility consistent with it, so no caller has to
     * remember to do that separately.
     */
    private void setSpeed(TickSpeed speed) {
        this.currentSpeed = speed;
        this.gameLoop.setSpeed(speed);
        boolean playing = speed != TickSpeed.PAUSED;
        this.jButton_play.setVisible(!playing);
        this.jButton_pause.setVisible(playing);
    }

    private void togglePause() {
        this.setSpeed(this.currentSpeed == TickSpeed.PAUSED ? TickSpeed.NORMAL : TickSpeed.PAUSED);
    }

    private void cycleSpeed() {
        this.setSpeed(this.currentSpeed.next());
    }

    public void paintBoard(Graphics2D g2) {
        this.painting = true;
        int time;

        synchronized (this.gameTimeLock) {
            time = this.gameTime;
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        g2.drawImage(this.backGround, 0, 0, null);

        Cell[][] cellGrid = this.engine.getCellGrid();
        if (cellGrid != null) {
            for (Cell[] cells : cellGrid) {
                for (int j = 0; j < cellGrid[0].length; j++) {
                    cells[j].paintEffect(g2);
                }
            }
        }

        EnemyPainter enemyPainter = new EnemyPainter(g2, time);
        for (EnemyMob enemy : this.context.getEnemies()) {
            enemy.accept(enemyPainter);
        }

        TowerSpritePainter spritePainter = new TowerSpritePainter(g2);
        for (Tower tower : this.engine.getTowers()) {
            tower.accept(spritePainter);
        }

        TowerEffectPainter effectPainter = new TowerEffectPainter(g2);
        for (Tower tower : this.engine.getTowers()) {
            tower.accept(effectPainter);
        }

        this.painting = false;
    }

    public void startPlacing(TowerFactory.type t, float r) {
        this.engine.startPlacing(t, r);
    }

    public void unSelectTower() {
        this.engine.unSelectTower();
        this.panelTowerInfo.unselectTower();
        this.panelTowerInfo.setExternalText(this.statusMessage);
    }

    public void clearCell(int x, int y) {
        this.engine.clearCell(x, y);
    }

    private void jPanel_boardMouseClicked(MouseEvent evt) {
        this.unSelectTower();
        boolean wasPlacing = this.engine.isPlacingTower();
        int boardX = evt.getX() - this.gameBoard.getX();
        int boardY = evt.getY() - this.gameBoard.getY();
        Tower selected = this.engine.mouseClicked(boardX, boardY);
        if (selected != null) {
            this.panelTowerInfo.setTower(selected);
        }
        if (wasPlacing && !this.engine.isPlacingTower()) {
            this.panelTowerSelector.stopPlacing();
        }
    }

    private void jPanel_boardMouseMoved(MouseEvent evt) {
        this.requestFocusInWindow();
        if (this.engine.isPlacingTower()) {
            this.engine.highlightCell(evt.getX() - this.gameBoard.getX(), evt.getY() - this.gameBoard.getY());
        }
    }

    private void keyTyped(char key) {
        switch (key) {
            case 'q' -> this.panelTowerSelector.doPlace(0);
            case 'w' -> this.panelTowerSelector.doPlace(1);
            case 'e' -> this.panelTowerSelector.doPlace(2);
            case 'r' -> this.panelTowerSelector.doPlace(3);
            case 't' -> this.panelTowerSelector.doPlace(4);
            case 'p' -> this.togglePause();
            case 'f' -> this.cycleSpeed();
            case 's' -> this.nextWave();
            case KeyEvent.VK_ESCAPE -> {
                if (this.engine.isPlacingTower()) {
                    this.engine.cancelPlacing();
                    this.panelTowerSelector.stopPlacing();
                }
            }
            default -> {
            }
        }
    }

    private void initComponents() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOG.warn("Could not set native look-and-feel, falling back to default", e);
        }

        addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent evt) {
                formKeyTyped(evt);
            }
        });

        GridBagConstraints gridBagConstraints;

        jPanel_board = new JPanel();
        jPanel_gameLost = new JPanel();
        jLabel_gameLostText = new JLabel();
        jPanel_gameWon = new JPanel();
        jLabel_gameWonText = new JLabel();
        jPanel_console = new JPanel();
        jLabel_name = new JLabel();
        jPanel_gameInfo = new JPanel();
        jLabel_waveText = new JLabel();
        jLabel_wave = new JLabel();
        jLabel_livesText = new JLabel();
        jLabel_lives = new JLabel();
        jLabel_scoreText = new JLabel();
        jLabel_score = new JLabel();
        jLabel_creditsText = new JLabel();
        jLabel_credits = new JLabel();
        jPanel_gameButtons = new JPanel();
        jButton_play = new JButton();
        jButton_pause = new JButton();
        jButton_fast = new JButton();
        jButton_superFast = new JButton();
        panelTowerInfo = new PanelTowerInfo();
        panelWaveInfo = new PanelWaveInfo();
        panelTowerSelector = new PanelTowerSelector();


        getContentPane().setLayout(new GridBagLayout());


        jPanel_board.setLayout(new GridBagLayout());

        jPanel_board.setBackground(new Color(0, 0, 0));
        jPanel_board.setForeground(new Color(220, 255, 220));
        jPanel_board.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                jPanel_boardMouseClicked(evt);
            }
        });
        jPanel_board.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent evt) {
                jPanel_boardMouseMoved(evt);
            }
        });

        jPanel_gameLost.setLayout(new GridBagLayout());

        jPanel_gameLost.setBackground(new Color(0, 0, 0, 80));
        jLabel_gameLostText.setBackground(new Color(0, 0, 0));
        jLabel_gameLostText.setFont(new Font("SansSerif", Font.BOLD, 18));
        jLabel_gameLostText.setForeground(new Color(220, 255, 220));
        jLabel_gameLostText.setText("Game Over!");
        jPanel_gameLost.add(jLabel_gameLostText, new GridBagConstraints());

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        jPanel_board.add(jPanel_gameLost, gridBagConstraints);

        jPanel_gameWon.setLayout(new GridBagLayout());

        jPanel_gameWon.setBackground(new Color(0, 0, 0, 80));
        jLabel_gameWonText.setBackground(new Color(0, 0, 0));
        jLabel_gameWonText.setFont(new Font("SansSerif", Font.BOLD, 18));
        jLabel_gameWonText.setForeground(new Color(220, 255, 220));
        jLabel_gameWonText.setText("Congratulations!");
        jPanel_gameWon.add(jLabel_gameWonText, new GridBagConstraints());

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        jPanel_board.add(jPanel_gameWon, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        getContentPane().add(jPanel_board, gridBagConstraints);


        jPanel_console.setLayout(new GridBagLayout());

        jPanel_console.setBackground(new Color(0, 0, 0));
        jPanel_console.setFocusable(false);
        jPanel_console.setMaximumSize(new Dimension(200, 2147483647));
        jPanel_console.setMinimumSize(new Dimension(200, 263));
        jPanel_console.setPreferredSize(new Dimension(200, 402));

        jLabel_name.setBackground(new Color(0, 0, 0));
        jLabel_name.setFont(new Font("Dialog", Font.BOLD, 16));
        jLabel_name.setForeground(new Color(220, 255, 220));
        jLabel_name.setText(NAME + " v" + VERSION);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new Insets(4, 10, 4, 10);
        jPanel_console.add(jLabel_name, gridBagConstraints);

        jPanel_gameInfo.setLayout(new GridBagLayout());

        jPanel_gameInfo.setBackground(new Color(0, 0, 0));
        jPanel_gameInfo.setBorder(BorderFactory.createTitledBorder(null, "Status", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.PLAIN, 11), new Color(220, 255, 220)));
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

        jLabel_wave.setBackground(new Color(0, 0, 0));
        jLabel_wave.setForeground(new Color(220, 255, 220));
        jLabel_wave.setHorizontalAlignment(SwingConstants.RIGHT);
        jLabel_wave.setText("xx/xx");
        jLabel_wave.setFocusable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        jPanel_gameInfo.add(jLabel_wave, gridBagConstraints);

        jLabel_livesText.setBackground(new Color(0, 0, 0));
        jLabel_livesText.setForeground(new Color(220, 255, 220));
        jLabel_livesText.setText("Lives:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new Insets(0, 2, 0, 2);
        jPanel_gameInfo.add(jLabel_livesText, gridBagConstraints);

        jLabel_lives.setBackground(new Color(0, 0, 0));
        jLabel_lives.setForeground(new Color(220, 255, 220));
        jLabel_lives.setHorizontalAlignment(SwingConstants.RIGHT);
        jLabel_lives.setText("xx");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new Insets(0, 2, 0, 2);
        jPanel_gameInfo.add(jLabel_lives, gridBagConstraints);

        jLabel_scoreText.setBackground(new Color(0, 0, 0));
        jLabel_scoreText.setForeground(new Color(220, 255, 220));
        jLabel_scoreText.setText("Score:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new Insets(0, 2, 0, 2);
        jPanel_gameInfo.add(jLabel_scoreText, gridBagConstraints);

        jLabel_score.setBackground(new Color(0, 0, 0));
        jLabel_score.setForeground(new Color(220, 255, 220));
        jLabel_score.setHorizontalAlignment(SwingConstants.RIGHT);
        jLabel_score.setText("0000000");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new Insets(0, 2, 0, 0);
        jPanel_gameInfo.add(jLabel_score, gridBagConstraints);

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

        jLabel_credits.setBackground(new Color(0, 0, 0));
        jLabel_credits.setForeground(new Color(220, 255, 220));
        jLabel_credits.setHorizontalAlignment(SwingConstants.RIGHT);
        jLabel_credits.setText("000000");
        jLabel_credits.setFocusable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new Insets(0, 2, 0, 0);
        jPanel_gameInfo.add(jLabel_credits, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.insets = new Insets(0, 2, 0, 0);
        jPanel_console.add(jPanel_gameInfo, gridBagConstraints);

        jPanel_gameButtons.setLayout(new GridBagLayout());

        jPanel_gameButtons.setBackground(new Color(0, 0, 0));
        jPanel_gameButtons.setBorder(BorderFactory.createEtchedBorder());
        jPanel_gameButtons.setFocusable(false);
        jButton_play.setBackground(new Color(0, 0, 0));
        jButton_play.setForeground(new Color(0, 0, 0));
        jButton_play.setText(">");
        jButton_play.setFocusable(false);
        jButton_play.addActionListener(this::jButton_playActionPerformed);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new Insets(5, 0, 0, 0);
        jPanel_gameButtons.add(jButton_play, gridBagConstraints);

        jButton_pause.setBackground(new Color(0, 0, 0));
        jButton_pause.setForeground(new Color(0, 0, 0));
        jButton_pause.setText("||");
        jButton_pause.setFocusable(false);
        jButton_pause.setVisible(false);
        jButton_pause.addActionListener(this::jButton_pauseActionPerformed);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new Insets(5, 0, 0, 0);
        jPanel_gameButtons.add(jButton_pause, gridBagConstraints);

        jButton_fast.setBackground(new Color(0, 0, 0));
        jButton_fast.setForeground(new Color(0, 0, 0));
        jButton_fast.setText(">>");
        jButton_fast.setFocusable(false);
        jButton_fast.addActionListener(this::jButton_fastActionPerformed);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new Insets(5, 2, 0, 0);
        jPanel_gameButtons.add(jButton_fast, gridBagConstraints);

        jButton_superFast.setBackground(new Color(0, 0, 0));
        jButton_superFast.setForeground(new Color(0, 0, 0));
        jButton_superFast.setText(">>>");
        jButton_superFast.setFocusable(false);
        jButton_superFast.addActionListener(this::jButton_superFastActionPerformed);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new Insets(5, 2, 0, 0);
        jPanel_gameButtons.add(jButton_superFast, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.01;
        jPanel_console.add(jPanel_gameButtons, gridBagConstraints);

        panelTowerInfo.setFocusable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new Insets(0, 2, 0, 0);
        jPanel_console.add(panelTowerInfo, gridBagConstraints);

        panelWaveInfo.setMinimumSize(null);
        panelWaveInfo.setName("Waving :)");
        panelWaveInfo.setPreferredSize(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new Insets(0, 2, 0, 0);
        jPanel_console.add(panelWaveInfo, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_END;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jPanel_console, gridBagConstraints);


        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_END;
        getContentPane().add(panelTowerSelector, gridBagConstraints);

    }

    private void jButton_playActionPerformed(ActionEvent evt) {
        boolean wasPaused = this.currentSpeed == TickSpeed.PAUSED;
        this.setSpeed(TickSpeed.NORMAL);
        if (this.engine.isWaveReady() && wasPaused) this.engine.requestNextWave();
    }

    private void jButton_pauseActionPerformed(ActionEvent evt) {
        this.setSpeed(TickSpeed.PAUSED);
    }

    private void jButton_fastActionPerformed(ActionEvent evt) {
        this.setSpeed(TickSpeed.FAST);
    }

    private void jButton_superFastActionPerformed(ActionEvent evt) {
        this.setSpeed(TickSpeed.SUPER_FAST);
    }

    private void formKeyTyped(KeyEvent evt) {
        this.keyTyped(evt.getKeyChar());
    }

}
