package td.enemy;

import td.effect.HealTemplate;

import java.util.List;

/** The Mender mob ({@code m}): heals the allies near it, and from Veteran itself when left alone. */
final class MenderEnemy {

    private MenderEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("m", "Mender mob", 60, 3, 1.28f, BodyArchetype.MENDER)
                        // "other" is load-bearing: a radius target excludes the caster.
                        .withDescription("Periodically restores health to every other ally near it - itself excluded.")
                        .withIdentifiedAbilities(List.of(heal(0.02f, 40))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(120, 8))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(240, 20)
                        .withDescription("Restores health to every other ally near it - itself excluded - and now "
                                + "quietly mends itself if left unattacked long enough.")
                        .withAdditionalAbilities(List.of(selfHeal(0.01f))))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(480, 50)
                        .withDescription("Its healing pulse now lasts twice as long, and a personal layer of armor, "
                                + "hardened against whichever damage type has hit hardest this level, joins its own "
                                + "quiet self-repair when left unattacked.")
                        .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(AdaptiveResist.againstDominant(0.6f))))
                        .withAdditionalAbilities(List.of(heal(0.02f, 80))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(960, 125)
                        .withDescription("Its healing pulse now restores double, for the same extended duration, and "
                                + "its own self-repair heals twice as strong too.")
                        .withAdditionalAbilities(List.of(heal(0.04f, 80), selfHeal(0.02f))))
                .build());
    }

    /** The pulse every other ally within reach gets each second. */
    private static IdentifiedAbility heal(float perTick, int durationTicks) {
        return IdentifiedAbility.named("heal", new Ability(new PeriodicTrigger(20),
                new ApplyEffectAction(new HealTemplate(perTick, durationTicks), new RadiusTarget(80f))));
    }

    private static IdentifiedAbility selfHeal(float perTick) {
        return IdentifiedAbility.named("selfHeal", new Ability(new TimeSinceLastHitTrigger(100),
                new ApplyEffectAction(new HealTemplate(perTick, 100), new SelfTarget())));
    }
}
