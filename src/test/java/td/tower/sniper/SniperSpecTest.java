package td.tower.sniper;

import org.junit.jupiter.api.Test;
import td.effect.Effect;
import td.fixtures.FakeEnemyMob;
import td.tower.targeting.Viewpoint;

import static org.assertj.core.api.Assertions.assertThat;

class SniperSpecTest {

    private static final SniperSpec SPEC = SniperSpec.from(new Viewpoint(0, 0, 100f, 32));

    private static FakeEnemyMob markedAt(double x) {
        FakeEnemyMob mob = FakeEnemyMob.at(x, 0);
        mob.applyEffect(Effect.marked(100, d -> {
        }));
        return mob;
    }

    @Test
    void theFirstSpecialBoughtDecidesTheAim() {
        SniperSpec spec = new RicochetPerk().refineSpec(new MomentumPerk().refineSpec(SPEC));

        assertThat(spec.chosenAim().label()).isEqualTo("highest rank");
    }

    @Test
    void withoutASpecialItAimsAtTheEnemyFurthestAlong() {
        assertThat(SPEC.chosenAim().label()).isEqualTo("first");
    }
}
