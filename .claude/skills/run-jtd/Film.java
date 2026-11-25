import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * What the previews write besides a still: a looping GIF, a strip of frames side by side (one PNG an agent can read
 * and a diff can show), and the "squint" view, grey and blurred, that shows a picture's values as bands. Lab code.
 */
final class Film {

    private static final Color BACK = new Color(0x0d0e10);
    private static final Color LABEL = new Color(0xb8bcc4);
    private static final int STRIP_COLUMNS = 6;

    private Film() {
    }

    /** Writes {@code frames} as a GIF that loops, each frame shown {@code delayMillis}. */
    static void gif(File file, List<BufferedImage> frames, int delayMillis) throws IOException {
        parent(file);
        file.delete();
        ImageWriter writer = ImageIO.getImageWritersBySuffix("gif").next();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(stream);
            writer.prepareWriteSequence(null);
            for (BufferedImage frame : frames) {
                BufferedImage image = opaque(frame);
                IIOMetadata meta = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image),
                        null);
                String format = meta.getNativeMetadataFormatName();
                IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(format);
                IIOMetadataNode control = child(root, "GraphicControlExtension");
                control.setAttribute("disposalMethod", "none");
                control.setAttribute("userInputFlag", "FALSE");
                control.setAttribute("transparentColorFlag", "FALSE");
                control.setAttribute("delayTime", Integer.toString(Math.max(2, Math.round(delayMillis / 10f))));
                control.setAttribute("transparentColorIndex", "0");
                IIOMetadataNode apps = child(root, "ApplicationExtensions");
                IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension");
                app.setAttribute("applicationID", "NETSCAPE");
                app.setAttribute("authenticationCode", "2.0");
                app.setUserObject(new byte[] {1, 0, 0});
                apps.appendChild(app);
                meta.setFromTree(format, root);
                writer.writeToSequence(new IIOImage(image, null, meta), null);
            }
            writer.endWriteSequence();
        }
        writer.dispose();
    }

    /**
     * At most {@code most} of {@code frames}, evenly picked, side by side in rows of {@link #STRIP_COLUMNS}, each under
     * its label.
     */
    static BufferedImage strip(List<BufferedImage> frames, List<String> labels, int most) {
        int count = Math.min(most, frames.size());
        int width = frames.getFirst().getWidth();
        int height = frames.getFirst().getHeight();
        int columns = Math.min(STRIP_COLUMNS, count);
        int rows = (count + columns - 1) / columns;
        int labelHeight = 16;
        BufferedImage strip = new BufferedImage(columns * (width + 4) + 4, rows * (height + labelHeight + 4) + 4,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = strip.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(BACK);
        g.fillRect(0, 0, strip.getWidth(), strip.getHeight());
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        for (int i = 0; i < count; i++) {
            int picked = count == 1 ? 0 : (int) Math.round(i * (frames.size() - 1) / (double) (count - 1));
            int x = 4 + (i % columns) * (width + 4);
            int y = 4 + (i / columns) * (height + labelHeight + 4);
            g.setColor(LABEL);
            g.drawString(labels.get(picked), x, y + 12);
            g.drawImage(frames.get(picked), x, y + labelHeight, null);
        }
        g.dispose();
        return strip;
    }

    /** {@code image} grey and blurred, so only its values are left: what stands apart from the ground shows. */
    static BufferedImage squint(BufferedImage image, double zoom) {
        BufferedImage grey = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                double luminance = 0.2126 * linear(rgb >> 16 & 0xff) + 0.7152 * linear(rgb >> 8 & 0xff)
                        + 0.0722 * linear(rgb & 0xff);
                int v = (int) Math.round(255 * Math.pow(luminance, 1 / 2.2));
                grey.setRGB(x, y, v << 16 | v << 8 | v);
            }
        }
        int size = Math.max(3, (int) Math.round(5 * zoom)) | 1;
        float[] weights = new float[size * size];
        java.util.Arrays.fill(weights, 1f / weights.length);
        ConvolveOp blur = new ConvolveOp(new Kernel(size, size, weights), ConvolveOp.EDGE_NO_OP, null);
        return blur.filter(blur.filter(grey, null), null);
    }

    static void png(File file, BufferedImage image) throws IOException {
        parent(file);
        ImageIO.write(image, "png", file);
    }

    private static double linear(int channel) {
        double c = channel / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static BufferedImage opaque(BufferedImage frame) {
        if (frame.getType() == BufferedImage.TYPE_INT_RGB) {
            return frame;
        }
        BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(BACK);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.drawImage(frame, 0, 0, null);
        g.dispose();
        return image;
    }

    private static void parent(File file) {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
    }

    private static IIOMetadataNode child(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(name)) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }
}
