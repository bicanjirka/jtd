package td.balance;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;
import td.damage.DamageMix;
import td.effect.EffectKind;
import td.effect.InvisibleTemplate;
import td.enemy.Ability;
import td.enemy.AbilityTerm;
import td.enemy.ApplyEffectAction;
import td.enemy.EnemyDefinition;
import td.enemy.ImmunityTrait;
import td.enemy.OnFirstDamageTakenTrigger;
import td.enemy.SelfTarget;
import td.fixtures.EnemyFixtures;
import td.level.LevelDefinition;
import td.stat.DisruptionAura;
import td.wave.Point;
import td.wave.WaveDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PowerBenchTest {

    private static final EnemyDefinition PLAIN = EnemyFixtures.simpleDefinition("plain");
    private static final EnemyDefinition FIREPROOF = EnemyFixtures.simpleDefinition("unburnt")
            .withTraits(List.of(new ImmunityTrait(EffectKind.BURN)));
    private static final EnemyDefinition PLATED = EnemyFixtures.simpleDefinition("shelled")
            .withPlating(ProbeRig.BLAST_HIT_POINTS);
    private static final EnemyDefinition HIDER = EnemyFixtures.simpleDefinition("hider")
            .withAbilities(List.of(new Ability(AbilityTerm.FADE, new OnFirstDamageTakenTrigger(),
                    new ApplyEffectAction(new InvisibleTemplate(100_000), new SelfTarget()))));
    private static final EnemyDefinition JAMMER = EnemyFixtures.simpleDefinition("jam")
            .withDisruption(new DisruptionAura(1_000f, 0.5f, 0f));
    private static final LevelDefinition CONTENT = LevelDefinition.unsmoothed("Bench Test", "", 3, 3,
            List.of(new Point(-1, 1), new Point(3, 1)), List.of(new WaveDefinition("plain")), 100, 5)
            .withCustomEnemies(List.of(PLAIN, FIREPROOF, PLATED, HIDER, JAMMER));

    @Test
    void nothingLandedLetsTheRoundThrough() {
        PowerBench.Outcome outcome = PowerBench.run(PowerBench.lane(CONTENT, "plain"), ProbeRig.Loadout.none(),
                DamageMix.none(), 1L);

        assertThat(outcome.leaked()).isTrue();
    }

    @Test
    void twiceTheHealthTakesAboutTwiceTheShot() {
        double one = shotFor("plain");
        double two = PowerBench.threshold(lane("plain", PLAIN.baseHealth() * 2, PLAIN),
                scale -> ProbeRig.Loadout.only(PowerProbe.SHOT, scale), 32, DamageMix.none());

        assertThat(two / one).isBetween(1.6, 2.4);
    }

    @Test
    void noBurnHoldsWhatIsImmuneToIt() {
        double burn = PowerBench.threshold(PowerBench.lane(CONTENT, "unburnt"),
                scale -> ProbeRig.Loadout.only(PowerProbe.BURN, scale), 32, DamageMix.none());

        assertThat(burn).isInfinite();
    }

    @Test
    void platingAsThickAsABlastHitStopsTheBlast() {
        double blast = PowerBench.threshold(PowerBench.lane(CONTENT, "shelled"),
                scale -> ProbeRig.Loadout.only(PowerProbe.BLAST, scale), 32, DamageMix.none());

        assertThat(blast).isInfinite();
    }

    @Test
    void anEnemyThatHidesTakesNoMoreShotThanAPlainOneOnceRevealed() {
        double hidden = shotFor("hider");
        double revealed = PowerBench.threshold(PowerBench.lane(CONTENT, "hider"),
                scale -> ProbeRig.Loadout.only(PowerProbe.SHOT, scale).revealing(), 32, DamageMix.none());

        assertThat(hidden).isGreaterThan(5 * shotFor("plain"));
        assertThat(revealed).isCloseTo(shotFor("plain"), Percentage.withPercentage(10));
    }

    @Test
    void anEnemyThatSlowsTheTowersBesideTheLaneTakesMoreOfTheRig() {
        assertThat(shotFor("jam") / shotFor("plain")).isBetween(1.6, 2.4);
    }

    private static double shotFor(String kind) {
        return PowerBench.threshold(PowerBench.lane(CONTENT, kind),
                scale -> ProbeRig.Loadout.only(PowerProbe.SHOT, scale), 32, DamageMix.none());
    }

    private static LevelDefinition lane(String kind, int health, EnemyDefinition from) {
        EnemyDefinition tougher = from.withId("tougher").withHealthAndPrice(health, from.price());
        return PowerBench.lane(CONTENT.withCustomEnemies(List.of(tougher)), "tougher");
    }
}
