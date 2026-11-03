package td.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * Breaks text into lines no wider than a width, filling each line greedily and splitting a word that does not fit
 * at the last of its hyphenation points that still does, as a word processor's automatic hyphenation does.
 */
final class JustifiedLineBreaker {

    private static final char HYPHEN = '-';
    private static final String SPACE = " ";

    private final Hyphenator hyphenator;
    private final ToIntFunction<String> widthOf;

    /** @param widthOf a piece of text's width, in the unit the line width is given in */
    JustifiedLineBreaker(Hyphenator hyphenator, ToIntFunction<String> widthOf) {
        this.hyphenator = hyphenator;
        this.widthOf = widthOf;
    }

    /** One line per row, a newline in {@code text} starting a new paragraph. */
    List<TextLine> lines(String text, int width) {
        int space = this.widthOf.applyAsInt(SPACE);
        return Arrays.stream(text.split("\n", -1)).flatMap(paragraph -> this.paragraphLines(paragraph, width, space).stream()).toList();
    }

    private List<TextLine> paragraphLines(String paragraph, int width, int space) {
        Deque<Piece> pieces = new ArrayDeque<>(Arrays.stream(paragraph.strip().split("\\s+")).filter(w -> !w.isEmpty()).map(this::pieceOf).toList());
        List<TextLine> lines = new ArrayList<>();
        List<String> line = new ArrayList<>();
        int used = 0;
        while (!pieces.isEmpty()) {
            Piece piece = pieces.pop();
            int gap = line.isEmpty() ? 0 : space;
            int pieceWidth = this.widthOf.applyAsInt(piece.text());
            if (used + gap + pieceWidth <= width) {
                line.add(piece.text());
                used += gap + pieceWidth;
                continue;
            }
            Optional<Cut> cut = this.lastCutThatFits(piece, width - used - gap);
            if (cut.isPresent()) {
                line.add(piece.headBefore(cut.get()));
                pieces.push(piece.tailAfter(cut.get()));
            } else if (line.isEmpty()) {
                // Wider than a whole line and unbreakable: it overflows rather than vanishing.
                line.add(piece.text());
            } else {
                pieces.push(piece);
            }
            lines.add(TextLine.stretched(line));
            line = new ArrayList<>();
            used = 0;
        }
        if (line.isEmpty() && !lines.isEmpty()) {
            // The paragraph ended on a word that overflowed a line of its own, which is then its last line.
            lines.set(lines.size() - 1, TextLine.last(lines.getLast().words()));
        } else {
            lines.add(TextLine.last(line));
        }
        return lines;
    }

    private Optional<Cut> lastCutThatFits(Piece piece, int room) {
        return piece.cuts().reversed().stream().filter(cut -> this.widthOf.applyAsInt(piece.headBefore(cut)) <= room).findFirst();
    }

    /**
     * A whitespace-separated word with where it may split: inside each run of letters where the hyphenator allows,
     * and just after a hyphen of its own. Punctuation before or after the word stays with its piece; any other
     * character in it means it never splits.
     */
    private Piece pieceOf(String word) {
        int lead = 0;
        while (lead < word.length() && !Character.isLetter(word.charAt(lead))) {
            lead++;
        }
        int end = word.length();
        while (end > lead && !Character.isLetter(word.charAt(end - 1))) {
            end--;
        }
        String core = word.substring(lead, end);
        boolean splittable = core.chars().allMatch(c -> Character.isLetter(c) || c == HYPHEN) && !core.contains("--");
        if (!splittable) {
            return new Piece(word, List.of());
        }
        int offset = lead;
        List<Cut> cuts = new ArrayList<>();
        int runStart = 0;
        for (int i = 0; i <= core.length(); i++) {
            if (i == core.length() || core.charAt(i) == HYPHEN) {
                int start = runStart;
                this.hyphenator.breaksOf(core.substring(start, i)).forEach(k -> cuts.add(Cut.hyphenated(offset + start + k)));
                if (i < core.length()) {
                    cuts.add(Cut.atOwnHyphen(offset + i + 1));
                }
                runStart = i + 1;
            }
        }
        return new Piece(word, cuts);
    }

    /** Where a piece may split: before {@code at}, adding a hyphen unless the piece already has one there. */
    private record Cut(int at, boolean addsHyphen) {

        static Cut hyphenated(int at) {
            return new Cut(at, true);
        }

        static Cut atOwnHyphen(int at) {
            return new Cut(at, false);
        }
    }

    /** What is left of a word, with the cuts of the whole word that fall inside it, in increasing order. */
    private record Piece(String text, List<Cut> cuts) {

        String headBefore(Cut cut) {
            return this.text.substring(0, cut.at()) + (cut.addsHyphen() ? String.valueOf(HYPHEN) : "");
        }

        Piece tailAfter(Cut cut) {
            List<Cut> rest = this.cuts.stream().filter(c -> c.at() > cut.at()).map(c -> new Cut(c.at() - cut.at(), c.addsHyphen())).toList();
            return new Piece(this.text.substring(cut.at()), rest);
        }
    }

    /**
     * One row of text.
     *
     * @param stretched whether its words spread to the full width; a paragraph's last line keeps its natural
     *                  spacing
     */
    record TextLine(List<String> words, boolean stretched) {

        TextLine {
            words = List.copyOf(words);
        }

        static TextLine stretched(List<String> words) {
            return new TextLine(words, true);
        }

        static TextLine last(List<String> words) {
            return new TextLine(words, false);
        }
    }
}
