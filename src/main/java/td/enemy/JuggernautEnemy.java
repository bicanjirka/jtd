package td.enemy;

import td.effect.EffectKind;
import td.effect.ShieldTemplate;

import java.util.List;

/**
 * The Juggernaut ({@code juggernaut}): slow and massive, with plating that stops most physical hits; magic walks
 * through it. Too heavy to freeze or chill for long, and too heavy to dodge anything; from Elite crits glance off,
 * and at Boss it wards itself against magic once badly hurt.
 */
final class JuggernautEnemy {

    private JuggernautEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("juggernaut", "Juggernaut", 600, 12, 0.64f, BodyArchetype.JUGGERNAUT)
                        .withDescription("Slow, and its plating blunts a huge flat chunk of every physical hit. "
                                + "Magic, Cracked and Sundered get past it. Too heavy to freeze or chill for long.")
                        .withMovement(new RotorMovement((float) Math.toRadians(1.5)))
                        .withPlating(25f)
                        .withMagicResist(15f)
                        .withIdentifiedTraits(List.of(
                                IdentifiedTrait.anonymous(EffectResistTrait.resisting(EffectKind.FREEZE, 0.3f)),
                                IdentifiedTrait.anonymous(EffectResistTrait.resisting(EffectKind.CHILL, 0.3f)))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(1200, 22))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(2400, 32).withPlating(30f))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(4800, 62)
                        .withDescription("Slow, its plating blunts a huge flat chunk of every physical hit, and crits "
                                + "glance off it. Magic, Cracked and Sundered get past it. Too heavy to freeze or chill "
                                + "for long.")
                        .withStats(15f, 15f, 30f, 0f, 100f, 0f, 10f))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(9600, 112)
                        .withDescription("Slow, its plating blunts a massive flat chunk of every physical hit, and "
                                + "crits glance off it. Once badly hurt it wards itself against magic. Cracked and "
                                + "Sundered still get past it. Too heavy to freeze or chill for long.")
                        .withStats(25f, 20f, 40f, 0f, 100f, 0f, 20f)
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(
                                new HealthThresholdTrigger(0.5f),
                                new ApplyEffectAction(ShieldTemplate.magicOnly(1500f, 200), new SelfTarget()))))))
                .build());
    }
}
