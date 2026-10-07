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

    @Test
    void aMineGoesOffTheTickTheFirstEnemyStepsWithinItAndIsThenSpent() {
        FakeEnemyMob first = FakeEnemyMob.at(100, 100);
        FakeEnemyMob second = FakeEnemyMob.at(105, 100);
        java.util.List<EnemyMob> by = new java.util.ArrayList<>();
        this.world.zones().add(Zone.mine(100, 100, 10, LIFETIME, this.owner, (mine, enemy) -> by.add(enemy)));
        this.world.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(500, 500)});
        this.tick(1, 4);
        this.world.enemies().setEnemies(new EnemyMob[]{first, second});

        this.tick(4, 5);
        this.tick(5, 20);

        assertThat(by).containsExactly(first);
        assertThat(this.world.zones().zones()).isEmpty();
    }

    @Test
    void aMineIsLookedAtEveryTickNotOnlyOnAPulse() {
        FakeEnemyMob walker = FakeEnemyMob.at(500, 500);
        int[] goneOff = {0};
        this.world.zones().add(Zone.mine(100, 100, 10, LIFETIME, this.owner, (mine, enemy) -> goneOff[0]++));
        this.world.enemies().setEnemies(new EnemyMob[]{walker});
        this.tick(1, 3);

        walker.moveTo(100, 100);
        this.tick(3, 4);

        assertThat(goneOff[0]).isEqualTo(1);
    }

    @Test
    void aCursedCloudGivesEveryEnemyInsideTheEffectsItCarries() {
        FakeEnemyMob inside = FakeEnemyMob.at(100, 100);
        FakeEnemyMob outside = FakeEnemyMob.at(100 + 5 * RADIUS, 100);
        this.world.enemies().setEnemies(new EnemyMob[]{inside, outside});
        this.world.zones().add(Zone.cloud(100, 100, RADIUS, LIFETIME, this.owner,
                java.util.List.of(Effect.vulnerable(2, 80, d -> {
                }))));

        this.tick(1, 2);

        assertThat(inside.activeEffectKinds()).contains(EffectKind.VULNERABLE);
        assertThat(outside.activeEffectKinds()).isEmpty();
    }

    @Test
    void aCursedCloudCountsTheDebuffsItCarriesDownWithItsOwnAge() {
        FakeEnemyMob inside = FakeEnemyMob.at(100, 100);
        this.world.enemies().setEnemies(new EnemyMob[]{inside});
        this.world.zones().add(Zone.cloud(100, 100, RADIUS, LIFETIME, this.owner,
                java.util.List.of(Effect.vulnerable(2, 40, d -> {
                }))));

        this.tick(1, LIFETIME);

        java.util.List<Effect> given = inside.appliedEffects();
        assertThat(given).isNotEmpty();
        assertThat(given).allMatch(effect -> effect.remainingTicks() <= 40);
        assertThat(given.getLast().remainingTicks()).isLessThan(given.getFirst().remainingTicks());
    }

    @Test
    void aCursedCloudStopsGivingADebuffOnceItsTimeIsUp() {
        FakeEnemyMob inside = FakeEnemyMob.at(100, 100);
        this.world.enemies().setEnemies(new EnemyMob[]{inside});
        this.world.zones().add(Zone.cloud(100, 100, RADIUS, LIFETIME, this.owner,
                java.util.List.of(Effect.vulnerable(2, 40, d -> {
                }))));
        this.tick(1, 41);
        int givenWhileItLasted = inside.appliedEffects().size();

        this.tick(41, LIFETIME);

        assertThat(inside.appliedEffects()).hasSize(givenWhileItLasted);
    }

    @Test
    void aTowerCanTakeAZoneOffTheBoard() {
        Zone zone = this.zone(ZoneKind.TAR);
        this.world.zones().add(zone);

        this.world.zones().remove(zone);

        assertThat(this.world.zones().zones()).isEmpty();
    }
}
