import td.TowerDefense;
import td.damage.Damage;
import td.economy.EconomyDelta;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.level.LevelDefinition;
import td.util.GameWorld;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JLabel;

/**
 * In-process REPL driver for jTD (a Swing app, not a browser/Electron app).
 * Reads one command per line from stdin; see SKILL.md in this directory for
 * the command list and why this drives the app in-process rather than via a
 * separate screenshotting process.
 */
public class Driver {

    private static TowerDefense game;
    private static Robot robot;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        // On the EDT, exactly as td.Main does it: TowerDefense builds its whole component tree
        // in its constructor, and Swing requires that on the Event Dispatch Thread. It asserts
        // as much, so constructing it on this thread fails outright.
        onEventDispatchThread(() -> game = new TowerDefense());
        // In-process bring-to-front: the app raising its own window. Doing this from a
        // *separate* process via Win32 SetForegroundWindow silently no-ops (Windows
        // foreground-lock) and risks screenshotting whatever unrelated window is actually
        // on top - see SKILL.md Gotchas.
        game.setAlwaysOnTop(true);
        game.toFront();
        game.requestFocus();
        Thread.sleep(500);

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            try {
                handle(line);
            } catch (Exception e) {
                System.out.println("ERROR: " + e);
            }
            System.out.flush();
        }
        quit();
    }

    private static void handle(String line) throws Exception {
        String[] parts = line.split("\\s+", 2);
        String cmd = parts[0];
        String rest = parts.length > 1 ? parts[1] : "";
        switch (cmd) {
            case "ss" -> screenshot(rest);
            case "list" -> list();
            case "click" -> click(Integer.parseInt(rest.trim()));
            case "hover" -> hover(rest);
            case "key" -> typeKey(rest.trim());
            case "state" -> state();
            case "boardclick" -> boardClick(rest.trim());
            case "level" -> selectLevel(Integer.parseInt(rest.trim()));
            case "menu" -> returnToMenu();
            case "setcredits" -> setCredits(rest.trim());
            case "setlives" -> setLives(rest.trim());
            case "spawn" -> spawnEnemy(rest.trim());
            case "kill" -> killEnemies();
            case "sleep" -> Thread.sleep(Long.parseLong(rest.trim()));
            case "quit" -> quit();
            default -> System.out.println("ERROR: unknown command '" + cmd + "'");
        }
    }

    private static void screenshot(String path) throws IOException {
        Rectangle bounds = new Rectangle(game.getLocationOnScreen(), game.getSize());
        BufferedImage img = robot.createScreenCapture(bounds);
        File file = new File(path);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        ImageIO.write(img, "png", file);
        System.out.println("OK ss " + path + " " + bounds);
    }

    // Clickable = an AbstractButton (JButton/JToggleButton/...) or any component with its
    // own MouseListener (e.g. PanelLevelSelect's level cards, which are plain JPanels with
    // a mouseClicked handler, not buttons). DFS pre-order, so indices are stable across
    // list/click calls within one run.
    private static List<Component> findClickables() {
        List<Component> out = new ArrayList<>();
        collect(game.getContentPane(), out);
        return out;
    }

    private static void collect(Container container, List<Component> out) {
        for (Component c : container.getComponents()) {
            // Motion listeners count too: a component can be hover-only, with no click
            // behaviour at all. PanelEnemy (the wave preview, which shows an enemy's name and
            // description on hover) registers only a MouseMotionListener and was invisible to
            // this listing until it was included here.
            if (c instanceof AbstractButton
                    || c.getMouseListeners().length > 0
                    || c.getMouseMotionListeners().length > 0) {
                out.add(c);
            }
            if (c instanceof Container inner) {
                collect(inner, out);
            }
        }
    }

    private static void list() {
        List<Component> clickables = findClickables();
        for (int i = 0; i < clickables.size(); i++) {
            System.out.println(i + ": " + describe(clickables.get(i)));
        }
        System.out.println("OK list " + clickables.size());
    }

    private static String describe(Component c) {
        String label = null;
        if (c instanceof AbstractButton b) {
            label = b.getText();
        } else if (c instanceof Container container) {
            label = firstLabelText(container);
        }
        return c.getClass().getSimpleName() + (label != null && !label.isBlank() ? " \"" + label + "\"" : "")
                + " visible=" + c.isVisible();
    }

    private static String firstLabelText(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel l) {
                return l.getText();
            }
            if (c instanceof Container inner) {
                String found = firstLabelText(inner);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void click(int index) throws AWTException, InterruptedException {
        List<Component> clickables = findClickables();
        if (index < 0 || index >= clickables.size()) {
            System.out.println("ERROR: index " + index + " out of range (0.." + (clickables.size() - 1) + ")");
            return;
        }
        Component target = clickables.get(index);
        if (target instanceof AbstractButton button) {
            button.doClick();
            System.out.println("OK click " + index + " (doClick)");
            return;
        }
        Point loc = target.getLocationOnScreen();
        int cx = loc.x + target.getWidth() / 2;
        int cy = loc.y + target.getHeight() / 2;
        robot.mouseMove(cx, cy);
        Thread.sleep(80);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(60);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        System.out.println("OK click " + index + " (robot at " + cx + "," + cy + ")");
    }

    // Moves the pointer onto a component without clicking, so its mouseEntered fires. jTD shows
    // a tower's pre-purchase stats on toolbar hover (PanelTowerSelector.mouseOver), which
    // click() cannot reach: a JToggleButton is an AbstractButton, so click() takes the
    // doClick() path and never generates a mouse-entered event at all.
    private static void hover(String args) throws AWTException, InterruptedException {
        String[] parts = args.trim().split("\s+");
        int index = Integer.parseInt(parts[0]);
        List<Component> clickables = findClickables();
        if (index < 0 || index >= clickables.size()) {
            System.out.println("ERROR: index " + index + " out of range (0.." + (clickables.size() - 1) + ")");
            return;
        }
        Component target = clickables.get(index);
        Point loc = target.getLocationOnScreen();
        // Default to the centre; an optional dx/dy targets a point inside the component instead.
        // Some panels map the pointer's x to a specific item - PanelEnemy divides x by the mob
        // scale to decide which preview enemy the pointer is over - so a centre-only hover can
        // land past every item and fire nothing.
        int cx = loc.x + (parts.length > 1 ? Integer.parseInt(parts[1]) : target.getWidth() / 2);
        int cy = loc.y + (parts.length > 2 ? Integer.parseInt(parts[2]) : target.getHeight() / 2);
        // Away first: moving from wherever the pointer already sits onto the target is what
        // generates mouseEntered. If it happens to be resting on the target already, a move to
        // the same point produces no event and the hover silently does nothing.
        robot.mouseMove(loc.x - 5, loc.y - 5);
        Thread.sleep(60);
        robot.mouseMove(cx, cy);
        Thread.sleep(120);
        System.out.println("OK hover " + index + " (robot at " + cx + "," + cy + ")");
    }

    // Simulates a real keypress for jTD's JFrame-level keyboard shortcuts (q/w/e/r/t build,
    // p pause, f speed, s next wave) - these are only wired via a KeyListener on the frame,
    // not buttons, so this goes through Robot rather than findClickables().
    private static void typeKey(String key) {
        int code = KeyEvent.getExtendedKeyCodeForChar(key.charAt(0));
        robot.keyPress(code);
        robot.keyRelease(code);
        System.out.println("OK key " + key);
    }

    // Reflects into TowerDefense's private engine field rather than adding test-only public
    // getters to production code just for this driver.
    private static void state() throws Exception {
        Field engineField = TowerDefense.class.getDeclaredField("engine");
        engineField.setAccessible(true);
        Object engine = engineField.get(game);
        Class<?> engineClass = engine.getClass();

        // GameEngine.cells() returns a td.cell.CellGrid, which owns the board and never
        // hands out its backing array - ask it for its dimensions rather than indexing.
        Object cellGrid = call(engine, engineClass, "cells");
        Class<?> gridClass = cellGrid.getClass();
        String gridDims = Boolean.TRUE.equals(call(cellGrid, gridClass, "isLoaded"))
                ? call(cellGrid, gridClass, "width") + "x" + call(cellGrid, gridClass, "height")
                : "not loaded";
        Object context = call(engine, engineClass, "getGameWorld");
        Class<?> contextClass = context.getClass();

        Field gameTimeField = TowerDefense.class.getDeclaredField("gameTime");
        gameTimeField.setAccessible(true);

        System.out.println("gameTime=" + gameTimeField.get(game));
        System.out.println("cellGrid=" + gridDims);
        System.out.println("wave=" + call(engine, engineClass, "getCurrentWaveIndex")
                + "/" + call(engine, engineClass, "getWaveCount"));
        System.out.println("waveReady=" + call(engine, engineClass, "isWaveReady"));
        // GameWorld hands its collaborators out rather than wrapping them, so the economy
        // getters live on EconomyLedger now - reflect through economy() to reach them.
        Object ledger = call(context, contextClass, "economy");
        Class<?> ledgerClass = ledger.getClass();
        System.out.println("credits=" + call(ledger, ledgerClass, "getCredits"));
        System.out.println("lives=" + call(ledger, ledgerClass, "getLives"));
        System.out.println("score=" + call(ledger, ledgerClass, "getScore"));
        System.out.println("OK state");
    }

    private static Object call(Object target, Class<?> type, String method) throws Exception {
        Method m = type.getMethod(method);
        return m.invoke(target);
    }

    // Reflects into TowerDefense's private gameWorld field once - shared by boardClick and the
    // state-cheat commands below, all of which need the real (public) GameWorld to call typed
    // methods on rather than going through the generic zero-arg call() helper.
    private static GameWorld getGameWorld() throws Exception {
        Field gameWorldField = TowerDefense.class.getDeclaredField("gameWorld");
        gameWorldField.setAccessible(true);
        return (GameWorld) gameWorldField.get(game);
    }

    // Clicks a board cell (not a Swing component, so not reachable via click(int)) by
    // reflecting into TowerDefense's private gameBoard field and using GameWorld.getBoard()'s
    // scale to turn a cell coordinate into a screen point.
    private static void boardClick(String args) throws Exception {
        String[] p = args.split("\\s+");
        int cellX = Integer.parseInt(p[0]);
        int cellY = Integer.parseInt(p[1]);

        Field gameBoardField = TowerDefense.class.getDeclaredField("gameBoard");
        gameBoardField.setAccessible(true);
        Component gameBoard = (Component) gameBoardField.get(game);
        int scale = getGameWorld().getBoard().scale();

        Point loc = gameBoard.getLocationOnScreen();
        int cx = loc.x + cellX * scale + scale / 2;
        int cy = loc.y + cellY * scale + scale / 2;
        robot.mouseMove(cx, cy);
        Thread.sleep(80);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(60);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        System.out.println("OK boardclick " + cellX + "," + cellY + " (robot at " + cx + "," + cy + ")");
    }

    // Reflects into TowerDefense's private startSelectedLevel(LevelDefinition) rather than
    // clicking a PanelLevelSelect card via Robot - a card is a plain JPanel with its own
    // MouseListener, not a button, and a Robot click at its on-screen center did not reliably
    // register in this environment (see SKILL.md Gotchas; button doClick() is unaffected).
    private static void selectLevel(int index) throws Exception {
        Field catalogField = TowerDefense.class.getDeclaredField("levelCatalog");
        catalogField.setAccessible(true);
        Object catalog = catalogField.get(game);
        @SuppressWarnings("unchecked")
        List<LevelDefinition> levels = (List<LevelDefinition>) catalog.getClass().getMethod("levels").invoke(catalog);
        LevelDefinition level = levels.get(index);

        Method startSelectedLevel = TowerDefense.class.getDeclaredMethod("startSelectedLevel", LevelDefinition.class);
        startSelectedLevel.setAccessible(true);
        // On the EDT, because that is where a real level-card click would call it from, and
        // TowerDefense now asserts it. The call only *starts* the change: the loop is stopped
        // on a lifecycle thread and the level installed on a later EDT pulse, so follow this
        // with `sleep` before screenshotting or asserting on `state`.
        onEventDispatchThread(() -> startSelectedLevel.invoke(game, level));
        System.out.println("OK level " + index + " (" + level.name() + ") - asynchronous, sleep before asserting");
    }

    // Reflects into TowerDefense's private returnToMenu() directly rather than
    // requestReturnToMenu() - the latter can pop a real JOptionPane confirm dialog mid-level,
    // which would block this call forever with no way to answer it non-interactively. Testing
    // the confirm dialog itself needs a human (or Robot clicking the dialog), not this driver.
    private static void returnToMenu() throws Exception {
        Method method = TowerDefense.class.getDeclaredMethod("returnToMenu");
        method.setAccessible(true);
        // EDT, and asynchronous, for the same reasons as selectLevel above.
        onEventDispatchThread(() -> method.invoke(game));
        System.out.println("OK menu - asynchronous, sleep before asserting");
    }

    /**
     * Runs a reflective call on the Event Dispatch Thread and rethrows whatever it threw, so a
     * TowerDefense method that asserts EDT ownership can be driven from this stdin loop. The
     * driver's own thread is not the EDT, and calling these directly is what the assertion in
     * TowerDefense.stopLoopThen exists to catch.
     */
    private static void onEventDispatchThread(ReflectiveCall call) throws Exception {
        java.util.concurrent.atomic.AtomicReference<Exception> failure =
                new java.util.concurrent.atomic.AtomicReference<>();
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                call.run();
            } catch (Exception e) {
                failure.set(e);
            }
        });
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    // Playtesting cheats: jump straight to an economy value instead of buying/selling towers
    // or surviving/leaking waves to get there. GameWorld.apply is safe to call from this thread -
    // it's already designed to be reachable from both the EDT and the game-loop thread (see
    // economyChanged's Javadoc on PanelTowerSelector), so a third caller is nothing new.
    private static void setCredits(String arg) throws Exception {
        GameWorld context = getGameWorld();
        int target = Integer.parseInt(arg.trim());
        context.economy().apply(EconomyDelta.credits(target - context.economy().getCredits()));
        System.out.println("OK setcredits " + target + " (credits=" + context.economy().getCredits() + ")");
    }

    private static void setLives(String arg) throws Exception {
        GameWorld context = getGameWorld();
        int target = Integer.parseInt(arg.trim());
        context.economy().apply(EconomyDelta.lives(target - context.economy().getLives()));
        System.out.println("OK setlives " + target + " (lives=" + context.economy().getLives() + ")");
    }

    // Spawns one instance of an exact EnemyCatalog id - unlike the game's own 'x' debug
    // keybinding (GameEngine.debugSpawnNextCatalogEnemy), which cycles through
    // EnemyCatalog.ids() in registration order and needs counting keypresses to reach a
    // specific one, this goes straight to the id asked for, immune to that order ever
    // changing. Mirrors debugSpawnNextCatalogEnemy's own mechanism (definition's own
    // baseHealth/price, delay 0, level 1) rather than inventing a second spawn path.
    private static void spawnEnemy(String id) throws Exception {
        GameWorld context = getGameWorld();
        EnemyCatalog catalog = context.getEnemyCatalog();
        EnemyDefinition definition = catalog.get(id);
        EnemyMob mob = catalog.spawn(id, context, 0, definition.baseHealth(), definition.price(), 1);
        context.enemies().add(mob);
        System.out.println("OK spawn " + id);
    }

    // Deals lethal damage to every currently-alive enemy through the same doDamage path a
    // tower's hit uses, so an on-death ability (the Warden's egg-spawn, via
    // spawnAtSamePositionAs) fires exactly as it would in real combat - useful for reaching
    // an ability-driven spawn without needing towers built or a real kill.
    private static void killEnemies() throws Exception {
        GameWorld context = getGameWorld();
        for (EnemyMob mob : context.enemies().getEnemies()) {
            mob.doDamage(Damage.physical(Integer.MAX_VALUE / 2));
        }
        System.out.println("OK kill");
    }

    private static void quit() {
        game.setAlwaysOnTop(false);
        System.out.println("OK quit");
        System.exit(0);
    }

    /**
     * A reflective invocation, which unlike Runnable is allowed to throw.
     */
    private interface ReflectiveCall {
        void run() throws Exception;
    }
}
