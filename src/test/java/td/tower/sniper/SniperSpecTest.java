package td.tower.sniper;

import org.junit.jupiter.api.Test;
import td.tower.targeting.Viewpoint;

import static org.assertj.core.api.Assertions.assertThat;

class SniperSpecTest {

    private static final SniperSpec SPEC = SniperSpec.from(new Viewpoint(0, 0, 100f, 32));

    @Test
    void theFirstAimSetDecidesTheAim() {
        SniperSpec spec = SPEC.aimingAt(new SniperAim(new td.tower.targeting.HighestRankSelector(), "highest rank"))
                .aimingAt(new SniperAim(new td.tower.targeting.HighestHealthSelector(), "most health"));

        assertThat(spec.chosenAim().label()).isEqualTo("highest rank");
    }

    @Test
    void withoutASpecialItAimsAtTheEnemyFurthestAlong() {
        assertThat(SPEC.chosenAim().label()).isEqualTo("first");
    }
}
