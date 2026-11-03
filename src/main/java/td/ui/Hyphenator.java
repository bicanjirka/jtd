package td.ui;

import td.util.GameStartupException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Where a word may break across two lines, by Liang's hyphenation patterns (the ones TeX uses): each pattern is a
 * run of letters with a digit between some of them, odd allowing a break there and even forbidding it, and the
 * highest digit any pattern puts between two letters wins. A word listed as an exception takes its own breaks.
 */
final class Hyphenator {

    private static final char WORD_EDGE = '.';
    private static final String COMMENT = "%";
    /** The fewest letters a break may leave before or after it, so no line starts or ends on a lone letter. */
    private static final int LEFT_MIN = 2;
    private static final int RIGHT_MIN = 3;

    /** A pattern's letters to the digit before each of them and after the last. */
    private final Map<String, int[]> priorities;
    private final Map<String, List<Integer>> exceptions;
    private final int longestPattern;

    /**
     * @param patterns   Liang patterns such as {@code .ach4} or {@code 1ba}
     * @param exceptions words hyphenated by hand, such as {@code as-so-ciate}
     */
    Hyphenator(List<String> patterns, List<String> exceptions) {
        Map<String, int[]> parsed = new HashMap<>();
        for (String pattern : patterns) {
            StringBuilder letters = new StringBuilder();
            List<Integer> digits = new ArrayList<>();
            int pending = 0;
            for (char c : pattern.toCharArray()) {
                if (Character.isDigit(c)) {
                    pending = c - '0';
                } else {
                    letters.append(c);
                    digits.add(pending);
                    pending = 0;
                }
            }
            digits.add(pending);
            parsed.put(letters.toString(), digits.stream().mapToInt(Integer::intValue).toArray());
        }
        this.priorities = Map.copyOf(parsed);
        this.longestPattern = parsed.keySet().stream().mapToInt(String::length).max().orElse(0);

        Map<String, List<Integer>> words = new HashMap<>();
        for (String exception : exceptions) {
            List<Integer> breaks = new ArrayList<>();
            StringBuilder word = new StringBuilder();
            for (char c : exception.toCharArray()) {
                if (c == '-') {
                    breaks.add(word.length());
                } else {
                    word.append(c);
                }
            }
            words.put(word.toString(), List.copyOf(breaks));
        }
        this.exceptions = Map.copyOf(words);
    }

    /**
     * American English, from the patterns TeX ships. It reads them on every call, so a caller loads it once while
     * the game is built, where a missing file stops start-up cleanly.
     */
    static Hyphenator loadEnglish() {
        return new Hyphenator(readLines("hyph-en-us.pat.txt"), readLines("hyph-en-us.hyp.txt"));
    }

    /**
     * The places {@code word} may break, each the number of letters before the hyphen, in increasing order. A word
     * with anything but letters in it never breaks.
     */
    List<Integer> breaksOf(String word) {
        if (word.length() < LEFT_MIN + RIGHT_MIN || !word.chars().allMatch(Character::isLetter)) {
            return List.of();
        }
        String lower = word.toLowerCase(Locale.ROOT);
        if (lower.length() != word.length()) {
            // A letter that lowercases to two characters would shift every break past it.
            return List.of();
        }
        List<Integer> listed = this.exceptions.get(lower);
        if (listed != null) {
            return listed;
        }

        String edged = WORD_EDGE + lower + WORD_EDGE;
        int[] best = new int[edged.length() + 1];
        for (int start = 0; start < edged.length(); start++) {
            for (int end = start + 1; end <= Math.min(edged.length(), start + this.longestPattern); end++) {
                int[] digits = this.priorities.get(edged.substring(start, end));
                if (digits != null) {
                    for (int i = 0; i < digits.length; i++) {
                        best[start + i] = Math.max(best[start + i], digits[i]);
                    }
                }
            }
        }
        // A break after k letters of the word sits before edged's character k + 1, past the leading edge.
        return IntStream.rangeClosed(LEFT_MIN, word.length() - RIGHT_MIN)
                .filter(k -> best[k + 1] % 2 == 1)
                .boxed()
                .toList();
    }

    private static List<String> readLines(String resource) {
        try (InputStream in = Hyphenator.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new GameStartupException("Missing hyphenation resource " + resource);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return reader.lines().map(String::strip).filter(line -> !line.isEmpty() && !line.startsWith(COMMENT)).toList();
        } catch (IOException e) {
            throw new GameStartupException("Cannot read hyphenation resource " + resource, e);
        }
    }
}
