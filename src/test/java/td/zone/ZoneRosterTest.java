package td.zone;

import org.junit.jupiter.api.Test;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneRosterTest {

    private static final int RADIUS = 50;
    private static final int LIFETIME = 100;

    private final GameWorld world = WorldFixtures.newWorld();
    private final FakeZoneOwner owner = new FakeZoneOwner();

    private Zone zone(ZoneKind kind) {
        return new Zone(kind, 100, 100, RADIUS, LIFETIME, 100, this.owner);
    }

    private long applied(FakeEnemyMob enemy, EffectKind kind) {
        return enemy.appliedEffects().stream().map(Effect::kind).filter(kind::equals).count();
    }

    private void tick(int from, int to) {
        for (int t = from; t < to; t++) {
            this.world.zones().doTick(t);
        }
    }

    @Test
    void aBurningGroundSetsEveryEnemyInsideAlightHiddenOnesIncludedAndLeavesTheOthersAlone() {
        FakeEnemyMob inside = FakeEnemyMob.at(100, 100);
        FakeEnemyMob hidden = FakeEnemyMob.ghostAt(120, 100);
        FakeEnemyMob outside = FakeEnemyMob.at(100 + RADIUS + 10, 100);
        this.world.enemies().setEnemies(new EnemyMob[]{inside, hidden, outside});
        this.world.zones().add(this.zone(ZoneKind.BURNING_GROUND));

        this.tick(1, 2);

        assertThat(inside.hasEffect(EffectKind.BURN)).isTrue();
        assertThat(hidden.hasEffect(EffectKind.BURN)).isTrue();
        assertThat(outside.activeEffectKinds()).isEmpty();
    }

    @Test
    void aZonePulsesTheTickItIsMadeThenTwiceASecond() {
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        this.world.enemies().setEnemies(new EnemyMob[]{enemy});
        this.world.zones().add(this.zone(ZoneKind.BURNING_GROUND));

        this.tick(3, 4);
        long onTheFirstTick = this.applied(enemy, EffectKind.BURN);
        this.tick(4, 10);
        long beforeTheNextPulse = this.applied(enemy, EffectKind.BURN);
        this.tick(10, 11);

        assertThat(onTheFirstTick).isEqualTo(1);
        assertThat(beforeTheNextPulse).isEqualTo(1);
        assertThat(this.applied(enemy, EffectKind.BURN)).isEqualTo(2);
    }

    @Test
    void aZoneEndsWhenItsLifeIsOver() {
        this.world.zones().add(this.zone(ZoneKind.TAR));

        this.tick(0, LIFETIME - 1);
        int whileItLasts = this.world.zones().zones().size();
        this.tick(LIFETIME - 1, LIFETIME);

        assertThat(whileItLasts).isEqualTo(1);
        assertThat(this.world.zones().zones()).isEmpty();
    }

    @Test
    void twoZonesOfOneKindNeverStackAnEnemyInBothTakesOnePulse() {
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        this.world.enemies().setEnemies(new EnemyMob[]{enemy});
        this.world.zones().add(this.zone(ZoneKind.BURNING_GROUND));
        this.world.zones().add(this.zone(ZoneKind.BURNING_GROUND));

        this.tick(10, 11);

        assertThat(this.applied(enemy, EffectKind.BURN)).isEqualTo(1);
    }

    @Test
    void zonesOfDifferentKindsBothTouchAnEnemyStandingInThem() {
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        this.world.enemies().setEnemies(new EnemyMob[]{enemy});
        this.world.zones().add(this.zone(ZoneKind.BURNING_GROUND));
        this.world.zones().add(this.zone(ZoneKind.TAR));

        this.tick(10, 11);

        assertThat(enemy.activeEffectKinds()).contains(EffectKind.BURN, EffectKind.TARRED, EffectKind.POISON);
    }

    @Test
    void tarSlowsPoisonsAndLeavesAnEnemyTarred() {
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        this.world.enemies().setEnemies(new EnemyMob[]{enemy});
        this.world.zones().add(this.zone(ZoneKind.TAR));

        this.tick(10, 11);

        assertThat(enemy.activeEffectKinds()).containsExactlyInAnyOrder(EffectKind.TARRED, EffectKind.POISON);
    }

    @Test
    void frostGroundChillsEachPulseAndFreezesAnEnemyThatStaysTwoSeconds() {
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        this.world.enemies().setEnemies(new EnemyMob[]{enemy});
        this.world.zones().add(this.zone(ZoneKind.FROST_GROUND));

        this.tick(0, 21);
        boolean frozenTooSoon = enemy.hasEffect(EffectKind.FREEZE);
        this.tick(21, 31);

        assertThat(frozenTooSoon).isFalse();
        assertThat(this.applied(enemy, EffectKind.CHILL)).isEqualTo(4);
        assertThat(this.applied(enemy, EffectKind.FREEZE)).isEqualTo(1);
    }

    @Test
    void anEnemyThatLeavesAFrostGroundStartsItsTimeInsideOver() {
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        this.world.enemies().setEnemies(new EnemyMob[]{enemy});
        this.world.zones().add(this.zone(ZoneKind.FROST_GROUND));
        this.tick(0, 21);

        enemy.moveTo(100 + 2 * RADIUS, 100);
        this.tick(21, 31);
        enemy.moveTo(100, 100);
        this.tick(31, 51);

        assertThat(this.applied(enemy, EffectKind.FREEZE)).isZero();
    }
}
