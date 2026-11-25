package td.ui;

import td.util.ThreadConfined;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Small pictures that many things on the board draw alike every frame (a shadow, a mark), drawn once per look and
 * zoom into a kept image and placed whole: placing a picture costs far less than filling its shapes again.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class Stamps {

    private static final float DEVICE_STEPS_PER_UNIT = 16f;
    private static final int PADDING = 2;
    // Looks are few; a run past this many means a key that changes every frame, which keeping would only leak.
    private static final int MOST_KEPT = 4096;

    private static final Map<Key, Stamp> KEPT = new HashMap<>();

    private record Key(Object look, int deviceSteps) {
    }

    private record Stamp(BufferedImage image, int centreX, int centreY) {
    }

    private Stamps() {
    }

    /**
     * Paints {@code drawing} centred at {@code x, y}. {@code look} names the picture: two calls with equal looks must
     * draw the same. The drawing draws in board pixels round its own centre, within {@code reach} pixels of it.
     */
    static void paint(Graphics2D g2, Object look, float x, float y, double reach, Consumer<Graphics2D> drawing) {
        AffineTransform board = g2.getTransform();
        boolean plain = board.getShearX() == 0 && board.getShearY() == 0 && board.getScaleX() > 0
                && Math.abs(board.getScaleX() - board.getScaleY()) < 1e-6;
        if (!plain) {
            g2.translate(x, y);
            drawing.accept(g2);
            g2.setTransform(board);
            return;
        }
        int deviceSteps = Math.max(1, Math.round((float) board.getScaleX() * DEVICE_STEPS_PER_UNIT));
        Stamp stamp = KEPT.get(new Key(look, deviceSteps));
        if (stamp == null) {
            if (KEPT.size() >= MOST_KEPT) {
                KEPT.clear();
            }
            stamp = draw(deviceSteps / DEVICE_STEPS_PER_UNIT, reach, drawing);
            KEPT.put(new Key(look, deviceSteps), stamp);
        }
        long deviceX = Math.round(board.getTranslateX() + board.getScaleX() * x);
        long deviceY = Math.round(board.getTranslateY() + board.getScaleY() * y);
        g2.setTransform(new AffineTransform());
        g2.drawImage(stamp.image(), (int) deviceX - stamp.centreX(), (int) deviceY - stamp.centreY(), null);
        g2.setTransform(board);
    }

    private static Stamp draw(float device, double reach, Consumer<Graphics2D> drawing) {
        int centre = (int) Math.ceil(reach * device) + PADDING;
        BufferedImage image = new BufferedImage(centre * 2, centre * 2, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(centre, centre);
        g.scale(device, device);
        drawing.accept(g);
        g.dispose();
        return new Stamp(image, centre, centre);
    }
}
