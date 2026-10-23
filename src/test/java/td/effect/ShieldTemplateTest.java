package td.effect;

import org.junit.jupiter.api.Test;
import td.damage.DamageType;
import td.damage.DamageUnits;

import static org.assertj.core.api.Assertions.assertThat;

class ShieldTemplateTest {

    @Test
    void aShieldTemplateHoldsItsPointsAndKeepsTheDamageTypeItIsLimitedTo() {
        Effect physical = ShieldTemplate.physicalOnly(3f, 40).toEffect(DamageSink.none());
        Effect any = new ShieldTemplate(3f, 40).toEffect(DamageSink.none());

        assertThat(physical.shieldMax()).isEqualTo((float) DamageUnits.ofPoints(3f));
        assertThat(physical.shieldRestrictedTo()).contains(DamageType.PHYSICAL);
        assertThat(any.shieldRestrictedTo()).isEmpty();
    }
}
