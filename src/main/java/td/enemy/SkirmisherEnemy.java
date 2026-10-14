package td.enemy;

import td.effect.InvisibleTemplate;
import td.effect.SprintTemplate;
import td.stat.EnemyStat;

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
                .startingAt(EnemyDefinition.of("skirmisher", "Skirmisher", 120, 4, 1.6f, BodyArchetype.SKIRMISHER)
                        .withDescription("Nimble: it dodges some aimed shots, and crits land softer on it. Area damage, "
                                + "freezes and accurate towers catch it.")
                        .withStat(EnemyStat.RESILIENCE, 20f)
                        .withIdentifiedTraits(List.of(nimble(40f))), RankGrowth.of(1.8f, 3))
                .thenAt(Rank.SOLDIER, e -> e
                        .withDescription("Nimble, crits land softer on it, and it sprints off for a moment after every "
                                + "shot it dodges.")
                        .withAdditionalTraits(List.of(nimble(50f)))
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(new OnDodgeTrigger(),
                                new ApplyEffectAction(new SprintTemplate(30), new SelfTarget()))))))
                .thenAt(Rank.VETERAN, e -> e.withAdditionalTraits(List.of(nimble(60f))))
                .thenAt(Rank.ELITE, e -> e
                        .withDescription("Nimble, crits land softer on it, it sprints off after every dodge, and the "
                                + "more it is hurt the harder it is to hit.")
                        .withAdditionalTraits(List.of(IdentifiedTrait.named("evasive", new HurtAgilityTrait(60f)))))
                .thenAt(Rank.BOSS, e -> e
                        .withDescription("Nimble, crits land softer on it, the more it is hurt the harder it is to hit, "
                                + "and after every dodge it sprints off and vanishes for a moment.")
                        .withAdditionalTraits(List.of(nimble(80f),
                                IdentifiedTrait.named("evasive", new HurtAgilityTrait(80f))))
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(new OnDodgeTrigger(),
                                new ApplyEffectAction(new InvisibleTemplate(15), new SelfTarget()))))))
                .build());
    }

    private static IdentifiedTrait nimble(float agility) {
        return IdentifiedTrait.named("agility", new AgilityTrait(agility));
    }
}
