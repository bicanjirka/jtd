package td.effect;

import org.junit.jupiter.api.Test;
import td.stat.EnemyStat;
import td.term.TermNumbers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EffectTermsTest {

    @Test
    void everyKindHasAUniqueKeyAndTitle() {
        Set<String> keys = new HashSet<>();
        Set<String> titles = new HashSet<>();
        for (EffectKind kind : EffectKind.values()) {
            keys.add(kind.key());
            titles.add(kind.title());
        }

        assertThat(keys).hasSize(EffectKind.values().length);
        assertThat(titles).hasSize(EffectKind.values().length);
    }

    @Test
    void everyKindHasASentenceWithItsSlotsFilled() {
        for (EffectKind kind : EffectKind.values()) {
            assertThat(kind.describe().plain()).as(kind.name()).isNotBlank().doesNotContain("{}");
        }
    }

    @Test
    void aStackingKindsSentenceQuotesItsOwnRule() {
        EffectKind kind = EffectKind.VULNERABLE;
        float perStack = kind.rule().perStack().get(EnemyStat.PHYSICAL_DAMAGE_TAKEN).flat();

        assertThat(kind.describe().plain()).contains(TermNumbers.signedPercent(perStack))
                .contains(kind.maxStacks() + " stacks");
    }

    @Test
    void sympathyNamesEveryKindItShares() {
        List<EffectKind> shared = Arrays.stream(EffectKind.values()).filter(EffectKind::sharedBySympathy).toList();

        assertThat(EffectKind.SYMPATHY.describe().mentions()).containsExactlyElementsOf(shared);
    }
}
