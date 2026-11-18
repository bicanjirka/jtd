package td.enemy;

import td.effect.ShieldTemplate;

import java.util.List;

/**
 * The Juggernaut ({@code juggernaut}): slow and massive, with plating that stops most physical hits; magic walks
 * through it, though from Elite it resists magic a little. Too heavy to freeze or chill for long, and too heavy to
 * dodge anything, so crits find it easily until Elite; from Veteran it blinds the towers around it as it dies until the
 * wave is over, wider from Elite; from Elite crits glance off, and at Boss it wards itself against magic twice on the
 * way down.
 */
final class JuggernautEnemy {

    /** The chance a hit from a tower inside its blast misses. */
    private static final float BLINDING_MISS_CHANCE = 0.75f;

    private JuggernautEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("juggernaut", "Juggernaut", 286, 6, 0.64f, BodyArchetype.JUGGERNAUT)
                        .withDescription("A slow fortress whose plating swallows small hits.")
                        .withMovement(new RotorMovement((float) Math.toRadians(1.5)))
                        .withAgility(-25f)
                        .withPlating(25f)
                        .withTenacity(43f),
                        RankGrowth.stepped(455, 845, 2504, 7485).paying(8, 14, 28, 46))
                .thenAt(Rank.VETERAN, e -> e
                        .withPlating(35f)
                        .withAdditionalAbilities(List.of(blast(2f))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(10f, 5f, 50f, 0f, -20f, 43f, 100f, 15f)
                        .withAdditionalAbilities(List.of(blast(4f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(25f, 10f, 80f, 0f, -5f, 43f, 100f, 30f)
                        .withAdditionalAbilities(List.of(bulwark(0.65f), bulwark(0.15f))))
                .build());
    }

    /** A ward against magic that holds until broken, raised below {@code healthFraction}. */
    private static IdentifiedAbility bulwark(float healthFraction) {
        return IdentifiedAbility.anonymous(new Ability(AbilityTerm.BULWARK,
                new HealthThresholdTrigger(healthFraction),
                new ApplyEffectAction(ShieldTemplate.magicOnlyUntilBroken(1000f), new SelfTarget())));
    }

    /** The field of blinded towers it leaves where it dies. */
    private static IdentifiedAbility blast(float radiusCells) {
        return IdentifiedAbility.named("blast", new Ability(AbilityTerm.BLINDING_BURST, new OnDeathTrigger(),
                LeaveZoneAction.blindingField(radiusCells, BLINDING_MISS_CHANCE)));
    }
}
