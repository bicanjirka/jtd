package td.enemy;

import td.effect.SprintTemplate;

import java.util.List;

/**
 * The Courier ({@code courier}): very fast, fragile and a little nimble, and pays well for the tower that catches it.
 * With rank it gets nimbler, from Elite it bolts when first hit, and at Boss it sprints off after every dodge.
 */
final class CourierEnemy {

    private CourierEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("courier", "Courier", 45, 5, 3.0f, BodyArchetype.COURIER)
                        .withDescription("A fast, fragile runner that slips past slow towers.")
                        .withAgility(25f), RankGrowth.of(12f))
                .thenAt(Rank.VETERAN, e -> e.withAgility(35f))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(5f, 5f, 0f, 0f, 15f, 35f, 10f)
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(AbilityTerm.BOLT,
                                new OnFirstDamageTakenTrigger(),
                                new ApplyEffectAction(new SprintTemplate(40), new SelfTarget()))))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(10f, 10f, 0f, 0f, 25f, 50f, 15f)
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(AbilityTerm.SIDESTEP, new OnDodgeTrigger(),
                                new ApplyEffectAction(new SprintTemplate(20), new SelfTarget()))))))
                .build());
    }
}
