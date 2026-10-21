package td.enemy;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AbilityTextTest {

    private static final float CELL = 40f;

    private final EnemyCatalog catalog = EnemyCatalog.builtIn();
    private final AbilityText.Words words = new AbilityText.Words(CELL, id -> this.catalog.get(id).displayName());

    private List<Ability> abilitiesOf(AbilityTerm term) {
        return this.catalog.ids().stream().flatMap(id -> java.util.Arrays.stream(Rank.values())
                .flatMap(rank -> this.catalog.get(id, rank).abilities().stream()))
                .filter(ability -> ability.term() == term).distinct().toList();
    }

    @Test
    void aThresholdShieldSaysWhenWhatHowMuchAndAgainstWhat() {
        List<Ability> braces = abilitiesOf(AbilityTerm.BRACE);

        List<String> texts = braces.stream().map(brace -> AbilityText.of(brace, this.words)).map(text -> text.plain()).toList();
        assertThat(texts).contains("once, below 50% health: gains Shield of 300 against physical for 5 s");
        assertThat(AbilityText.of(braces.getFirst(), this.words).mentions()).containsExactly(EffectKind.SHIELD);
    }

    @Test
    void anAreaHealSaysHowFarItReachesInCells() {
        Ability mend = abilitiesOf(AbilityTerm.MEND).getFirst();

        assertThat(AbilityText.of(mend, this.words).plain()).startsWith("every ").contains("every other enemy within 2 cells Heal of");
    }

    @Test
    void aSpawnNamesWhatItCalls() {
        Ability brood = abilitiesOf(AbilityTerm.BROOD).getFirst();

        assertThat(AbilityText.of(brood, this.words).plain()).isEqualTo("when it dies: calls 3 Mite Broodling");
    }

    @Test
    void everyBuiltInAbilityHasATerm() {
        for (String id : this.catalog.ids()) {
            for (Rank rank : Rank.values()) {
                for (Ability ability : this.catalog.get(id, rank).abilities()) {
                    assertThat(ability.term()).as(id + " " + rank).isNotNull();
                }
            }
        }
    }
}
