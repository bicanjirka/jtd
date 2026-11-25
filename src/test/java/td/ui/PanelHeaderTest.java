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
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class PanelHeaderTest {

    private final PanelHeader header = new PanelHeader();

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
    void onlyTheButtonOfTheSpeedShownIsSelectedAndNoneForARateBetweenPresets() {
        this.header.showSpeed(TickSpeed.FAST);

        assertThat(this.selectedTexts()).containsExactly("▶▶");

        this.header.showSpeed(TickSpeed.ofTicksPerSecond(30));

        assertThat(this.selectedTexts()).isEmpty();
    }

    @Test
    void theRestartButtonReportsItsClickOnlyOnceThereIsAWaveToRestart() {
        List<String> clicked = new ArrayList<>();
        this.header.onRestart(() -> clicked.add("restart"));
        AbstractButton restart = PanelHeaderTest.buttons(this.header).stream()
                .filter(button -> chipOf(button).equals(GameKey.RESTART.chip())).findFirst().orElseThrow();

        restart.doClick();
        this.header.setRestartWave(OptionalInt.of(7));
        restart.doClick();

        assertThat(clicked).containsExactly("restart");
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
    void whileLivesAreUnlimitedTheBarShowsInfinityInsteadOfHeartsThroughLaterNumbers() {
        this.header.show(progress(1, 18), false, LevelOutcome.PLAYING, new EconomyState(0, 0, 5));

        this.header.setCheats(true, false);
        this.header.show(progress(1, 18), false, LevelOutcome.PLAYING, new EconomyState(0, 0, 5));
        this.header.show(progress(2, 18), false, LevelOutcome.PLAYING, new EconomyState(0, 0, 5));

        StatusReadout readout = this.header.icon(StatusTerm.LIVES).readout();
        assertThat(readout.value(StatusTerm.LIVES)).isEqualTo("∞");
        assertThat(readout.hearts()).isEmpty();
    }

    @Test
    void turningUnlimitedLivesOffBringsTheHeartsBack() {
        this.header.setCheats(true, false);
        this.header.show(progress(1, 18), false, LevelOutcome.PLAYING, new EconomyState(0, 0, 5));

        this.header.setCheats(false, false);
        this.header.show(progress(1, 18), false, LevelOutcome.PLAYING, new EconomyState(0, 0, 5));

        assertThat(this.header.icon(StatusTerm.LIVES).readout().hearts()).hasValue(5);
    }

    @Test
    void duringFreeBuildTheCashIconGrowsForItsTagAndStillSaysTheRealCash() {
        this.header.show(progress(1, 18), false, LevelOutcome.PLAYING, new EconomyState(60, 0, 5));
        int plain = this.header.icon(StatusTerm.CASH).getPreferredSize().width;

        this.header.setCheats(false, true);
        this.header.show(progress(1, 18), false, LevelOutcome.PLAYING, new EconomyState(60, 0, 5));

        assertThat(this.header.icon(StatusTerm.CASH).getPreferredSize().width).isGreaterThan(plain);
        assertThat(this.header.icon(StatusTerm.CASH).readout().value(StatusTerm.CASH)).isEqualTo("60");
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

    private static void collect(Container parent, List<Component> into) {
        for (Component child : parent.getComponents()) {
            into.add(child);
            if (child instanceof Container inner) {
                collect(inner, into);
            }
        }
    }
}
