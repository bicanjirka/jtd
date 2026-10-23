package td.effect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EffectKindTest {

    @Test
    void aStackingKindAddsItsPerStackModifiersOncePerStack() {
        for (EffectKind kind : EffectKind.values()) {
            if (kind.maxStacks() < 2) {
                continue;
            }

            assertThat(kind.modifiersAt(0).isNone()).as(kind.name()).isTrue();
            assertThat(kind.modifiersAt(1)).as(kind.name()).isEqualTo(kind.rule().perStack());
            assertThat(kind.modifiersAt(2)).as(kind.name()).isEqualTo(kind.rule().perStack().times(2));
        }
    }

    @Test
    void aKindThatDoesNotStackCountsOnceHoweverOftenItIsAsked() {
        for (EffectKind kind : EffectKind.values()) {
            if (kind.maxStacks() > 0) {
                continue;
            }

            assertThat(kind.modifiersAt(5)).as(kind.name()).isEqualTo(kind.modifiersAt(1));
        }
    }
}
