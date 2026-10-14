package td.enemy;

import td.effect.ShieldTemplate;
import td.stat.EnemyStat;

import java.util.List;

/**
 * The Juggernaut ({@code juggernaut}): slow and massive, with plating that stops most physical hits; magic walks
 * through it. Too heavy to freeze or chill for long; from Elite crits glance off, and at Boss it wards itself
 * against magic once badly hurt.
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
                        .withStat(EnemyStat.MAGIC_RESIST, 15f)
                        .withStat(EnemyStat.FREEZE_RESIST, 0.3f)
                        .withStat(EnemyStat.CHILL_RESIST, 0.3f)
                        .withIdentifiedTraits(List.of(IdentifiedTrait.named("plating", FlatResistTrait.physicalOnly(25f)))),
                        RankGrowth.of(2f, 10))
                .thenAt(Rank.VETERAN, e -> e
                        .withAdditionalTraits(List.of(IdentifiedTrait.named("plating", FlatResistTrait.physicalOnly(30f)))))
                .thenAt(Rank.ELITE, e -> e
                        .withDescription("Slow, its plating blunts a huge flat chunk of every physical hit, and crits "
                                + "glance off it. Magic, Cracked and Sundered get past it. Too heavy to freeze or chill "
                                + "for long.")
                        .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(new CriticalImmunityTrait()))))
                .thenAt(Rank.BOSS, e -> e
                        .withDescription("Slow, its plating blunts a massive flat chunk of every physical hit, and "
                                + "crits glance off it. Once badly hurt it wards itself against magic. Cracked and "
                                + "Sundered still get past it. Too heavy to freeze or chill for long.")
                        .withAdditionalTraits(List.of(IdentifiedTrait.named("plating", FlatResistTrait.physicalOnly(40f))))
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(
                                new HealthThresholdTrigger(0.5f),
                                new ApplyEffectAction(ShieldTemplate.magicOnly(1500f, 200), new SelfTarget()))))))
                .build());
    }
}
