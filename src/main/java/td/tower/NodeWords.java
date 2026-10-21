package td.tower;

import td.term.TermText;
import td.tower.upgrade.UpgradeBonus;

import java.util.List;

/**
 * What one upgrade node says to the player: its name, its value rows, its sentences (its data's and its
 * mechanics'), and, once bought, the nodes it was chosen over.
 */
public record NodeWords(String name, List<UpgradeBonus> stats, List<TermText> sentences, List<String> chosenOver) {

    public NodeWords {
        stats = List.copyOf(stats);
        sentences = List.copyOf(sentences);
        chosenOver = List.copyOf(chosenOver);
    }

    public static NodeWords of(String name, List<UpgradeBonus> stats, List<TermText> sentences) {
        return new NodeWords(name, stats, sentences, List.of());
    }

    public NodeWords withChosenOver(List<String> names) {
        return new NodeWords(this.name, this.stats, this.sentences, names);
    }
}
