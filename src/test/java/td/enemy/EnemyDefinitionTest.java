package td.enemy;

import org.junit.jupiter.api.Test;
import td.effect.ShieldTemplate;
import td.fixtures.EnemyFixtures;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link EnemyDefinition#withAdditionalTraits}/{@link EnemyDefinition#withAdditionalAbilities} -
 * the {@link TraitId} identity mechanism a rank step, or a spawn shape's trait override, composes
 * a definition with. {@code withTraits}/{@code withAbilities} (full replace) need no test of
 * their own: they're exercised throughout {@code BuiltInEnemies} already.
 */
class EnemyDefinitionTest {

    private final EnemyDefinition base = EnemyFixtures.simpleDefinition("test");

    @Test
    void twoAnonymousAdditionsBothStackEvenWhenStructurallyEqual() {
        Trait resist = new PercentResistTrait(0.8f, 0f);
        EnemyDefinition withOne = this.base.withAdditionalTraits(List.of(IdentifiedTrait.anonymous(resist)));
        EnemyDefinition withTwo = withOne.withAdditionalTraits(List.of(IdentifiedTrait.anonymous(resist)));

        assertThat(withTwo.traits()).containsExactly(resist, resist);
    }

    @Test
    void aNamedAdditionWithNoExistingMatchIsAppended() {
        Trait resist = new PercentResistTrait(0.8f, 0f);
        EnemyDefinition result = this.base.withAdditionalTraits(List.of(IdentifiedTrait.named("shield", resist)));

        assertThat(result.traits()).containsExactly(resist);
    }

    @Test
    void aNamedAdditionReplacesTheEarlierEntryWithTheSameIdInPlace() {
        Trait weakShield = new PercentResistTrait(0.8f, 0f);
        Trait strongShield = new PercentResistTrait(0.5f, 0f);
        Trait unrelated = new CriticalImmunityTrait();
        EnemyDefinition rankOne = this.base.withAdditionalTraits(
                List.of(IdentifiedTrait.named("shield", weakShield), IdentifiedTrait.anonymous(unrelated)));

        EnemyDefinition rankTwo = rankOne.withAdditionalTraits(List.of(IdentifiedTrait.named("shield", strongShield)));

        assertThat(rankTwo.traits()).containsExactly(strongShield, unrelated);
    }

    @Test
    void aNamedAbilityAdditionReplacesTheEarlierEntryWithTheSameId() {
        Ability weakHeal = new Ability(new OnceTrigger(10), new ApplyEffectAction(new ShieldTemplate(0.3f, 100), new SelfTarget()));
        Ability strongHeal = new Ability(new OnceTrigger(20), new ApplyEffectAction(new ShieldTemplate(0.5f, 100), new SelfTarget()));
        EnemyDefinition rankOne = this.base.withAdditionalAbilities(List.of(IdentifiedAbility.named("heal", weakHeal)));

        EnemyDefinition rankTwo = rankOne.withAdditionalAbilities(List.of(IdentifiedAbility.named("heal", strongHeal)));

        assertThat(rankTwo.abilities()).containsExactly(strongHeal);
    }
}
