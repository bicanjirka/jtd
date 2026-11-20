package td.zone;

import org.junit.jupiter.api.Test;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.List;

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

        assertThat(enemy.activeEffectKinds()).contains(EffectKind.TARRED, EffectKind.POISON);
    }

    @Test
    void frostGroundChillsEachPulseAndFreezesAnEnemyThatStaysItsPulses() {
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        this.world.enemies().setEnemies(new EnemyMob[]{enemy});
        this.world.zones().add(this.zone(ZoneKind.FROST_GROUND));
        int allButLast = (Zone.FROST_PULSES_TO_FREEZE - 1) * ZoneEffects.PULSE_TICKS;

        this.tick(0, allButLast);
        boolean frozenTooSoon = enemy.hasEffect(EffectKind.FREEZE);
        this.tick(allButLast, allButLast + 1);

        assertThat(frozenTooSoon).isFalse();
        assertThat(this.applied(enemy, EffectKind.CHILL)).isEqualTo(Zone.FROST_PULSES_TO_FREEZE);
        assertThat(this.applied(enemy, EffectKind.FREEZE)).isEqualTo(1);
    }

    @Test
    void anEnemyThatLeavesAFrostGroundStartsItsTimeInsideOver() {
        FakeEnemyMob enemy = FakeEnemyMob.at(100, 100);
        this.world.enemies().setEnemies(new EnemyMob[]{enemy});
        this.world.zones().add(this.zone(ZoneKind.FROST_GROUND));
        int allButLast = (Zone.FROST_PULSES_TO_FREEZE - 1) * ZoneEffects.PULSE_TICKS;
        this.tick(0, allButLast);

        enemy.moveTo(100 + 2 * RADIUS, 100);
        this.tick(allButLast, allButLast + ZoneEffects.PULSE_TICKS);
        enemy.moveTo(100, 100);
        int back = allButLast + ZoneEffects.PULSE_TICKS;
        this.tick(back, back + allButLast);

        assertThat(this.applied(enemy, EffectKind.FREEZE)).isZero();
    }

    @Test
    void aMineGoesOffTheTickTheFirstEnemyStepsWithinItAndIsThenSpent() {
        FakeEnemyMob first = FakeEnemyMob.at(100, 100);
        FakeEnemyMob second = FakeEnemyMob.at(105, 100);
        List<EnemyMob> by = new ArrayList<>();
        this.world.zones().add(Zone.wardMine(100, 100, 10, LIFETIME, this.owner, (mine, enemy) -> by.add(enemy)));
        this.world.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(500, 500)});
        int armed = Math.round(TickRate.TICKS_PER_SECOND) + 1;
        this.tick(0, armed);
        this.world.enemies().setEnemies(new EnemyMob[]{first, second});

        this.tick(armed, armed + 1);
        this.tick(armed + 1, armed + 20);

        assertThat(by).containsExactly(first);
        assertThat(this.world.zones().zones()).isEmpty();
    }

    @Test
    void aWardMineNobodyCanSetOffUntilItHasLainASecond() {
        List<EnemyMob> by = new ArrayList<>();
        this.world.zones().add(Zone.wardMine(100, 100, 10, LIFETIME, this.owner, (mine, enemy) -> by.add(enemy)));
        this.world.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(100, 100)});
        int armingTicks = Math.round(TickRate.TICKS_PER_SECOND);

        this.tick(0, armingTicks);
        boolean wentOffUnarmed = !by.isEmpty();
        this.tick(armingTicks, armingTicks + 1);

        assertThat(wentOffUnarmed).isFalse();
        assertThat(by).hasSize(1);
    }

    @Test
    void aZoneThatLastsTheWaveFadesOutOnlyOnceTheWaveEnds() {
        this.world.zones().add(Zone.haste(100, 100, RADIUS, Zone.UNTIL_WAVE_END, this.owner));

        this.tick(0, 10 * LIFETIME);
        int beforeTheWaveEnds = this.world.zones().zones().size();
        this.world.zones().endWave();
        this.tick(10 * LIFETIME, 10 * LIFETIME + Zone.SETTLE_TICKS);

        assertThat(beforeTheWaveEnds).isEqualTo(1);
        assertThat(this.world.zones().zones()).isEmpty();
    }

    @Test
    void onceTheWaveEndsEveryZoneFadesOutOverTheSettleWhateverLifeItHadLeft() {
        Zone lasting = Zone.haste(100, 100, RADIUS, 1000 * LIFETIME, this.owner);
        this.world.zones().add(lasting);
        this.tick(0, 1);
        float before = lasting.lifeLeft();

        this.world.zones().endWave();
        this.tick(1, 1 + Zone.SETTLE_TICKS / 2);
        float halfway = lasting.lifeLeft();
        this.tick(1 + Zone.SETTLE_TICKS / 2, 1 + Zone.SETTLE_TICKS);

        assertThat(halfway).isLessThan(before).isGreaterThan(0f);
        assertThat(this.world.zones().zones()).isEmpty();
    }

    @Test
    void aZoneMadeAfterTheWaveEndedFadesToo() {
        this.world.zones().endWave();

        this.world.zones().add(Zone.haste(100, 100, RADIUS, 1000 * LIFETIME, this.owner));
        this.tick(0, Zone.SETTLE_TICKS);

        assertThat(this.world.zones().zones()).isEmpty();
    }

    @Test
    void aMineIsLookedAtEveryTickNotOnlyOnAPulse() {
        FakeEnemyMob walker = FakeEnemyMob.at(500, 500);
        int[] goneOff = {0};
        this.world.zones().add(Zone.wardMine(100, 100, 10, LIFETIME, this.owner, (mine, enemy) -> goneOff[0]++));
        this.world.enemies().setEnemies(new EnemyMob[]{walker});
        int armed = Math.round(TickRate.TICKS_PER_SECOND) + 1;
        this.tick(0, armed + 2);

        walker.moveTo(100, 100);
        this.tick(armed + 2, armed + 3);

        assertThat(goneOff[0]).isEqualTo(1);
    }
}
