package td.ui;

import org.junit.jupiter.api.Test;
import td.ui.render.EnemyLook;

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
