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
                .startingAt(EnemyDefinition.of("courier", "Courier", 60, 10, 3.0f, BodyArchetype.COURIER)
                        .withDescription("Very fast and fragile, nimble enough to dodge an aimed shot now and then, "
                                + "with a high bounty. Worth hunting.")
                        .withAgility(25f))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(102, 20))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(173, 30).withAgility(35f))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(295, 60)
                        .withDescription("Very fast and fragile, nimble enough to dodge an aimed shot now and then, and "
                                + "bolts the moment it is first hit. A high bounty. Worth hunting.")
                        .withStats(5f, 5f, 0f, 0f, 15f, 35f, 10f)
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(
                                new OnFirstDamageTakenTrigger(),
                                new ApplyEffectAction(new SprintTemplate(40), new SelfTarget()))))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(501, 110)
                        .withDescription("Very fast and nimble, bolts the moment it is first hit and sprints off again "
                                + "after every shot it dodges. A huge bounty. Worth hunting.")
                        .withStats(10f, 10f, 0f, 0f, 25f, 50f, 15f)
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(new OnDodgeTrigger(),
                                new ApplyEffectAction(new SprintTemplate(20), new SelfTarget()))))))
                .build());
    }
}
