package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class AgilityTest {

    private static final AttackProfile AIMED = AttackProfile.none().asAimed();

    /** A still, nimble enemy in a world whose every roll succeeds, so it dodges whatever it may dodge. */
    private static DefinedEnemyMob nimble() {
        GameWorld world = WorldFixtures.newWorldOnBoard(() -> 0.0, 1, 1001, 1001);
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.register(EnemyDefinition.of("nimble", "Nimble", 100, 1, 0f, BodyArchetype.CIRCLE)
                .withAgility(50f));
        return (DefinedEnemyMob) catalog.spawn("nimble", world, 0, 100, 1, Rank.GRUNT);
    }

    @Test
    void aDodgedHitTakesNothingAndIsCounted() {
        DefinedEnemyMob enemy = nimble();
        int health = enemy.getHealth();

        HitResult result = enemy.takeHit(Damage.physical(1000), AIMED);
        enemy.doTick(1);

        assertThat(result.dodged()).isTrue();
        assertThat(result.credited()).isZero();
        assertThat(enemy.getHealth()).isEqualTo(health);
        assertThat(enemy.ticksSinceDodge(1)).hasValue(0);
    }

    @Test
    void aDodgedHitIsNoHitForTheIdleTimer() {
        DefinedEnemyMob enemy = nimble();
        for (int t = 1; t <= 10; t++) {
            enemy.doTick(t);
        }

        enemy.takeHit(Damage.physical(1000), AIMED);

        assertThat(enemy.ticksSinceLastHit()).isEqualTo(10);
    }

    @Test
    void aHitThatIsNotAimedIsNeverDodged() {
        DefinedEnemyMob enemy = nimble();

        HitResult result = enemy.takeHit(Damage.physical(1000), AttackProfile.none());

        assertThat(result.dodged()).isFalse();
        assertThat(result.landed()).isEqualTo(Damage.physical(1000));
    }

    @Test
    void aFrozenOrDazedEnemyCannotDodge() {
        DefinedEnemyMob frozen = nimble();
        DefinedEnemyMob dazed = nimble();
        frozen.applyEffect(Effect.freeze(100, DamageSink.none()));
        dazed.applyEffect(Effect.dazed(100, DamageSink.none()));

        assertThat(frozen.takeHit(Damage.physical(1000), AIMED).dodged()).isFalse();
        assertThat(dazed.takeHit(Damage.physical(1000), AIMED).dodged()).isFalse();
    }

    @Test
    void aMarkedEnemyCannotDodgeTheHitThatSpendsTheMark() {
        DefinedEnemyMob enemy = nimble();
        enemy.applyEffect(Effect.marked(100, DamageSink.none()));

        HitResult marked = enemy.takeHit(Damage.physical(1000), AIMED);
        HitResult next = enemy.takeHit(Damage.physical(1000), AIMED);

        assertThat(marked.dodged()).isFalse();
        assertThat(marked.critical()).isTrue();
        assertThat(next.dodged()).isTrue();
    }
}
