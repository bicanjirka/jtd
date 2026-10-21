package td.ui;

import org.junit.jupiter.api.Test;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class PictogramTest {

    private static Pictogram parse(String spec) {
        return Pictogram.parse(spec, key -> {
            throw new IllegalStateException("No pictogram " + key);
        });
    }

    private static Point2D lastPoint(Shape shape) {
        double[] coords = new double[6];
        Point2D last = new Point2D.Double();
        for (PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next()) {
            it.currentSegment(coords);
            last = new Point2D.Double(coords[0], coords[1]);
        }
        return last;
    }

    @Test
    void eachOpLetterPaintsItsWay() {
        List<Pictogram.Op> ops = "FESTDKX".chars().mapToObj(letter -> Pictogram.Op.of((char) letter)).toList();

        assertThat(ops).containsExactly(Pictogram.Op.FILL, Pictogram.Op.EVEN_ODD, Pictogram.Op.STROKE,
                Pictogram.Op.THIN, Pictogram.Op.DIM, Pictogram.Op.KNOCK_OUT, Pictogram.Op.KNOCK_OUT_STROKE);
    }

    @Test
    void aRotationTurnsAboutTheCentre() {
        Pictogram turned = parse("S@90 M12 2V6");

        Point2D end = lastPoint(turned.layers().getFirst().shape());

        assertThat(end.getX()).isCloseTo(18.0, within(0.01));
        assertThat(end.getY()).isCloseTo(12.0, within(0.01));
    }

    @Test
    void aCircleIsAFullEllipse() {
        Shape circle = parse("F O12 12 5").layers().getFirst().shape();

        assertThat(circle.getBounds2D().getWidth()).isEqualTo(10.0);
        assertThat(circle.contains(12, 12)).isTrue();
        assertThat(circle.contains(18, 12)).isFalse();
    }

    @Test
    void anEqualsSpecReusesAnotherPictogram() {
        Pictogram first = parse("F O12 12 5");

        Pictogram reused = Pictogram.parse("=other", key -> first);

        assertThat(reused).isSameAs(first);
    }

    @Test
    void malformedPathDataIsRejected() {
        assertThatThrownBy(() -> parse("F M12 L")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parse("Q M1 1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parse("F")).isInstanceOf(IllegalArgumentException.class);
    }
}
