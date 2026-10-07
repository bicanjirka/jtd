package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.board.BoardGeometry;
import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.level.BuiltInLevelCatalog;
import td.level.LevelCatalog;
import td.level.LevelDefinition;
import td.level.LevelOutcome;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.ui.BoardOverlays;
import td.ui.BoardRenderer;
import td.ui.GameBoard;
import td.ui.Hud;
import td.ui.Java2DFrameRenderer;
import td.ui.PanelDev;
import td.ui.PanelGameConsole;
import td.ui.PanelLevelSelect;
import td.ui.PanelTowerSelector;
import td.ui.render.AsciiBoardRenderer;
import td.ui.render.InfoSheet;
import td.ui.render.RenderFrame;
import td.util.GameHost;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.util.Threads;
import td.wave.WaveProgress;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

// Inspired by HexTD

/**
 * The application window and the only class on both sides of the headless/Swing boundary: it owns
 * the {@link GameLoop}, turns input into engine calls, and is the engine's {@link GameHost}.
 * <p>
 * A {@link CardLayout} switches between the level-select menu and the game.
 * {@link #startSelectedLevel} is re-enterable on the same engine and loop.
 * <p>
 * {@link #doTick} and {@link #economyChanged} run on the {@code game-loop} thread, so their Swing
 * work goes through {@code invokeLater}. New gameplay rules belong in the engine, not here.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class TowerDefense extends JFrame implements EconomyListener, GameHost {

    private static final Logger LOG = LoggerFactory.getLogger(TowerDefense.class);
    static final String VERSION = loadVersion();
    @Serial
    private static final long serialVersionUID = 1L;
    private static final String NAME = "Tower Defense";
    private static final String CARD_MENU = "menu";
    private static final String CARD_GAME = "game";
    private static final int MENU_WIDTH = 1040;
    private static final int MENU_HEIGHT = 700;
    /** Credits one press of the debug grant key adds. */
    private static final int DEBUG_CREDIT_GRANT = 1000;
    private static final KeyStroke DEV_PANEL_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_D,
            InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);

    private final LevelCatalog levelCatalog = new BuiltInLevelCatalog();

    private final GameEngine engine;
    private final GameWorld gameWorld;
    private final DevControls devControls;
    private final GameBoard gameBoard;
    private final BoardRenderer boardRenderer;
    private final Java2DFrameRenderer frameRenderer = new Java2DFrameRenderer();
    private final AsciiBoardRenderer asciiBoardRenderer = new AsciiBoardRenderer();
    private final PanelGameConsole gameConsole = new PanelGameConsole(NAME + " v" + VERSION);
    private final BoardOverlays boardOverlays = new BoardOverlays();
    private final String statusMessage = welcomeText();

    private final GameLoop gameLoop = new GameLoop(this::doGameTick, this::buildAndPublishFrame);
    /**
     * Runs {@link GameLoop#stop()}, which must not block the EDT. Single-threaded so two level
     * changes cannot interleave their teardowns.
     */
    private final ExecutorService lifecycleExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "game-lifecycle");
        t.setDaemon(true);
        return t;
    });
    /** Bumped on the EDT per level change, so a superseded install drops itself. */
    private final AtomicInteger levelGeneration = new AtomicInteger();
    // Reset on the EDT only while the loop is stopped, so there is one writer at a time.
    private volatile int gameTime;
    private volatile RenderFrame latestFrame;
    private boolean painting = false;
    // Set by a board click that asked for an enemy; cleared with every deselection.
    private boolean inspectingEnemy = false;
    private TickSpeed currentSpeed = TickSpeed.NORMAL;
    // Input and the loop can fire before a level exists and after one is torn down.
    private volatile boolean levelLoaded = false;

    private CardLayout contentCardLayout;
    private JPanel jPanel_game;
    private PanelLevelSelect panelLevelSelect;
    private PanelTowerSelector panelTowerSelector;
    private PanelDev panelDev;
    /** The window's size before the dev panel opened; {@code null} while it is closed. */
    private Dimension sizeWithoutDevPanel;
    private LevelDefinition currentLevel;
    private JPanel jPanel_board;

    /**
     * Builds the component tree on the EDT. The frame is shown last, so it is never realized before
     * its components exist.
     */
    public TowerDefense() {
        Threads.assertEventDispatchThread("TowerDefense construction");
        this.setLayout(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle(NAME);
        this.setFocusable(true);
        this.engine = new GameEngine(this);
        this.gameWorld = this.engine.getGameWorld();
        this.devControls = new DevControls(this.gameWorld);
        this.boardRenderer = new BoardRenderer(this.gameWorld);
        this.gameWorld.economy().addEconomyListener(this);
        this.gameWorld.economy().addEconomyListener(this.devControls);
        this.gameBoard = new GameBoard(this, this.gameWorld);
        initComponents();
        this.gameConsole.setGameWorld(this.gameWorld);
        this.gameConsole.onPlay(this::playPressed);
        this.gameConsole.onPause(this::pausePressed);
        this.gameConsole.onFast(this::fastPressed);
        this.gameConsole.onSuperFast(this::superFastPressed);
        this.gameConsole.getWaveInfo().onEnemyHover(sheet -> {
            this.unSelectTower();
            this.gameConsole.getTowerInfo().showSheet(sheet);
        });
        this.boardOverlays.onBackToMenu(this::requestReturnToMenu);
        this.panelTowerSelector.doInit(this.gameWorld, this);
        this.panelDev.setGameWorld(this.gameWorld, this.devControls);
        this.panelDev.onSkipWave(this::debugSkipWave);
        this.panelDev.onJumpToWave(this::debugJumpToWave);
        this.panelDev.onRestart(this::restartLevel);
        this.panelDev.onStep(this::stepOneTick);
        this.panelDev.onResetTower(this::rebuildSelectedTower);
        this.panelDev.onCellGrid(this.boardRenderer::setCellGridShown);
        this.panelDev.onInfoText(this::setInfoText);
        this.panelDev.onReleaseFocus(this::requestFocusInWindow);
        // A dispatcher, not a key binding: the frame itself usually holds the focus, and a
        // Window runs no bindings. It also sees keys while a dev panel field has the focus.
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this::dispatchShortcut);

        // Same cell as the win/lose overlays so they stack; in a cell of its own the board gets
        // pushed out of view when an overlay appears.
        GridBagConstraints boardConstraints = new GridBagConstraints();
        boardConstraints.gridx = 0;
        boardConstraints.gridy = 0;
        boardConstraints.fill = GridBagConstraints.BOTH;
        boardConstraints.weightx = 0.1;
        boardConstraints.weighty = 0.1;
        this.jPanel_board.add(this.gameBoard, boardConstraints);

        this.setSize(MENU_WIDTH, MENU_HEIGHT);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    /**
     * Reads the version Maven filters into {@code version.properties}, so it always matches the
     * pom.
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

    /**
     * One logic tick, on the game-loop thread. Also called on the EDT to single-step while paused,
     * when the loop is not ticking.
     */
    /** The welcome text: a line per tower from the types themselves, so a shop name never drifts from its key. */
    static String welcomeText() {
        StringBuilder text = new StringBuilder("Welcome to Tower Defense\nShortcuts:\n\n");
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            text.append(type.placementKey).append(" - build ").append(type.displayName().toLowerCase(Locale.ROOT))
                    .append('\n');
        }
        return text.append("esc - cancel placing\np - pause\nf - cycle speed\ns - start wave\nm - back to menu\n"
                + "ctrl+shift+d - dev panel").toString();
    }

    private void doGameTick() {
        if (this.engine.outcome().isOver()) {
            return;
        }
        int time = ++this.gameTime;
        this.doTick(time);
        // Alpha 0: a tick-boundary dump wants the tick's result, not an interpolation.
        if (LOG.isDebugEnabled()) {
            LOG.debug("Board state after tick {}:\n{}", time,
                    this.asciiBoardRenderer.render(this.boardRenderer.buildFrame(time, 0.0, 0.0)));
        }
    }

    /**
     * The render pulse, on the {@code game-loop} thread: builds an immutable {@link RenderFrame},
     * publishes it and asks the EDT to paint it, so the EDT never reads live simulation state.
     * <p>
     * Keeps running after the game ends, so the final board can still be inspected.
     */
    private void buildAndPublishFrame() {
        this.devControls.runPending();
        int time = this.gameTime;
        this.latestFrame = this.boardRenderer.buildFrame(time,
                this.gameLoop.tickInterpolationAlpha(), this.gameLoop.animationSeconds(),
                this.engine.inspectSelectedEnemy());
        SwingUtilities.invokeLater(this::repaintPublishedFrame);
    }

    /**
     * Repaints from the last published frame and refreshes the side panel's live tower stats,
     * skipping the repaint while one is still in flight.
     */
    private void repaintPublishedFrame() {
        this.gameConsole.refreshSelected();
        this.showEnemyInspection();
        // Cosmetic animation runs on the EDT render cadence, never in tick code.
        this.gameConsole.getWaveInfo().doTick(this.gameTime);
        if (this.panelDev.isVisible()) {
            this.panelDev.doTick(this.gameTime);
        }
        if (!this.painting) {
            // The container, not the board: repainting the board alone paints over the stacked
            // overlay without repainting it.
            this.jPanel_board.repaint();
        }
    }

    /**
     * The level-select callback. Safe to run any number of times and from a dirty state, including
     * a fast double-click.
     */
    private void startSelectedLevel(LevelDefinition level) {
        this.stopLoopThen(() -> this.installLevel(level));
    }

    /**
     * Stops the loop off the EDT, then runs {@code installOnEdt} on the EDT. Because that is
     * asynchronous, {@link #levelGeneration} is bumped here so an install for a superseded request
     * drops itself.
     */
    private void stopLoopThen(Runnable installOnEdt) {
        Threads.assertEventDispatchThread("Level lifecycle change");
        int generation = this.levelGeneration.incrementAndGet();
        this.levelLoaded = false;
        this.lifecycleExecutor.execute(() -> {
            this.gameLoop.stop();
            SwingUtilities.invokeLater(() -> {
                if (this.levelGeneration.get() == generation) {
                    installOnEdt.run();
                }
            });
        });
    }

    private void installLevel(LevelDefinition level) {
        this.currentLevel = level;
        this.gameConsole.setLevelEnded(false);
        this.boardOverlays.reset();
        this.unSelectTower();
        this.panelTowerSelector.stopPlacing();
        this.latestFrame = null;
        this.engine.loadLevel(level);
        this.contentCardLayout.show(getContentPane(), CARD_GAME);
        this.gameBoard.recalculateBoard(level.width(), level.height());
        this.sizeWithoutDevPanel = null;
        this.panelDev.refresh();
        this.fitDevPanel();
        this.setLocationRelativeTo(null);
        this.startLevel();
        this.gameTime = 0;
        this.levelLoaded = true;
        this.setSpeed(TickSpeed.NORMAL);
        this.gameLoop.start();
        this.requestFocusInWindow();
    }

    /** The single route back to the menu. Confirms first while the level is still in progress. */
    private void requestReturnToMenu() {
        if (!this.levelLoaded) {
            return;
        }
        if (!this.engine.outcome().isOver() && !this.confirmAbandonLevel()) {
            return;
        }
        this.returnToMenu();
    }

    /**
     * Undoes only what {@link #startSelectedLevel} set up; the engine is reset by the next level
     * load.
     */
    private void returnToMenu() {
        this.stopLoopThen(this::showMenu);
    }

    private void showMenu() {
        this.setSpeed(TickSpeed.PAUSED);
        this.boardOverlays.reset();
        this.unSelectTower();
        this.gameConsole.getWaveInfo().clearWaves();
        this.panelTowerSelector.stopPlacing();
        this.panelDev.setVisible(false);
        this.boardRenderer.setCellGridShown(false);
        this.sizeWithoutDevPanel = null;
        this.contentCardLayout.show(getContentPane(), CARD_MENU);
        this.setSize(MENU_WIDTH, MENU_HEIGHT);
        this.setLocationRelativeTo(null);
    }

    /**
     * Pauses while the dialog is open, since the loop keeps running behind a modal, and restores
     * the speed on "no".
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

    public void showInfoSheet(InfoSheet sheet) {
        this.unSelectTower();
        this.gameConsole.getTowerInfo().showSheet(sheet);
    }

    private void setWavePreview() {
        WaveProgress progress = this.engine.waveProgress();
        this.gameConsole.getWaveInfo().clearWaves();
        if (!progress.current().isEmpty()) {
            this.gameConsole.getWaveInfo().setWaveCur(progress.currentNumber(), progress.current());
        }
        if (!progress.next().isEmpty()) {
            this.gameConsole.getWaveInfo().setWaveNext(progress.nextNumber(), progress.next());
        }
    }

    private void nextWave() {
        if (this.engine.nextWave()) {
            this.setWavePreview();
            this.updateInfo();
            this.syncTransportButtons();
        }
    }

    private void debugSkipWave() {
        this.engine.debugSkipCurrentWave();
        this.setWavePreview();
        this.updateInfo();
        this.syncTransportButtons();
        this.showOutcome(this.engine.outcome());
    }

    private void debugJumpToWave(int number) {
        int next = this.engine.debugJumpToWave(number);
        this.setWavePreview();
        this.updateInfo();
        this.syncTransportButtons();
        this.panelDev.showMessage(next == 0 ? "No level in play" : "Wave " + next + " is next");
    }

    /** Opens or closes the dev panel. The cell grid shows only while the panel is open. */
    private void toggleDevPanel() {
        if (!this.levelLoaded) {
            return;
        }
        boolean show = !this.panelDev.isVisible();
        this.panelDev.refresh();
        this.panelDev.setVisible(show);
        this.boardRenderer.setCellGridShown(show && this.panelDev.isCellGridOn());
        if (!show) {
            this.requestFocusInWindow();
        }
        this.fitDevPanel();
    }

    /**
     * Grows the window to fit the open dev panel, wider too when the board is narrower than the
     * panel, and keeps it on screen; closing the panel restores the size it had.
     * <p>
     * At least the game card's preferred size: below it, {@link GridBagLayout} falls back to
     * minimum sizes and the board collapses.
     */
    private void fitDevPanel() {
        if (this.panelDev.isVisible()) {
            if (this.sizeWithoutDevPanel == null) {
                this.sizeWithoutDevPanel = this.getSize();
            }
            Dimension game = this.jPanel_game.getPreferredSize();
            Insets frame = this.getInsets();
            this.setSize(Math.max(this.sizeWithoutDevPanel.width, game.width + frame.left + frame.right),
                    Math.max(this.sizeWithoutDevPanel.height + this.panelDev.getPreferredSize().height,
                            game.height + frame.top + frame.bottom));
        } else if (this.sizeWithoutDevPanel != null) {
            this.setSize(this.sizeWithoutDevPanel);
            this.sizeWithoutDevPanel = null;
        }
        this.keepOnScreen();
        this.validate();
    }

    private void keepOnScreen() {
        GraphicsConfiguration screen = this.getGraphicsConfiguration();
        Rectangle bounds = screen.getBounds();
        Insets taskbar = Toolkit.getDefaultToolkit().getScreenInsets(screen);
        int bottom = bounds.y + bounds.height - taskbar.bottom;
        int y = Math.max(bounds.y + taskbar.top, Math.min(this.getY(), bottom - this.getHeight()));
        this.setLocation(this.getX(), y);
    }

    /**
     * Ctrl+Shift+D anywhere in this window, and the dev panel's own shortcuts while it shows.
     *
     * @return whether the key was used up
     */
    private boolean dispatchShortcut(KeyEvent evt) {
        if (evt.getID() != KeyEvent.KEY_PRESSED || !this.isFocused()) {
            return false;
        }
        KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(evt);
        if (stroke.equals(DEV_PANEL_KEY)) {
            this.toggleDevPanel();
            return true;
        }
        return this.panelDev.runShortcut(stroke);
    }

    private void restartLevel() {
        if (this.currentLevel != null) {
            this.startSelectedLevel(this.currentLevel);
        }
    }

    /** Pauses if needed, then runs exactly one tick. */
    private void stepOneTick() {
        if (this.currentSpeed != TickSpeed.PAUSED) {
            this.setSpeed(TickSpeed.PAUSED);
        }
        this.doGameTick();
    }

    /**
     * Sells the selected tower and builds a fresh one of its type on its cell, through the same
     * input path a player uses, then selects it: its upgrades and XP start over.
     */
    private void rebuildSelectedTower() {
        Optional<Tower> selected = this.devControls.selectedTower();
        if (selected.isEmpty()) {
            this.panelDev.showMessage("Select a tower first");
            return;
        }
        Tower tower = selected.get();
        BoardGeometry board = this.gameWorld.getBoard();
        int scale = board.scale();
        int boardX = board.cellX(tower.getX()) * scale + scale / 2;
        int boardY = board.cellY(tower.getY()) * scale + scale / 2;
        this.unSelectTower();
        this.gameWorld.towers().sell(tower);
        this.engine.startPlacing(tower.getType(), 0f);
        this.engine.mouseClicked(boardX, boardY);
        this.engine.mouseClicked(boardX, boardY).ifPresent(this.gameConsole::selectTower);
        this.panelDev.showMessage("Rebuilt fresh: upgrades and XP reset");
    }

    private void updateInfo() {
        WaveProgress progress = this.engine.waveProgress();
        this.gameConsole.setWaveProgress(progress.index(), progress.count());
    }

    /** Runs on the game-loop thread too. */
    public void economyChanged(EconomyState state) {
        SwingUtilities.invokeLater(this::updateInfo);
    }

    /** Callers pass an outcome only on the transition, so each overlay shows once. */
    private void showOutcome(LevelOutcome outcome) {
        this.syncTransportButtons();
        switch (outcome) {
            case PLAYING -> {
            }
            case WON -> {
                this.gameConsole.setLevelEnded(true);
                this.boardOverlays.showWon();
            }
            case LOST -> {
                this.gameConsole.setLevelEnded(true);
                this.boardOverlays.showLost();
            }
        }
    }

    public void startLevel() {
        this.engine.startLevel();
        this.setWavePreview();
    }

    /** Runs on the game-loop thread. */
    public void doTick(int time) {
        boolean wasReady = this.engine.isWaveReady();
        boolean waveStarted = this.engine.doTick(time);
        if (waveStarted) {
            SwingUtilities.invokeLater(() -> {
                this.setWavePreview();
                this.updateInfo();
                this.syncTransportButtons();
            });
        } else if (this.engine.isWaveReady() != wasReady) {
            SwingUtilities.invokeLater(this::syncTransportButtons);
        }
        LevelOutcome outcome = this.engine.outcome();
        if (outcome.isOver()) {
            SwingUtilities.invokeLater(() -> this.showOutcome(outcome));
        }
    }

    /** The single place tick speed changes, keeping the transport buttons in step. */
    private void setSpeed(TickSpeed speed) {
        this.currentSpeed = speed;
        this.gameLoop.setSpeed(speed);
        this.syncTransportButtons();
    }

    /**
     * Shows play when pressing it would do what the player waits for - unpause, or send a ready
     * wave - and pause only while a wave runs. Speed alone cannot decide this: between waves the
     * loop runs at normal speed with an empty board, and after the level ends no wave runs at all.
     */
    private void syncTransportButtons() {
        boolean waveRunning = this.currentSpeed != TickSpeed.PAUSED && !this.engine.isWaveReady()
                && !this.engine.outcome().isOver();
        this.gameConsole.setPlaying(waveRunning);
    }

    private void togglePause() {
        this.setSpeed(this.currentSpeed == TickSpeed.PAUSED ? TickSpeed.NORMAL : TickSpeed.PAUSED);
    }

    private void cycleSpeed() {
        this.setSpeed(this.currentSpeed.next());
    }

    /** Paints the last published frame; reads no simulation state. */
    public void paintBoard(Graphics2D g2) {
        RenderFrame frame = this.latestFrame;
        if (frame == null) {
            return;
        }
        this.painting = true;
        this.frameRenderer.paint(g2, frame);
        this.painting = false;
    }

    public void startPlacing(TowerFactory.Type t, float r) {
        this.engine.startPlacing(t, r);
    }

    public void unSelectTower() {
        this.engine.unSelectTower();
        this.engine.clearEnemySelection();
        this.inspectingEnemy = false;
        this.gameConsole.unselectTower();
        this.gameConsole.getTowerInfo().setExternalText(this.statusMessage);
    }

    public void clearCell(int x, int y) {
        this.engine.clearCell(x, y);
    }

    private void jPanel_boardMouseClicked(MouseEvent evt) {
        if (!this.levelLoaded) {
            return;
        }
        this.requestFocusInWindow();
        this.unSelectTower();
        boolean wasPlacing = this.engine.isPlacingTower();
        int boardX = evt.getX() - this.gameBoard.getX();
        int boardY = evt.getY() - this.gameBoard.getY();
        BoardGeometry board = this.gameWorld.getBoard();
        if (board.containsPixel(boardX, boardY)) {
            this.panelDev.showClickedCell(board.cellX(boardX), board.cellY(boardY));
        }
        Optional<Tower> clicked = this.engine.mouseClicked(boardX, boardY);
        clicked.ifPresent(this.gameConsole::selectTower);
        if (clicked.isEmpty() && !wasPlacing) {
            this.engine.requestEnemySelectionAt(boardX, boardY);
            this.inspectingEnemy = true;
        }
        if (wasPlacing && !this.engine.isPlacingTower()) {
            this.panelTowerSelector.stopPlacing();
        }
    }

    /**
     * Shows the selected enemy's inspector text from the published frame. Ignored until a click
     * requested a selection, so a frame built before a clear can't bring stale text back.
     */
    private void showEnemyInspection() {
        RenderFrame frame = this.latestFrame;
        if (!this.inspectingEnemy || frame == null || this.gameConsole.getTowerInfo().hasSelectedTower()) {
            return;
        }
        frame.enemyInspection().ifPresent(this.gameConsole.getTowerInfo()::showSheet);
    }

    private void jPanel_boardMouseMoved(MouseEvent evt) {
        if (!this.levelLoaded) {
            return;
        }
        // Not away from a dev panel field: glancing at the board mid-typing must not eat the text.
        if (!this.panelDev.holdsFocus()) {
            this.requestFocusInWindow();
        }
        int boardX = evt.getX() - this.gameBoard.getX();
        int boardY = evt.getY() - this.gameBoard.getY();
        if (this.engine.isPlacingTower()) {
            this.engine.highlightCell(boardX, boardY);
        }
        BoardGeometry board = this.gameWorld.getBoard();
        if (board.containsPixel(boardX, boardY)) {
            this.panelDev.showPointerCell(board.cellX(boardX), board.cellY(boardY));
        } else {
            this.panelDev.clearPointerCell();
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
        // After a win or loss the board stays inspectable, but nothing may change the outcome.
        if (this.engine.outcome().isOver()) {
            return;
        }
        // A digit buys the selected tower's offered upgrade by number; no placement key is a digit.
        if (key >= '1' && key <= '9') {
            this.engine.buyUpgradeForSelected(key - '0');
            this.gameConsole.refreshSelected();
            return;
        }
        TowerFactory.Type[] types = TowerFactory.Type.values();
        for (int i = 0; i < types.length; i++) {
            if (types[i].placementKey == key) {
                this.panelTowerSelector.doPlace(i);
                return;
            }
        }
        switch (key) {
            case 'p' -> this.togglePause();
            case 'f' -> this.cycleSpeed();
            case 's' -> this.nextWave();
            case 'n', 'x', 'c' -> {
                if (this.panelDev.isVisible()) {
                    this.debugKeyTyped(key);
                }
            }
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

    /** The debug keys, live only while the dev panel is open. */
    private void debugKeyTyped(char key) {
        switch (key) {
            case 'n' -> this.debugSkipWave();
            case 'x' -> this.setInfoText(this.engine.debugSpawnNextCatalogEnemy()
                    .map(id -> "Debug spawned: " + id)
                    .orElse("Debug spawn needs a level loaded first"));
            case 'c' -> this.engine.debugGrantCredits(DEBUG_CREDIT_GRANT);
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
        panelDev = new PanelDev(this::showInfoSheet);
        panelDev.setVisible(false);
        panelLevelSelect = new PanelLevelSelect(this.levelCatalog.levels(), this::startSelectedLevel);

        contentCardLayout = new CardLayout();
        getContentPane().setLayout(contentCardLayout);
        jPanel_game.setLayout(new GridBagLayout());
        jPanel_game.setBackground(new Color(0, 0, 0));

        jPanel_board.setLayout(new GridBagLayout());

        jPanel_board.setBackground(new Color(0, 0, 0));
        jPanel_board.setForeground(new Color(220, 255, 220));
        jPanel_board.setBorder(Hud.outlineBorder());
        jPanel_board.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                jPanel_boardMouseClicked(evt);
            }

            public void mouseExited(MouseEvent evt) {
                panelDev.clearPointerCell();
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
        // No fill: the board keeps its exact pixel size, centred in its cell.
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        jPanel_game.add(jPanel_board, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_END;
        gridBagConstraints.weighty = 1.0;
        jPanel_game.add(this.gameConsole, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.PAGE_END;
        jPanel_game.add(panelTowerSelector, gridBagConstraints);

        // Between the board and the tower buttons, but added last, so the run-jtd Driver's
        // component indices for everything else stay as they were.
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        jPanel_game.add(panelDev, gridBagConstraints);

        getContentPane().add(panelLevelSelect, CARD_MENU);
        getContentPane().add(jPanel_game, CARD_GAME);
        contentCardLayout.show(getContentPane(), CARD_MENU);
    }

    /** Resumes and sends the next wave if one is ready, whether or not the game was paused. */
    private void playPressed() {
        this.setSpeed(TickSpeed.NORMAL);
        if (this.engine.isWaveReady()) {
            this.engine.requestNextWave();
        }
    }

    private void pausePressed() {
        this.setSpeed(TickSpeed.PAUSED);
    }

    /** While paused, single-steps one tick instead of changing speed. */
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

    /** An Alt or Ctrl chord also types its letter; that letter is not a bare shortcut. */
    private void formKeyTyped(KeyEvent evt) {
        if (evt.isAltDown() || evt.isControlDown()) {
            return;
        }
        this.keyTyped(evt.getKeyChar());
    }

}
