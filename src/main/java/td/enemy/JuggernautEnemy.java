package td.enemy;

import java.util.List;

/** The Juggernaut ({@code juggernaut}): slow, with plating that stops most physical hits; magic walks through it. */
final class JuggernautEnemy {

    private JuggernautEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("juggernaut", "Juggernaut", 300, 15, 0.64f, BodyArchetype.JUGGERNAUT)
                        .withDescription("Slow, and its plating blunts a huge flat chunk of every physical hit. "
                                + "Magic, Cracked and Sundered get past it.")
                        .withMovement(new RotorMovement((float) Math.toRadians(1.5)))
                        .withIdentifiedTraits(List.of(IdentifiedTrait.anonymous(FlatResistTrait.physicalOnly(25f)))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(600, 38))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(1200, 94))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(2400, 235))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(4800, 586))
                .build());
    }
}
