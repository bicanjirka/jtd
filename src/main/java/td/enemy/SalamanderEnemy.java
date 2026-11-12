package td.enemy;

import td.effect.EffectKind;
import td.effect.HealTemplate;

import java.util.List;

/**
 * The Salamander ({@code salamander}): immune to burn, so a Cinder-only defence does nothing; Soulfire still burns
 * it. It regrows what it loses, faster with rank; from Veteran its hide hardens against whichever damage type the
 * level has dealt most, and from Elite it sheds its wounds once badly hurt.
 */
final class SalamanderEnemy {

    private SalamanderEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("salamander", "Salamander", 94, 3, 1.28f, BodyArchetype.SALAMANDER)
                        .withMovement(new StrideMovement((float) Math.toRadians(25.0), 8))
                        .withDescription("A fire-proof walker that slowly regrows what it loses.")
                        .withIdentifiedTraits(List.of(
                                IdentifiedTrait.anonymous(new ImmunityTrait(EffectKind.BURN)),
                                regrowth(1f))), RankGrowth.stepped(164, 501, 1259, 3132).paying(5, 8, 15, 25))
                .thenAt(Rank.VETERAN, e -> e.withAdditionalTraits(List.of(regrowth(2f), hide(25f))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(10f, 10f, 0f, 0f, 10f, 10f, 15f)
                        .withAdditionalTraits(List.of(regrowth(4f), hide(50f)))
                        .withAdditionalAbilities(List.of(shed(4f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(15f, 15f, 0f, 0f, 20f, 10f, 25f)
                        .withAdditionalTraits(List.of(regrowth(8f), hide(67f)))
                        .withAdditionalAbilities(List.of(shed(8f))))
                .build());
    }

    /** A hide that hardens against whichever damage type the level has dealt most. */
    private static IdentifiedTrait hide(float fullArmor) {
        return IdentifiedTrait.named("hide", AdaptiveResist.againstDominant(fullArmor));
    }

    private static IdentifiedTrait regrowth(float pointsPerSecond) {
        return IdentifiedTrait.named("regrowth", new RegenerationTrait(pointsPerSecond));
    }

    private static IdentifiedAbility shed(float perTick) {
        return IdentifiedAbility.named("shed", new Ability(AbilityTerm.SHED_SKIN, new HealthThresholdTrigger(0.3f),
                new ApplyEffectAction(new HealTemplate(perTick, 60), new SelfTarget())));
    }
}
