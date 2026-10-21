package td.ui;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.ui.render.TooltipKey;

import static org.assertj.core.api.Assertions.assertThat;

class TooltipChainTest {

    private static final TooltipKey FREEZE = TooltipKey.ofTerm("effect", EffectKind.FREEZE);
    private static final TooltipKey CHILL = TooltipKey.ofTerm("term", EffectKind.CHILL);
    private static final TooltipKey BURN = TooltipKey.ofTerm("term", EffectKind.BURN);

    @Test
    void aKeyOpensAtTheDepthOfItsSource() {
        TooltipChain chain = TooltipChain.empty().open(0, FREEZE).orElseThrow();

        assertThat(chain.keys()).containsExactly(FREEZE);
        assertThat(chain.open(1, CHILL).orElseThrow().keys()).containsExactly(FREEZE, CHILL);
    }

    @Test
    void openingFromAShallowerSourceClosesTheDeeperOnes() {
        TooltipChain chain = TooltipChain.empty().open(0, FREEZE).orElseThrow().open(1, CHILL).orElseThrow();

        TooltipChain next = chain.open(0, BURN).orElseThrow();

        assertThat(next.keys()).containsExactly(BURN);
    }

    @Test
    void aKeyAlreadyOpenShallowerIsRefused() {
        TooltipChain chain = TooltipChain.empty().open(0, CHILL).orElseThrow();

        assertThat(chain.open(1, CHILL)).isEmpty();
    }

    @Test
    void theSameKeyAtTheSameDepthIsRefused() {
        TooltipChain chain = TooltipChain.empty().open(0, FREEZE).orElseThrow();

        assertThat(chain.open(0, FREEZE)).isEmpty();
    }

    @Test
    void aTermOpenShallowerUnderAnotherKindOfKeyIsRefused() {
        TooltipChain chain = TooltipChain.empty().open(0, FREEZE).orElseThrow().open(1, CHILL).orElseThrow();

        assertThat(chain.open(2, TooltipKey.ofTerm("term", EffectKind.FREEZE))).isEmpty();
    }

    @Test
    void aSourceMaySwapItsTooltipForAnotherKeyOfTheSameTerm() {
        TooltipChain chain = TooltipChain.empty().open(0, CHILL).orElseThrow();

        assertThat(chain.open(0, TooltipKey.ofTerm("effect", EffectKind.CHILL))).isPresent();
    }

    @Test
    void closedFromKeepsOnlyTheShallowerOnes() {
        TooltipChain chain = TooltipChain.empty().open(0, FREEZE).orElseThrow().open(1, CHILL).orElseThrow();

        assertThat(chain.closedFrom(1).keys()).containsExactly(FREEZE);
        assertThat(chain.closedFrom(0).keys()).isEmpty();
        assertThat(chain.closedFrom(5).keys()).containsExactly(FREEZE, CHILL);
    }
}
