package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.DamageMix;
import td.fixtures.EnemyFixtures;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AffixTest {

    private static final AffixContext WHOLE = new AffixContext(Rank.GRUNT, 1f);

    @Test
    void anAffixsTraitStacksWithANamedTraitTheEnemyAlreadyHas() {
        EnemyDefinition named = EnemyFixtures.simpleDefinition("grub")
                .withIdentifiedTraits(List.of(IdentifiedTrait.named("armor", new RegenerationTrait(1f))));

        EnemyDefinition affixed = named.withAffixes(List.of(Affix.ARMORED), WHOLE);

        assertThat(affixed.traitsFor(DamageMix.none())).hasSize(2);
    }

    @Test
    void aDefinitionRecordsItsAffixesAndItsTitleNamesThemBeforeTheNoun() {
        EnemyDefinition plain = EnemyFixtures.simpleDefinition("grub").withDisplayName("Grub");

        EnemyDefinition affixed = plain.withAffixes(List.of(Affix.ARMORED), WHOLE);

        assertThat(affixed.affixes()).containsExactly(Affix.ARMORED);
        assertThat(affixed.title()).isEqualTo(Affix.ARMORED.title() + " Grub");
        assertThat(affixed.displayName()).isEqualTo("Grub");
        assertThat(plain.title()).isEqualTo("Grub");
    }
}
