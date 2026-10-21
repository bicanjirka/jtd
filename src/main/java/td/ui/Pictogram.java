package td.ui;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** A term's symbol: layers of path data on a {@value #UNIT}-unit grid, each painted one way. */
record Pictogram(List<Layer> layers) {

    static final float UNIT = 24f;
    private static final double CENTRE = UNIT / 2;

    Pictogram {
        layers = List.copyOf(layers);
    }

    /** How a layer paints: with the ink, the dim ink, or the face's fill (a knock-out). */
    enum Op {
        FILL, EVEN_ODD, STROKE, THIN, DIM, KNOCK_OUT, KNOCK_OUT_STROKE;

        static Op of(char letter) {
            return switch (letter) {
                case 'F' -> FILL;
                case 'E' -> EVEN_ODD;
                case 'S' -> STROKE;
                case 'T' -> THIN;
                case 'D' -> DIM;
                case 'K' -> KNOCK_OUT;
                case 'X' -> KNOCK_OUT_STROKE;
                default -> throw new IllegalArgumentException("No pictogram op " + letter);
            };
        }
    }

    record Layer(Op op, Shape shape) {
    }

    /**
     * {@code spec}'s layers, " ; " between them, each "op[@degrees] path"; "=key" is {@code byKey}'s pictogram.
     *
     * @throws IllegalArgumentException for a malformed layer or path
     */
    static Pictogram parse(String spec, Function<String, Pictogram> byKey) {
        String trimmed = spec.strip();
        if (trimmed.startsWith("=")) {
            return byKey.apply(trimmed.substring(1).strip());
        }
        List<Layer> layers = new ArrayList<>();
        for (String text : trimmed.split(";")) {
            String layer = text.strip();
            int space = layer.indexOf(' ');
            if (space < 1) {
                throw new IllegalArgumentException("Malformed pictogram layer: " + layer);
            }
            String head = layer.substring(0, space);
            int at = head.indexOf('@');
            Shape shape = PictogramPath.parse(layer.substring(space + 1));
            if (at > 0) {
                double degrees = Double.parseDouble(head.substring(at + 1));
                shape = AffineTransform.getRotateInstance(Math.toRadians(degrees), CENTRE, CENTRE)
                        .createTransformedShape(shape);
            }
            layers.add(new Layer(Op.of(head.charAt(0)), shape));
        }
        return new Pictogram(layers);
    }
}
