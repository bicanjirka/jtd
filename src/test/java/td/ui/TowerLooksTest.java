package td.ui;

import org.junit.jupiter.api.Test;
import td.ui.render.TowerLook;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TowerLooksTest {

    @Test
    void aTableMissingALookIsRejectedByName() {
        Map<TowerLook, TowerLooks.Row> rows = new EnumMap<>(TowerLook.class);
        for (TowerLook look : TowerLook.values()) {
            rows.put(look, TowerLooks.of(look));
        }
        rows.remove(TowerLook.MORTAR);

        assertThatThrownBy(() -> TowerLooks.checkedComplete(rows))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(TowerLook.MORTAR.name());
    }
}
