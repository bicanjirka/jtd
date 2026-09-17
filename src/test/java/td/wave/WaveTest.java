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
import static org.assertj.core.api.Assertions.within;

/**
 * Wave turns an already-parsed {@link WaveContent} into live, world-bound enemies - see
 * {@link WaveScriptTest} for the token-parsing behavior itself, which needs no GameWorld.
 */
class WaveTest {

    private static final long SEED = 1L; // arbitrary, fixed: only the scatter-specific tests below care what it is

    private final GameWorld context = new GameWorld(new RecordingGameHost());
    private final EnemyCatalog catalog = EnemyCatalog.builtIn();

    private Wave wave(int baseHealth, int basePrice, int level, String tokens) {
        return new Wave(this.context, baseHealth, basePrice, level, WaveScript.parse(tokens, this.catalog), SEED);
    }

    /**
     * A straight, horizontal path - so a lateral offset lands purely on Y, which is what the
     * shape tests below read.
     */
    private void setStraightHorizontalPath() {
        this.context.setBoard(BoardGeometry.of(32, 100, 100));
        this.context.setPath(new PathNormal(List.of(new Vec2(0, 50), new Vec2(200, 50))));
    }

    @Test
    void spawningTwiceProducesTwoIndependentSetsOfEnemies() {
        // spawn() is a factory, not an accessor: it binds fresh mobs to the path installed at
        // the moment it is called, which is what lets a level be published in one write.
        Wave built = wave(100, 5, 1, "c c");

        EnemyMob[] first = built.spawn();
        EnemyMob[] second = built.spawn();

        assertThat(first).hasSize(2);
        assertThat(second).hasSize(2);
        assertThat(first[0]).isNotSameAs(second[0]);
    }

    @Test
    void aSpacerOccupiesATimingSlotButSpawnsNoMob() {
        Wave built = wave(100, 5, 1, "c e c");

        assertThat(built.spawn()).hasSize(2);
        assertThat(built.enemyCount()).isEqualTo(2);
    }

    @Test
    void enemySetAndEnemyCountDelegateToTheParsedContent() {
        EnemyDefinition simple = this.catalog.get("c");
        EnemyDefinition armored = this.catalog.get("s");
        Wave built = wave(100, 5, 1, "2 c s");

        assertThat(built.enemySet()).containsExactlyInAnyOrder(simple, armored);
        assertThat(built.enemyCount(simple)).isEqualTo(2);
        assertThat(built.enemyCount(armored)).isEqualTo(1);
    }

    @Test
    void gettersReturnConstructorArguments() {
        Wave wave = new Wave(this.context, 251, 2, 3, new WaveContent(List.of()), SEED);

        assertThat(wave.getBaseHealth()).isEqualTo(251);
        assertThat(wave.getBasePrice()).isEqualTo(2);
        assertThat(wave.getLevel()).isEqualTo(3);
    }

    @Test
    void aBossSlotDoublesSizeHalvesSpeedAndDoublesBounty() {
        DefinedEnemyMob normal = (DefinedEnemyMob) wave(100, 10, 1, "c").spawn()[0];
        DefinedEnemyMob boss = (DefinedEnemyMob) wave(100, 10, 1, "boss c").spawn()[0];

        assertThat(boss.getBodyScale()).isEqualTo(normal.getBodyScale() * 2f);
        assertThat(boss.getSpeed()).isEqualTo(normal.getSpeed() * 0.5f);
        assertThat(boss.getBounty()).isEqualTo(normal.getBounty() * 2);
        assertThat(boss.getHealth()).isEqualTo(normal.getHealth());
    }

    @Test
    void anEliteSlotIncreasesSizeHealthAndBounty() {
        DefinedEnemyMob normal = (DefinedEnemyMob) wave(100, 10, 1, "c").spawn()[0];
        DefinedEnemyMob elite = (DefinedEnemyMob) wave(100, 10, 1, "elite c").spawn()[0];

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
        DefinedEnemyMob boss = (DefinedEnemyMob) wave(100, 10, 1, "boss c").spawn()[0];
        float speedBeforeHit = boss.getSpeed();

        boss.doDamage(Damage.physical(1));

        assertThat(boss.getSpeed()).isEqualTo(speedBeforeHit);
    }

    @Test
    void aSwarmSlotSplitsBountyExactlyAcrossItsMembersSummingToOneNormalSpawn() {
        EnemyMob normal = wave(90, 10, 1, "c").spawn()[0];
        EnemyMob[] swarm = wave(90, 10, 1, "swarm 3 c").spawn();

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
        EnemyMob[] separateSlots = wave(100, 5, 1, "3 c").spawn();
        EnemyMob[] column = wave(100, 5, 1, "column 3 c").spawn();

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
        EnemyMob[] separateSlots = wave(100, 5, 1, "3 c").spawn();
        EnemyMob[] drip = wave(100, 5, 1, "drip 3 c").spawn();

        assertThat(drip).hasSize(3);
        int[] separateTicks = activationTicks(separateSlots);
        int[] dripTicks = activationTicks(drip);

        assertThat(dripTicks[0]).isEqualTo(separateTicks[0]);
        assertThat(dripTicks[1]).isGreaterThan(separateTicks[1]);
        assertThat(dripTicks[2]).isGreaterThan(separateTicks[2]);
    }

    @Test
    void flankPlacesItsTwoMembersAtOppositeMaximumLateralOffsets() {
        setStraightHorizontalPath();

        EnemyMob[] flank = wave(100, 10, 1, "flank c").spawn();

        assertThat(flank).hasSize(2);
        double offset0 = flank[0].getY() - 50.0;
        double offset1 = flank[1].getY() - 50.0;
        assertThat(offset0).isNotEqualTo(0.0);
        assertThat(offset0).isCloseTo(-offset1, within(1e-9));
    }

    @Test
    void lineSpreadsItsMembersEvenlyWithTheMiddleOneOnThePathCentre() {
        setStraightHorizontalPath();

        EnemyMob[] line = wave(100, 10, 1, "line 3 c").spawn();

        assertThat(line).hasSize(3);
        double first = line[0].getY() - 50.0;
        double middle = line[1].getY() - 50.0;
        double last = line[2].getY() - 50.0;
        assertThat(middle).isCloseTo(0.0, within(1e-9));
        assertThat(first).isLessThan(middle);
        assertThat(middle).isLessThan(last);
    }

    @Test
    void swarmScatterIsReproducibleForTheSameWaveAndSlot() {
        setStraightHorizontalPath();
        WaveContent content = WaveScript.parse("swarm 5 c", this.catalog);

        EnemyMob[] first = new Wave(this.context, 100, 10, 1, content, 42L).spawn();
        EnemyMob[] second = new Wave(this.context, 100, 10, 1, content, 42L).spawn();

        for (int i = 0; i < first.length; i++) {
            assertThat(first[i].getX()).isEqualTo(second[i].getX());
            assertThat(first[i].getY()).isEqualTo(second[i].getY());
        }
    }

    @Test
    void swarmScatterDiffersForADifferentWaveSeed() {
        setStraightHorizontalPath();
        WaveContent content = WaveScript.parse("swarm 5 c", this.catalog);

        EnemyMob[] first = new Wave(this.context, 100, 10, 1, content, 1L).spawn();
        EnemyMob[] second = new Wave(this.context, 100, 10, 1, content, 2L).spawn();

        boolean anyDifferent = false;
        for (int i = 0; i < first.length; i++) {
            if (first[i].getX() != second[i].getX() || first[i].getY() != second[i].getY()) {
                anyDifferent = true;
            }
        }
        assertThat(anyDifferent).isTrue();
    }

    @Test
    void swarmMembersFillADiscAroundTheSpawnPointForEveryMemberCount() {
        setStraightHorizontalPath();
        double maxRadius = this.context.getBoard().scale() * PathCoverage.PATH_WIDTH_CELLS / 2.0 * 0.7;
        double spawnX = 0.0;
        double spawnY = 50.0;

        for (int n = 1; n <= SpawnShape.MAX_MEMBERS; n++) {
            EnemyMob[] swarm = wave(100, 10, 1, "swarm " + n + " c").spawn();

            assertThat(swarm).hasSize(n);
            for (EnemyMob member : swarm) {
                double dx = member.getX() - spawnX;
                double dy = member.getY() - spawnY;
                assertThat(Math.sqrt(dx * dx + dy * dy)).isLessThanOrEqualTo(maxRadius + 1e-9);
            }
        }
    }

    @Test
    void aShapedMembersOffsetFromTheCentrelineStaysConstantThroughAnUnroundedCorner() {
        // A hard, unsmoothed right-angle turn - PathBuilder joins consecutive corners with one
        // straight leg each, so this path's facing is genuinely discontinuous at (300, 100),
        // exactly the case that used to make an offset member's position jump sideways there.
        this.context.setBoard(BoardGeometry.of(32, 500, 500));
        this.context.setPath(new PathNormal(List.of(new Vec2(0, 100), new Vec2(300, 100), new Vec2(300, 400))));

        EnemyMob member = wave(100, 10, 1, "flank c").spawn()[0];
        EnemyMob centerline = wave(100, 10, 1, "c").spawn()[0];

        member.doTick(1);
        centerline.doTick(1);
        double expectedOffsetX = member.getX() - centerline.getX();
        double expectedOffsetY = member.getY() - centerline.getY();

        // 400 ticks at ~1.28px/tick covers well past the corner (at distance 300) while staying
        // short of the path's full 600px length, so this never crosses the wrap-to-start seam.
        for (int t = 2; t <= 400; t++) {
            member.doTick(t);
            centerline.doTick(t);

            assertThat(member.getX() - centerline.getX()).isCloseTo(expectedOffsetX, within(1e-6));
            assertThat(member.getY() - centerline.getY()).isCloseTo(expectedOffsetY, within(1e-6));
        }
    }

    @Test
    void aLateralOffsetNearTheBoardEdgeIsClampedRatherThanLeavingTheBoard() {
        this.context.setBoard(BoardGeometry.of(32, 100, 100));
        // hugging the board's top edge, so an unclamped upward offset would go negative
        this.context.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(200, 0))));

        EnemyMob[] flank = wave(100, 10, 1, "flank c").spawn();

        for (EnemyMob member : flank) {
            assertThat(member.getY()).isBetween(0.0, (double) this.context.getBoard().maxY());
        }
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
