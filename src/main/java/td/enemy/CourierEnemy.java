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
                        .withIdentifiedTraits(List.of(nimble(25f))), RankGrowth.of(1.7f, 10))
                .thenAt(Rank.VETERAN, e -> e.withAdditionalTraits(List.of(nimble(35f))))
                .thenAt(Rank.ELITE, e -> e
                        .withDescription("Very fast and fragile, nimble enough to dodge an aimed shot now and then, and "
                                + "bolts the moment it is first hit. A high bounty. Worth hunting.")
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(
                                new OnFirstDamageTakenTrigger(),
                                new ApplyEffectAction(new SprintTemplate(40), new SelfTarget()))))))
                .thenAt(Rank.BOSS, e -> e
                        .withDescription("Very fast and nimble, bolts the moment it is first hit and sprints off again "
                                + "after every shot it dodges. A huge bounty. Worth hunting.")
                        .withAdditionalTraits(List.of(nimble(50f)))
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(new OnDodgeTrigger(),
                                new ApplyEffectAction(new SprintTemplate(20), new SelfTarget()))))))
                .build());
    }

    private static IdentifiedTrait nimble(float agility) {
        return IdentifiedTrait.named("agility", new AgilityTrait(agility));
    }
}
