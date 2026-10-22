package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionPenalty;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;
import td.zone.Zone;
import td.zone.ZoneKind;
import td.zone.ZoneOwner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The zones and blasts enemies leave, wired through real mobs on a real world. */
class EnemyZoneTest {

    private static final Damage LETHAL = Damage.physical(5_000_000);
    private static final int CELL = 32;

    private final GameWorld world = worldWithStraightPath();

    private static GameWorld worldWithStraightPath() {
        GameWorld world = WorldFixtures.newWorldOnBoard(CELL, 1000, 1000);
        world.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(1000, 0))));
        return world;
    }

    private DefinedEnemyMob spawn(String id, Rank rank) {
        return this.spawnIn(this.world, id, rank);
    }

    private DefinedEnemyMob spawnIn(GameWorld in, String id, Rank rank) {
        DefinedEnemyMob mob = (DefinedEnemyMob) in.getEnemyCatalog().spawn(id, in, 0, 60, 3, rank);
        in.enemies().add(mob);
        return mob;
    }

    private void kill(DefinedEnemyMob mob, int tick) {
        mob.doDamage(LETHAL);
        mob.doTick(tick);
    }

    @Test
    void aVeteranMenderDyingHealsAndShieldsTheAlliesAroundIt() {
        DefinedEnemyMob mender = this.spawn("m", Rank.VETERAN);
        DefinedEnemyMob ally = this.spawn("c", Rank.GRUNT);
        mender.doTick(1);

        this.kill(mender, 2);

        assertThat(ally.hasEffect(EffectKind.HEAL)).isTrue();
        assertThat(ally.hasEffect(EffectKind.SHIELD)).isTrue();
    }

    @Test
    void aBossMenderDyingHealsButLeavesNoShield() {
        DefinedEnemyMob mender = this.spawn("m", Rank.BOSS);
        DefinedEnemyMob ally = this.spawn("c", Rank.GRUNT);
        mender.doTick(1);

        this.kill(mender, 2);

        assertThat(ally.hasEffect(EffectKind.HEAL)).isTrue();
        assertThat(ally.hasEffect(EffectKind.SHIELD)).isFalse();
    }

    @Test
    void aMenderLeavesNoGroundAsItWalks() {
        DefinedEnemyMob mender = this.spawn("m", Rank.ELITE);

        for (int t = 1; t <= 100; t++) {
            mender.doTick(t);
        }

        assertThat(this.world.zones().zones()).isEmpty();
    }

    @Test
    void healingGroundHealsWhateverEnemyStandsOnIt() {
        DefinedEnemyMob ally = this.spawn("c", Rank.GRUNT);
        this.world.zones().add(Zone.healing(ally.getX(), ally.getY(), CELL, 100, 50, ZoneOwner.nobody()));

        this.world.zones().doTick(1);

        assertThat(ally.hasEffect(EffectKind.HEAL)).isTrue();
    }

    @Test
    void hasteGroundSpeedsUpWhatCrossesItAndShakesOffASlow() {
        DefinedEnemyMob runner = this.spawn("c", Rank.GRUNT);
        runner.applyEffect(Effect.chill(0.5f, 100, DamageSink.none()));
        this.world.zones().add(Zone.haste(runner.getX(), runner.getY(), CELL, Zone.UNTIL_WAVE_END, ZoneOwner.nobody()));

        this.world.zones().doTick(1);

        assertThat(runner.hasEffect(EffectKind.HASTENED)).isTrue();
        assertThat(runner.hasEffect(EffectKind.CHILL)).isFalse();
    }

    @Test
    void aHastenedEnemyTakesNoSlowButAFreezeStillStopsIt() {
        DefinedEnemyMob runner = this.spawn("c", Rank.GRUNT);
        runner.applyEffect(Effect.hastened(20, DamageSink.none()));

        runner.applyEffect(Effect.chill(0.5f, 100, DamageSink.none()));
        runner.applyEffect(Effect.freeze(20, DamageSink.none()));

        assertThat(runner.hasEffect(EffectKind.CHILL)).isFalse();
        assertThat(runner.hasEffect(EffectKind.FREEZE)).isTrue();
    }

    @Test
    void anEliteFrenziedMobThrowsHasteGroundThatLastsTheWholeWave() {
        DefinedEnemyMob frenzied = this.spawn("t", Rank.ELITE);
        frenzied.doTick(1);

        frenzied.doDamage(Damage.physical(frenzied.getHealth() * 40 / 100));
        frenzied.doTick(2);

        assertThat(this.world.zones().zones()).extracting(Zone::kind).contains(ZoneKind.HASTE_GROUND);
        this.world.zones().endWave();
        assertThat(this.world.zones().zones()).isEmpty();
    }

    @Test
    void aFrenziedBossLeavesNoHasteGround() {
        DefinedEnemyMob frenzied = this.spawn("t", Rank.BOSS);
        frenzied.doTick(1);

        frenzied.doDamage(Damage.physical(frenzied.getHealth() * 40 / 100));
        frenzied.doTick(2);

        assertThat(this.world.zones().zones()).extracting(Zone::kind).doesNotContain(ZoneKind.HASTE_GROUND);
    }

    @Test
    void aWardMineWaitsToBeArmedThenShieldsTheFirstEnemyToStepOnIt() {
        DefinedEnemyMob armored = this.spawn("s", Rank.ELITE);
        DefinedEnemyMob walker = this.spawn("c", Rank.GRUNT);
        for (int t = 1; t <= 60; t++) {
            armored.doTick(t);
        }
        Zone mine = this.world.zones().zones().stream().filter(zone -> zone.kind() == ZoneKind.WARD_MINE).findFirst()
                .orElseThrow();
        armored.jumpToDistance(600);
        walker.jumpToDistance(600);

        for (int t = 61; t <= 100; t++) {
            this.world.zones().doTick(t);
        }
        assertThat(this.world.zones().zones()).contains(mine);
        walker.jumpToDistance((int) mine.x());
        this.world.zones().doTick(101);

        assertThat(walker.hasEffect(EffectKind.SHIELD)).isTrue();
        assertThat(this.world.zones().zones()).doesNotContain(mine);
    }

    @Test
    void aVeteranJuggernautDyingBlindsTheTowersWithinTwoCellsUntilTheWaveIsOver() {
        DefinedEnemyMob juggernaut = this.spawn("juggernaut", Rank.VETERAN);
        juggernaut.doTick(1);
        double x = juggernaut.getX();
        double y = juggernaut.getY();

        this.kill(juggernaut, 2);

        assertThat(this.world.zones().disruptionAt(x + 1.9 * CELL, y).missChance()).isEqualTo(0.75f);
        assertThat(this.world.zones().disruptionAt(x + 2.1 * CELL, y).isNone()).isTrue();
        this.world.zones().doTick(10_000);
        assertThat(this.world.zones().disruptionAt(x, y).missChance()).isEqualTo(0.75f);
        this.world.zones().endWave();
        assertThat(this.world.zones().disruptionAt(x, y).isNone()).isTrue();
    }

    @Test
    void anEliteAndABossJuggernautBlindTheTowersWithinFourCells() {
        for (Rank rank : List.of(Rank.ELITE, Rank.BOSS)) {
            GameWorld fresh = worldWithStraightPath();
            DefinedEnemyMob juggernaut = this.spawnIn(fresh, "juggernaut", rank);
            juggernaut.doTick(1);

            this.kill(juggernaut, 2);

            assertThat(fresh.zones().disruptionAt(juggernaut.getX() + 3.9 * CELL, juggernaut.getY()).missChance())
                    .as(rank.name()).isEqualTo(0.75f);
            assertThat(fresh.zones().disruptionAt(juggernaut.getX() + 4.1 * CELL, juggernaut.getY()).isNone())
                    .as(rank.name()).isTrue();
        }
    }

    @Test
    void aJuggernautBelowVeteranLeavesNothingWhenItDies() {
        DefinedEnemyMob juggernaut = this.spawn("juggernaut", Rank.SOLDIER);
        juggernaut.doTick(1);

        this.kill(juggernaut, 2);

        assertThat(this.world.zones().zones()).isEmpty();
    }

    @Test
    void blindingFieldsNeverStackTheStrongestCounts() {
        this.world.zones().add(Zone.blinding(0, 0, CELL, Zone.UNTIL_WAVE_END, DisruptionPenalty.blinding(0.5f),
                ZoneOwner.nobody()));
        this.world.zones().add(Zone.blinding(0, 0, CELL, Zone.UNTIL_WAVE_END, DisruptionPenalty.blinding(0.75f),
                ZoneOwner.nobody()));

        assertThat(this.world.zones().disruptionAt(0, 0).missChance()).isEqualTo(0.75f);
    }
}
