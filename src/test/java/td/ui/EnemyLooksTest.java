package td.ui;

import org.junit.jupiter.api.Test;
import td.ui.render.EnemyLook;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnemyLooksTest {

    @Test
    void aTableMissingALookIsRejectedByName() {
        Map<EnemyLook, EnemyLooks.Row> rows = new EnumMap<>(EnemyLook.class);
        for (EnemyLook look : EnemyLook.values()) {
            rows.put(look, EnemyLooks.of(look));
        }
        rows.remove(EnemyLook.GHOST);

        assertThatThrownBy(() -> EnemyLooks.checkedComplete(rows))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(EnemyLook.GHOST.name());
    }

    @Test
    void aLooksReachCoversItsBodyAndDetailOnEveryAxis() {
        for (EnemyLook look : EnemyLook.values()) {
            EnemyLooks.Row row = EnemyLooks.of(look);

            assertThat(row.reach()).as("%s body", look).isGreaterThanOrEqualTo(extentOf(row.body().at(1f)));
            row.detail().ifPresent(detail ->
                    assertThat(row.reach()).as("%s detail", look).isGreaterThanOrEqualTo(extentOf(detail.shape().at(1f))));
        }
    }

    private static float extentOf(Shape shape) {
        Rectangle2D bounds = shape.getBounds2D();
        return (float) Math.max(Math.max(-bounds.getMinX(), bounds.getMaxX()), Math.max(-bounds.getMinY(), bounds.getMaxY()));
    }

    @Test
    void everyLookDrawsItsBodyDetailAndBurst() {
        float scale = 10f;

        for (EnemyLook look : EnemyLook.values()) {
            EnemyLooks.Row row = EnemyLooks.of(look);

            assertThat(row.body().at(scale).getBounds2D().isEmpty()).as("%s body", look).isFalse();
            row.detail().ifPresent(detail ->
                    assertThat(detail.shape().at(scale).getBounds2D().isEmpty()).as("%s detail", look).isFalse());
            assertThat(row.fade().at(scale, scale * 2)).as("%s burst", look).isNotEmpty();
        }
    }
}
