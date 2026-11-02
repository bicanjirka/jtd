package td.level;

import td.effect.EffectKind;
import td.enemy.Ability;
import td.enemy.AbilitySpawnShape;
import td.enemy.AbilityTerm;
import td.enemy.BodyArchetype;
import td.enemy.EffectResistTrait;
import td.enemy.EnemyDefinition;
import td.enemy.HealthThresholdTrigger;
import td.enemy.HurtSpeedTrait;
import td.enemy.IdentifiedAbility;
import td.enemy.IdentifiedTrait;
import td.enemy.LeaveZoneAction;
import td.enemy.OnDeathTrigger;
import td.enemy.PeriodicTrigger;
import td.enemy.PulseMovement;
import td.enemy.Rank;
import td.enemy.RankedEnemy;
import td.enemy.SpawnEnemiesAction;
import td.enemy.SwingCurve;
import td.enemy.SwingMovement;

import java.util.List;

/**
 * The Iron Colossus: a boss in two bodies that ask for opposite damage. Its iron shell blunts physical hits, and
 * once badly hurt it melts down into a Molten Core that shrugs off magic, runs faster the more it is hurt, heals
 * the escorts behind it and bursts into embers when it dies. Both bodies are unique; the embers are not.
 */
final class ColossusEnemy {

    private ColossusEnemy() {
    }

    /** What the core bursts into: fast, fireproof and fragile, each one a life if it gets through. */
    static EnemyDefinition ember() {
        return EnemyDefinition.of("ember", "Ember", 160, 1, 1.7f, BodyArchetype.EMBER)
                .withDescription("A burning shard of a Molten Core, quick and fireproof.")
                .withAgility(20f)
                .withIdentifiedTraits(List.of(IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.BURN))));
    }

    /** The core first, so the shell's reforge names a registered id. */
    static List<RankedEnemy> chain() {
        return List.of(RankedEnemy.unique(moltenCore()), RankedEnemy.unique(colossus()));
    }

    private static EnemyDefinition colossus() {
        return EnemyDefinition.of("colossus", "The Iron Colossus", 5000, 40, 0.45f, BodyArchetype.COLOSSUS)
                .withDescription("A walking foundry in an iron shell. Break the shell and what is inside comes out running.")
                .withMovement(new SwingMovement((float) Math.toRadians(10.0), 60, SwingCurve.SMOOTH))
                .withStats(35f, 10f, 15f, 0f, 100f, 0f, 0f)
                .withIdentifiedTraits(List.of(
                        IdentifiedTrait.anonymous(EffectResistTrait.resisting(EffectKind.FREEZE, 0.5f))))
                .withIdentifiedAbilities(List.of(
                        IdentifiedAbility.anonymous(new Ability(AbilityTerm.WARD_MINE, new PeriodicTrigger(60),
                                LeaveZoneAction.wardMine(0.6f, 240, 150f))),
                        IdentifiedAbility.anonymous(new Ability(AbilityTerm.REINFORCE, new PeriodicTrigger(300),
                                new SpawnEnemiesAction("armored", 2, false).atRank(Rank.SOLDIER))),
                        IdentifiedAbility.anonymous(new Ability(AbilityTerm.REFORGE, new HealthThresholdTrigger(0.5f),
                                new SpawnEnemiesAction("moltenCore", 1, true)))));
    }

    private static EnemyDefinition moltenCore() {
        return EnemyDefinition.of("moltenCore", "The Molten Core", 3800, 40, 0.6f, BodyArchetype.MOLTEN_CORE)
                .withDescription("The Colossus's burning heart, loose at last.")
                .withMovement(new PulseMovement(0.12f, 30, 15))
                .withStats(0f, 60f, 0f, 4f, 60f, 0f, 0f)
                .withIdentifiedTraits(List.of(
                        IdentifiedTrait.anonymous(new HurtSpeedTrait(1.5f)),
                        IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.BURN)),
                        IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.FREEZE))))
                .withIdentifiedAbilities(List.of(
                        IdentifiedAbility.anonymous(new Ability(AbilityTerm.HEALING_POOL, new PeriodicTrigger(30),
                                LeaveZoneAction.healingGround(1f, 120, 1f))),
                        IdentifiedAbility.anonymous(new Ability(AbilityTerm.BROOD, new OnDeathTrigger(),
                                new SpawnEnemiesAction("ember", AbilitySpawnShape.brood(4, 1f, 1f), false)))));
    }
}
