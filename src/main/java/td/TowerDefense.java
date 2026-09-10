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
import td.ui.Hud;
import td.ui.Java2DFrameRenderer;
import td.ui.PanelGameConsole;
import td.ui.PanelLevelSelect;
import td.ui.PanelTowerSelector;
import td.ui.render.AsciiBoardRenderer;
import td.ui.render.RenderFrame;
import td.util.GameHost;
import td.util.GameWorld;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.util.Properties;

// Inspired by HexTD
/**
 * The application window, and the wiring between the headless {@link GameEngine} and Swing.
 * It owns the {@link GameLoop}, translates mouse and key events into engine calls, and is
 * itself the {@link GameHost} the engine calls back through - so this is the only class that
 * sits on both sides of the headless/Swing boundary described in CLAUDE.md.
 * <p>
 * The content pane is a {@link CardLayout} with two cards: the level-select menu and the
 * game. Nothing ticks and no board exists until a level is chosen, and
 * {@link #startSelectedLevel} is re-enterable - returning to the menu and picking another
 * level runs it again on the same engine and the same loop, both of which are documented as
 * safe to restart.
 * <p>
 * Two of the callbacks implemented here ({@link #enemyDied}, {@link #economyChanged}) are
 * reached from the {@code game-loop} thread, not the EDT. Swing mutations in those must be
 * deferred through {@code SwingUtilities.invokeLater}; see CLAUDE.md's Threading model.
 * <p>
 * This class is a shrinking legacy shell: new gameplay rules belong in {@code GameEngine} or
 * the domain packages, where they can be tested without a display.
 */
public class TowerDefense extends JFrame implements EconomyListener, GameHost {

    private static final Logger LOG = LoggerFactory.getLogger(TowerDefense.class);

    @Serial
    private static final long serialVersionUID = 1L;
    private static final String NAME = "Tower Defense";
    static final String VERSION = loadVersion();
    private static final String CARD_MENU = "menu";
    private static final String CARD_GAME = "game";
    private static final int MENU_WIDTH = 1040;
    private static final int MENU_HEIGHT = 700;

    private final LevelCatalog levelCatalog = new BuiltInLevelCatalog();

    private final GameEngine engine;
    private final GameWorld gameWorld;
    private final GameBoard gameBoard;
    private final BoardRenderer boardRenderer;
    private final Java2DFrameRenderer frameRenderer = new Java2DFrameRenderer();
    private final AsciiBoardRenderer asciiBoardRenderer = new AsciiBoardRenderer();
    private final PanelGameConsole gameConsole = new PanelGameConsole(NAME + " v" + VERSION);
    private final BoardOverlays boardOverlays = new BoardOverlays();
    private final String statusMessage = """
            Welcome to Tower Defense
            Shortcuts:

            q - build triangle
            w - build circle
            e - build sunshine
            r - build stardust
            t - build power
            esc - cancel placing
            p - pause
            f - cycle speed
            s - start wave
            m - back to menu""";

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
    // Guards input and the game loop against firing before a level has finished loading (the
    // JFrame-level KeyListener is live the instant the frame is shown, well before any level
    // and its cellGrid exists) or after returnToMenu() has torn one down. Cleared at the start
    // of startSelectedLevel()/returnToMenu() and set again once startSelectedLevel() finishes.
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

    /**
     * Reads the version baked into {@code version.properties} by Maven resource filtering
     * (see {@code pom.xml}), so the displayed/logged version always matches the pom's
     * {@code <version>} rather than a second hand-maintained copy.
     */
    private static String loadVersion() {
        try (InputStream in = TowerDefense.class.getResourceAsStream("/version.properties")) {
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("version");
        } catch (IOException e) {
            LOG.error("Failed to load version.properties", e);
            return "unknown";
        }
    }

    public TowerDefense() {
        this.engine = new GameEngine(this);
        this.gameWorld = this.engine.getGameWorld();
        this.boardRenderer = new BoardRenderer(this.engine, this.gameWorld.getEnemyRegistry(),
                this.gameWorld::getBoard, this.gameWorld::getPath);
        this.gameWorld.addEconomyListener(this);
        this.gameBoard = new GameBoard(this, this.gameWorld);
        initComponents();
        this.gameConsole.setGameWorld(this.gameWorld);
        this.gameConsole.onPlay(this::playPressed);
        this.gameConsole.onPause(this::pausePressed);
        this.gameConsole.onFast(this::fastPressed);
        this.gameConsole.onSuperFast(this::superFastPressed);
        this.boardOverlays.onBackToMenu(this::requestReturnToMenu);
        this.panelTowerSelector.doInit(this.gameWorld, this);

        // Explicitly the same cell the win/lose overlays occupy, so the two stack rather than
        // sitting side by side. Added with no constraints, the board landed in a RELATIVE cell
        // of its own and got shoved out of view the moment an overlay claimed cell (0,0) -
        // which is what made the board go black on a win or a loss.
        GridBagConstraints boardConstraints = new GridBagConstraints();
        boardConstraints.gridx = 0;
        boardConstraints.gridy = 0;
        boardConstraints.fill = GridBagConstraints.BOTH;
        boardConstraints.weightx = 0.1;
        boardConstraints.weighty = 0.1;
        this.jPanel_board.add(this.gameBoard, boardConstraints);

        // CardLayout starts on the menu card; give the frame a size that fits it. The game
        // card gets its own size from recalculateBoard() once a level is actually loaded.
        this.setSize(MENU_WIDTH, MENU_HEIGHT);
        this.setLocationRelativeTo(null);
    }

    /**
     * Runs one logic tick, called from the game loop thread. GameLoop itself
     * already skips calling this while TickSpeed.PAUSED is selected, so the
     * only remaining guard here is for after the game has ended. Also called
     * directly from the EDT by fastPressed() to single-step while paused -
     * safe because GameLoop guarantees it never calls this concurrently in
     * that state.
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
     * The UI refresh pulse, reached from the game loop via SwingUtilities.invokeLater and so
     * always running on the EDT. Repaints the board, and refreshes the side panel's live
     * per-tower stats - both at a flat ~60fps regardless of tick speed, rather than once per
     * tick. Skips the repaint while a previous paint is still in flight.
     * <p>
     * Deliberately keeps running after the game has ended, unlike {@link #doGameTick}: the
     * simulation is frozen at that point, but the player can still look around the final
     * board and click towers to read their stats, and both need the board to keep painting.
     */
    private void requestRender() {
        this.gameConsole.getTowerInfo().refreshSelected();
        if (!this.painting) {
            // The container, not the board component: the win/lose overlay is a sibling
            // stacked on top of the board, and a JPanel claims its children never overlap.
            // Repainting the board alone therefore paints over the overlay without painting
            // the overlay back, and the message disappears on the next frame.
            this.jPanel_board.repaint();
        }
    }

    /**
     * The level-select screen's callback - also the target of returning from a level in
     * progress and picking a (possibly different) one, so it is written to run any number of
     * times, not just once. Safe to call from a dirty state: {@link GameEngine#loadLevel} is
     * itself idempotent, and {@link GameLoop#start}/{@code stop} now tolerate being called
     * more than once, so a fast double-click on a level card just tears the same level down
     * and rebuilds it rather than leaving two loops ticking one engine.
     */
    private void startSelectedLevel(LevelDefinition level) {
        this.gameLoop.stop();
        this.levelLoaded = false;
        this.setGameStopped(false);
        this.boardOverlays.reset();
        this.unSelectTower();
        this.panelTowerSelector.stopPlacing();
        this.engine.loadLevel(level);
        this.contentCardLayout.show(getContentPane(), CARD_GAME);
        this.gameBoard.recalculateBoard(level.width(), level.height());
        this.setLocationRelativeTo(null);
        this.startLevel();
        synchronized (this.gameTimeLock) {
            this.gameTime = 0;
        }
        this.levelLoaded = true;
        this.setSpeed(TickSpeed.NORMAL);
        this.gameLoop.start();
        this.requestFocusInWindow();
    }

    /**
     * The single route back to the level-select menu, shared by each overlay's "Back to menu"
     * button and the 'm' shortcut. Confirms first if the level is still in progress, so a
     * misclick or stray keypress can't destroy a long run; skips the dialog once the level has
     * already ended (the overlay button case), where there is nothing left to abandon.
     */
    private void requestReturnToMenu() {
        if (!this.levelLoaded) {
            return;
        }
        if (!this.gameStopped && !this.confirmAbandonLevel()) {
            return;
        }
        this.returnToMenu();
    }

    /**
     * Tears down no engine state - {@link GameEngine#loadLevel} already makes a fresh
     * {@link #startSelectedLevel} safe from any prior state, so this only needs to undo what
     * it, specifically, set up: the running loop, the UI showing the board, and the overlays/
     * selections a level in progress leaves behind.
     */
    private void returnToMenu() {
        this.gameLoop.stop();
        this.levelLoaded = false;
        this.setSpeed(TickSpeed.PAUSED);
        this.boardOverlays.reset();
        this.unSelectTower();
        this.gameConsole.getWaveInfo().clearWaves();
        this.panelTowerSelector.stopPlacing();
        this.contentCardLayout.show(getContentPane(), CARD_MENU);
        this.setSize(MENU_WIDTH, MENU_HEIGHT);
        this.setLocationRelativeTo(null);
    }

    /**
     * Pauses for the duration of the modal (the EDT is blocked by it anyway, but the
     * game-loop thread is not) and restores whatever speed was active if the answer is "no".
     */
    private boolean confirmAbandonLevel() {
        TickSpeed previousSpeed = this.currentSpeed;
        this.setSpeed(TickSpeed.PAUSED);
        boolean confirmed = JOptionPane.showConfirmDialog(this,
                "Abandon this level and return to the menu?", "Back to menu",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        if (!confirmed) {
            this.setSpeed(previousSpeed);
        }
        return confirmed;
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
            SwingUtilities.invokeLater(this::syncTransportButtons);
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
            this.syncTransportButtons();
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
        LOG.info("Game over - lost, score={}", this.gameWorld.getScore());
        this.setGameStopped(true);
        SwingUtilities.invokeLater(this.boardOverlays::showLost);
    }

    private void gameWon() {
        LOG.info("Game won, score={}", this.gameWorld.getScore());
        this.setGameStopped(true);
        SwingUtilities.invokeLater(this.boardOverlays::showWon);
    }

    /**
     * The single place the level-over flag moves, so every part of the UI that has to refuse
     * moves in a decided game stays in step with it. Reachable from the game-loop thread (both
     * endings are), hence the Swing work is deferred to the EDT.
     */
    private void setGameStopped(boolean stopped) {
        this.gameStopped = stopped;
        SwingUtilities.invokeLater(() -> this.gameConsole.getTowerInfo().setLevelEnded(stopped));
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
                this.syncTransportButtons();
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
        this.syncTransportButtons();
    }

    /**
     * Shows play whenever pressing it would do something the player is waiting for - the game
     * is paused, or a wave is sitting ready to be sent - and pause only while a wave is
     * actually running. Speed alone is not enough to decide this: between waves the loop is
     * still ticking at normal speed with nothing on the board, and offering "pause" there is
     * what made starting the first wave take two clicks.
     */
    private void syncTransportButtons() {
        boolean waveRunning = this.currentSpeed != TickSpeed.PAUSED && !this.engine.isWaveReady();
        this.gameConsole.setPlaying(waveRunning);
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
        // See keyTyped: once the level has ended a board click may still select a tower to
        // inspect, but must not place one.
        if (this.gameStopped && this.engine.isPlacingTower()) {
            this.engine.cancelPlacing();
            this.panelTowerSelector.stopPlacing();
        }
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
        if (key == 'm') {
            this.requestReturnToMenu();
            return;
        }
        // The board stays visible and selectable after a win or a loss, but the level is over:
        // nothing that would change its outcome - building, selling, sending a wave, changing
        // speed - still applies. Leaving the menu is the only way on from here.
        if (this.gameStopped) {
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
        jPanel_board.setBorder(Hud.outlineBorder());
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

    /**
     * Resumes, and sends the next wave if one is ready - the same thing the 's' shortcut does.
     * Deliberately not conditioned on having been paused first: at level start, and again after
     * each wave is cleared, the loop is already running at normal speed and the only thing the
     * player is waiting to do is send the next wave.
     */
    private void playPressed() {
        this.setSpeed(TickSpeed.NORMAL);
        if (this.engine.isWaveReady()) {
            this.engine.requestNextWave();
        }
    }

    private void pausePressed() {
        this.setSpeed(TickSpeed.PAUSED);
    }

    /** While paused, ">>" single-steps one tick instead of changing tick speed. */
    private void fastPressed() {
        if (this.currentSpeed == TickSpeed.PAUSED) {
            this.doGameTick();
            return;
        }
        this.setSpeed(TickSpeed.FAST);
    }

    private void superFastPressed() {
        this.setSpeed(TickSpeed.SUPER_FAST);
    }

    private void formKeyTyped(KeyEvent evt) {
        this.keyTyped(evt.getKeyChar());
    }

}
