package td.tower.sniper;

import org.junit.jupiter.api.Test;
import td.tower.targeting.Viewpoint;

import static org.assertj.core.api.Assertions.assertThat;

class SniperSpecTest {

    private static final SniperSpec SPEC = SniperSpec.from(new Viewpoint(0, 0, 100f, 32));

    @Test
    void withoutASpecialItAimsAtTheEnemyFurthestAlong() {
        assertThat(SPEC.aim().label()).isEqualTo("first");
    }
}
