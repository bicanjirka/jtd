package td.ui;

import td.ui.render.RenderFrame;
import td.util.ThreadConfined;

import javax.swing.SwingUtilities;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

/**
 * Times how long a built frame takes to paint: on the event dispatch thread, which owns the painter as it does in the
 * game, into an opaque image of the board at 1x. For the performance harness, which has no window: its thread only
 * hands frames over and waits.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public final class FramePaintTimer {

    private final Java2DFrameRenderer painter = new Java2DFrameRenderer();
    private final long[] lastNanos = new long[1];
    private BufferedImage canvas = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

    /** Paints {@code frame} and returns the nanoseconds the paint alone took. */
    public long paint(RenderFrame frame) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                if (this.canvas.getWidth() != frame.maxX() || this.canvas.getHeight() != frame.maxY()) {
                    this.canvas = new BufferedImage(frame.maxX(), frame.maxY(), BufferedImage.TYPE_INT_RGB);
                }
                Graphics2D g2 = this.canvas.createGraphics();
                long start = System.nanoTime();
                this.painter.paint(g2, frame);
                this.lastNanos[0] = System.nanoTime() - start;
                g2.dispose();
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while painting", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Painting a frame failed", e.getCause());
        }
        return this.lastNanos[0];
    }
}
