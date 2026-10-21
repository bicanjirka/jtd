package td.ui.render;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;

import static org.assertj.core.api.Assertions.assertThat;

class TooltipKeyTest {

    @Test
    void keysOfOneTermShareTheirSubjectWhateverTheirKind() {
        TooltipKey effect = TooltipKey.ofTerm("effect", EffectKind.CHILL);
        TooltipKey mention = TooltipKey.ofTerm("term", EffectKind.CHILL);
        TooltipKey trait = TooltipKey.ofTerm("trait", 2, EffectKind.CHILL);

        assertThat(effect.sameSubject(mention)).isTrue();
        assertThat(effect.sameSubject(trait)).isTrue();
        assertThat(effect.sameSubject(TooltipKey.ofTerm("effect", EffectKind.BURN))).isFalse();
        assertThat(effect).isNotEqualTo(mention);
    }

    @Test
    void keysWithoutATermShareTheirSubjectOnlyByTheirId() {
        TooltipKey armor = TooltipKey.of("stat", "ARMOR");

        assertThat(armor.sameSubject(TooltipKey.of("stat", "ARMOR"))).isTrue();
        assertThat(armor.sameSubject(TooltipKey.of("stat", "SPEED"))).isFalse();
        assertThat(armor.sameSubject(TooltipKey.ofTerm("effect", EffectKind.CHILL))).isFalse();
    }
}
