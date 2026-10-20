package td.save;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameSaveTextTest {

    @Test
    void aTowerLineMissingItsTallyIsAProblemNotASave() {
        String text = "level Curly Path\nwave 17\neconomy 120 3400 5\ntower SNIPER 6 11 400\n";

        GameSaveText.Read read = GameSaveText.read(text);

        assertThat(read.save()).isEmpty();
        assertThat(read.problem()).contains("tower SNIPER 6 11 400");
    }

    @Test
    void aLevelNameWithSpacesAndATowerWithNoUpgradesReadBack() {
        String text = "level Twisted Hourglass\nwave 4\neconomy 50 0 5\ntower MORTAR 3 2 0 0 0 0\n";

        GameSaveText.Read read = GameSaveText.read(text);

        assertThat(read.save()).hasValueSatisfying(save -> {
            assertThat(save.level()).isEqualTo("Twisted Hourglass");
            assertThat(save.towers()).singleElement().satisfies(tower -> assertThat(tower.nodes()).isEmpty());
        });
        assertThat(GameSaveText.write(read.save().orElseThrow())).isEqualTo(text);
    }
}
