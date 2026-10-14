package td.enemy;

import td.effect.InvisibleTemplate;

import java.util.List;

/**
 * The Ghost mob ({@code g}): half there, so physical hits pass partly through; vanishes from towers the first time
 * it is hit, for longer from Veteran; from Elite it is hard to hit and shrouds its allies; at Boss it fades out on
 * its own now and then.
 */
final class GhostEnemy {

    private GhostEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("g", "Ghost mob", 200, 5, 1.28f, BodyArchetype.GHOST)
                        .withDescription("Half there: physical hits pass partly through it. Turns invisible to towers "
                                + "for a while the first time it's hit. Area damage still finds it.")
                        .withArmor(20f)
                        .withIdentifiedAbilities(List.of(fade(200))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(360, 9))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(648, 13)
                        .withDescription("Half there: physical hits pass partly through it. Turns invisible to towers "
                                + "for a long while the first time it's hit. Area damage still finds it.")
                        .withAdditionalAbilities(List.of(fade(300))))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(1166, 25)
                        .withDescription("Half there: physical hits pass partly through it and aimed shots often miss. "
                                + "Turns invisible to towers for a long while the first time it's hit, and permanently "
                                + "shrouds every other ally near it - itself excluded.")
                        .withArmor(30f)
                        .withMagicResist(10f)
                        .withResilience(20f)
                        .withAgility(15f)
                        .withSpirit(15f)
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(
                                // Each application outlasts the interval, so an ally in radius never flickers visible.
                                new PeriodicTrigger(20),
                                new ApplyEffectAction(new InvisibleTemplate(40), new RadiusTarget(100f)))))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(2100, 45)
                        .withDescription("Half there: physical hits pass partly through it and aimed shots often miss. "
                                + "Turns invisible the first time it's hit and again for a few seconds out of every "
                                + "ten, and permanently shrouds every other ally near it - itself excluded.")
                        .withArmor(40f)
                        .withMagicResist(15f)
                        .withResilience(30f)
                        .withAgility(20f)
                        .withSpirit(25f)
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(
                                new PeriodicTrigger(200), new ApplyEffectAction(new InvisibleTemplate(60),
                                new SelfTarget()))))))
                .build());
    }

    /** Its vanishing act; a hit that also freezes suppresses the cast outright, since a frozen mob cannot cast. */
    private static IdentifiedAbility fade(int durationTicks) {
        return IdentifiedAbility.named("fade", new Ability(new OnFirstDamageTakenTrigger(),
                new ApplyEffectAction(new InvisibleTemplate(durationTicks), new SelfTarget())));
    }
}
