package td.enemy;

import td.effect.InvisibleTemplate;

import java.util.List;

/** The Ghost mob ({@code g}): vanishes from towers the first time it is hit, and from Elite shrouds its allies. */
final class GhostEnemy {

    private GhostEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("g", "Ghost mob", 100, 4, 1.28f, BodyArchetype.GHOST)
                        .withDescription("An ordinary mob that turns invisible to towers for a while "
                                + "the first time it's hit. Area damage still finds it.")
                        .withAbilities(List.of(new Ability(
                                // A hit that also freezes suppresses this cast outright: a frozen mob cannot cast.
                                new OnFirstDamageTakenTrigger(),
                                new ApplyEffectAction(new InvisibleTemplate(200), new SelfTarget())))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(200, 10))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(400, 25))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(800, 63)
                        .withDescription("Turns invisible to towers for a while the first time it's "
                                + "hit, and permanently shrouds every other ally near it - itself excluded.")
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(
                                // Each application outlasts the interval, so an ally in radius never flickers visible.
                                new PeriodicTrigger(20),
                                new ApplyEffectAction(new InvisibleTemplate(40), new RadiusTarget(100f)))))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(1600, 158))
                .build());
    }
}
