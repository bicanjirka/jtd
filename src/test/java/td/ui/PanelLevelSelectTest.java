package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import td.fixtures.LevelFixtures;
import td.level.LevelDefinition;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class PanelLevelSelectTest {

    private final List<LevelDefinition> levels = List.of(LevelFixtures.level("First", 5, 5),
            LevelFixtures.level("Second", 5, 5), LevelFixtures.level("Third", 5, 5));
    private final List<LevelDefinition> started = new ArrayList<>();
    private final PanelLevelSelect menu = new PanelLevelSelect(this.levels, this.started::add);

    @Test
    void aCardsDigitStartsItsLevel() {
        boolean used = this.menu.startByKey('2');

        assertThat(used).isTrue();
        assertThat(this.started).containsExactly(this.levels.get(1));
    }

    @Test
    void aDigitPastTheLastCardOrAnyOtherKeyStartsNothing() {
        boolean past = this.menu.startByKey('4');
        boolean zero = this.menu.startByKey('0');
        boolean letter = this.menu.startByKey('q');

        assertThat(List.of(past, zero, letter)).containsOnly(false);
        assertThat(this.started).isEmpty();
    }
}
