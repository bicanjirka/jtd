package td.enemy;

import td.effect.EffectKind;

import java.util.List;

/** The Salamander ({@code salamander}): immune to burn, so a Cinder-only defence does nothing; Soulfire still burns it. */
final class SalamanderEnemy {

    private SalamanderEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("salamander", "Salamander", 90, 5, 1.28f, BodyArchetype.SALAMANDER)
                        .withDescription("Immune to burn. Soulfire still burns it.")
                        .withIdentifiedTraits(List.of(
                                IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.BURN)))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(180, 13))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(360, 31))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(720, 78))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(1440, 195))
                .build());
    }
}
