package td.enemy;

import td.effect.EffectKind;

import java.util.List;

/** The Yeti ({@code yeti}): immune to freeze and half as chilled, so a Seeker-only defence fails; Dazed still stops it. */
final class YetiEnemy {

    private YetiEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("yeti", "Yeti", 110, 6, 1.1f, BodyArchetype.YETI)
                        .withDescription("Immune to freeze, and chills half as long. Dazed still stops it.")
                        .withIdentifiedTraits(List.of(
                                IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.FREEZE)),
                                IdentifiedTrait.anonymous(EffectResistTrait.resisting(EffectKind.CHILL, 0.5f)))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(220, 15))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(440, 38))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(880, 94))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(1760, 235))
                .build());
    }
}
