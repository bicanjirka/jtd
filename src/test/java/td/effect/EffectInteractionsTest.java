package td.effect;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EffectInteractionsTest {

    private static final Set<EffectKind> NOTHING = EnumSet.noneOf(EffectKind.class);

    @Test
    void brittleCountsOnlyWhileAFreezeIsActive() {
        assertThat(EffectInteractions.contributes(EffectKind.BRITTLE, EnumSet.of(EffectKind.BRITTLE))).isFalse();
        assertThat(EffectInteractions.contributes(EffectKind.BRITTLE, EnumSet.of(EffectKind.BRITTLE, EffectKind.FREEZE)))
                .isTrue();
        assertThat(EffectInteractions.contributes(EffectKind.VULNERABLE, NOTHING)).isTrue();
    }

    @Test
    void aRevealedEnemyCountsAsExposed() {
        assertThat(EffectInteractions.isActiveOrImplied(EffectKind.EXPOSED, EnumSet.of(EffectKind.REVEALED))).isTrue();
        assertThat(EffectInteractions.isActiveOrImplied(EffectKind.EXPOSED, EnumSet.of(EffectKind.MARKED))).isFalse();
    }

    @Test
    void eachPoolEarnsItsLastingMarks() {
        assertThat(EffectInteractions.earnedBy(EffectKind.BURN)).containsExactly(EffectKind.SCORCHED);
        assertThat(EffectInteractions.earnedBy(EffectKind.POISON)).containsExactly(EffectKind.SICKENED);
        assertThat(EffectInteractions.earnedBy(EffectKind.SOULFIRE)).containsExactly(EffectKind.SCORCHED, EffectKind.SICKENED);
        assertThat(EffectInteractions.earnedBy(EffectKind.CHILL)).isEmpty();
    }

    @Test
    void anUndertowCountsAsAQuarterChillForAFreeze() {
        assertThat(EffectInteractions.freezeLengthFactor(EnumSet.of(EffectKind.UNDERTOW), 0f)).isCloseTo(1.25f, within(1e-6f));
        assertThat(EffectInteractions.freezeLengthFactor(NOTHING, 0.4f)).isCloseTo(1.4f, within(1e-6f));
    }

    @Test
    void rimeDoublesWhatAChillBuysAFreeze() {
        assertThat(EffectInteractions.freezeLengthFactor(EnumSet.of(EffectKind.RIME), 0.4f)).isCloseTo(1.8f, within(1e-6f));
    }

    @Test
    void tarMakesAFreezeLastASecondLonger() {
        assertThat(EffectInteractions.freezeExtraTicks(EnumSet.of(EffectKind.TARRED))).isEqualTo(20);
        assertThat(EffectInteractions.freezeExtraTicks(NOTHING)).isZero();
    }

    @Test
    void tarMakesAFreshFireStartAtDouble() {
        assertThat(EffectInteractions.fireStartFactor(EnumSet.of(EffectKind.TARRED))).isEqualTo(2f);
        assertThat(EffectInteractions.fireStartFactor(NOTHING)).isEqualTo(1f);
    }

    @Test
    void underRimeAFreezeLandsAllOfTheBurnElseAtLeastHalf() {
        assertThat(EffectInteractions.freezeBurstShare(EnumSet.of(EffectKind.RIME), 0.5f)).isEqualTo(1f);
        assertThat(EffectInteractions.freezeBurstShare(NOTHING, 0.2f)).isEqualTo(0.5f);
        assertThat(EffectInteractions.freezeBurstShare(NOTHING, 1.5f)).isEqualTo(1.5f);
    }

    @Test
    void anAshenEnemyTakesAQuarterOfAChill() {
        assertThat(EffectInteractions.chillTaken(EnumSet.of(EffectKind.ASH))).isEqualTo(0.25f);
        assertThat(EffectInteractions.chillTaken(NOTHING)).isEqualTo(1f);
    }

    @Test
    void underAshAPoolHoldsTwiceAsMuch() {
        assertThat(EffectInteractions.poolFactor(EnumSet.of(EffectKind.ASH))).isEqualTo(2);
        assertThat(EffectInteractions.poolFactor(NOTHING)).isEqualTo(1);
    }
}
