package td.tower;

import td.term.TermText;
import td.tower.upgrade.UpgradeBonus;

import java.util.List;

/**
 * What one upgrade node says to the player: its name, its value rows, its sentences (its data's and its
 * mechanics'), once bought the nodes it was chosen over, and what the line it deepens comes to in all with it.
 *
 * @param totals what the line it deepens gives in all with it owned, beyond what it adds itself; empty for most nodes
 */
public record NodeWords(String name, List<UpgradeBonus> stats, List<TermText> sentences, List<String> chosenOver,
                        List<TermText> totals) {

    public NodeWords {
        stats = List.copyOf(stats);
        sentences = List.copyOf(sentences);
        chosenOver = List.copyOf(chosenOver);
        totals = List.copyOf(totals);
    }

    public static NodeWords of(String name, List<UpgradeBonus> stats, List<TermText> sentences) {
        return new NodeWords(name, stats, sentences, List.of(), List.of());
    }

    public NodeWords withChosenOver(List<String> names) {
        return new NodeWords(this.name, this.stats, this.sentences, names, this.totals);
    }

    public NodeWords withTotals(List<TermText> lines) {
        return new NodeWords(this.name, this.stats, this.sentences, this.chosenOver, lines);
    }
}
