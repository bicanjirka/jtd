package td.enemy;

import td.effect.EffectKind;
import td.effect.ShieldTemplate;

import java.util.List;

/**
 * The Juggernaut ({@code juggernaut}): slow and massive, with plating that stops most physical hits; magic walks
 * through it. Too heavy to freeze or chill for long, and too heavy to dodge anything; from Veteran it blinds the towers
 * around it as it dies until the wave is over, wider from Elite; from Elite crits glance off, and at Boss it wards
 * itself against magic once badly hurt.
 */
final class JuggernautEnemy {

    /** The chance a hit from a tower inside its blast misses. */
    private static final float BLINDING_MISS_CHANCE = 0.75f;

    private JuggernautEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("juggernaut", "Juggernaut", 450, 6, 0.64f, BodyArchetype.JUGGERNAUT)
                        .withDescription("A slow fortress whose plating swallows small hits.")
                        .withMovement(new RotorMovement((float) Math.toRadians(1.5)))
                        .withPlating(25f)
                        .withMagicResist(15f)
                        .withIdentifiedTraits(List.of(
                                IdentifiedTrait.anonymous(EffectResistTrait.resisting(EffectKind.FREEZE, 0.3f)),
                                IdentifiedTrait.anonymous(EffectResistTrait.resisting(EffectKind.CHILL, 0.3f)))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(900, 11))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(1800, 16).withPlating(30f)
                        .withAdditionalAbilities(List.of(blast(2f))))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(3600, 31)
                        .withStats(15f, 15f, 30f, 0f, 100f, 0f, 10f)
                        .withAdditionalAbilities(List.of(blast(4f))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(7200, 56)
                        .withStats(25f, 20f, 40f, 0f, 100f, 0f, 20f)
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(AbilityTerm.BULWARK,
                                new HealthThresholdTrigger(0.5f),
                                new ApplyEffectAction(ShieldTemplate.magicOnly(1500f, 200), new SelfTarget()))))))
                .build());
    }

    /** The field of blinded towers it leaves where it dies. */
    private static IdentifiedAbility blast(float radiusCells) {
        return IdentifiedAbility.named("blast", new Ability(AbilityTerm.BLINDING_BURST, new OnDeathTrigger(),
                LeaveZoneAction.blindingField(radiusCells, BLINDING_MISS_CHANCE)));
    }
}
