package td.ui;

import org.junit.jupiter.api.Test;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RememberedShapeTest {

    private final AtomicInteger built = new AtomicInteger();
    private final RememberedShape remembered = new RememberedShape(scale -> {
        this.built.incrementAndGet();
        return new Rectangle2D.Float(0, 0, scale, scale);
    });

    @Test
    void aShapeIsBuiltOnceForAScale() {
        Shape first = this.remembered.at(10f);

        Shape second = this.remembered.at(10f);

        assertThat(second).isSameAs(first);
        assertThat(this.built).hasValue(1);
    }

    @Test
    void anotherScaleGetsItsOwnShape() {
        Shape small = this.remembered.at(10f);

        Shape large = this.remembered.at(20f);

        assertThat(large.getBounds().width).isEqualTo(20);
        assertThat(small.getBounds().width).isEqualTo(10);
    }

    @Test
    void pastTheLimitWhatIsRememberedStartsOverInsteadOfGrowing() {
        for (int i = 0; i < RememberedShape.MOST_SCALES; i++) {
            this.remembered.at(i);
        }

        this.remembered.at(RememberedShape.MOST_SCALES);
        this.remembered.at(0);

        assertThat(this.built).hasValue(RememberedShape.MOST_SCALES + 2);
    }
}
