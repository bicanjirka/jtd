package td.enemy;

import td.effect.SprintTemplate;

import java.util.List;

/**
 * The Skirmisher ({@code skirmisher}): nimble enough to dodge aimed shots, from Soldier it sprints
 * off after every dodge, and from Elite it gets harder to hit the more it is hurt.
 */
final class SkirmisherEnemy {

    private SkirmisherEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("skirmisher", "Skirmisher", 60, 3, 1.6f, BodyArchetype.SKIRMISHER)
                        .withDescription("Nimble: it dodges some aimed shots. Area damage, freezes and accurate "
                                + "towers catch it.")
                        .withIdentifiedTraits(List.of(nimble(40f))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(120, 8)
                        .withDescription("Nimble, and sprints off for a moment after every shot it dodges.")
                        .withAdditionalTraits(List.of(nimble(50f)))
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(new OnDodgeTrigger(),
                                new ApplyEffectAction(new SprintTemplate(30), new SelfTarget()))))))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(240, 20)
                        .withAdditionalTraits(List.of(nimble(60f))))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(480, 50)
                        .withDescription("Nimble, sprints off after every dodge, and the more it is hurt the "
                                + "harder it is to hit.")
                        .withAdditionalTraits(List.of(IdentifiedTrait.named("evasive", new HurtAgilityTrait(60f)))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(960, 125)
                        .withAdditionalTraits(List.of(nimble(80f),
                                IdentifiedTrait.named("evasive", new HurtAgilityTrait(80f)))))
                .build());
    }

    private static IdentifiedTrait nimble(float agility) {
        return IdentifiedTrait.named("agility", new AgilityTrait(agility));
    }
}
