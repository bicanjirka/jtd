package td.effect;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class EffectInteractionsTest {

    @Test
    void eachPoolEarnsItsLastingMarks() {
        assertThat(EffectInteractions.earnedBy(EffectKind.BURN)).containsExactly(EffectKind.SCORCHED);
        assertThat(EffectInteractions.earnedBy(EffectKind.POISON)).containsExactly(EffectKind.SICKENED);
        assertThat(EffectInteractions.earnedBy(EffectKind.SOULFIRE)).containsExactly(EffectKind.SCORCHED, EffectKind.SICKENED);
        assertThat(EffectInteractions.earnedBy(EffectKind.CHILL)).isEmpty();
    }

    @Test
    void aRevealIsWhatKeepsInvisibilityOut() {
        assertThat(EffectInteractions.keptOutBy(EffectKind.INVISIBLE, EnumSet.of(EffectKind.REVEALED))).contains(EffectKind.REVEALED);
        assertThat(EffectInteractions.keptOutBy(EffectKind.INVISIBLE, EnumSet.of(EffectKind.INVERSION))).isEmpty();
        assertThat(EffectInteractions.blockedBy(EnumSet.of(EffectKind.REVEALED))).containsExactly(EffectKind.INVISIBLE);
    }
}
