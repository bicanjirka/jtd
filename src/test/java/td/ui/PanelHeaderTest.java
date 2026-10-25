package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import td.TickSpeed;
import td.economy.EconomyState;
import td.economy.StatusTerm;
import td.enemy.Rank;
import td.level.LevelOutcome;
import td.wave.RoundSummary;
import td.wave.WaveProgress;

import javax.swing.AbstractButton;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class PanelHeaderTest {

    private final PanelHeader header = new PanelHeader("Tower Defense v1");

    private static WaveProgress progress(int index, int count) {
        return new WaveProgress(index, Collections.nCopies(count, new RoundSummary(Rank.GRUNT, false, false)), List.of(),
                List.of());
    }

    @Test
    void theHeaderWritesTheLevelsNumbersBesideTheirIcons() {
        this.header.show(progress(7, 18), false, LevelOutcome.PLAYING, new EconomyState(4964, 1240, 5));

        assertThat(this.header.icon(StatusTerm.WAVE).readout().value(StatusTerm.WAVE)).isEqualTo("7/18");
        assertThat(this.header.icon(StatusTerm.LIVES).readout().value(StatusTerm.LIVES)).isEqualTo("5");
        assertThat(this.header.icon(StatusTerm.SCORE).readout().value(StatusTerm.SCORE)).isEqualTo("1240");
        assertThat(this.header.icon(StatusTerm.CASH).readout().value(StatusTerm.CASH)).isEqualTo("4964");
    }

    @Test
    void aLifeLostFlashesTheLivesIconAndNothingElse() {
        this.header.show(progress(3, 18), false, LevelOutcome.PLAYING, new EconomyState(100, 0, 5));

        this.header.show(progress(3, 18), false, LevelOutcome.PLAYING, new EconomyState(140, 40, 4));

        assertThat(this.header.icon(StatusTerm.LIVES).flash()).isPositive();
        assertThat(this.header.icon(StatusTerm.WAVE).flash()).isZero();
        assertThat(this.header.icon(StatusTerm.CASH).flash()).isZero();
        assertThat(this.header.icon(StatusTerm.SCORE).flash()).isZero();
    }

    @Test
    void aRoundStartingFlashesTheWaveIcon() {
        this.header.show(progress(3, 18), true, LevelOutcome.PLAYING, new EconomyState(100, 0, 5));

        this.header.show(progress(4, 18), false, LevelOutcome.PLAYING, new EconomyState(100, 0, 5));

        assertThat(this.header.icon(StatusTerm.WAVE).flash()).isPositive();
    }

    @Test
    void theFirstNumbersOfALevelAreABaselineAndFlashNothing() {
        this.header.show(progress(9, 18), false, LevelOutcome.PLAYING, new EconomyState(100, 0, 2));
        this.header.reset();

        this.header.show(progress(0, 12), true, LevelOutcome.PLAYING, new EconomyState(100, 0, 1));

        assertThat(this.header.icon(StatusTerm.LIVES).flash()).isZero();
        assertThat(this.header.icon(StatusTerm.WAVE).flash()).isZero();
    }

    @Test
    void theSpeedButtonsAndTheMenuButtonReportTheirClicks() {
        List<String> clicked = new ArrayList<>();
        this.header.onPause(() -> clicked.add("pause"));
        this.header.onPlay(() -> clicked.add("play"));
        this.header.onFast(() -> clicked.add("fast"));
        this.header.onSuperFast(() -> clicked.add("super fast"));
        this.header.onMenu(() -> clicked.add("menu"));

        PanelHeaderTest.buttons(this.header).forEach(AbstractButton::doClick);

        assertThat(clicked).containsExactlyInAnyOrder("pause", "play", "fast", "super fast", "menu");
    }

    @Test
    void onlyTheButtonOfTheSpeedShownIsSelectedAndNoneForARateBetweenPresets() {
        this.header.showSpeed(TickSpeed.FAST);

        assertThat(this.selectedTexts()).containsExactly("▶▶");

        this.header.showSpeed(TickSpeed.ofTicksPerSecond(30));

        assertThat(this.selectedTexts()).isEmpty();
    }

    @Test
    void everySpeedButtonAndTheMenuButtonShowTheirKeysOnChips() {
        List<String> chips = PanelHeaderTest.buttons(this.header).stream().map(PanelHeaderTest::chipOf).toList();

        assertThat(chips).containsExactlyInAnyOrder(GameKey.PAUSE.chip(), GameKey.PLAY.chip(), GameKey.FAST.chip(),
                GameKey.SUPER_FAST.chip(), GameKey.MENU.chip());
    }

    private List<String> selectedTexts() {
        return PanelHeaderTest.buttons(this.header).stream().filter(AbstractButton::isSelected)
                .map(AbstractButton::getText).toList();
    }

    private static String chipOf(AbstractButton button) {
        return switch (button) {
            case HudButton push -> push.face().chip();
            case HudToggleButton toggle -> toggle.face().chip();
            default -> "";
        };
    }

    @Test
    void theLivesAreAsWideAsTheirHeartsNeedAndNeverNarrowerThanAScore() {
        int score = this.header.icon(StatusTerm.SCORE).getPreferredSize().width;

        this.header.show(progress(1, 18), false, LevelOutcome.PLAYING, new EconomyState(0, 0, 1));
        int one = this.header.icon(StatusTerm.LIVES).getPreferredSize().width;
        this.header.show(progress(1, 18), false, LevelOutcome.PLAYING, new EconomyState(0, 0, 5));
        int five = this.header.icon(StatusTerm.LIVES).getPreferredSize().width;
        this.header.show(progress(1, 18), false, LevelOutcome.PLAYING, new EconomyState(0, 0, 10));
        int ten = this.header.icon(StatusTerm.LIVES).getPreferredSize().width;
        this.header.show(progress(1, 18), false, LevelOutcome.PLAYING, new EconomyState(0, 0, 11));
        int eleven = this.header.icon(StatusTerm.LIVES).getPreferredSize().width;

        assertThat(one).isEqualTo(score);
        assertThat(five).isGreaterThan(score);
        assertThat(ten).isGreaterThan(five);
        assertThat(eleven).isEqualTo(score);
    }

    private static List<AbstractButton> buttons(Container root) {
        List<AbstractButton> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof AbstractButton button) {
                found.add(button);
            } else if (child instanceof Container container) {
                found.addAll(buttons(container));
            }
        }
        return found;
    }
}
