package td.enemy;

import td.effect.ShieldTemplate;
import td.effect.SprintTemplate;

import java.util.List;

/**
 * The Courier ({@code courier}): very fast, fragile and a little nimble, and pays well for the tower that catches it.
 * With rank it gets nimbler and shakes off slows sooner; from Veteran it bolts when first hit, from Elite each dodge
 * raises a brief shield, and at Boss each dodge also sends it sprinting off.
 */
final class CourierEnemy {

    private CourierEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("courier", "Courier", 29, 4, 3.0f, BodyArchetype.COURIER)
                                .withDescription("Fast and fragile, it slips past slow towers.")
                                .withAgility(25f)
                                .withTenacity(15f),
                        RankGrowth.stepped(47, 196, 649, 1155).paying(6, 9, 13, 17))
                .thenAt(Rank.VETERAN, e -> e
                        .withAgility(45f)
                        .withTenacity(25f)
                        .withAdditionalAbilities(List.of(bolt())))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(15f, 25f, 0f, 2f, 55f, 35f, 20f, 25f)
                        .withAdditionalAbilities(List.of(parry())))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(30f, 55f, 0f, 5f, 70f, 55f, 45f, 45f)
                        .withAdditionalAbilities(List.of(sidestep())))
                .build());
    }

    private static IdentifiedAbility bolt() {
        return IdentifiedAbility.anonymous(new Ability(AbilityTerm.BOLT,
                new OnFirstDamageTakenTrigger(),
                new ApplyEffectAction(new SprintTemplate(40), new SelfTarget())));
    }

    /** A shield on each dodge, short enough that only the shots straight after the miss meet it. */
    private static IdentifiedAbility parry() {
        return IdentifiedAbility.anonymous(new Ability(AbilityTerm.PARRY, new OnDodgeTrigger(),
                new ApplyEffectAction(new ShieldTemplate(45, 60), new SelfTarget())));
    }

    private static IdentifiedAbility sidestep() {
        return IdentifiedAbility.anonymous(new Ability(AbilityTerm.SIDESTEP, new OnDodgeTrigger(),
                new ApplyEffectAction(new SprintTemplate(20), new SelfTarget())));
    }
}
