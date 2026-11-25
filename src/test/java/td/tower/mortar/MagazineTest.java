package td.tower.mortar;

import org.junit.jupiter.api.Test;
import td.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MagazineTest {

    /** Every roll the same, just under 1: the draw takes the last kind in the weights' order. */
    private static final RandomSource LAST_KIND = () -> 0.999;

    private static Magazine allNormal() {
        return new Magazine(Collections.nCopies(Magazine.SIZE, ShellKind.NORMAL));
    }

    @Test
    void aDrawPicksEachKindItOwnsByItsWeightOverTheSum() {
        MagazineSpec spec = MagazineSpec.standard().withWeight(ShellKind.TAR, 5);
        int total = spec.weightOf(ShellKind.NORMAL) + spec.weightOf(ShellKind.HEAVY) + 5;

        Map<ShellKind, Float> chances = spec.chances();

        assertThat(chances.get(ShellKind.TAR)).isCloseTo(5f / total, within(1e-3f));
        assertThat(chances.get(ShellKind.HEAVY)).isCloseTo((float) spec.weightOf(ShellKind.HEAVY) / total,
                within(1e-3f));
        assertThat(chances.get(ShellKind.NAPALM)).isZero();
    }

    @Test
    void aNormalHeldRightBehindAPayloadCarriesItAndTheFrontOneIsBehindTheShellFiredLast() {
        Magazine magazine = new Magazine(List.of(ShellKind.NORMAL, ShellKind.TAR, ShellKind.NORMAL, ShellKind.NORMAL,
                ShellKind.HEAVY, ShellKind.NORMAL), Optional.of(ShellKind.NAPALM));

        List<Optional<ShellKind>> carried = magazine.held().stream().map(HeldShell::carried).toList();

        assertThat(carried).containsExactly(Optional.of(ShellKind.NAPALM), Optional.empty(), Optional.of(ShellKind.TAR),
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Test
    void firingRemembersTheLastShellItFired() {
        Magazine magazine = new Magazine(List.of(ShellKind.NORMAL, ShellKind.CRYO, ShellKind.NORMAL, ShellKind.NORMAL,
                ShellKind.NORMAL, ShellKind.NORMAL, ShellKind.NORMAL, ShellKind.NORMAL, ShellKind.NORMAL, ShellKind.NORMAL));
        MagazineSpec cryo = MagazineSpec.standard().withWeight(ShellKind.CRYO, 1);

        Magazine fired = magazine.fired(2, cryo, LAST_KIND);

        assertThat(fired.lastFired()).hasValue(ShellKind.CRYO);
        assertThat(fired.held().getFirst().carried()).hasValue(ShellKind.CRYO);
    }

    @Test
    void firingTakesTheFrontAndDrawsTheBackFull() {
        Magazine magazine = allNormal();

        Magazine fired = magazine.fired(3, MagazineSpec.standard(), LAST_KIND);

        assertThat(fired.shells()).hasSize(Magazine.SIZE)
                .endsWith(ShellKind.HEAVY, ShellKind.HEAVY, ShellKind.HEAVY)
                .startsWith(Collections.nCopies(Magazine.SIZE - 3, ShellKind.NORMAL).toArray(ShellKind[]::new));
    }

    @Test
    void withFusionAHeavyDrawnBehindAHeavyFusesWithItIntoANuke() {
        MagazineSpec fusing = MagazineSpec.standard().fusing();

        Magazine fired = allNormal().fired(2, fusing, LAST_KIND);

        assertThat(fired.shells()).endsWith(ShellKind.NUKE, ShellKind.HEAVY);
    }

    @Test
    void aHeavyDrawnBehindANukeStaysAHeavy() {
        MagazineSpec fusing = MagazineSpec.standard().fusing();

        Magazine fired = allNormal().fired(3, fusing, LAST_KIND);

        assertThat(fired.shells()).endsWith(ShellKind.NORMAL, ShellKind.NUKE, ShellKind.NUKE, ShellKind.HEAVY);
    }

    @Test
    void withRichMixANewShellIsNeverTheKindInFrontOfIt() {
        MagazineSpec mixing = MagazineSpec.standard().withWeight(ShellKind.NAPALM, 3).mixing();
        RandomSource seeded = RandomSource.seeded(7);
        Magazine magazine = allNormal();
        List<ShellKind> drawn = new ArrayList<>();

        for (int i = 0; i < 40; i++) {
            magazine = magazine.fired(1, mixing, seeded);
            drawn.add(magazine.shells().getLast());
        }

        for (int i = 1; i < drawn.size(); i++) {
            assertThat(drawn.get(i)).isNotEqualTo(drawn.get(i - 1));
        }
    }

    @Test
    void withRichMixAndOnlyTwoKindsTheyComeUpHalfAndHalfWhateverTheirWeights() {
        MagazineSpec mixing = MagazineSpec.standard().mixing();

        Map<ShellKind, Float> chances = mixing.chances();

        assertThat(chances.get(ShellKind.NORMAL)).isCloseTo(0.5f, within(1e-3f));
        assertThat(chances.get(ShellKind.HEAVY)).isCloseTo(0.5f, within(1e-3f));
    }

    @Test
    void withFusionTheOddsCountANukeAsOneShellInPlaceOfTwoHeavies() {
        MagazineSpec fusing = MagazineSpec.standard().withWeight(ShellKind.HEAVY, 3).fusing();

        Map<ShellKind, Float> chances = fusing.chances();

        assertThat(chances.get(ShellKind.NUKE)).isPositive();
        assertThat(chances.values().stream().mapToDouble(Float::doubleValue).sum()).isCloseTo(1.0, within(1e-3));
        assertThat(chances.get(ShellKind.HEAVY))
                .isLessThan((float) fusing.weightOf(ShellKind.HEAVY) / (fusing.weightOf(ShellKind.HEAVY)
                        + fusing.weightOf(ShellKind.NORMAL)));
    }

    @Test
    void aKindNoLongerOwnedLeavesAndTheShellsBehindMoveUp() {
        Magazine magazine = new Magazine(List.of(ShellKind.TAR, ShellKind.HEAVY, ShellKind.NORMAL, ShellKind.TAR,
                ShellKind.NORMAL, ShellKind.NORMAL, ShellKind.NORMAL, ShellKind.NORMAL, ShellKind.NORMAL,
                ShellKind.NORMAL));

        Magazine kept = magazine.keptTo(MagazineSpec.standard(), LAST_KIND);

        assertThat(kept.shells()).hasSize(Magazine.SIZE).doesNotContain(ShellKind.TAR)
                .startsWith(ShellKind.HEAVY, ShellKind.NORMAL, ShellKind.NORMAL);
    }

    @Test
    void aMagazineOfOwnedKindsIsKeptAsItIs() {
        Magazine magazine = Magazine.opener();

        assertThat(magazine.keptTo(MagazineSpec.standard(), LAST_KIND)).isSameAs(magazine);
    }

    @Test
    void twoSpecsThatDrawAlikeAreEqual() {
        MagazineSpec once = MagazineSpec.standard().withWeight(ShellKind.TAR, 3).fusing();
        MagazineSpec again = MagazineSpec.standard().withWeight(ShellKind.TAR, 3).fusing();

        assertThat(once).isEqualTo(again).hasSameHashCodeAs(again).isNotEqualTo(again.mixing());
    }
}
