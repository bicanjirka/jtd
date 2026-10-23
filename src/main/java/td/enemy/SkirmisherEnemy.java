package td.enemy;

import td.effect.InvisibleTemplate;
import td.effect.SprintTemplate;

import java.util.List;

/**
 * The Skirmisher ({@code skirmisher}): nimble enough to dodge aimed shots and hard to catch cleanly, so crits land
 * softer on it; from Soldier it sprints off after every dodge, from Elite it gets harder to hit the more it is hurt,
 * and at Boss it vanishes for a moment after every dodge.
 */
final class SkirmisherEnemy {

    private SkirmisherEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("skirmisher", "Skirmisher", 90, 2, 1.6f, BodyArchetype.SKIRMISHER)
                        .withDescription("Nimble and hard to pin down with aimed shots.")
                        .withResilience(20f)
                        .withAgility(40f))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(162, 4)
                        .withAgility(50f)
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(AbilityTerm.SIDESTEP, new OnDodgeTrigger(),
                                new ApplyEffectAction(new SprintTemplate(30), new SelfTarget()))))))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(292, 5).withAgility(60f))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(525, 10)
                        .withStats(10f, 10f, 0f, 0f, 20f, 60f, 10f)
                        .withAdditionalTraits(List.of(IdentifiedTrait.named("evasive", new HurtAgilityTrait(60f)))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(945, 17)
                        .withStats(15f, 15f, 0f, 0f, 30f, 80f, 20f)
                        .withAdditionalTraits(List.of(IdentifiedTrait.named("evasive", new HurtAgilityTrait(80f))))
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(AbilityTerm.VANISH, new OnDodgeTrigger(),
                                new ApplyEffectAction(new InvisibleTemplate(15), new SelfTarget()))))))
                .build());
    }
}
