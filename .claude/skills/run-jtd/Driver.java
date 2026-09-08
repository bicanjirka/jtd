import td.TowerDefense;

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
        game = new TowerDefense();
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
            case "key" -> typeKey(rest.trim());
            case "state" -> state();
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
            if (c instanceof AbstractButton || c.getMouseListeners().length > 0) {
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

        Object cellGrid = call(engine, engineClass, "getCellGrid");
        String gridDims = "null";
        if (cellGrid != null) {
            Object[] grid = (Object[]) cellGrid;
            Object[] col0 = (Object[]) grid[0];
            gridDims = grid.length + "x" + col0.length;
        }
        Object context = call(engine, engineClass, "getContext");
        Class<?> contextClass = context.getClass();

        System.out.println("cellGrid=" + gridDims);
        System.out.println("wave=" + call(engine, engineClass, "getCurrentWaveIndex")
                + "/" + call(engine, engineClass, "getWaveCount"));
        System.out.println("waveReady=" + call(engine, engineClass, "isWaveReady"));
        System.out.println("credits=" + call(context, contextClass, "getCredits"));
        System.out.println("lives=" + call(context, contextClass, "getLives"));
        System.out.println("score=" + call(context, contextClass, "getScore"));
        System.out.println("OK state");
    }

    private static Object call(Object target, Class<?> type, String method) throws Exception {
        Method m = type.getMethod(method);
        return m.invoke(target);
    }

    private static void quit() {
        game.setAlwaysOnTop(false);
        System.out.println("OK quit");
        System.exit(0);
    }
}
