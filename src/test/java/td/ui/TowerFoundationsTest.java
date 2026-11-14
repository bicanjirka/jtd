package td.ui;

import org.junit.jupiter.api.Test;
import td.ui.render.TowerLook;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TowerFoundationsTest {

    @Test
    void aTableMissingALookIsRejectedByName() {
        Map<TowerLook, String> rows = new EnumMap<>(TowerLook.class);
        for (TowerLook look : TowerLook.values()) {
            rows.put(look, look.name());
        }
        rows.remove(TowerLook.SEEKER);

        assertThatThrownBy(() -> TowerFoundations.checkedComplete(rows))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(TowerLook.SEEKER.name());
    }

    @Test
    void everyLooksFoundationNeverShrinksAndEndsWiderThanItStarts() {
        for (TowerLook look : TowerLook.values()) {
            for (int tier = 1; tier < TowerFoundations.TIERS; tier++) {
                TowerFoundations.Foundation lower = TowerFoundations.of(look, tier - 1);
                TowerFoundations.Foundation upper = TowerFoundations.of(look, tier);

                assertThat(upper.footprint()).as("%s tier %d", look, tier).isGreaterThanOrEqualTo(lower.footprint());
                assertThat(upper.parts()).as("%s tier %d", look, tier).isNotEmpty();
            }
            assertThat(TowerFoundations.of(look, TowerFoundations.TIERS - 1).footprint()).as(look.name())
                    .isGreaterThan(TowerFoundations.of(look, 0).footprint());
        }
    }

    @Test
    void aTierAboveTheLastWearsTheLastsFoundation() {
        TowerFoundations.Foundation last = TowerFoundations.of(TowerLook.SNIPER, TowerFoundations.TIERS - 1);

        assertThat(TowerFoundations.of(TowerLook.SNIPER, TowerFoundations.TIERS + 2)).isSameAs(last);
    }
}
