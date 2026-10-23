package td.enemy;

import org.junit.jupiter.api.Test;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.BoardFixtures;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionPenalty;
import td.util.GameWorld;
import td.util.TickRate;
import td.zone.Zone;
import td.zone.ZoneKind;
import td.zone.ZoneOwner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The ground enemies leave, working on real mobs. */
class EnemyZoneTest {

    private static final int CELL = BoardFixtures.SCALE;

    private final GameWorld world = WorldFixtures.newWorldOnStraightPath();

    private DefinedEnemyMob anEnemy() {
        return EnemyFixtures.plainMob(this.world, 60, 3);
    }

    @Test
    void healingGroundHealsWhateverEnemyStandsOnIt() {
        DefinedEnemyMob ally = this.anEnemy();
        this.world.zones().add(Zone.healing(ally.getX(), ally.getY(), CELL, 100, 50, ZoneOwner.nobody()));

        this.world.zones().doTick(1);

        assertThat(ally.hasEffect(EffectKind.HEAL)).isTrue();
    }

    @Test
    void hasteGroundSpeedsUpWhatCrossesItAndShakesOffASlow() {
        DefinedEnemyMob runner = this.anEnemy();
        runner.applyEffect(Effect.chill(0.5f, 100, DamageSink.none()));
        this.world.zones().add(Zone.haste(runner.getX(), runner.getY(), CELL, Zone.UNTIL_WAVE_END, ZoneOwner.nobody()));

        this.world.zones().doTick(1);

        assertThat(runner.hasEffect(EffectKind.HASTENED)).isTrue();
        assertThat(runner.hasEffect(EffectKind.CHILL)).isFalse();
    }

    @Test
    void aHastenedEnemyTakesNoSlowButAFreezeStillStopsIt() {
        DefinedEnemyMob runner = this.anEnemy();
        runner.applyEffect(Effect.hastened(20, DamageSink.none()));

        runner.applyEffect(Effect.chill(0.5f, 100, DamageSink.none()));
        runner.applyEffect(Effect.freeze(20, DamageSink.none()));

        assertThat(runner.hasEffect(EffectKind.CHILL)).isFalse();
        assertThat(runner.hasEffect(EffectKind.FREEZE)).isTrue();
    }

    @Test
    void aWardMineAnEnemyLeavesShieldsTheFirstEnemyToStepOnIt() {
        DefinedEnemyMob miner = EnemyFixtures.liveMob(this.world, EnemyFixtures.simpleDefinition("miner")
                .withAbilities(List.of(new Ability(AbilityTerm.GUARD, new OnceTrigger(1),
                        LeaveZoneAction.wardMine(1f, 1000, 20f)))));
        DefinedEnemyMob walker = this.anEnemy();
        miner.doTick(1);
        Zone mine = this.world.zones().zones().stream().filter(zone -> zone.kind() == ZoneKind.WARD_MINE).findFirst()
                .orElseThrow();
        miner.jumpToDistance(600);
        walker.jumpToDistance(600);
        int armed = Math.round(TickRate.TICKS_PER_SECOND);
        for (int t = 2; t <= armed + 1; t++) {
            this.world.zones().doTick(t);
        }

        walker.jumpToDistance(mine.x());
        this.world.zones().doTick(armed + 2);

        assertThat(walker.hasEffect(EffectKind.SHIELD)).isTrue();
        assertThat(miner.hasEffect(EffectKind.SHIELD)).isFalse();
        assertThat(this.world.zones().zones()).doesNotContain(mine);
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
