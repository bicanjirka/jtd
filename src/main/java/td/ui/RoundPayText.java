package td.ui;

import td.economy.StatusTerm;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Row;
import td.wave.RoundPay;

import java.util.ArrayList;
import java.util.List;

/** What a round's pay says, however far the round has got. Pure. */
final class RoundPayText {

    private RoundPayText() {
    }

    /**
     * What a round's heading marks: its bounty, then the clear bonus it offers until it is over and then what it paid,
     * such as {@code 24+5}; {@code 24} with no bonus, and empty for a round that pays nothing.
     */
    static String mark(RoundPay pay, RoundStrip.State state) {
        int bonus = state.isOver() ? pay.bonusPaid() : pay.clearBonus();
        if (pay.bounty() == 0 && bonus == 0) {
            return "";
        }
        return bonus > 0 ? pay.bounty() + "+" + bonus : Integer.toString(pay.bounty());
    }

    /** The bounty, then the clear bonus; a round that pays neither has no lines. */
    static List<SheetLine> lines(RoundPay pay, RoundStrip.State state) {
        List<SheetLine> lines = new ArrayList<>();
        if (pay.bounty() > 0) {
            lines.add(line("+" + pay.bounty() + " bounty"));
        }
        if (pay.clearBonus() > 0) {
            String bonus = "+" + pay.clearBonus();
            switch (state) {
                case AHEAD, CURRENT -> lines.add(line(bonus + " if no leaks"));
                case LEAKED -> lines.add(line(bonus + " lost"));
                case CLEARED -> {
                    if (pay.bonusPaid() > 0) {
                        lines.add(line("+" + pay.bonusPaid() + " paid"));
                    }
                }
            }
        }
        return lines;
    }

    private static Row line(String phrase) {
        return Row.faced(TermLooks.faceOf(StatusTerm.CASH), phrase, "");
    }
}
