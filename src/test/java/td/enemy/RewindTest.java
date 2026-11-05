package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
import td.stat.StatCurves;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A rewind on a live mob: how far back it reaches, whom it reaches, and what keeps it out or turns it round. */
class RewindTest {

    private static final int WINDOW = 10;
    private static final int CAST = 2 * WINDOW;
    private static final int LONG = 1000;

    private final GameWorld world = WorldFixtures.newWorldOnStraightPath();

    private DefinedEnemyMob rewinding(EffectTarget target) {
        return EnemyFixtures.liveMob(this.world, EnemyFixtures.simpleDefinition("keeper").withAbilities(List.of(
                new Ability(AbilityTerm.REWIND, new PeriodicTrigger(CAST), new RewindAction(WINDOW, target)))));
    }

    /** Lives until just before {@code CAST - WINDOW}, takes {@code damage}, then lives through the cast. */
    private void hurtBeforeTheWindowThenCast(DefinedEnemyMob target, int damage) {
        EnemyFixtures.tickEnemies(this.world, 1, CAST - WINDOW);
        target.doDamage(Damage.physical(damage));
        EnemyFixtures.tickEnemies(this.world, CAST - WINDOW + 1, CAST);
    }

    @Test
    void aRewindTurnsHealthBackToWhatItWasBeforeTheWindow() {
        DefinedEnemyMob keeper = this.rewinding(new SelfTarget());
        int full = keeper.getHealth();

        this.hurtBeforeTheWindowThenCast(keeper, full / 2);

        assertThat(keeper.getHealth()).isEqualTo(full);
    }

    @Test
    void damageFromBeforeTheWindowStays() {
        DefinedEnemyMob keeper = this.rewinding(new SelfTarget());
        int full = keeper.getHealth();
        EnemyFixtures.tickEnemies(this.world, 1, WINDOW / 2);
        keeper.doDamage(Damage.physical(full / 2));

        EnemyFixtures.tickEnemies(this.world, WINDOW / 2 + 1, CAST);

        assertThat(keeper.getHealth()).isEqualTo(full - full / 2);
    }

    @Test
    void aRewindAtARadiusReachesTheAlliesNearItAndNotItself() {
        DefinedEnemyMob keeper = this.rewinding(new RadiusTarget(80f));
        DefinedEnemyMob ally = EnemyFixtures.plainMob(this.world, 100, 1);
        int allyFull = ally.getHealth();
        int keeperFull = keeper.getHealth();
        EnemyFixtures.tickEnemies(this.world, 1, CAST - WINDOW);
        ally.doDamage(Damage.physical(allyFull / 2));
        keeper.doDamage(Damage.physical(keeperFull / 2));

        EnemyFixtures.tickEnemies(this.world, CAST - WINDOW + 1, CAST);

        assertThat(ally.getHealth()).isEqualTo(allyFull);
        assertThat(keeper.getHealth()).isEqualTo(keeperFull - keeperFull / 2);
    }

    @Test
    void aRewindIsWorthNothingAtTheLowestSpirit() {
        DefinedEnemyMob keeper = this.rewinding(new SelfTarget());
        int full = keeper.getHealth();
        keeper.applyEffect(Effect.deadZone(LONG, DamageSink.none()));

        this.hurtBeforeTheWindowThenCast(keeper, full / 2);

        assertThat(keeper.getHealth()).isEqualTo(full - full / 2);
    }

    @Test
    void aRewindGivesBackWhatSpiritMakesItWorth() {
        DefinedEnemyMob keeper = this.rewinding(new SelfTarget());
        int full = keeper.getHealth();
        int lost = full / 2;
        keeper.applyEffect(Effect.sickened(50));

        this.hurtBeforeTheWindowThenCast(keeper, lost);

        float worth = StatCurves.restoreFactor(keeper.stats().value(EnemyStat.SPIRIT));
        assertThat(worth).isBetween(0f, 1f);
        assertThat(keeper.getHealth()).isEqualTo(full - lost + Math.round(lost * worth));
    }

    @Test
    void underInversionWhatARewindWouldGiveBackIsDealtAsMagic() {
        DefinedEnemyMob keeper = this.rewinding(new SelfTarget());
        int full = keeper.getHealth();
        List<Damage> credited = new ArrayList<>();
        keeper.applyEffect(Effect.hex(EffectKind.INVERSION, LONG, damage -> credited.add(damage)));

        this.hurtBeforeTheWindowThenCast(keeper, full / 4);

        assertThat(credited).containsExactly(Damage.magic(full / 4));
    }
}
