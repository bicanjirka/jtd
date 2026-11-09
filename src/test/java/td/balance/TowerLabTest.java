package td.balance;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.effect.InvisibleTemplate;
import td.enemy.Ability;
import td.enemy.AbilityTerm;
import td.enemy.ApplyEffectAction;
import td.enemy.EnemyDefinition;
import td.enemy.ImmunityTrait;
import td.enemy.OnFirstDamageTakenTrigger;
import td.enemy.Rank;
import td.enemy.SelfTarget;
import td.fixtures.EnemyFixtures;
import td.level.LevelDefinition;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.wave.Point;
import td.wave.WaveDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TowerLabTest {

    private static final EnemyDefinition FIREPROOF = EnemyFixtures.simpleDefinition("salamandrine")
            .withTraits(List.of(new ImmunityTrait(EffectKind.BURN)));
    private static final EnemyDefinition HIDER = EnemyFixtures.simpleDefinition("hider").withArmor(20f)
            .withAbilities(List.of(new Ability(AbilityTerm.FADE, new OnFirstDamageTakenTrigger(),
                    new ApplyEffectAction(new InvisibleTemplate(300), new SelfTarget()))));
    private static final LevelDefinition LEVEL = LevelDefinition.unsmoothed("Lab Test", "", 7, 5,
            List.of(new Point(-1, 2), new Point(7, 2)), List.of(new WaveDefinition("salamandrine")),
            100, 5).withCustomEnemies(List.of(FIREPROOF, HIDER));
    private static final LevelDefinition PROBES = TowerLab.probeLevel(LEVEL, TowerLab.PLAIN, -1);
    private static final Point BESIDE_THE_PATH = new Point(3, 1);
    private static final Point NEXT_TO_IT = new Point(2, 1);
    private static final String HIDER_KEY = TowerLab.kindKey("hider", TowerLab.labRank());
    /** Partners share targets, so a tower that does nothing for them still moves their damage by a little. */
    private static final double NEXT_TO_NOTHING = 0.05;

    @Test
    void aTowerOutOfReachOfThePathIsWorthNothingThere() {
        Map<Point, double[]> values = TowerLab.cellValuesOf(BoardSurvey.of(LEVEL), TowerFactory.Type.PULSE);

        assertThat(values.get(new Point(0, 0))[0]).isZero();
        assertThat(values.get(BESIDE_THE_PATH)[0]).isPositive();
    }

    @Test
    void aBurnImmuneKindShrugsOffTheScorcherButNotTheSniper() {
        String fireproof = TowerLab.kindKey("salamandrine", Rank.GRUNT);

        double scorcher = TowerLab.Duel.calibrated(LEVEL, TowerFactory.Type.CINDER, BESIDE_THE_PATH).kindFactor(fireproof);
        double sniper = TowerLab.Duel.calibrated(LEVEL, TowerFactory.Type.SNIPER, BESIDE_THE_PATH).kindFactor(fireproof);

        assertThat(scorcher).isLessThan(0.1);
        assertThat(sniper).isGreaterThan(0.5);
    }

    @Test
    void aTowerThatOnlyHitsGivesItsNeighbourNothing() {
        double gain = supportTrial(TowerFactory.Type.SNIPER).gainOwning(Optional.empty());

        assertThat(gain).isLessThan(NEXT_TO_NOTHING);
    }

    @Test
    void aTowerThatMarksGivesItsNeighbourMore() {
        UpgradeNode marking = TowerLab.nodesApplying(PROBES, TowerFactory.Type.SONAR, BESIDE_THE_PATH, EffectKind.MARKED)
                .getFirst();

        double gain = supportTrial(TowerFactory.Type.SONAR).gainOwning(Optional.of(marking));

        assertThat(gain).isGreaterThan(NEXT_TO_NOTHING);
    }

    @Test
    void aSupportGainAddsTheStepsOfTheNodesOwnedAndNeverFallsBelowNone() {
        TowerLab.Partnering partners = new TowerLab.Partnering(0, 0.25, Map.of("up", 0.5, "down", -1.0));

        assertThat(partners.supportGainOwning(List.of())).isEqualTo(0.25);
        assertThat(partners.supportGainOwning(List.of("up", "unmeasured"))).isEqualTo(0.75);
        assertThat(partners.supportGainOwning(List.of("down"))).isZero();
    }

    @Test
    void aNodeThatRevealsShowsAHidingKindToTheSniperBesideIt() {
        TowerLab.RevealTrial trial = TowerLab.RevealTrial.of(LEVEL, HIDER_KEY, NEXT_TO_IT, TowerFactory.Type.SONAR, BESIDE_THE_PATH);

        double shown = TowerLab.nodesApplying(PROBES, TowerFactory.Type.SONAR, BESIDE_THE_PATH, EffectKind.REVEALED).stream()
                .mapToDouble(trial::shareOwning).max().orElseThrow();

        assertThat(shown).isPositive();
    }

    @Test
    void aRevealShareIsTheMostAnyOwnedNodeShowsAndNothingBare() {
        TowerLab.Sight sight = new TowerLab.Sight(Map.of(), Map.of("wide", Map.of("kind", 0.75), "narrow", Map.of("kind", 0.25)));

        assertThat(sight.revealShareOwning(List.of(), "kind")).isZero();
        assertThat(sight.revealShareOwning(List.of("narrow", "wide"), "kind")).isEqualTo(0.75);
    }

    @Test
    void keepingAHidingKindRevealedChangesWhatASniperLandsOnIt() {
        TowerLab.Duel duel = TowerLab.Duel.calibrated(LEVEL, TowerFactory.Type.SNIPER, BESIDE_THE_PATH);

        assertThat(duel.revealedFactor(HIDER_KEY)).isNotEqualTo(duel.kindFactor(HIDER_KEY));
    }

    @Test
    void onlyAKindThatTurnsInvisibleCountsAsHiding() {
        List<String> keys = List.of(TowerLab.kindKey("salamandrine", Rank.GRUNT), HIDER_KEY);

        assertThat(TowerLab.hidingKinds(LEVEL, keys)).containsExactly(HIDER_KEY);
    }

    @Test
    void theLevelsOwnKindsAreProbedEvenThoseNoRoundSends() {
        assertThat(TowerLab.probedKinds(LEVEL)).contains("salamandrine", "hider");
    }

    @Test
    void theLevelsOwnKindsAreMeasuredAtTheRanksItSends() {
        assertThat(TowerLab.ranksIn(LEVEL)).containsExactly(TowerLab.kindKey("salamandrine", Rank.GRUNT));
    }

    private static TowerLab.SupportTrial supportTrial(TowerFactory.Type type) {
        return TowerLab.SupportTrial.around(BoardSurvey.of(LEVEL), PROBES, type, BESIDE_THE_PATH).orElseThrow();
    }
}
