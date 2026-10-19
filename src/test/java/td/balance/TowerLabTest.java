package td.balance;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import td.enemy.Rank;
import td.level.LevelDefinition;
import td.tower.TowerFactory;
import td.wave.Point;
import td.wave.WaveDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TowerLabTest {

    private static TowerLab lab;

    @BeforeAll
    static void measureASmallLevel() {
        LevelDefinition level = LevelDefinition.unsmoothed("Lab Test", "", 7, 5,
                List.of(new Point(-1, 2), new Point(7, 2)), List.of(new WaveDefinition("salamander", Rank.GRUNT)),
                100, 5);
        lab = TowerLab.of(BoardSurvey.of(level));
    }

    @Test
    void aTowerOutOfReachOfThePathIsWorthNothingThere() {
        double far = lab.cellValue(TowerFactory.Type.PULSE, new Point(0, 0));
        double beside = lab.cellValue(TowerFactory.Type.PULSE, new Point(3, 1));

        assertThat(far).isZero();
        assertThat(beside).isPositive();
    }

    @Test
    void aBurnImmuneKindShrugsOffTheScorcherButNotTheSniper() {
        double scorcher = lab.kindFactor(TowerFactory.Type.CINDER, "salamander", Rank.GRUNT);
        double sniper = lab.kindFactor(TowerFactory.Type.SNIPER, "salamander", Rank.GRUNT);

        assertThat(scorcher).isLessThan(0.1);
        assertThat(sniper).isGreaterThan(0.5);
    }

    @Test
    void aTowerThatOnlyHitsGivesItsNeighbourNothingWhileOneThatMarksGivesItMore() {
        double sniper = lab.supportGain(TowerFactory.Type.SNIPER, List.of());
        double markingRadar = lab.supportGain(TowerFactory.Type.SONAR, List.of("sonar.special.mark_on_sweep"));

        assertThat(sniper).isLessThan(0.05);
        assertThat(markingRadar).isGreaterThan(0.1);
    }

    @Test
    void wideBandShowsAHidingKindToTheSniperBesideItWhichLandsMoreOnItRevealed() {
        String ghost = TowerLab.kindKey("g", TowerLab.labRank());

        double bare = lab.revealShare(TowerFactory.Type.SONAR, List.of(), ghost);
        double wideBand = lab.revealShare(TowerFactory.Type.SONAR, List.of("sonar.special.wide_band"), ghost);

        assertThat(bare).isZero();
        assertThat(wideBand).isGreaterThan(0.3);
        assertThat(lab.revealedFactor(TowerFactory.Type.SNIPER, ghost))
                .isGreaterThan(lab.kindFactor(TowerFactory.Type.SNIPER, ghost));
    }

    @Test
    void theLevelsOwnKindsAreMeasuredAtTheRanksItSends() {
        assertThat(lab.levelRanks()).containsExactly(TowerLab.kindKey("salamander", Rank.GRUNT));
    }
}
