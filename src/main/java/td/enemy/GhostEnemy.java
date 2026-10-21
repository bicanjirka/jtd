package td.enemy;

import td.effect.InvisibleTemplate;

import java.util.List;

/**
 * The Ghost mob ({@code g}): half there, so physical hits pass partly through; vanishes from towers the first time
 * it is hit, for longer from Veteran; from Elite it is hard to hit and shrouds its allies; at Boss it fades out on
 * its own now and then.
 */
final class GhostEnemy {

    /** How often an Elite Ghost shrouds its allies, and how long each shroud lasts. */
    static final int SHROUD_TICKS = 60;

    private GhostEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("g", "Ghost mob", 200, 5, 1.28f, BodyArchetype.GHOST)
                        .withDescription("Half there: hard to hit and hard to see, but area damage finds it.")
                        .withArmor(20f)
                        .withIdentifiedAbilities(List.of(fade(200))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(360, 9))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(648, 13)
                        .withAdditionalAbilities(List.of(fade(300))))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(1166, 25)
                        .withStats(30f, 10f, 0f, 0f, 20f, 15f, 15f)
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(AbilityTerm.SHROUD,
                                // A tick longer than the interval, so an ally that stays in radius never flickers
                                // visible, while a reveal holds until the next cast.
                                new PeriodicTrigger(SHROUD_TICKS),
                                new ApplyEffectAction(new InvisibleTemplate(SHROUD_TICKS + 1), new RadiusTarget(100f)))))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(2100, 45)
                        .withStats(40f, 15f, 0f, 0f, 30f, 20f, 25f)
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(AbilityTerm.PHASE,
                                new PeriodicTrigger(200), new ApplyEffectAction(new InvisibleTemplate(60),
                                new SelfTarget()))))))
                .build());
    }

    /** Its vanishing act; a hit that also freezes suppresses the cast outright, since a frozen mob cannot cast. */
    private static IdentifiedAbility fade(int durationTicks) {
        return IdentifiedAbility.named("fade", new Ability(AbilityTerm.FADE, new OnFirstDamageTakenTrigger(),
                new ApplyEffectAction(new InvisibleTemplate(durationTicks), new SelfTarget())));
    }
}
