package td.wave;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageMix;
import td.enemy.BodyArchetype;
import td.enemy.EnemyDefinition;
import td.enemy.IdentifiedTrait;
import td.enemy.PercentResistTrait;
import td.enemy.Trait;
import td.enemy.TraitContext;
import td.enemy.TraitTemplate;
import td.util.GameStartupException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void armoredIsOneMemberWithNoMultiplierChangeButATraitOverride() {
        SpawnShape shape = SpawnShape.armored();

        assertThat(shape.members()).isEqualTo(1);
        assertThat(shape.sizeMultiplier()).isEqualTo(1f);
        assertThat(shape.speedMultiplier()).isEqualTo(1f);
        assertThat(shape.healthMultiplier()).isEqualTo(1f);
        assertThat(shape.bountyMultiplier()).isEqualTo(1f);
        assertThat(shape.traitOverride()).isPresent();
    }

    @Test
    void armoredStacksWithAnArmorTraitTheEnemyAlreadyHas() {
        EnemyDefinition plated = EnemyDefinition.of("plated", "Plated", 10, 1, 1f, BodyArchetype.SQUARE)
                .withIdentifiedTraits(List.of(IdentifiedTrait.named("armor", new PercentResistTrait(0.5f))));

        EnemyDefinition armored = plated.withAdditionalTraits(List.of(SpawnShape.armored().traitOverride().orElseThrow()));

        assertThat(armored.traitsFor(DamageMix.none())).hasSize(2);
    }

    @Test
    void armoredsTraitBlocksAFifthToFourFifthsOfPhysicalDamageByPhysicalShareAndNoMagic() {
        TraitTemplate plating = SpawnShape.armored().traitOverride().orElseThrow().template();
        Trait fresh = plating.resolvedFor(DamageMix.none()).orElseThrow();
        Trait allPhysical = plating.resolvedFor(DamageMix.of(Damage.physical(10))).orElseThrow();
        TraitContext traitContext = new TraitContext(1f);

        assertThat(fresh.onHit(Damage.physical(100), traitContext)).isEqualTo(Damage.physical(80));
        assertThat(allPhysical.onHit(Damage.physical(100), traitContext)).isEqualTo(Damage.physical(20));
        assertThat(allPhysical.onHit(Damage.magic(100), traitContext)).isEqualTo(Damage.magic(100));
    }

    @Test
    void everyOtherShapeHasNoTraitOverride() {
        assertThat(SpawnShape.normal().traitOverride()).isEmpty();
        assertThat(SpawnShape.swarm(2).traitOverride()).isEmpty();
        assertThat(SpawnShape.line(2).traitOverride()).isEmpty();
        assertThat(SpawnShape.flank().traitOverride()).isEmpty();
        assertThat(SpawnShape.column(2).traitOverride()).isEmpty();
        assertThat(SpawnShape.drip(2).traitOverride()).isEmpty();
    }

    @Test
    void swarmSplitsSizeAndHealthAcrossItsMembersButKeepsFullBounty() {
        SpawnShape shape = SpawnShape.swarm(4);

        assertThat(shape.members()).isEqualTo(4);
        assertThat(shape.sizeMultiplier()).isEqualTo(0.5f);
        assertThat(shape.healthMultiplier()).isEqualTo(0.25f);
        assertThat(shape.bountyMultiplier()).isEqualTo(1f);
        assertThat(shape.spread()).isEqualTo(SpawnSpread.SCATTERED);
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
    void bountySharesSumToExactlyOneNormalSpawnsBountyEvenWhenItDoesNotDivideEvenly() {
        int[] shares = SpawnShape.swarm(3).bountyShares(10);

        assertThat(shares).hasSize(3);
        assertThat(shares).containsExactly(4, 3, 3);
        assertThat(sum(shares)).isEqualTo(10);
    }

    @Test
    void bountySharesReflectTheShapesBountyMultiplier() {
        // no built-in shape has a non-1 bountyMultiplier, so this uses the canonical constructor
        SpawnShape doubledBounty = new SpawnShape(1, 1f, 1f, 1f, 2.0f, Optional.empty(), SpawnSpread.NONE, 0.0);

        int[] shares = doubledBounty.bountyShares(10);

        assertThat(shares).containsExactly(20);
    }

    @Test
    void bountySharesSumExactlyAcrossEveryMemberCountUpToTheMaximum() {
        for (int members = 1; members <= SpawnShape.MAX_MEMBERS; members++) {
            int[] shares = SpawnShape.swarm(members).bountyShares(37);
            assertThat(sum(shares)).isEqualTo(37);
        }
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }
}
