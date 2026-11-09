package td.level;

import td.effect.EffectKind;
import td.effect.SprintTemplate;
import td.enemy.Ability;
import td.enemy.AbilitySpawnShape;
import td.enemy.AbilityTerm;
import td.enemy.ApplyEffectAction;
import td.enemy.BodyArchetype;
import td.enemy.EnemyDefinition;
import td.enemy.HealthThresholdTrigger;
import td.enemy.HurtSpeedTrait;
import td.enemy.IdentifiedAbility;
import td.enemy.IdentifiedTrait;
import td.enemy.ImmunityTrait;
import td.enemy.KinFallenTrigger;
import td.enemy.LeaveZoneAction;
import td.enemy.OnDodgeTrigger;
import td.enemy.PathDirectionalMovement;
import td.enemy.PulseMovement;
import td.enemy.RankedEnemy;
import td.enemy.SelfTarget;
import td.enemy.SpawnEnemiesAction;
import td.stat.DisruptionAura;

import java.util.List;

/**
 * The storm's twins, a boss in two bodies on two lanes that ask for opposite damage: the Thunder, slow and plated
 * against physical hits, and the Lightning, quick and insulated against magic. Whichever falls first, the other calls
 * it back, reborn and stronger, unless it falls too before the count runs out: the fight is won by bringing both down
 * close together. A reborn twin has no bond, so the chain ends there. Every body is unique.
 */
final class StormTwinsEnemy {

    /** How long a twin may lie fallen before the other calls it back. */
    private static final int BOND_TICKS = 120;

    private StormTwinsEnemy() {
    }

    /** The reborn first, so each bond names a registered id. */
    static List<RankedEnemy> pair() {
        return List.of(RankedEnemy.unique(thunderReborn()), RankedEnemy.unique(lightningReborn()),
                RankedEnemy.unique(thunder()), RankedEnemy.unique(lightning()));
    }

    private static EnemyDefinition thunder() {
        return EnemyDefinition.of("thunder", "The Thunder", 4200, 40, 0.6f, BodyArchetype.THUNDER)
                .withDescription("The slow half of the storm. It shakes the towers near it, and it never comes alone.")
                .withMovement(new PulseMovement(0.06f, 40, 20))
                .withStats(35f, 0f, 6f, 0f, 60f, 0f, 0f)
                .withTenacity(100f)
                .withDisruption(new DisruptionAura(120f, 0.3f, 0.6f))
                .withIdentifiedTraits(List.of(IdentifiedTrait.anonymous(new ImmunityTrait(EffectKind.DAZED))))
                .withIdentifiedAbilities(List.of(
                        bond("lightning", "lightningReborn"),
                        IdentifiedAbility.anonymous(new Ability(AbilityTerm.THUNDERCLAP, new HealthThresholdTrigger(0.5f),
                                LeaveZoneAction.blindingField(2.5f, 0.35f)))));
    }

    private static EnemyDefinition lightning() {
        return EnemyDefinition.of("lightning", "The Lightning", 2400, 40, 1.6f, BodyArchetype.LIGHTNING)
                .withDescription("The quick half of the storm. Strike it where it will be, not where it is.")
                .withMovement(new PathDirectionalMovement())
                .withStats(0f, 40f, 0f, 5f, 0f, 30f, 0f)
                .withTenacity(100f)
                .withIdentifiedTraits(List.of(
                        IdentifiedTrait.anonymous(new HurtSpeedTrait(1.4f)),
                        IdentifiedTrait.anonymous(new ImmunityTrait(EffectKind.DAZED))))
                .withIdentifiedAbilities(List.of(
                        bond("thunder", "thunderReborn"),
                        IdentifiedAbility.anonymous(new Ability(AbilityTerm.SIDESTEP, new OnDodgeTrigger(),
                                new ApplyEffectAction(new SprintTemplate(20), new SelfTarget())))));
    }

    private static EnemyDefinition thunderReborn() {
        return EnemyDefinition.of("thunderReborn", "The Thunder Reborn", 2600, 0, 0.7f, BodyArchetype.THUNDER)
                .withDescription("Called back by the Lightning, louder than before.")
                .withMovement(new PulseMovement(0.1f, 30, 10))
                .withStats(50f, 0f, 8f, 0f, 60f, 0f, 0f)
                .withTenacity(100f)
                .withDisruption(new DisruptionAura(140f, 0.45f, 0.8f))
                .withIdentifiedTraits(List.of(IdentifiedTrait.anonymous(new ImmunityTrait(EffectKind.DAZED))));
    }

    private static EnemyDefinition lightningReborn() {
        return EnemyDefinition.of("lightningReborn", "The Lightning Reborn", 1500, 0, 2.0f, BodyArchetype.LIGHTNING)
                .withDescription("Called back by the Thunder, quicker than before.")
                .withMovement(new PathDirectionalMovement())
                .withStats(0f, 55f, 0f, 7f, 0f, 40f, 0f)
                .withTenacity(100f)
                .withIdentifiedTraits(List.of(
                        IdentifiedTrait.anonymous(new HurtSpeedTrait(1.5f)),
                        IdentifiedTrait.anonymous(new ImmunityTrait(EffectKind.DAZED))));
    }

    /** Calls {@code rebornId} back where this twin stands, worth no bounty, once its kin has been down long enough. */
    private static IdentifiedAbility bond(String kinId, String rebornId) {
        return IdentifiedAbility.anonymous(new Ability(AbilityTerm.BOND, new KinFallenTrigger(kinId, BOND_TICKS),
                new SpawnEnemiesAction(rebornId, new AbilitySpawnShape(1, 1f, 1f, 0f, 0.0), false)));
    }
}
