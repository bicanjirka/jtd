package td.ui;

import org.junit.jupiter.api.Test;
import td.ui.render.TowerLook;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TowerIconsTest {

    @Test
    void aTableMissingALookIsRejectedByName() {
        Map<TowerLook, TowerIcons.Icon> icons = new EnumMap<>(TowerLook.class);
        for (TowerLook look : TowerLook.values()) {
            icons.put(look, TowerIcons.of(look));
        }
        icons.remove(TowerLook.MORTAR);

        assertThatThrownBy(() -> TowerIcons.checkedComplete(icons))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(TowerLook.MORTAR.name());
    }

    @Test
    void everyIconHasABodyAndAMovingPartInsideItsCell() {
        for (TowerLook look : TowerLook.values()) {
            TowerIcons.Icon icon = TowerIcons.of(look);

            assertThat(icon.body().getBounds2D().isEmpty()).as("%s body", look).isFalse();
            assertThat(icon.moving().getBounds2D().isEmpty()).as("%s moving part", look).isFalse();
            assertThat(icon.body().getBounds2D().createUnion(icon.moving().getBounds2D()).getWidth())
                    .as("%s width in cells", look).isLessThanOrEqualTo(1.0);
        }
    }
}
