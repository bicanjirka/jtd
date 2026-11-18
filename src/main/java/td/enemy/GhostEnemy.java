package td.enemy;

import td.damage.DamageType;
import td.effect.EffectKind;
import td.effect.InvisibleTemplate;
import td.effect.ShieldTemplate;

import java.util.List;

/**
 * The Ghost ({@code ghost}): half there, so physical hits pass partly through; vanishes from towers the first time
 * it is hit, for longer from Veteran. From Elite it is hard to hit, nothing freezes it, it thins further against
 * physical hits the more of them the level deals, it shrouds its allies, and once nearly gone it thickens into a shield
 * that holds until broken; at Boss it fades out on its own now and then.
 */
final class GhostEnemy {

    /** How often an Elite Ghost shrouds its allies, and how long each shroud lasts. */
    static final int SHROUD_TICKS = 60;

    private GhostEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("ghost", "Ghost", 120, 3, 1.28f, BodyArchetype.GHOST)
                                // A sheet that floats rather than walks.
                                .withMovement(new SwingMovement((float) Math.toRadians(12.0), 45, SwingCurve.SMOOTH))
                                .withDescription("Half there: hard to hit and hard to see, but area damage finds it.")
                                .withArmor(20f)
                                .withIdentifiedAbilities(List.of(fade(200))),
                        RankGrowth.stepped(209, 653, 1500, 3000).paying(5, 8, 11, 14))
                .thenAt(Rank.VETERAN, e -> e
                        .withAdditionalAbilities(List.of(fade(300))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(30f, 10f, 0f, 0f, 15f, 0f, 20f, 15f)
                        .withAdditionalTraits(List.of(thinning(), unfreezable()))
                        .withAdditionalAbilities(List.of(shroud(), ectoplasm(350f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(70f, 35f, 0f, 0f, 20f, 0f, 40f, 40f)
                        .withAdditionalAbilities(List.of(ectoplasm(900f), phase())))
                .build());
    }

    private static IdentifiedTrait unfreezable() {
        return IdentifiedTrait.named("unfreezable", new ImmunityTrait(EffectKind.FREEZE));
    }

    /** Always a little armor against physical hits, far more where the level leans on them. */
    private static IdentifiedTrait thinning() {
        return IdentifiedTrait.named("thinning", AdaptiveResist.against(DamageType.PHYSICAL, 10, 60));
    }

    /** The shield it gathers once nearly gone, so the last of it has to be broken twice. */
    private static IdentifiedAbility ectoplasm(float points) {
        return IdentifiedAbility.named("ectoplasm", new Ability(AbilityTerm.ECTOPLASM, new HealthThresholdTrigger(0.15f),
                new ApplyEffectAction(ShieldTemplate.untilBroken(points), new SelfTarget())));
    }

    private static IdentifiedAbility phase() {
        return IdentifiedAbility.anonymous(new Ability(AbilityTerm.PHASE, new PeriodicTrigger(200),
                new ApplyEffectAction(new InvisibleTemplate(60), new SelfTarget())));
    }

    private static IdentifiedAbility shroud() {
        return IdentifiedAbility.anonymous(new Ability(AbilityTerm.SHROUD,
                // A tick longer than the interval, so an ally that stays in radius never flickers
                // visible, while a reveal holds until the next cast.
                new PeriodicTrigger(SHROUD_TICKS),
                new ApplyEffectAction(new InvisibleTemplate(SHROUD_TICKS + 1), new RadiusTarget(100f))));
    }

    /** Its vanishing act; a hit that also freezes suppresses the cast outright, since a frozen mob cannot cast. */
    private static IdentifiedAbility fade(int durationTicks) {
        return IdentifiedAbility.named("fade", new Ability(AbilityTerm.FADE, new OnFirstDamageTakenTrigger(),
                new ApplyEffectAction(new InvisibleTemplate(durationTicks), new SelfTarget())));
    }
}
