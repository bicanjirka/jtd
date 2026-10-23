package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HurtSpeedTraitTest {

    private static final float MOST = 1.4f;

    private final DefinedEnemyMob hasty = EnemyFixtures.liveMob(WorldFixtures.newWorld(),
            EnemyFixtures.simpleDefinition("hasty").withTraits(List.of(new HurtSpeedTrait(MOST))));

    @Test
    void speedIsUnchangedBeforeAnyDamageIsTaken() {
        assertThat(this.hasty.getSpeed()).isEqualTo(this.hasty.definition().baseSpeed());
    }

    @Test
    void speedRisesInProportionToTheHealthLost() {
        this.hasty.doDamage(Damage.physical(this.hasty.getHealth() / 2));

        assertThat(this.hasty.getSpeed())
                .isCloseTo(this.hasty.definition().baseSpeed() * (1 + (MOST - 1) * 0.5f), within(1e-4f));
    }
}
