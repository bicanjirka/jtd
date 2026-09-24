package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.DamageMix;
import td.effect.EffectKind;
import td.effect.InvisibleTemplate;
import td.effect.ShieldTemplate;
import td.fixtures.EnemyFixtures;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnemyDefinitionTest {

    private final EnemyDefinition base = EnemyFixtures.simpleDefinition("test");

    @Test
    void twoAnonymousAdditionsBothStackEvenWhenStructurallyEqual() {
        Trait resist = new PercentResistTrait(0.8f);
        EnemyDefinition withOne = this.base.withAdditionalTraits(List.of(IdentifiedTrait.anonymous(resist)));
        EnemyDefinition withTwo = withOne.withAdditionalTraits(List.of(IdentifiedTrait.anonymous(resist)));

        assertThat(withTwo.traitsFor(DamageMix.none())).containsExactly(resist, resist);
    }

    @Test
    void aNamedAdditionWithNoExistingMatchIsAppended() {
        Trait resist = new PercentResistTrait(0.8f);
        EnemyDefinition result = this.base.withAdditionalTraits(List.of(IdentifiedTrait.named("shield", resist)));

        assertThat(result.traitsFor(DamageMix.none())).containsExactly(resist);
    }

    @Test
    void aNamedAdditionReplacesTheEarlierEntryWithTheSameIdInPlace() {
        Trait weakShield = new PercentResistTrait(0.8f);
        Trait strongShield = new PercentResistTrait(0.5f);
        Trait unrelated = new CriticalImmunityTrait();
        EnemyDefinition rankOne = this.base.withAdditionalTraits(
                List.of(IdentifiedTrait.named("shield", weakShield), IdentifiedTrait.anonymous(unrelated)));

        EnemyDefinition rankTwo = rankOne.withAdditionalTraits(List.of(IdentifiedTrait.named("shield", strongShield)));

        assertThat(rankTwo.traitsFor(DamageMix.none())).containsExactly(strongShield, unrelated);
    }

    @Test
    void aNamedAbilityAdditionReplacesTheEarlierEntryWithTheSameId() {
        Ability weakHeal = new Ability(new OnceTrigger(10), new ApplyEffectAction(new ShieldTemplate(0.3f, 100), new SelfTarget()));
        Ability strongHeal = new Ability(new OnceTrigger(20), new ApplyEffectAction(new ShieldTemplate(0.5f, 100), new SelfTarget()));
        EnemyDefinition rankOne = this.base.withAdditionalAbilities(List.of(IdentifiedAbility.named("heal", weakHeal)));

        EnemyDefinition rankTwo = rankOne.withAdditionalAbilities(List.of(IdentifiedAbility.named("heal", strongHeal)));

        assertThat(rankTwo.abilities()).containsExactly(strongHeal);
    }

    @Test
    void aDefinitionWithOnlySelfTargetedAbilitiesHasNoSupportAura() {
        EnemyDefinition withSelfShield = this.base.withAbilities(List.of(
                new Ability(new OnceTrigger(10), new ApplyEffectAction(new ShieldTemplate(0.3f, 100), new SelfTarget()))));

        assertThat(withSelfShield.supportAura()).isEmpty();
    }

    @Test
    void aDefinitionWithARadiusTargetedAbilityReportsItsKindAndRadius() {
        EnemyDefinition withAura = this.base.withAbilities(List.of(
                new Ability(new PeriodicTrigger(20), new ApplyEffectAction(new ShieldTemplate(0.3f, 100), new RadiusTarget(75f)))));

        assertThat(withAura.supportAura()).contains(new SupportAura(EffectKind.SHIELD, 75f));
    }

    @Test
    void theLargestRadiusAmongSeveralRadiusTargetedAbilitiesWins() {
        EnemyDefinition withTwoAuras = this.base.withAbilities(List.of(
                new Ability(new PeriodicTrigger(20), new ApplyEffectAction(new ShieldTemplate(0.3f, 100), new RadiusTarget(50f))),
                new Ability(new PeriodicTrigger(30), new ApplyEffectAction(new InvisibleTemplate(40), new RadiusTarget(120f)))));

        assertThat(withTwoAuras.supportAura()).contains(new SupportAura(EffectKind.INVISIBLE, 120f));
    }
}
