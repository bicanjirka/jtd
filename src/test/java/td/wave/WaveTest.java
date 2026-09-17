package td.wave;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.damage.Damage;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wave turns an already-parsed {@link WaveContent} into live, world-bound enemies - see
 * {@link WaveScriptTest} for the token-parsing behavior itself, which needs no GameWorld.
 */
class WaveTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());
    private final EnemyCatalog catalog = EnemyCatalog.builtIn();

    @Test
    void spawningTwiceProducesTwoIndependentSetsOfEnemies() {
        // spawn() is a factory, not an accessor: it binds fresh mobs to the path installed at
        // the moment it is called, which is what lets a level be published in one write.
        Wave wave = new Wave(this.context, 100, 5, 1, WaveScript.parse("c c", this.catalog));

        EnemyMob[] first = wave.spawn();
        EnemyMob[] second = wave.spawn();

        assertThat(first).hasSize(2);
        assertThat(second).hasSize(2);
        assertThat(first[0]).isNotSameAs(second[0]);
    }

    @Test
    void aSpacerOccupiesATimingSlotButSpawnsNoMob() {
        Wave wave = new Wave(this.context, 100, 5, 1, WaveScript.parse("c e c", this.catalog));

        assertThat(wave.spawn()).hasSize(2);
        assertThat(wave.enemyCount()).isEqualTo(2);
    }

    @Test
    void enemySetAndEnemyCountDelegateToTheParsedContent() {
        EnemyDefinition simple = this.catalog.get("c");
        EnemyDefinition armored = this.catalog.get("s");
        Wave wave = new Wave(this.context, 100, 5, 1, WaveScript.parse("2 c s", this.catalog));

        assertThat(wave.enemySet()).containsExactlyInAnyOrder(simple, armored);
        assertThat(wave.enemyCount(simple)).isEqualTo(2);
        assertThat(wave.enemyCount(armored)).isEqualTo(1);
    }

    @Test
    void gettersReturnConstructorArguments() {
        Wave wave = new Wave(this.context, 251, 2, 3, new WaveContent(List.of()));

        assertThat(wave.getBaseHealth()).isEqualTo(251);
        assertThat(wave.getBasePrice()).isEqualTo(2);
        assertThat(wave.getLevel()).isEqualTo(3);
    }

    @Test
    void aBossSlotDoublesSizeHalvesSpeedAndDoublesBounty() {
        DefinedEnemyMob normal = (DefinedEnemyMob) new Wave(this.context, 100, 10, 1,
                WaveScript.parse("c", this.catalog)).spawn()[0];
        DefinedEnemyMob boss = (DefinedEnemyMob) new Wave(this.context, 100, 10, 1,
                WaveScript.parse("boss c", this.catalog)).spawn()[0];

        assertThat(boss.getBodyScale()).isEqualTo(normal.getBodyScale() * 2f);
        assertThat(boss.getSpeed()).isEqualTo(normal.getSpeed() * 0.5f);
        assertThat(boss.getBounty()).isEqualTo(normal.getBounty() * 2);
        assertThat(boss.getHealth()).isEqualTo(normal.getHealth());
    }

    @Test
    void anEliteSlotIncreasesSizeHealthAndBounty() {
        DefinedEnemyMob normal = (DefinedEnemyMob) new Wave(this.context, 100, 10, 1,
                WaveScript.parse("c", this.catalog)).spawn()[0];
        DefinedEnemyMob elite = (DefinedEnemyMob) new Wave(this.context, 100, 10, 1,
                WaveScript.parse("elite c", this.catalog)).spawn()[0];

        assertThat(elite.getBodyScale()).isEqualTo(normal.getBodyScale() * 1.5f);
        assertThat(elite.getHealth()).isEqualTo(normal.getHealth() * 2);
        assertThat(elite.getBounty()).isEqualTo(Math.round(normal.getBounty() * 1.5f));
    }

    @Test
    void aBosssSpeedMultiplierSurvivesTakingDamage() {
        // The trap this closes: DefinedEnemyMob.doDamage recomputes intrinsic speed from
        // definition.baseSpeed() on every hit, so a one-time setSpeed() at construction would be
        // silently wiped by the first shot. The shape's speed multiplier must be folded into
        // that recomputation instead.
        DefinedEnemyMob boss = (DefinedEnemyMob) new Wave(this.context, 100, 10, 1,
                WaveScript.parse("boss c", this.catalog)).spawn()[0];
        float speedBeforeHit = boss.getSpeed();

        boss.doDamage(Damage.physical(1));

        assertThat(boss.getSpeed()).isEqualTo(speedBeforeHit);
    }

    @Test
    void aSwarmSlotSplitsBountyExactlyAcrossItsMembersSummingToOneNormalSpawn() {
        EnemyMob normal = new Wave(this.context, 90, 10, 1, WaveScript.parse("c", this.catalog)).spawn()[0];
        EnemyMob[] swarm = new Wave(this.context, 90, 10, 1, WaveScript.parse("swarm 3 c", this.catalog)).spawn();

        assertThat(swarm).hasSize(3);
        int totalBounty = 0;
        for (EnemyMob member : swarm) {
            totalBounty += member.getBounty();
        }
        assertThat(totalBounty).isEqualTo(normal.getBounty());
    }

    @Test
    void columnPacksItsSecondAndThirdMembersTighterThanThreeSeparateSlots() {
        this.context.setBoard(BoardGeometry.of(32, 100, 100)); // wide enough that validTarget()'s bounds check never fails
        EnemyMob[] separateSlots = new Wave(this.context, 100, 5, 1, WaveScript.parse("3 c", this.catalog)).spawn();
        EnemyMob[] column = new Wave(this.context, 100, 5, 1, WaveScript.parse("column 3 c", this.catalog)).spawn();

        assertThat(column).hasSize(3);
        int[] separateTicks = activationTicks(separateSlots);
        int[] columnTicks = activationTicks(column);

        // The leader shares the slot's own position either way; only the trailing members'
        // spacing differs, and Column packs it tighter than one slot-delay apart.
        assertThat(columnTicks[0]).isEqualTo(separateTicks[0]);
        assertThat(columnTicks[1]).isLessThan(separateTicks[1]);
        assertThat(columnTicks[2]).isLessThan(separateTicks[2]);
    }

    @Test
    void dripSpacesItsSecondAndThirdMembersLooserThanThreeSeparateSlots() {
        this.context.setBoard(BoardGeometry.of(32, 100, 100));
        EnemyMob[] separateSlots = new Wave(this.context, 100, 5, 1, WaveScript.parse("3 c", this.catalog)).spawn();
        EnemyMob[] drip = new Wave(this.context, 100, 5, 1, WaveScript.parse("drip 3 c", this.catalog)).spawn();

        assertThat(drip).hasSize(3);
        int[] separateTicks = activationTicks(separateSlots);
        int[] dripTicks = activationTicks(drip);

        assertThat(dripTicks[0]).isEqualTo(separateTicks[0]);
        assertThat(dripTicks[1]).isGreaterThan(separateTicks[1]);
        assertThat(dripTicks[2]).isGreaterThan(separateTicks[2]);
    }

    /**
     * Ticks each mob from 1 until it becomes a valid target, returning the tick that happened
     * on (0 if it was already active) - the spawn delay is otherwise a private tick countdown
     * with no accessor, so this is the black-box way to pin it.
     */
    private static int[] activationTicks(EnemyMob[] mobs) {
        int[] ticks = new int[mobs.length];
        for (int i = 0; i < mobs.length; i++) {
            ticks[i] = activationTick(mobs[i]);
        }
        return ticks;
    }

    private static int activationTick(EnemyMob mob) {
        if (mob.validTarget()) {
            return 0;
        }
        for (int t = 1; t <= 500; t++) {
            mob.doTick(t);
            if (mob.validTarget()) {
                return t;
            }
        }
        throw new AssertionError("Mob never activated within 500 ticks");
    }
}
