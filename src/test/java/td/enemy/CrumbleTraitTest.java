package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.DamageUnits;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CrumbleTraitTest {

    private static final float PER_SECOND = 20f;
    private static final int SECOND = (int) TickRate.TICKS_PER_SECOND;

    private final GameWorld world = WorldFixtures.newWorldOnStraightPath();

    private DefinedEnemyMob crumbling(int healthPoints, float spirit) {
        return EnemyFixtures.liveMob(this.world, EnemyFixtures.simpleDefinition("sand")
                .withHealthAndPrice(healthPoints, 3)
                .withSpirit(spirit)
                .withTraits(List.of(new CrumbleTrait(PER_SECOND))));
    }

    @Test
    void itLosesItsShareOfHealthEverySecondWhateverItsSpirit() {
        DefinedEnemyMob plain = this.crumbling(100, 0f);
        DefinedEnemyMob spirited = this.crumbling(100, 80f);
        int full = plain.getHealth();

        EnemyFixtures.tickEnemies(this.world, 1, SECOND);

        assertThat(full - plain.getHealth()).isEqualTo(DamageUnits.ofPoints(PER_SECOND));
        assertThat(spirited.getHealth()).isEqualTo(plain.getHealth());
    }

    @Test
    void crumblingAwayIsAKillThatPaysItsBounty() {
        DefinedEnemyMob sand = this.crumbling(10, 0f);
        int credits = this.world.economy().getCredits();

        EnemyFixtures.tickEnemies(this.world, 1, 2 * SECOND);

        assertThat(sand.fate()).isEqualTo(EnemyInspection.Fate.KILLED);
        assertThat(this.world.economy().getCredits()).isEqualTo(credits + 3);
    }
}
