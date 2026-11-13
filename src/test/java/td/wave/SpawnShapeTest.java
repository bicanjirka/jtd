package td.wave;

import org.junit.jupiter.api.Test;
import td.util.GameStartupException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class SpawnShapeTest {

    @Test
    void normalIsOneMemberWithEveryMultiplierAtOne() {
        SpawnShape shape = SpawnShape.normal();

        assertThat(shape.members()).isEqualTo(1);
        assertThat(shape.sizeMultiplier()).isEqualTo(1f);
        assertThat(shape.speedMultiplier()).isEqualTo(1f);
        assertThat(shape.healthMultiplier()).isEqualTo(1f);
        assertThat(shape.bountyMultiplier()).isEqualTo(1f);
        assertThat(shape.spread()).isEqualTo(SpawnSpread.NONE);
        assertThat(shape.delaySpacingSlots()).isZero();
    }

    @Test
    void aSwarmOfOneIsOneWholeEnemyScattered() {
        SpawnShape shape = SpawnShape.swarm(1);

        assertThat(shape.members()).isEqualTo(1);
        assertThat(shape.sizeMultiplier()).isEqualTo(1f);
        assertThat(shape.healthMultiplier()).isEqualTo(1f);
        assertThat(shape.bountyMultiplier()).isEqualTo(1f);
        assertThat(shape.spread()).isEqualTo(SpawnSpread.SCATTERED);
    }

    @Test
    void aBiggerSwarmHasWeakerMembersButMoreHealthInAll() {
        for (int members = 2; members <= SpawnShape.MAX_MEMBERS; members++) {
            SpawnShape smaller = SpawnShape.swarm(members - 1);
            SpawnShape bigger = SpawnShape.swarm(members);

            assertThat(bigger.healthMultiplier()).isLessThan(smaller.healthMultiplier());
            assertThat(bigger.healthMultiplier() * members).isGreaterThan(smaller.healthMultiplier() * (members - 1));
        }
    }

    @Test
    void aSwarmMembersBodyCoversAsMuchAsItsShareOfHealthAndNeverLessThanHalfItsSize() {
        for (int members = 1; members <= SpawnShape.MAX_MEMBERS; members++) {
            SpawnShape shape = SpawnShape.swarm(members);
            float size = shape.sizeMultiplier();

            assertThat(size).isGreaterThanOrEqualTo(0.5f);
            assertThat(size * size).isCloseTo(shape.healthMultiplier(), within(1e-4f));
        }
    }

    @Test
    void lineSpreadsMembersEvenlyWithNoMultipliers() {
        SpawnShape shape = SpawnShape.line(3);

        assertThat(shape.members()).isEqualTo(3);
        assertThat(shape.spread()).isEqualTo(SpawnSpread.EVEN);
    }

    @Test
    void flankIsAlwaysExactlyTwoMembersAtOppositeEdges() {
        SpawnShape shape = SpawnShape.flank();

        assertThat(shape.members()).isEqualTo(2);
        assertThat(shape.spread()).isEqualTo(SpawnSpread.EDGES);
    }

    @Test
    void columnPacksMembersTighterThanOneSlotApart() {
        SpawnShape shape = SpawnShape.column(3);

        assertThat(shape.members()).isEqualTo(3);
        assertThat(shape.delaySpacingSlots()).isLessThan(1.0);
        assertThat(shape.spread()).isEqualTo(SpawnSpread.NONE);
    }

    @Test
    void dripSpacesMembersLooserThanOneSlotApart() {
        SpawnShape shape = SpawnShape.drip(3);

        assertThat(shape.members()).isEqualTo(3);
        assertThat(shape.delaySpacingSlots()).isGreaterThan(1.0);
    }

    @Test
    void aMemberCountAboveTheMaximumFailsToConstruct() {
        assertThatThrownBy(() -> SpawnShape.swarm(SpawnShape.MAX_MEMBERS + 1))
                .isInstanceOf(GameStartupException.class)
                .hasMessageContaining(String.valueOf(SpawnShape.MAX_MEMBERS));
    }

    @Test
    void aZeroMemberCountFailsToConstruct() {
        assertThatThrownBy(() -> SpawnShape.line(0)).isInstanceOf(GameStartupException.class);
    }

    @Test
    void bountySharesSumToExactlyTheHealthSentEvenWhenItDoesNotDivideEvenly() {
        int[] shares = sharingOneSpawnsHealth(3).bountyShares(10);

        assertThat(shares).hasSize(3);
        assertThat(shares).containsExactly(4, 3, 3);
        assertThat(sum(shares)).isEqualTo(10);
    }

    @Test
    void bountySharesReflectTheShapesBountyMultiplier() {
        // no built-in shape has a non-1 bountyMultiplier, so this uses the canonical constructor
        SpawnShape doubledBounty = new SpawnShape(1, 1f, 1f, 1f, 2.0f, SpawnSpread.NONE, 0.0);

        int[] shares = doubledBounty.bountyShares(10);

        assertThat(shares).containsExactly(20);
    }

    @Test
    void bountySharesSumExactlyAcrossEveryMemberCountUpToTheMaximum() {
        for (int members = 1; members <= SpawnShape.MAX_MEMBERS; members++) {
            int[] shares = sharingOneSpawnsHealth(members).bountyShares(37);
            assertThat(sum(shares)).isEqualTo(37);
        }
    }

    private static SpawnShape sharingOneSpawnsHealth(int members) {
        return new SpawnShape(members, 1f, 1f, 1f / members, 1f, SpawnSpread.NONE, 0.0);
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }
}
