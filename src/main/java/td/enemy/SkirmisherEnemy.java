package td.enemy;

import td.effect.InvisibleTemplate;
import td.effect.SprintTemplate;

import java.util.List;

/**
 * The Skirmisher ({@code skirmisher}): nimble enough to dodge aimed shots and hard to catch cleanly, so crits land
 * softer on it and slows slip off it sooner; from Soldier it sprints off after every dodge, from Elite it gets harder
 * to hit the more it is hurt, and at Boss it vanishes for a moment after every dodge.
 */
final class SkirmisherEnemy {

    private SkirmisherEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("skirmisher", "Skirmisher", 86, 3, 1.6f, BodyArchetype.SKIRMISHER)
                                .withDescription("Nimble and hard to pin down with aimed shots.")
                                .withResilience(20f)
                                .withAgility(40f)
                                .withTenacity(20f),
                        RankGrowth.stepped(120, 371, 958, 1949).paying(4, 8, 15, 25))
                .thenAt(Rank.SOLDIER, e -> e
                        .withAgility(50f)
                        .withTenacity(25f)
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(AbilityTerm.SIDESTEP, new OnDodgeTrigger(),
                                new ApplyEffectAction(new SprintTemplate(30), new SelfTarget()))))))
                .thenAt(Rank.VETERAN, e -> e.withAgility(60f))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(30f, 30f, 0f, 0f, 80f, 30f, 20f, 40f)
                        .withAdditionalTraits(List.of(skittish(100f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(45f, 30f, 0f, 0f, 95f, 40f, 30f, 60f)
                        .withAdditionalTraits(List.of(skittish(250f)))
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(AbilityTerm.VANISH, new OnDodgeTrigger(),
                                new ApplyEffectAction(new InvisibleTemplate(15), new SelfTarget()))))))
                .build());
    }

    /** Agility has no ceiling: dodging only nears certainty, so a large maximum is a steep climb, not a wall. */
    private static IdentifiedTrait skittish(float maxAgility) {
        return IdentifiedTrait.named("skittish", new HurtAgilityTrait(maxAgility));
    }
}
