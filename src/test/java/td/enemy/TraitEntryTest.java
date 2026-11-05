package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.DamageType;
import td.effect.EffectKind;

import static org.assertj.core.api.Assertions.assertThat;

class TraitEntryTest {

    @Test
    void anImmunityNamesTheEffectItKeepsOut() {
        TraitEntry entry = new ImmunityTrait(EffectKind.FREEZE).entry();

        assertThat(entry.term()).isEqualTo(EffectKind.FREEZE);
        assertThat(entry.immune()).isTrue();
        assertThat(entry.text().plain()).isEqualTo("immune to Freeze");
    }

    @Test
    void anAdaptiveResistShowsTheMostItCanReach() {
        TraitEntry entry = AdaptiveResist.against(DamageType.PHYSICAL, 10f, 40f).entry();

        assertThat(entry.term()).isEqualTo(TraitTerm.ADAPTATION);
        assertThat(entry.text().plain()).contains("+40 armor");
    }
}
