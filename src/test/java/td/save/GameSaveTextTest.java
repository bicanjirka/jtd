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
    void aSaveWithAByteOrderMarkReadsAsWithout() {
        String text = "level Curly Path\nwave 17\neconomy 120 3400 5\n";

        GameSaveText.Read read = GameSaveText.read("﻿" + text);

        assertThat(read.save()).isEqualTo(GameSaveText.read(text).save());
        assertThat(read.save()).isPresent();
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

    @Test
    void everyFactOfAGameBetweenRoundsReadsBackAsWritten() {
        String text = """
                level Curly Path
                wave 9
                economy 120 3400 5
                seed -4116902350129335519
                leaked 3 7
                bonus 0 40
                bonus 2 55
                damage 1250000 480000
                tower PULSE 4 7 400 12 123456 40 paid=97 xp+=0.25 base.attune +base.awaken
                tower PULSE 9 7 0 0 0 0 paid=112 unused=tower base.attune
                pin 4 7 9 7
                """;

        GameSave save = GameSaveText.read(text).save().orElseThrow();

        assertThat(save.seed()).isEqualTo(-4116902350129335519L);
        assertThat(save.history().leaked()).containsExactlyInAnyOrder(3, 7);
        assertThat(save.history().bonusesPaid()).containsEntry(0, 40).containsEntry(2, 55);
        assertThat(save.damage().magic()).isEqualTo(480000L);
        assertThat(save.towers().getFirst().paid()).hasValue(97);
        assertThat(save.towers().getFirst().tally().xpCarried()).isEqualTo(0.25f);
        assertThat(save.towers().getFirst().nodes()).containsExactly("base.attune", "base.awaken");
        assertThat(save.towers().getFirst().use().unusedNodes()).containsExactly("base.awaken");
        assertThat(save.towers().getLast().use().allUnused()).isTrue();
        assertThat(save.pins()).singleElement().satisfies(pin -> assertThat(pin.host().x()).isEqualTo(9));
        assertThat(GameSaveText.write(save)).isEqualTo(text);
    }
}
