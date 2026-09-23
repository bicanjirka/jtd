package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.economy.EconomyListener;
import td.economy.EconomyState;
import td.level.BuiltInLevelCatalog;
import td.level.LevelCatalog;
import td.level.LevelDefinition;
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
import td.util.ThreadConfined;
import td.util.Threads;
import td.wave.WaveProgress;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
 * deferred through {@code SwingUtilities.invokeLater}; see CLAUDE.md §3 (Threading).
 * <p>
 * This class is a shrinking legacy shell: new gameplay rules belong in {@code GameEngine} or
 * the domain packages, where they can be tested without a display.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
// Swing components and input state; the fields that genuinely cross are volatile
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
    /**
     * Debug keybinding: how many credits `c` grants in one press - see docs/features/FEATURE-playtesting-and-balance-tooling.md.
     */
    private static final int DEBUG_CREDIT_GRANT = 1000;

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

            q - build sniper
            w - build splash
            e - build sonar
            r - build pulse
            t - build aura
            y - build mortar
            u - build seeker
            i - build cinder
            esc - cancel placing
            p - pause
            f - cycle speed
            s - start wave
            m - back to menu""";

    private final GameLoop gameLoop = new GameLoop(this::doGameTick, this::buildAndPublishFrame);
    /**
     * Where {@link GameLoop#stop()} is called from. It joins the simulation thread without
     * bound, so it must not run on the EDT; single-threaded so two level changes in flight
     * cannot interleave their teardowns. See {@link #stopLoopThen}.
     */
    private final ExecutorService lifecycleExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "game-lifecycle");
        t.setDaemon(true);
        return t;
    });
    /**
     * Bumped on the EDT per level change, so a superseded install drops itself.
     */
    private final AtomicInteger levelGeneration = new AtomicInteger();
    // Incremented only by doGameTick on the game-loop thread, and read on the EDT by the
    // render pulse. It is reset to 0 by installLevel on the EDT, which is safe because
    // stopLoopThen has already joined the loop by then - there is never a second writer live
    // at the same time, so an independent volatile scalar is the whole requirement and the
    // lock this used to be guarded by is gone. See CLAUDE.md 3 rule 2.
    private volatile int gameTime;
    // The one channel simulation state takes to the EDT: built on the game-loop thread, which
    // owns that state, and read by paintBoard() on the EDT. Immutable, so publishing the
    // reference through this volatile publishes everything reachable from it. See CLAUDE.md 3.
    private volatile RenderFrame latestFrame;
    // requestRender() and paintBoard() are both always invoked on the EDT (the former via
    // SwingUtilities.invokeLater, the latter via Swing's own paint dispatch), so this needs
    // no synchronization of its own - it's just in-flight bookkeeping on a single thread.
    private boolean painting = false;
    // Single source of truth for both the tick speed and pause state (TickSpeed.PAUSED);
    // EDT-only, since it's only ever touched from button/key listeners.
    private TickSpeed currentSpeed = TickSpeed.NORMAL;
    // Set from the game-loop thread (gameLost/gameWon are reached from a tick) and from the
    // EDT (starting a level), and read on both. Volatile, not plain: this decides whether input
    // is refused and whether an ending has already been announced.
    private volatile boolean gameStopped = false;
    // Guards input and the game loop against firing before a level has finished loading (the
    // JFrame-level KeyListener is live the instant the frame is shown, well before any level
    // and its cellGrid exists) or after returnToMenu() has torn one down. Cleared by
    // stopLoopThen on the EDT and set again once installLevel finishes; volatile because
    // doGameTick reads it from the game-loop thread.
    private volatile boolean levelLoaded = false;

    private CardLayout contentCardLayout;
    private JPanel jPanel_game;
    private PanelLevelSelect panelLevelSelect;
    private PanelTowerSelector panelTowerSelector;
    private JPanel jPanel_board;

    /**
     * Builds the whole component tree. <strong>Must run on the Event Dispatch Thread</strong>
     * - see {@code Main}, which is why it is invoked through {@code invokeAndWait} rather than
     * called directly. The frame is shown on the last line rather than in an initializer
     * block, so it is never realized before the components it contains exist.
     */
    public TowerDefense() {
        Threads.assertEventDispatchThread("TowerDefense construction");
        this.setLayout(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle(NAME);
        this.setFocusable(true);
        this.engine = new GameEngine(this);
        this.gameWorld = this.engine.getGameWorld();
        this.boardRenderer = new BoardRenderer(this.gameWorld);
        this.gameWorld.economy().addEconomyListener(this);
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
        int time = ++this.gameTime;
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
     * The render pulse, called by {@link GameLoop} on the {@code game-loop} thread at a flat
     * ~60fps regardless of tick speed. Describes the board as an immutable {@link RenderFrame}
     * here, where the simulation state it walks is owned, then publishes it and asks the EDT
     * to paint what was published - so the EDT never reads a live enemy, tower or projectile.
     * <p>
     * Deliberately keeps running after the game has ended, unlike {@link #doGameTick}: the
     * simulation is frozen at that point, but the player can still look around the final
     * board and click towers to read their stats, and both need the board to keep painting.
     */
    private void buildAndPublishFrame() {
        int time = this.gameTime;
        this.latestFrame = this.boardRenderer.buildFrame(time,
                this.gameLoop.tickInterpolationAlpha(), this.gameLoop.animationSeconds());
        SwingUtilities.invokeLater(this::repaintPublishedFrame);
    }

    /**
     * The EDT half of the render pulse: repaints the board from whatever
     * {@link #buildAndPublishFrame} last published, and refreshes the side panel's live
     * per-tower stats. Skips the repaint while a previous paint is still in flight.
     */
    private void repaintPublishedFrame() {
        this.gameConsole.getTowerInfo().refreshSelected();
        // The wave-preview panels animate their own display-only mobs. This used to be driven
        // straight from doTick on the game-loop thread, which touched Swing from tick code and
        // wrote PanelEnemy's clock across a thread boundary. Driving it from the render pulse
        // instead keeps it on the EDT and, being cosmetic, it belongs on the render cadence
        // rather than the simulation's anyway.
        this.gameConsole.getWaveInfo().doTick(this.gameTime);
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
        this.stopLoopThen(() -> this.installLevel(level));
    }

    /**
     * The two-step every level lifecycle change goes through: stop the loop off the EDT, then
     * do the EDT-side work once it has actually stopped.
     * <p>
     * {@link GameLoop#stop()} joins the simulation thread without bound, so calling it from
     * the EDT would freeze the UI for as long as a wedged tick ran - it asserts against that.
     * It therefore runs on {@link #lifecycleExecutor}, and {@code installOnEdt} is posted back
     * afterwards. That hop is what makes a level change asynchronous, and asynchronous means a
     * second request can arrive while the first is still in flight: {@link #levelGeneration}
     * is bumped synchronously here, on the EDT, so an install posted for a superseded request
     * finds a newer generation and drops itself rather than loading a level on top of a loop
     * the newer request already restarted.
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

    /**
     * The EDT half of {@link #startSelectedLevel}, run once the loop has stopped.
     */
    private void installLevel(LevelDefinition level) {
        this.setGameStopped(false);
        this.boardOverlays.reset();
        this.unSelectTower();
        this.panelTowerSelector.stopPlacing();
        // stopLoopThen has already joined the loop, so no frame can be published between here
        // and the new level's first pulse - this just makes sure the outgoing level's last
        // frame is not what gets painted in the meantime.
        this.latestFrame = null;
        this.engine.loadLevel(level);
        this.contentCardLayout.show(getContentPane(), CARD_GAME);
        this.gameBoard.recalculateBoard(level.width(), level.height());
        this.setLocationRelativeTo(null);
        this.startLevel();
        this.gameTime = 0;
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
        this.stopLoopThen(this::showMenu);
    }

    /**
     * The EDT half of {@link #returnToMenu}, run once the loop has stopped.
     */
    private void showMenu() {
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
        WaveProgress progress = this.engine.waveProgress();
        if (enemiesLeft == 0 && progress.hasNextWave()) {
            LOG.info("Wave {} cleared, ready for the next one", progress.currentNumber());
            this.engine.setWaveReady(true);
            SwingUtilities.invokeLater(this::syncTransportButtons);
        } else if (enemiesLeft == 0) {
            this.gameWon();
        }
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

    /**
     * Debug keybinding ('n'): clears the current wave with no penalty and starts the next.
     */
    private void debugSkipWave() {
        this.engine.debugSkipCurrentWave();
        this.setWavePreview();
        this.updateInfo();
        this.syncTransportButtons();
    }

    private void updateInfo() {
        WaveProgress progress = this.engine.waveProgress();
        this.gameConsole.setWaveProgress(progress.index(), progress.count());
    }

    /**
     * Also reachable from the game-loop thread - see enemyDied().
     */
    public void economyChanged(EconomyState state) {
        SwingUtilities.invokeLater(this::updateInfo);
        if (state.isGameOver()) {
            this.gameLost();
        }
    }

    /**
     * Reached from {@link #economyChanged}, which fires on every economy event - so once lives
     * hit zero, every later event would re-announce the loss without this guard.
     */
    private void gameLost() {
        if (this.gameStopped) {
            return;
        }
        LOG.info("Game over - lost, score={}", this.gameWorld.economy().getScore());
        this.setGameStopped(true);
        SwingUtilities.invokeLater(this.boardOverlays::showLost);
    }

    /**
     * Guarded like {@link #gameLost()}: the last enemy of the last wave reports once.
     */
    private void gameWon() {
        if (this.gameStopped) {
            return;
        }
        LOG.info("Game won, score={}", this.gameWorld.economy().getScore());
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

    /**
     * Called from doGameTick() on the game-loop thread, not the EDT.
     */
    public void doTick(int time) {
        boolean waveStarted = this.engine.doTick(time);
        if (waveStarted) {
            SwingUtilities.invokeLater(() -> {
                this.setWavePreview();
                this.updateInfo();
                this.syncTransportButtons();
            });
        }
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

    /**
     * Paints the frame {@link #buildAndPublishFrame} last published. This method builds
     * nothing and reads no simulation state: everything it draws was described on the
     * {@code game-loop} thread and handed over as one immutable value.
     */
    public void paintBoard(Graphics2D g2) {
        RenderFrame frame = this.latestFrame;
        if (frame == null) {
            // Before the loop's first render pulse - a level was just selected, or none has
            // been. Nothing to paint yet; the next pulse is at most ~16ms away.
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
        this.engine.mouseClicked(boardX, boardY)
                .ifPresent(this.gameConsole.getTowerInfo()::setTower);
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
        // Each tower type carries its own placement key, so a new tower needs no change here.
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
            case 'n' -> this.debugSkipWave();
            case 'x' -> this.setInfoText(this.engine.debugSpawnNextCatalogEnemy()
                    .map(id -> "Debug spawned: " + id)
                    .orElse("Debug spawn needs a level loaded first"));
            case 'c' -> this.engine.debugGrantCredits(DEBUG_CREDIT_GRANT);
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
        jPanel_game.setBackground(new Color(0, 0, 0));

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
        // No fill: jPanel_board is already sized to the loaded level's exact pixel dimensions
        // (GameBoard.recalculateBoard), so it must not stretch to fill extra window space -
        // it stays at its natural size, centered in its cell by the default CENTER anchor.
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

    /**
     * While paused, ">>" single-steps one tick instead of changing tick speed.
     */
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
