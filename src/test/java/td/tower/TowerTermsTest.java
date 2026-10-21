package td.tower;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TowerTermsTest {

    @Test
    void everyTowerTermHasAUniqueKeyAndTitle() {
        Set<String> keys = new HashSet<>();
        Set<String> titles = new HashSet<>();
        for (TowerTerm term : TowerTerm.values()) {
            keys.add(term.key());
            titles.add(term.title());
            assertThat(term.describe().plain()).as(term.name()).isNotBlank().doesNotContain("{}");
        }

        assertThat(keys).hasSize(TowerTerm.values().length);
        assertThat(titles).hasSize(TowerTerm.values().length);
    }

    @Test
    void noTitleIsAlsoAnEffectsTitle() {
        Set<String> effectTitles = new HashSet<>();
        Arrays.stream(EffectKind.values()).forEach(kind -> effectTitles.add(kind.title()));

        assertThat(Arrays.stream(TowerTerm.values()).map(TowerTerm::title)).doesNotContainAnyElementsOf(effectTitles);
    }
}
