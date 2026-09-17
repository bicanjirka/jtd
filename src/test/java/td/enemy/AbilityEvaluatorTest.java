package td.enemy;

import org.junit.jupiter.api.Test;
import td.effect.EffectTemplate;
import td.effect.ShieldTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Headless proof of every {@link AbilityTrigger} kind's firing rule and both {@link AbilityAction} shapes.
 */
class AbilityEvaluatorTest {

    private static List<Boolean> fireSequence(AbilityTrigger trigger, AbilityState state, AbilityContext context, int ticks) {
        List<Boolean> results = new ArrayList<>();
        for (int i = 0; i < ticks; i++) {
            results.add(AbilityEvaluator.shouldFire(trigger, state, context));
        }
        return results;
    }

    @Test
    void aPeriodicTriggerFiresEveryIntervalTicksWhileAlive() {
        AbilityTrigger trigger = new PeriodicTrigger(3);
        AbilityState state = AbilityState.forTrigger(trigger);
        FakeAbilityContext context = new FakeAbilityContext();

        assertThat(fireSequence(trigger, state, context, 6)).containsExactly(false, false, true, false, false, true);
    }

    @Test
    void aOnceTriggerFiresExactlyOnceAfterItsDelayThenNeverAgain() {
        AbilityTrigger trigger = new OnceTrigger(2);
        AbilityState state = AbilityState.forTrigger(trigger);
        FakeAbilityContext context = new FakeAbilityContext();

        assertThat(fireSequence(trigger, state, context, 5)).containsExactly(false, true, false, false, false);
    }

    @Test
    void aHealthThresholdTriggerFiresExactlyOnceWhenHealthFirstCrossesTheFraction() {
        AbilityTrigger trigger = new HealthThresholdTrigger(0.5f);
        AbilityState state = AbilityState.forTrigger(trigger);
        FakeAbilityContext context = new FakeAbilityContext();
        context.setHealthFraction(0.8f);

        assertThat(AbilityEvaluator.shouldFire(trigger, state, context)).isFalse();

        context.setHealthFraction(0.4f);
        assertThat(AbilityEvaluator.shouldFire(trigger, state, context)).isTrue();
        // still below the threshold, but already fired once - must not fire again every tick
        assertThat(AbilityEvaluator.shouldFire(trigger, state, context)).isFalse();
    }

    @Test
    void anOnDeathTriggerFiresOnlyOnTheTickDeathIsObserved() {
        AbilityTrigger trigger = new OnDeathTrigger();
        AbilityState state = AbilityState.forTrigger(trigger);
        FakeAbilityContext context = new FakeAbilityContext();

        assertThat(AbilityEvaluator.shouldFire(trigger, state, context)).isFalse();

        context.setJustDied(true);
        assertThat(AbilityEvaluator.shouldFire(trigger, state, context)).isTrue();
        assertThat(AbilityEvaluator.shouldFire(trigger, state, context)).isFalse();
    }

    @Test
    void aTimeSinceLastHitTriggerFiresAfterItsWindowElapsesAndCanFireAgainAfterAHitResetsIt() {
        AbilityTrigger trigger = new TimeSinceLastHitTrigger(5);
        AbilityState state = AbilityState.forTrigger(trigger);
        FakeAbilityContext context = new FakeAbilityContext();
        context.setTicksSinceLastHit(4);

        assertThat(AbilityEvaluator.shouldFire(trigger, state, context)).isFalse();

        context.setTicksSinceLastHit(5);
        assertThat(AbilityEvaluator.shouldFire(trigger, state, context)).isTrue();
        // still idle past the window, but already fired for this idle stretch
        assertThat(AbilityEvaluator.shouldFire(trigger, state, context)).isFalse();

        context.setTicksSinceLastHit(0); // a hit landed, resetting the window
        assertThat(AbilityEvaluator.shouldFire(trigger, state, context)).isFalse();

        context.setTicksSinceLastHit(5); // idle long enough again
        assertThat(AbilityEvaluator.shouldFire(trigger, state, context)).isTrue();
    }

    @Test
    void executingAnApplyEffectActionAppliesItsTemplateToItsTarget() {
        FakeAbilityContext context = new FakeAbilityContext();
        EffectTemplate template = new ShieldTemplate(0.5f, 10);
        EffectTarget target = new SelfTarget();

        AbilityEvaluator.execute(new ApplyEffectAction(template, target), context);

        assertThat(context.appliedEffects).containsExactly(new FakeAbilityContext.AppliedEffect(template, target));
    }

    @Test
    void executingASpawnEnemiesActionSpawnsWithItsParameters() {
        FakeAbilityContext context = new FakeAbilityContext();

        AbilityEvaluator.execute(new SpawnEnemiesAction("wardenEgg", 1, true), context);

        assertThat(context.spawnCalls).containsExactly(new FakeAbilityContext.SpawnCall("wardenEgg", 1, true));
    }
}
