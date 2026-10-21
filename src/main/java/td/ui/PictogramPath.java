package td.ui;

import td.util.ThreadConfined;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/** The SVG path subset pictograms are written in, plus {@code O cx cy r} for a circle. */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
final class PictogramPath {

    private final String data;
    private int pos;

    private PictogramPath(String data) {
        this.data = data;
    }

    /** @throws IllegalArgumentException for data it cannot read */
    static Path2D.Float parse(String data) {
        return new PictogramPath(data).read();
    }

    private Path2D.Float read() {
        Path2D.Float path = new Path2D.Float();
        float x = 0, y = 0, startX = 0, startY = 0, ctrlX = 0, ctrlY = 0;
        char command = ' ';
        char previous = ' ';
        while (true) {
            skipSeparators();
            if (this.pos >= this.data.length()) break;
            char c = this.data.charAt(this.pos);
            if (Character.isLetter(c)) {
                command = c;
                this.pos++;
            } else if (command == 'M') {
                command = 'L';
            } else if (command == 'm') {
                command = 'l';
            } else if (command == ' ') {
                throw new IllegalArgumentException("Path data starts with a number: " + this.data);
            }
            boolean rel = Character.isLowerCase(command);
            float ox = rel ? x : 0, oy = rel ? y : 0;
            switch (Character.toUpperCase(command)) {
                case 'M' -> { x = ox + num(); y = oy + num(); path.moveTo(x, y); startX = x; startY = y; }
                case 'L' -> { x = ox + num(); y = oy + num(); path.lineTo(x, y); }
                case 'H' -> { x = (rel ? x : 0) + num(); path.lineTo(x, y); }
                case 'V' -> { y = (rel ? y : 0) + num(); path.lineTo(x, y); }
                case 'C' -> {
                    float x1 = ox + num(), y1 = oy + num(), x2 = ox + num(), y2 = oy + num();
                    x = ox + num(); y = oy + num();
                    path.curveTo(x1, y1, x2, y2, x, y); ctrlX = x2; ctrlY = y2;
                }
                case 'S' -> {
                    boolean smooth = "CcSs".indexOf(previous) >= 0;
                    float x1 = smooth ? 2 * x - ctrlX : x, y1 = smooth ? 2 * y - ctrlY : y;
                    float x2 = ox + num(), y2 = oy + num();
                    x = ox + num(); y = oy + num();
                    path.curveTo(x1, y1, x2, y2, x, y); ctrlX = x2; ctrlY = y2;
                }
                case 'Q' -> {
                    float x1 = ox + num(), y1 = oy + num();
                    x = ox + num(); y = oy + num();
                    path.quadTo(x1, y1, x, y); ctrlX = x1; ctrlY = y1;
                }
                case 'T' -> {
                    boolean smooth = "QqTt".indexOf(previous) >= 0;
                    float x1 = smooth ? 2 * x - ctrlX : x, y1 = smooth ? 2 * y - ctrlY : y;
                    x = ox + num(); y = oy + num();
                    path.quadTo(x1, y1, x, y); ctrlX = x1; ctrlY = y1;
                }
                case 'A' -> {
                    float rx = num(), ry = num(), rotation = num();
                    boolean large = flag(), sweep = flag();
                    float x2 = ox + num(), y2 = oy + num();
                    arcTo(path, x, y, rx, ry, rotation, large, sweep, x2, y2);
                    x = x2; y = y2;
                }
                case 'Z' -> { path.closePath(); x = startX; y = startY; }
                case 'O' -> {
                    float cx = num(), cy = num(), r = num();
                    path.append(new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r), false);
                    x = cx; y = cy; command = ' ';
                }
                default -> throw new IllegalArgumentException("Unknown path command " + command + " in " + this.data);
            }
            previous = command;
            if (Character.toUpperCase(command) == 'Z') command = ' ';
        }
        return path;
    }

    /** An SVG elliptical arc from (x1, y1) to (x2, y2), by the endpoint-to-centre conversion. */
    private static void arcTo(Path2D.Float path, float x1, float y1, float rxIn, float ryIn, float degrees,
                              boolean large, boolean sweep, float x2, float y2) {
        if (x1 == x2 && y1 == y2) return;
        double rx = Math.abs(rxIn), ry = Math.abs(ryIn);
        if (rx == 0 || ry == 0) { path.lineTo(x2, y2); return; }
        double phi = Math.toRadians(degrees);
        double cos = Math.cos(phi), sin = Math.sin(phi);
        double dx = (x1 - x2) / 2.0, dy = (y1 - y2) / 2.0;
        double x1p = cos * dx + sin * dy, y1p = -sin * dx + cos * dy;
        double lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry);
        if (lambda > 1) { rx *= Math.sqrt(lambda); ry *= Math.sqrt(lambda); }
        double num = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p;
        double den = rx * rx * y1p * y1p + ry * ry * x1p * x1p;
        double coef = (large != sweep ? 1 : -1) * Math.sqrt(Math.max(0, num / den));
        double cxp = coef * rx * y1p / ry, cyp = -coef * ry * x1p / rx;
        double cx = cos * cxp - sin * cyp + (x1 + x2) / 2.0, cy = sin * cxp + cos * cyp + (y1 + y2) / 2.0;
        double ux = (x1p - cxp) / rx, uy = (y1p - cyp) / ry, vx = (-x1p - cxp) / rx, vy = (-y1p - cyp) / ry;
        double start = Math.atan2(uy, ux);
        double extent = Math.atan2(ux * vy - uy * vx, ux * vx + uy * vy);
        if (!sweep && extent > 0) extent -= 2 * Math.PI;
        if (sweep && extent < 0) extent += 2 * Math.PI;
        Arc2D.Double arc = new Arc2D.Double(cx - rx, cy - ry, 2 * rx, 2 * ry, -Math.toDegrees(start), -Math.toDegrees(extent), Arc2D.OPEN);
        Shape shape = phi == 0 ? arc : AffineTransform.getRotateInstance(phi, cx, cy).createTransformedShape(arc);
        path.append(shape, true);
    }

    private void skipSeparators() {
        while (this.pos < this.data.length() && (Character.isWhitespace(this.data.charAt(this.pos)) || this.data.charAt(this.pos) == ',')) {
            this.pos++;
        }
    }

    private boolean flag() {
        skipSeparators();
        char c = this.data.charAt(this.pos++);
        if (c != '0' && c != '1') throw new IllegalArgumentException("Arc flag must be 0 or 1 in " + this.data);
        return c == '1';
    }

    private float num() {
        skipSeparators();
        int start = this.pos;
        if (this.pos < this.data.length() && (this.data.charAt(this.pos) == '-' || this.data.charAt(this.pos) == '+')) this.pos++;
        boolean dot = false;
        while (this.pos < this.data.length()) {
            char c = this.data.charAt(this.pos);
            if (Character.isDigit(c)) {
                this.pos++;
            } else if (c == '.' && !dot) {
                dot = true;
                this.pos++;
            } else {
                break;
            }
        }
        if (start == this.pos) throw new IllegalArgumentException("Expected a number at " + start + " in " + this.data);
        return Float.parseFloat(this.data.substring(start, this.pos));
    }
}
