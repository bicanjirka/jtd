package td.ui;

import org.junit.jupiter.api.Test;
import td.economy.EconomyState;
import td.economy.StatusTerm;
import td.wave.WaveProgress;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PanelHeaderTest {

    private final PanelHeader header = new PanelHeader("Tower Defense v1");

    private static WaveProgress progress(int index, int count) {
        return new WaveProgress(index, count, List.of(), List.of());
    }

    @Test
    void theHeaderWritesTheLevelsNumbersBesideTheirIcons() {
        this.header.show(progress(7, 18), false, false, new EconomyState(4964, 1240, 5));

        assertThat(this.header.icon(StatusTerm.WAVE).readout().value(StatusTerm.WAVE)).isEqualTo("7/18");
        assertThat(this.header.icon(StatusTerm.LIVES).readout().value(StatusTerm.LIVES)).isEqualTo("5");
        assertThat(this.header.icon(StatusTerm.SCORE).readout().value(StatusTerm.SCORE)).isEqualTo("1240");
        assertThat(this.header.icon(StatusTerm.CASH).readout().value(StatusTerm.CASH)).isEqualTo("4964");
    }

    @Test
    void aLifeLostFlashesTheLivesIconAndNothingElse() {
        this.header.show(progress(3, 18), false, false, new EconomyState(100, 0, 5));

        this.header.show(progress(3, 18), false, false, new EconomyState(140, 40, 4));

        assertThat(this.header.icon(StatusTerm.LIVES).flash()).isPositive();
        assertThat(this.header.icon(StatusTerm.WAVE).flash()).isZero();
        assertThat(this.header.icon(StatusTerm.CASH).flash()).isZero();
        assertThat(this.header.icon(StatusTerm.SCORE).flash()).isZero();
    }

    @Test
    void aRoundStartingFlashesTheWaveIcon() {
        this.header.show(progress(3, 18), true, false, new EconomyState(100, 0, 5));

        this.header.show(progress(4, 18), false, false, new EconomyState(100, 0, 5));

        assertThat(this.header.icon(StatusTerm.WAVE).flash()).isPositive();
    }

    @Test
    void theFirstNumbersOfALevelAreABaselineAndFlashNothing() {
        this.header.show(progress(9, 18), false, false, new EconomyState(100, 0, 2));
        this.header.reset();

        this.header.show(progress(0, 12), true, false, new EconomyState(100, 0, 1));

        assertThat(this.header.icon(StatusTerm.LIVES).flash()).isZero();
        assertThat(this.header.icon(StatusTerm.WAVE).flash()).isZero();
    }

    @Test
    void theSpeedButtonsReportTheirClicks() {
        AtomicInteger clicks = new AtomicInteger();
        this.header.onPlay(clicks::incrementAndGet);
        this.header.onFast(() -> clicks.addAndGet(10));
        this.header.onSuperFast(() -> clicks.addAndGet(100));

        PanelHeaderTest.buttons(this.header).stream().filter(b -> !"❚❚".equals(b.getText())).forEach(HudButton::doClick);

        assertThat(clicks.get()).isEqualTo(111);
    }

    @Test
    void playAndPauseShareOneSlotAndTheLoopDecidesWhichShows() {
        this.header.setPlaying(true);

        assertThat(PanelHeaderTest.buttons(this.header).stream().filter(b -> "❚❚".equals(b.getText()))
                .allMatch(HudButton::isVisible)).isTrue();
        assertThat(PanelHeaderTest.buttons(this.header).stream().filter(b -> "▶".equals(b.getText()))
                .noneMatch(HudButton::isVisible)).isTrue();
    }

    @Test
    void theLivesAreAsWideAsTheirHeartsNeedAndNeverNarrowerThanAScore() {
        int score = this.header.icon(StatusTerm.SCORE).getPreferredSize().width;

        this.header.show(progress(1, 18), false, false, new EconomyState(0, 0, 1));
        int one = this.header.icon(StatusTerm.LIVES).getPreferredSize().width;
        this.header.show(progress(1, 18), false, false, new EconomyState(0, 0, 5));
        int five = this.header.icon(StatusTerm.LIVES).getPreferredSize().width;
        this.header.show(progress(1, 18), false, false, new EconomyState(0, 0, 10));
        int ten = this.header.icon(StatusTerm.LIVES).getPreferredSize().width;
        this.header.show(progress(1, 18), false, false, new EconomyState(0, 0, 11));
        int eleven = this.header.icon(StatusTerm.LIVES).getPreferredSize().width;

        assertThat(one).isEqualTo(score);
        assertThat(five).isGreaterThan(score);
        assertThat(ten).isGreaterThan(five);
        assertThat(eleven).isEqualTo(score);
    }

    private static List<HudButton> buttons(java.awt.Container root) {
        List<HudButton> found = new java.util.ArrayList<>();
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof HudButton button) {
                found.add(button);
            } else if (child instanceof java.awt.Container container) {
                found.addAll(buttons(container));
            }
        }
        return found;
    }
}
