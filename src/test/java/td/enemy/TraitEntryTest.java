package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.DamageType;
import td.effect.EffectKind;

import static org.assertj.core.api.Assertions.assertThat;

class TraitEntryTest {

    @Test
    void aFullResistanceIsTheEffectFullyResisted() {
        TraitEntry entry = EffectResistTrait.immuneTo(EffectKind.FREEZE).entry();

        assertThat(entry.term()).isEqualTo(EffectKind.FREEZE);
        assertThat(entry.immune()).isTrue();
        assertThat(entry.text().plain()).isEqualTo("immune to Freeze");
    }

    @Test
    void aPartialResistanceKeepsItsShare() {
        TraitEntry entry = EffectResistTrait.resisting(EffectKind.CHILL, 0.25f).entry();

        assertThat(entry.immune()).isFalse();
        assertThat(entry.resisted()).isEqualTo(0.25f);
        assertThat(entry.text().plain()).isEqualTo("Chill lasts 25% less on it");
    }

    @Test
    void anAdaptiveResistShowsTheMostItCanReach() {
        TraitEntry entry = AdaptiveResist.against(DamageType.PHYSICAL, 10f, 40f).entry();

        assertThat(entry.term()).isEqualTo(TraitTerm.ADAPTATION);
        assertThat(entry.text().plain()).contains("+40 armor");
    }
}
