package td;

import td.cell.Cell;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.ui.GameBoard;
import td.util.Cache;
import td.util.Context;
import td.util.ContextListener;
import td.util.GameHost;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;

// Inspired by HexTD
public class TowerDefence extends JFrame implements ContextListener, GameHost {
    private static final long serialVersionUID = 1L;
    private static final String NAME = "Tower Defence";
    private static final String VERSION = "1.3";

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
    private final Object paintLock = new Object();
    private boolean painting = false;
    private TickSpeed currentSpeed = TickSpeed.NORMAL; // EDT-only bookkeeping for the 'f' key cycle
    private boolean paused = false;
    private boolean gameStopped = false;

    private javax.swing.JButton jButton_play;
    private javax.swing.JButton jButton_pause;
    private javax.swing.JButton jButton_fast;
    private javax.swing.JButton jButton_superFast;
    private javax.swing.JLabel jLabel_waveText;
    private javax.swing.JLabel jLabel_gameLostText;
    private javax.swing.JLabel jLabel_gameWonText;
    private javax.swing.JLabel jLabel_creditsText;
    private javax.swing.JLabel jLabel_livesText;
    private javax.swing.JLabel jLabel_name;
    private javax.swing.JLabel jLabel_scoreText;
    private javax.swing.JLabel jLabel_credits;
    private javax.swing.JLabel jLabel_lives;
    private javax.swing.JLabel jLabel_score;
    private javax.swing.JLabel jLabel_wave;
    private td.ui.PanelTowerSelector panelTowerSelector;
    private javax.swing.JPanel jPanel_gameLost;
    private javax.swing.JPanel jPanel_gameWon;
    private javax.swing.JPanel jPanel_board;
    private javax.swing.JPanel jPanel_console;
    private javax.swing.JPanel jPanel_gameButtons;
    private javax.swing.JPanel jPanel_gameInfo;
    private td.ui.PanelTowerInfo panelTowerInfo;
    private td.ui.PanelWaveInfo panelWaveInfo;

    {
        this.setLayout(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle(NAME);
        this.setFocusable(true);
        this.setVisible(true);
    }

    public TowerDefence() {
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
        this.gameLoop.start();
    }

    /**
     * Runs one logic tick, called from the game loop thread. A no-op while
     * paused or after the game has ended.
     */
    private void doGameTick() {
        if (this.gameStopped || this.paused) {
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
        boolean stillPainting;
        synchronized (this.paintLock) {
            stillPainting = this.painting;
        }
        if (!stillPainting) {
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
        this.gameStopped = true;
        this.jPanel_gameLost.setVisible(true);
    }

    private void gameWon() {
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

    private void setSpeed(TickSpeed speed) {
        this.currentSpeed = speed;
        this.gameLoop.setSpeed(speed);
    }

    private void cycleSpeed() {
        this.setSpeed(this.currentSpeed.next());
    }

    public void paintBoard(Graphics2D g2) {
        synchronized (this.paintLock) {
            this.painting = true;
        }
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

        if (this.context.enemies != null) {
            for (int i = 0; i < this.context.enemies.length; i++) {
                this.context.enemies[i].paint(g2, time);
            }
        }

        for (Tower tower : this.engine.getTowers()) {
            tower.paint(g2, time);
        }

        for (Tower tower : this.engine.getTowers()) {
            tower.paintEffect(g2, time);
        }

        synchronized (this.paintLock) {
            this.painting = false;
        }
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

    private void jPanel_boardMouseClicked(java.awt.event.MouseEvent evt) {
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

    private void jPanel_boardMouseMoved(java.awt.event.MouseEvent evt) {
        this.requestFocusInWindow();
        if (this.engine.isPlacingTower()) {
            this.engine.highlightCell(evt.getX() - this.gameBoard.getX(), evt.getY() - this.gameBoard.getY());
        }
    }

    private void playPause(boolean play) {
        this.paused = !play;
        this.jButton_play.setVisible(!play);
        this.jButton_pause.setVisible(play);
    }

    private void keyTyped(char key) {
        switch (key) {
            case 'q' -> this.panelTowerSelector.doPlace(0);
            case 'w' -> this.panelTowerSelector.doPlace(1);
            case 'e' -> this.panelTowerSelector.doPlace(2);
            case 'r' -> this.panelTowerSelector.doPlace(3);
            case 't' -> this.panelTowerSelector.doPlace(4);
            case 'p' -> this.playPause(this.paused);
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
            System.out.println("Error setting native LAF: " + e);
        }

        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(KeyEvent evt) {
                formKeyTyped(evt);
            }
        });

        java.awt.GridBagConstraints gridBagConstraints;

        jPanel_board = new javax.swing.JPanel();
        jPanel_gameLost = new javax.swing.JPanel();
        jLabel_gameLostText = new javax.swing.JLabel();
        jPanel_gameWon = new javax.swing.JPanel();
        jLabel_gameWonText = new javax.swing.JLabel();
        jPanel_console = new javax.swing.JPanel();
        jLabel_name = new javax.swing.JLabel();
        jPanel_gameInfo = new javax.swing.JPanel();
        jLabel_waveText = new javax.swing.JLabel();
        jLabel_wave = new javax.swing.JLabel();
        jLabel_livesText = new javax.swing.JLabel();
        jLabel_lives = new javax.swing.JLabel();
        jLabel_scoreText = new javax.swing.JLabel();
        jLabel_score = new javax.swing.JLabel();
        jLabel_creditsText = new javax.swing.JLabel();
        jLabel_credits = new javax.swing.JLabel();
        jPanel_gameButtons = new javax.swing.JPanel();
        jButton_play = new javax.swing.JButton();
        jButton_pause = new javax.swing.JButton();
        jButton_fast = new javax.swing.JButton();
        jButton_superFast = new javax.swing.JButton();
        panelTowerInfo = new td.ui.PanelTowerInfo();
        panelWaveInfo = new td.ui.PanelWaveInfo();
        panelTowerSelector = new td.ui.PanelTowerSelector();


        getContentPane().setLayout(new java.awt.GridBagLayout());


        jPanel_board.setLayout(new java.awt.GridBagLayout());

        jPanel_board.setBackground(new java.awt.Color(0, 0, 0));
        jPanel_board.setForeground(new java.awt.Color(220, 255, 220));
        jPanel_board.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanel_boardMouseClicked(evt);
            }
        });
        jPanel_board.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jPanel_boardMouseMoved(evt);
            }
        });

        jPanel_gameLost.setLayout(new java.awt.GridBagLayout());

        jPanel_gameLost.setBackground(new java.awt.Color(0, 0, 0, 80));
        jLabel_gameLostText.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_gameLostText.setFont(new java.awt.Font("SansSerif", Font.BOLD, 18));
        jLabel_gameLostText.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_gameLostText.setText("Game Over!");
        jPanel_gameLost.add(jLabel_gameLostText, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        jPanel_board.add(jPanel_gameLost, gridBagConstraints);

        jPanel_gameWon.setLayout(new java.awt.GridBagLayout());

        jPanel_gameWon.setBackground(new java.awt.Color(0, 0, 0, 80));
        jLabel_gameWonText.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_gameWonText.setFont(new java.awt.Font("SansSerif", Font.BOLD, 18));
        jLabel_gameWonText.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_gameWonText.setText("Congratulations!");
        jPanel_gameWon.add(jLabel_gameWonText, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        jPanel_board.add(jPanel_gameWon, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        getContentPane().add(jPanel_board, gridBagConstraints);


        jPanel_console.setLayout(new java.awt.GridBagLayout());

        jPanel_console.setBackground(new java.awt.Color(0, 0, 0));
        jPanel_console.setFocusable(false);
        jPanel_console.setMaximumSize(new java.awt.Dimension(200, 2147483647));
        jPanel_console.setMinimumSize(new java.awt.Dimension(200, 263));
        jPanel_console.setPreferredSize(new java.awt.Dimension(200, 402));

        jLabel_name.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_name.setFont(new java.awt.Font("Dialog", Font.BOLD, 16));
        jLabel_name.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_name.setText(NAME + " v" + VERSION);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 4, 10);
        jPanel_console.add(jLabel_name, gridBagConstraints);

        jPanel_gameInfo.setLayout(new java.awt.GridBagLayout());

        jPanel_gameInfo.setBackground(new java.awt.Color(0, 0, 0));
        jPanel_gameInfo.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Status", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", Font.PLAIN, 11), new java.awt.Color(220, 255, 220)));
        jPanel_gameInfo.setForeground(new java.awt.Color(220, 255, 220));
        jPanel_gameInfo.setFocusable(false);
        jLabel_waveText.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_waveText.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_waveText.setText("Wave:");
        jLabel_waveText.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel_gameInfo.add(jLabel_waveText, gridBagConstraints);

        jLabel_wave.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_wave.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_wave.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel_wave.setText("xx/xx");
        jLabel_wave.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel_gameInfo.add(jLabel_wave, gridBagConstraints);

        jLabel_livesText.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_livesText.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_livesText.setText("Lives:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        jPanel_gameInfo.add(jLabel_livesText, gridBagConstraints);

        jLabel_lives.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_lives.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_lives.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel_lives.setText("xx");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        jPanel_gameInfo.add(jLabel_lives, gridBagConstraints);

        jLabel_scoreText.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_scoreText.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_scoreText.setText("Score:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        jPanel_gameInfo.add(jLabel_scoreText, gridBagConstraints);

        jLabel_score.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_score.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_score.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel_score.setText("0000000");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        jPanel_gameInfo.add(jLabel_score, gridBagConstraints);

        jLabel_creditsText.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_creditsText.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_creditsText.setText("Cash:");
        jLabel_creditsText.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        jPanel_gameInfo.add(jLabel_creditsText, gridBagConstraints);

        jLabel_credits.setBackground(new java.awt.Color(0, 0, 0));
        jLabel_credits.setForeground(new java.awt.Color(220, 255, 220));
        jLabel_credits.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel_credits.setText("000000");
        jLabel_credits.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        jPanel_gameInfo.add(jLabel_credits, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        jPanel_console.add(jPanel_gameInfo, gridBagConstraints);

        jPanel_gameButtons.setLayout(new java.awt.GridBagLayout());

        jPanel_gameButtons.setBackground(new java.awt.Color(0, 0, 0));
        jPanel_gameButtons.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel_gameButtons.setFocusable(false);
        jButton_play.setBackground(new java.awt.Color(0, 0, 0));
        jButton_play.setForeground(new java.awt.Color(0, 0, 0));
        jButton_play.setText(">");
        jButton_play.setFocusable(false);
        jButton_play.addActionListener(this::jButton_playActionPerformed);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        jPanel_gameButtons.add(jButton_play, gridBagConstraints);

        jButton_pause.setBackground(new java.awt.Color(0, 0, 0));
        jButton_pause.setForeground(new java.awt.Color(0, 0, 0));
        jButton_pause.setText("||");
        jButton_pause.setFocusable(false);
        jButton_pause.setVisible(false);
        jButton_pause.addActionListener(this::jButton_pauseActionPerformed);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        jPanel_gameButtons.add(jButton_pause, gridBagConstraints);

        jButton_fast.setBackground(new java.awt.Color(0, 0, 0));
        jButton_fast.setForeground(new java.awt.Color(0, 0, 0));
        jButton_fast.setText(">>");
        jButton_fast.setFocusable(false);
        jButton_fast.addActionListener(this::jButton_fastActionPerformed);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(5, 2, 0, 0);
        jPanel_gameButtons.add(jButton_fast, gridBagConstraints);

        jButton_superFast.setBackground(new java.awt.Color(0, 0, 0));
        jButton_superFast.setForeground(new java.awt.Color(0, 0, 0));
        jButton_superFast.setText(">>>");
        jButton_superFast.setFocusable(false);
        jButton_superFast.addActionListener(this::jButton_superFastActionPerformed);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(5, 2, 0, 0);
        jPanel_gameButtons.add(jButton_superFast, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.01;
        jPanel_console.add(jPanel_gameButtons, gridBagConstraints);

        panelTowerInfo.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        jPanel_console.add(panelTowerInfo, gridBagConstraints);

        panelWaveInfo.setMinimumSize(null);
        panelWaveInfo.setName("Waving :)");
        panelWaveInfo.setPreferredSize(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        jPanel_console.add(panelWaveInfo, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jPanel_console, gridBagConstraints);


        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        getContentPane().add(panelTowerSelector, gridBagConstraints);

    }

    private void jButton_playActionPerformed(java.awt.event.ActionEvent evt) {
        this.setSpeed(TickSpeed.NORMAL);
        if (this.engine.isWaveReady() && !this.paused) this.engine.requestNextWave();
        this.playPause(true);
    }

    private void jButton_pauseActionPerformed(java.awt.event.ActionEvent evt) {
        this.playPause(false);
    }

    private void jButton_fastActionPerformed(java.awt.event.ActionEvent evt) {
        this.jButton_play.setVisible(true);
        this.jButton_pause.setVisible(false);
        this.setSpeed(TickSpeed.FAST);
    }

    private void jButton_superFastActionPerformed(java.awt.event.ActionEvent evt) {
        this.jButton_play.setVisible(true);
        this.jButton_pause.setVisible(false);
        this.setSpeed(TickSpeed.SUPER_FAST);
    }

    private void formKeyTyped(KeyEvent evt) {
        this.keyTyped(evt.getKeyChar());
    }

}
