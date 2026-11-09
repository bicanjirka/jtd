package td.wave;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.enemy.Affix;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class WaveTest {

    private static final long SEED = 1L; // arbitrary, fixed: only the scatter-specific tests below care what it is

    private final GameWorld context = WorldFixtures.newWorld();
    private final EnemyCatalog catalog = EnemyCatalog.builtIn();

    private Wave wave(String tokens) {
        return new Wave(this.context, WaveScript.parse(tokens, Rank.GRUNT, this.catalog), SEED);
    }

    /** Horizontal, so a lateral offset lands purely on y. */
    private void setStraightHorizontalPath() {
        this.context.setBoard(BoardGeometry.of(32, 100, 100));
        this.context.setPath(new Path(List.of(new Vec2(0, 50), new Vec2(200, 50))));
    }

    @Test
    void spawningTwiceProducesTwoIndependentSetsOfEnemies() {
        Wave built = wave("walker walker");

        EnemyMob[] first = built.spawn();
        EnemyMob[] second = built.spawn();

        assertThat(first).hasSize(2);
        assertThat(second).hasSize(2);
        assertThat(first[0]).isNotSameAs(second[0]);
    }

    @Test
    void aSpacerOccupiesATimingSlotButSpawnsNoMob() {
        Wave built = wave("walker e walker");

        assertThat(built.spawn()).hasSize(2);
        assertThat(built.enemyCount()).isEqualTo(2);
    }

    @Test
    void enemySetAndEnemyCountDelegateToTheParsedContent() {
        EnemyDefinition simple = this.catalog.get("walker");
        EnemyDefinition armored = this.catalog.get("knight");
        Wave built = wave("2 walker knight");

        assertThat(built.enemySet()).containsExactlyInAnyOrder(simple, armored);
        assertThat(built.enemyCount(simple)).isEqualTo(2);
        assertThat(built.enemyCount(armored)).isEqualTo(1);
    }

    @Test
    void aWavesBountyIsWhatItsSpawnedEnemiesPayInEveryShape() {
        Wave wave = wave("2 walker e elite swarm 5 walker e column 3 knight");

        int spawned = Arrays.stream(wave.spawn()).mapToInt(EnemyMob::getBounty).sum();

        assertThat(wave.bounty()).isEqualTo(spawned);
    }

    @Test
    void gettersReturnConstructorArguments() {
        Wave wave = new Wave(this.context, new WaveContent(List.of()), SEED, 2, 1.5f);

        assertThat(wave.getPathIndex()).isEqualTo(2);
    }

    @Test
    void anAffixThatGrowsTheBodyGrowsEveryMemberOfTheSlot() {
        EnemyMob plain = wave("swarm 2 walker").spawn()[0];
        EnemyMob hulking = wave("swarm 2 hulking walker").spawn()[0];

        assertThat(hulking.getBodyScale()).isGreaterThan(plain.getBodyScale());
    }

    @Test
    void anAffixedSlotSpawnsItsAffixedDefinition() {
        DefinedEnemyMob affixed = (DefinedEnemyMob) wave("armored walker").spawn()[0];

        assertThat(affixed.definition().affixes()).containsExactly(Affix.ARMORED);
    }

    @Test
    void aSwarmSlotSplitsBountyExactlyAcrossItsMembersSummingToOneNormalSpawn() {
        EnemyMob normal = wave("walker").spawn()[0];
        EnemyMob[] swarm = wave("swarm 3 walker").spawn();

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
        EnemyMob[] separateSlots = wave("3 walker").spawn();
        EnemyMob[] column = wave("column 3 walker").spawn();

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
        EnemyMob[] separateSlots = wave("3 walker").spawn();
        EnemyMob[] drip = wave("drip 3 walker").spawn();

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

        EnemyMob[] flank = wave("flank walker").spawn();

        assertThat(flank).hasSize(2);
        double offset0 = flank[0].getY() - 50.0;
        double offset1 = flank[1].getY() - 50.0;
        assertThat(offset0).isNotEqualTo(0.0);
        assertThat(offset0).isCloseTo(-offset1, within(1e-9));
    }

    @Test
    void lineSpreadsItsMembersEvenlyWithTheMiddleOneOnThePathCentre() {
        setStraightHorizontalPath();

        EnemyMob[] line = wave("line 3 walker").spawn();

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
        WaveContent content = WaveScript.parse("swarm 5 walker", Rank.GRUNT, this.catalog);

        EnemyMob[] first = new Wave(this.context, content, 42L).spawn();
        EnemyMob[] second = new Wave(this.context, content, 42L).spawn();

        for (int i = 0; i < first.length; i++) {
            assertThat(first[i].getX()).isEqualTo(second[i].getX());
            assertThat(first[i].getY()).isEqualTo(second[i].getY());
        }
    }

    @Test
    void swarmScatterDiffersForADifferentWaveSeed() {
        setStraightHorizontalPath();
        WaveContent content = WaveScript.parse("swarm 5 walker", Rank.GRUNT, this.catalog);

        EnemyMob[] first = new Wave(this.context, content, 1L).spawn();
        EnemyMob[] second = new Wave(this.context, content, 2L).spawn();

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
            EnemyMob[] swarm = wave("swarm " + n + " walker").spawn();

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
        // a hard right-angle turn: facing is discontinuous at the corner
        this.context.setBoard(BoardGeometry.of(32, 500, 500));
        this.context.setPath(new Path(List.of(new Vec2(0, 100), new Vec2(300, 100), new Vec2(300, 400))));

        EnemyMob member = wave("flank walker").spawn()[0];
        EnemyMob centerline = wave("walker").spawn()[0];

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
        this.context.setPath(new Path(List.of(new Vec2(0, 0), new Vec2(200, 0))));

        EnemyMob[] flank = wave("flank walker").spawn();

        for (EnemyMob member : flank) {
            assertThat(member.getY()).isBetween(0.0, (double) this.context.getBoard().maxY());
        }
    }

    @Test
    void aLeadingSpacingTokenScalesTheDelayBetweenSlots() {
        this.setStraightHorizontalPath();

        int[] normal = activationTicks(wave("walker walker").spawn());
        int[] doubled = activationTicks(wave("w44.8: walker walker").spawn());

        assertThat(normal[0]).isZero();
        assertThat(normal[1]).isGreaterThan(0);
        assertThat(doubled[1]).isCloseTo(normal[1] * 2, within(1));
    }

    /**
     * The tick each mob becomes targetable (0 if already), measuring the private spawn delay from
     * outside.
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
