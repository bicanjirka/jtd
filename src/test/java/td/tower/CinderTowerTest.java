package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.tower.cinder.FlashoverMechanic;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** The Scorcher only feeds pools, so what it does shows as the effects it puts on enemies. */
class CinderTowerTest {

    private final GameWorld context = WorldFixtures.newWorldOnBoard(() -> 0.5, BoardFixtures.SCALE, 20, 20);

    /** A tower whose centre is at (112, 112), its nozzle facing +X. */
    private CinderTower tower(String... nodes) {
        CinderTower tower = new CinderTower(this.context, 3, 3);
        UpgradePaths.buy(tower, this.context, nodes);
        return tower;
    }

    private void enemies(EnemyMob... enemies) {
        this.context.enemies().setEnemies(enemies);
    }

    private static void tickThrough(CinderTower tower, int fromInclusive, int toInclusive) {
        for (int t = fromInclusive; t <= toInclusive; t++) {
            tower.doTick(t);
        }
    }

    /** The fuel one tick of its stream pours into an enemy outside the Core, in damage units. */
    private static float perTick(CinderTower tower) {
        return tower.stats().damage() / TickRate.TICKS_PER_SECOND;
    }

    private static List<Effect> poured(FakeEnemyMob enemy, EffectKind fire) {
        return enemy.appliedEffects().stream().filter(effect -> effect.kind() == fire).toList();
    }

    private static float fuelPoured(FakeEnemyMob enemy, EffectKind fire) {
        return (float) poured(enemy, fire).stream().mapToDouble(Effect::fuelLevel).sum();
    }

    @Test
    void aStreamPoursFlameIntoTheBurnOfAnEnemyInItsConeEveryTick() {
        CinderTower tower = this.tower();
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112);
        this.enemies(ahead);

        tickThrough(tower, 1, 3);

        assertThat(poured(ahead, EffectKind.BURN)).hasSize(3)
                .allSatisfy(burn -> assertThat(burn.fuelLevel()).isCloseTo(perTick(tower), within(1f)));
    }

    @Test
    void aStreamPoursForItsLengthThenWaitsOutItsReload() {
        CinderTower tower = this.tower();
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112);
        this.enemies(ahead);
        int stream = Math.round(tower.stats().value(TowerStat.STREAM));
        int reload = Math.round(tower.stats().value(TowerStat.RELOAD));

        tickThrough(tower, 1, stream + reload);
        int afterOneCycle = poured(ahead, EffectKind.BURN).size();
        tower.doTick(stream + reload + 1);

        assertThat(afterOneCycle).isEqualTo(stream);
        assertThat(poured(ahead, EffectKind.BURN)).hasSize(stream + 1);
    }

    @Test
    void attackSpeedShortensTheReloadButNotTheStream() {
        CinderTower tower = TowerFixtures.buffed(this.tower(), this.context, TowerModifiers.attackSpeed(1f));
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112);
        this.enemies(ahead);
        int stream = Math.round(tower.stats().value(TowerStat.STREAM));
        int reload = Math.round(tower.stats().value(TowerStat.RELOAD));

        tickThrough(tower, 1, stream + reload / 2 + 1);

        assertThat(poured(ahead, EffectKind.BURN)).hasSize(stream + 1);
    }

    @Test
    void noStreamStartsWithoutAVisibleEnemyInRange() {
        CinderTower tower = this.tower();
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(150, 112);
        this.enemies(ghost);

        tickThrough(tower, 1, 10);

        assertThat(tower.isStreaming()).isFalse();
        assertThat(ghost.appliedEffects()).isEmpty();
    }

    @Test
    void aHiddenEnemyInTheConeOfAStreamAtAVisibleOneBurnsToo() {
        CinderTower tower = this.tower();
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112);
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(160, 114);
        this.enemies(ahead, ghost);

        tickThrough(tower, 1, 2);

        assertThat(poured(ghost, EffectKind.BURN)).hasSize(2);
    }

    @Test
    void anEnemyOutOfRangeTakesNoFlame() {
        CinderTower tower = this.tower();
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112);
        FakeEnemyMob far = FakeEnemyMob.at(112 + tower.getRangeReal() + 10, 112);
        this.enemies(ahead, far);

        tickThrough(tower, 1, 5);

        assertThat(far.appliedEffects()).isEmpty();
    }

    @Test
    void aStreamWhoseTargetLeavesRunsItsTimeOutIntoTheAir() {
        CinderTower tower = this.tower();
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112);
        this.enemies(ahead);
        int stream = Math.round(tower.stats().value(TowerStat.STREAM));
        tickThrough(tower, 1, 2);

        this.enemies();
        tickThrough(tower, 3, stream - 1);
        boolean stillStreaming = tower.isStreaming();
        tower.doTick(stream);

        assertThat(stillStreaming).isTrue();
        assertThat(tower.isStreaming()).isFalse();
    }

    @Test
    void searScorchesAnEnemyOnTheFirstTouchOfEachStreamOnly() {
        CinderTower tower = this.tower();
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112);
        this.enemies(ahead);
        int stream = Math.round(tower.stats().value(TowerStat.STREAM));
        int reload = Math.round(tower.stats().value(TowerStat.RELOAD));

        tickThrough(tower, 1, stream);
        int afterOne = poured(ahead, EffectKind.SCORCHED).size();
        tickThrough(tower, stream + 1, stream + reload + 1);

        assertThat(afterOne).isEqualTo(1);
        assertThat(poured(ahead, EffectKind.SCORCHED)).hasSize(2);
    }

    @Test
    void kindlingMakesSearScorchMore() {
        FakeEnemyMob plain = FakeEnemyMob.at(150, 112);
        this.enemies(plain);
        this.tower().doTick(1);
        FakeEnemyMob kindled = FakeEnemyMob.at(150, 112);
        this.enemies(kindled);

        this.tower("Kindling").doTick(1);

        assertThat(kindled.effectStacks(EffectKind.SCORCHED)).isGreaterThan(plain.effectStacks(EffectKind.SCORCHED));
    }

    @Test
    void theCorePoursMoreIntoItsTargetThanTheConeDoesAwayFromIt() {
        CinderTower tower = this.tower("Core");
        FakeEnemyMob target = FakeEnemyMob.at(150, 112);
        double off = Math.toRadians(20);
        FakeEnemyMob aside = FakeEnemyMob.at(112 + 50 * Math.cos(off), 112 + 50 * Math.sin(off));
        this.enemies(target, aside);

        tower.doTick(1);

        assertThat(fuelPoured(aside, EffectKind.BURN)).isCloseTo(perTick(tower), within(1f));
        assertThat(fuelPoured(target, EffectKind.BURN)).isGreaterThan(fuelPoured(aside, EffectKind.BURN) * 1.1f);
    }

    @Test
    void soulfirePoursSoulfireInPlaceOfBurn() {
        CinderTower tower = this.tower("Soulfire");
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112);
        this.enemies(ahead);

        tower.doTick(1);

        assertThat(poured(ahead, EffectKind.SOULFIRE)).hasSize(1);
        assertThat(poured(ahead, EffectKind.BURN)).isEmpty();
    }

    @Test
    void thermalShockPoursMoreIntoAHeldEnemy() {
        CinderTower tower = this.tower("Thermal Shock");
        FakeEnemyMob held = FakeEnemyMob.at(150, 112);
        held.reportFrozen();
        FakeEnemyMob free = FakeEnemyMob.at(160, 114);
        this.enemies(held, free);

        tower.doTick(1);

        assertThat(fuelPoured(free, EffectKind.BURN)).isCloseTo(perTick(tower), within(1f));
        assertThat(fuelPoured(held, EffectKind.BURN)).isGreaterThan(fuelPoured(free, EffectKind.BURN) * 1.1f);
    }

    @Test
    void meltdownAddsAShareOfTheFuelForEachPointOfArmorAndPlating() {
        CinderTower tower = this.tower("Meltdown");
        FakeEnemyMob light = FakeEnemyMob.at(150, 112);
        FakeEnemyMob armored = FakeEnemyMob.at(160, 114);
        FakeEnemyMob heavier = FakeEnemyMob.at(165, 110);
        armored.reportArmorAndPlating(50f);
        heavier.reportArmorAndPlating(100f);
        this.enemies(light, armored, heavier);

        tower.doTick(1);

        float base = fuelPoured(light, EffectKind.BURN);
        float firstFifty = fuelPoured(armored, EffectKind.BURN) - base;
        assertThat(firstFifty).isPositive();
        assertThat(fuelPoured(heavier, EffectKind.BURN) - base).isCloseTo(2 * firstFifty, within(2f));
    }

    @Test
    void flashoverPoursPartOfTheFlameIntoTheEnemiesBesideAHotBurn() {
        CinderTower tower = this.tower("Flashover");
        FakeEnemyMob hot = FakeEnemyMob.at(150, 112);
        hot.applyEffect(Effect.burn(Damage.magic(DamageUnits.ofPoints(2 * FlashoverMechanic.HOT_FUEL_POINTS)),
                DamageSink.none()));
        FakeEnemyMob beside = FakeEnemyMob.at(150, 150);
        this.enemies(hot, beside);

        tower.doTick(1);

        assertThat(fuelPoured(beside, EffectKind.BURN))
                .isCloseTo(perTick(tower) * FlashoverMechanic.SHARE, within(1f));
        assertThat(tower.flashes()).hasSize(1);
    }

    @Test
    void aCoolBurnDoesNotFlashOver() {
        CinderTower tower = this.tower("Flashover");
        FakeEnemyMob cool = FakeEnemyMob.at(150, 112);
        FakeEnemyMob beside = FakeEnemyMob.at(150, 150);
        this.enemies(cool, beside);

        tower.doTick(1);

        assertThat(beside.appliedEffects()).isEmpty();
    }

    @Test
    void itsSheetShowsItsFlameAndWhatAWholeStreamGivesAnEnemy() {
        CinderTower tower = this.tower();
        float flame = DamageUnits.inPoints(tower.stats().damage());
        float seconds = tower.stats().value(TowerStat.STREAM) / TickRate.TICKS_PER_SECOND;

        assertThat(rowValue(tower, TowerStatRow.FLAME)).isCloseTo(flame, within(1e-3f));
        assertThat(rowValue(tower, TowerStatRow.STREAM_FUEL)).isCloseTo(flame * seconds, within(1e-3f));
    }

    private static float rowValue(CinderTower tower, TowerStatRow row) {
        return tower.inspect().stats().stream().filter(line -> line.stat() == row).findFirst().orElseThrow()
                .breakdown().current();
    }

    @Test
    void itAppliesBurnAndItsSearsScorchedOnceEachFromItself() {
        CinderTower tower = this.tower();

        assertThat(tower.applies()).extracting(AppliedEffect::term).containsExactly(EffectKind.BURN, EffectKind.SCORCHED);
        assertThat(tower.applies().getFirst().sources()).hasSize(1);
        assertThat(tower.inspect().targeting()).isEqualTo("nearest");
    }
}
