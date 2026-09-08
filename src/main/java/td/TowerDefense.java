package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.level.BuiltInLevelCatalog;
import td.level.LevelCatalog;
import td.level.LevelDefinition;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.ui.BoardOverlays;
import td.ui.BoardRenderer;
import td.ui.GameBoard;
import td.ui.Java2DFrameRenderer;
import td.ui.PanelGameConsole;
import td.ui.PanelLevelSelect;
import td.ui.PanelTowerSelector;
import td.ui.render.AsciiBoardRenderer;
import td.ui.render.RenderFrame;
import td.util.GameHost;
import td.util.GameWorld;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.Serial;

// Inspired by HexTD
public class TowerDefense extends JFrame implements EconomyListener, GameHost {

    private static final Logger LOG = LoggerFactory.getLogger(TowerDefense.class);

    @Serial
    private static final long serialVersionUID = 1L;
    private static final String NAME = "Tower Defense";
    static final String VERSION = "1.4";
    private static final String CARD_MENU = "menu";
    private static final String CARD_GAME = "game";
    private static final int MENU_WIDTH = 480;
    private static final int MENU_HEIGHT = 420;

    private final LevelCatalog levelCatalog = new BuiltInLevelCatalog();

    private final GameEngine engine;
    private final GameWorld context;
    private final GameBoard gameBoard;
    private final BoardRenderer boardRenderer;
    private final Java2DFrameRenderer frameRenderer = new Java2DFrameRenderer();
    private final AsciiBoardRenderer asciiBoardRenderer = new AsciiBoardRenderer();
    private final PanelGameConsole gameConsole = new PanelGameConsole(NAME + " v" + VERSION);
    private final BoardOverlays boardOverlays = new BoardOverlays();
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
    // Guards input and the game loop against firing before startSelectedLevel() has run -
    // the JFrame-level KeyListener is live the instant the frame is shown, well before any
    // level (and its cellGrid) exists.
    private boolean levelLoaded = false;

    private CardLayout contentCardLayout;
    private JPanel jPanel_game;
    private PanelLevelSelect panelLevelSelect;
    private PanelTowerSelector panelTowerSelector;
    private JPanel jPanel_board;

    {
        this.setLayout(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle(NAME);
        this.setFocusable(true);
        this.setVisible(true);
    }

    public TowerDefense() {
        this.engine = new GameEngine(this);
        this.context = this.engine.getGameWorld();
        this.boardRenderer = new BoardRenderer(this.engine, this.context.getEnemyRegistry(),
                this.context::getBoard, this.context::getPath);
        this.context.addEconomyListener(this);
        this.gameBoard = new GameBoard(this, this.context);
        initComponents();
        this.gameConsole.setGameWorld(this.context);
        this.gameConsole.onPlay(this::playPressed);
        this.gameConsole.onPause(this::pausePressed);
        this.gameConsole.onFast(this::fastPressed);
        this.gameConsole.onSuperFast(this::superFastPressed);
        this.panelTowerSelector.doInit(this.context, this);

        this.jPanel_board.add(this.gameBoard);

        // CardLayout starts on the menu card; give the frame a size that fits it. The game
        // card gets its own size from recalculateBoard() once a level is actually loaded.
        this.setSize(MENU_WIDTH, MENU_HEIGHT);
        this.setLocationRelativeTo(null);
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
        // Off by default (see Logging in CLAUDE.md); the isDebugEnabled() guard skips
        // building a frame for this every tick when it is. Alpha 0 - a tick-boundary
        // dump wants this tick's resulting state, not a partial interpolation of it.
        if (LOG.isDebugEnabled()) {
            LOG.debug("Board state after tick {}:\n{}", time,
                    this.asciiBoardRenderer.render(this.boardRenderer.buildFrame(time, 0.0, 0.0)));
        }
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

    /**
     * The level-select screen's callback. Ignores a second call (e.g. a fast double-click on a
     * level card) since {@link GameLoop#start()} spawns a new thread every time it's called,
     * with no guard of its own - two calls would leave two loops ticking the same engine.
     */
    private void startSelectedLevel(LevelDefinition level) {
        if (this.levelLoaded) {
            return;
        }
        this.levelLoaded = true;
        this.contentCardLayout.show(getContentPane(), CARD_GAME);
        this.engine.loadLevel(level);
        this.gameBoard.recalculateBoard(level.width(), level.height());
        this.boardOverlays.reset();
        this.startLevel();
        this.gameTime = 0;
        this.setSpeed(TickSpeed.NORMAL);
        this.gameLoop.start();
        this.requestFocusInWindow();
    }

    public void setInfoText(String s) {
        this.unSelectTower();
        this.gameConsole.getTowerInfo().setExternalText(s);
    }

    /**
     * A GameHost callback fired synchronously from GameWorld, which is reached
     * from enemy/tower doTick() during a tick - i.e. this can run on the
     * game-loop thread, not the EDT. Any Swing mutation here is deferred via
     * invokeLater; engine-state changes are not, since they aren't Swing calls.
     */
    public void enemyDied(int enemiesLeft) {
        if (enemiesLeft == 0 && this.engine.getCurrentWaveIndex() < this.engine.getWaveCount()) {
            LOG.info("Wave {} cleared, ready for the next one", this.engine.getCurrentWaveIndex());
            this.engine.setWaveReady(true);
            SwingUtilities.invokeLater(() -> this.gameConsole.setPlaying(false));
        } else if (enemiesLeft == 0) {
            this.gameWon();
        }
    }

    private void setWavePreview() {
        this.gameConsole.getWaveInfo().clearWaves();
        int wave = this.engine.getCurrentWaveIndex();
        if (wave - 1 >= 0) {
            this.gameConsole.getWaveInfo().setWaveCur(wave, this.engine.getWaveAt(wave - 1));
        }
        if (wave < this.engine.getWaveCount()) {
            this.gameConsole.getWaveInfo().setWaveNext(wave + 1, this.engine.getWaveAt(wave));
        }
    }

    private void nextWave() {
        if (this.engine.nextWave()) {
            this.setWavePreview();
            this.updateInfo();
        }
    }

    private void updateInfo() {
        this.gameConsole.setWaveProgress(this.engine.getCurrentWaveIndex(), this.engine.getWaveCount());
    }

    /** Also reachable from the game-loop thread - see enemyDied(). */
    public void economyChanged(EconomyState state) {
        SwingUtilities.invokeLater(this::updateInfo);
        if (state.isGameOver()) {
            this.gameLost();
        }
    }

    private void gameLost() {
        LOG.info("Game over - lost, score={}", this.context.getScore());
        this.gameStopped = true;
        SwingUtilities.invokeLater(this.boardOverlays::showLost);
    }

    private void gameWon() {
        LOG.info("Game won, score={}", this.context.getScore());
        this.gameStopped = true;
        SwingUtilities.invokeLater(this.boardOverlays::showWon);
    }

    public void startLevel() {
        this.engine.startLevel();
        this.setWavePreview();
    }

    /** Called from doGameTick() on the game-loop thread, not the EDT. */
    public void doTick(int time) {
        boolean waveStarted = this.engine.doTick(time);
        if (waveStarted) {
            SwingUtilities.invokeLater(() -> {
                this.setWavePreview();
                this.updateInfo();
            });
        }
        this.gameConsole.getWaveInfo().doTick(time);
    }

    /**
     * The single place that changes tick speed - this owns keeping the
     * play/pause button visibility consistent with it, so no caller has to
     * remember to do that separately.
     */
    private void setSpeed(TickSpeed speed) {
        this.currentSpeed = speed;
        this.gameLoop.setSpeed(speed);
        this.gameConsole.setPlaying(speed != TickSpeed.PAUSED);
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

        RenderFrame frame = this.boardRenderer.buildFrame(time, this.gameLoop.tickInterpolationAlpha(), this.gameLoop.animationSeconds());
        this.frameRenderer.paint(g2, frame);

        this.painting = false;
    }

    public void startPlacing(TowerFactory.type t, float r) {
        this.engine.startPlacing(t, r);
    }

    public void unSelectTower() {
        this.engine.unSelectTower();
        this.gameConsole.getTowerInfo().unselectTower();
        this.gameConsole.getTowerInfo().setExternalText(this.statusMessage);
    }

    public void clearCell(int x, int y) {
        this.engine.clearCell(x, y);
    }

    private void jPanel_boardMouseClicked(MouseEvent evt) {
        if (!this.levelLoaded) {
            return;
        }
        this.unSelectTower();
        boolean wasPlacing = this.engine.isPlacingTower();
        int boardX = evt.getX() - this.gameBoard.getX();
        int boardY = evt.getY() - this.gameBoard.getY();
        Tower selected = this.engine.mouseClicked(boardX, boardY);
        if (selected != null) {
            this.gameConsole.getTowerInfo().setTower(selected);
        }
        if (wasPlacing && !this.engine.isPlacingTower()) {
            this.panelTowerSelector.stopPlacing();
        }
    }

    private void jPanel_boardMouseMoved(MouseEvent evt) {
        if (!this.levelLoaded) {
            return;
        }
        this.requestFocusInWindow();
        if (this.engine.isPlacingTower()) {
            this.engine.highlightCell(evt.getX() - this.gameBoard.getX(), evt.getY() - this.gameBoard.getY());
        }
    }

    private void keyTyped(char key) {
        if (!this.levelLoaded) {
            return;
        }
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
        jPanel_game = new JPanel();
        panelTowerSelector = new PanelTowerSelector();
        panelLevelSelect = new PanelLevelSelect(this.levelCatalog.levels(), this::startSelectedLevel);

        contentCardLayout = new CardLayout();
        getContentPane().setLayout(contentCardLayout);
        jPanel_game.setLayout(new GridBagLayout());

        jPanel_board.setLayout(new GridBagLayout());

        jPanel_board.setBackground(new Color(0, 0, 0));
        jPanel_board.setForeground(new Color(220, 255, 220));
        jPanel_board.setBorder(BorderFactory.createEtchedBorder());
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

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        this.boardOverlays.addTo(jPanel_board, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        jPanel_game.add(jPanel_board, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_END;
        gridBagConstraints.weighty = 1.0;
        jPanel_game.add(this.gameConsole, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_END;
        jPanel_game.add(panelTowerSelector, gridBagConstraints);

        getContentPane().add(panelLevelSelect, CARD_MENU);
        getContentPane().add(jPanel_game, CARD_GAME);
        contentCardLayout.show(getContentPane(), CARD_MENU);
    }

    private void playPressed() {
        boolean wasPaused = this.currentSpeed == TickSpeed.PAUSED;
        this.setSpeed(TickSpeed.NORMAL);
        if (this.engine.isWaveReady() && wasPaused) this.engine.requestNextWave();
    }

    private void pausePressed() {
        this.setSpeed(TickSpeed.PAUSED);
    }

    private void fastPressed() {
        this.setSpeed(TickSpeed.FAST);
    }

    private void superFastPressed() {
        this.setSpeed(TickSpeed.SUPER_FAST);
    }

    private void formKeyTyped(KeyEvent evt) {
        this.keyTyped(evt.getKeyChar());
    }

}
