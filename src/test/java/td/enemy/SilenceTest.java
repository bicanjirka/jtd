package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.HealTemplate;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Effects that stop an enemy from helping itself: silence, dispel and anchor. */
class SilenceTest {

    private static final int HEAL_INTERVAL = 20;

    private final GameWorld world = WorldFixtures.newWorldOnStraightPath();

    private static EnemyDefinition healingAround(String id, AbilityTrigger trigger) {
        return EnemyFixtures.simpleDefinition(id).withAbilities(List.of(new Ability(AbilityTerm.MEND, trigger,
                new ApplyEffectAction(new HealTemplate(1f, HEAL_INTERVAL), new RadiusTarget(80f)))));
    }

    private DefinedEnemyMob woundedAlly() {
        DefinedEnemyMob ally = EnemyFixtures.plainMob(this.world, 100, 4);
        ally.doDamage(Damage.physical(ally.getHealth() / 2));
        return ally;
    }

    private static void silence(DefinedEnemyMob mob, int ticks) {
        mob.applyEffect(Effect.silenced(ticks, DamageSink.none()));
    }

    @Test
    void aSilencedEnemyCastsNothing() {
        DefinedEnemyMob healer = EnemyFixtures.liveMob(this.world, healingAround("healer", new PeriodicTrigger(HEAL_INTERVAL)));
        DefinedEnemyMob ally = this.woundedAlly();
        int woundedHealth = ally.getHealth();
        silence(healer, 1000);

        EnemyFixtures.tickEnemies(this.world, 1, 2 * HEAL_INTERVAL);

        assertThat(ally.getHealth()).isEqualTo(woundedHealth);
    }

    @Test
    void aSilencedEnemyStillWalks() {
        DefinedEnemyMob healer = EnemyFixtures.liveMob(this.world, healingAround("healer", new PeriodicTrigger(HEAL_INTERVAL)));
        silence(healer, 1000);

        healer.doTick(1);

        assertThat(healer.getX()).isGreaterThan(0.0);
    }

    @Test
    void aSilencedEnemysDeathAbilityStillFires() {
        DefinedEnemyMob martyr = EnemyFixtures.liveMob(this.world, healingAround("martyr", new OnDeathTrigger()));
        DefinedEnemyMob ally = this.woundedAlly();
        int woundedHealth = ally.getHealth();
        silence(martyr, 1000);

        martyr.doDamage(Damage.physical(1_000_000_000));
        martyr.doTick(1);
        ally.doTick(2);

        assertThat(ally.getHealth()).isGreaterThan(woundedHealth);
    }

    @Test
    void aSilenceThatRanOutLetsTheEnemyCastAgain() {
        DefinedEnemyMob healer = EnemyFixtures.liveMob(this.world, healingAround("healer", new PeriodicTrigger(HEAL_INTERVAL)));
        DefinedEnemyMob ally = this.woundedAlly();
        int woundedHealth = ally.getHealth();
        silence(healer, 5);

        EnemyFixtures.tickEnemies(this.world, 1, 3 * HEAL_INTERVAL);

        assertThat(healer.hasEffect(EffectKind.SILENCED)).isFalse();
        assertThat(ally.getHealth()).isGreaterThan(woundedHealth);
    }

    @Test
    void dispellingStripsAnEnemysShieldAndHealButNothingElse() {
        DefinedEnemyMob enemy = EnemyFixtures.plainMob(this.world, 100, 4);
        enemy.applyEffect(Effect.shield(0.3f, 100, DamageSink.none()));
        enemy.applyEffect(Effect.heal(5, 100, DamageSink.none()));
        enemy.applyEffect(Effect.exposed(100, DamageSink.none()));

        enemy.dispelRestoratives();

        assertThat(enemy.activeEffectKinds()).containsExactly(EffectKind.EXPOSED);
    }

    @Test
    void anAnchorHoldsAnEnemyWhoseWoundsWouldSpeedItUpToTheSamePaceAsAnUnhurtOne() {
        EnemyDefinition hastyWhenHurt = EnemyFixtures.simpleDefinition("hasty").withTraits(List.of(new HurtSpeedTrait(1.4f)));
        DefinedEnemyMob hurt = EnemyFixtures.liveMob(this.world, hastyWhenHurt);
        DefinedEnemyMob unhurt = EnemyFixtures.liveMob(this.world, hastyWhenHurt);
        hurt.doDamage(Damage.physical(hurt.getHealth() * 9 / 10));
        float hurried = hurt.getSpeed();

        hurt.applyEffect(Effect.anchored(100, DamageSink.none()));
        unhurt.applyEffect(Effect.anchored(100, DamageSink.none()));

        assertThat(hurried).isGreaterThan(hastyWhenHurt.baseSpeed());
        assertThat(hurt.getSpeed()).isEqualTo(unhurt.getSpeed());
    }
}
