package td.level;

import td.enemy.Ability;
import td.enemy.AbilitySpawnShape;
import td.enemy.AbilityTerm;
import td.enemy.BodyArchetype;
import td.enemy.EnemyDefinition;
import td.enemy.HealthThresholdTrigger;
import td.enemy.IdentifiedAbility;
import td.enemy.PeriodicTrigger;
import td.enemy.Rank;
import td.enemy.RankedEnemy;
import td.enemy.RewindAction;
import td.enemy.SelfTarget;
import td.enemy.SpawnEnemiesAction;
import td.enemy.SwingCurve;
import td.enemy.SwingMovement;
import td.stat.DisruptionAura;

import java.util.List;

/**
 * The Keeper of Hours: a boss that undoes what was done to it. Every few seconds it turns its health back to what it
 * was a moment before, it calls Timekeepers that turn it back further, and once nearly dead it turns the glass one
 * last, long way. What stops a cast stops a rewind (a freeze, a daze, a silence); what keeps heals out keeps it out;
 * an Inversion turns each one into damage; or it has to fall faster than it turns back. Unique.
 */
final class KeeperEnemy {

    private KeeperEnemy() {
    }

    /** Registered after the Sandling and the Timekeeper, which it calls. */
    static RankedEnemy keeper() {
        return RankedEnemy.unique(EnemyDefinition.of("keeperOfHours", "The Keeper of Hours", 6000, 50, 0.45f, BodyArchetype.KEEPER)
                .withDescription("It has turned the glass before. It will turn it again - unless the sand is held still.")
                .withMovement(new SwingMovement((float) Math.toRadians(14.0), 50, SwingCurve.SMOOTH))
                .withStats(20f, 20f, 2f, 2f, 40f, 0f, 0f)
                .withDisruption(new DisruptionAura(110f, 0.25f, 0.4f))
                .withIdentifiedAbilities(List.of(
                        IdentifiedAbility.anonymous(new Ability(AbilityTerm.REWIND, new PeriodicTrigger(120),
                                new RewindAction(60, new SelfTarget()))),
                        IdentifiedAbility.anonymous(new Ability(AbilityTerm.REINFORCE, new PeriodicTrigger(200),
                                new SpawnEnemiesAction("timekeeper", 1, false).atRank(Rank.VETERAN))),
                        IdentifiedAbility.anonymous(new Ability(AbilityTerm.SWARM, new HealthThresholdTrigger(0.5f),
                                new SpawnEnemiesAction("sandling", AbilitySpawnShape.brood(6, 1f, 1f), false)
                                        .atRank(Rank.SOLDIER))),
                        IdentifiedAbility.anonymous(new Ability(AbilityTerm.REWIND, new HealthThresholdTrigger(0.15f),
                                new RewindAction(160, new SelfTarget()))))));
    }
}
