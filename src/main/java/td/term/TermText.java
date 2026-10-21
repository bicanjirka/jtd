package td.term;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Words that may mention terms: plain runs and mentions in reading order. {@link #plus} joins two texts and
 * {@link #empty()} is its identity. Texts are written lower-case first with no full stop so they compose;
 * {@link #asSentence()} finishes one for display.
 */
public record TermText(List<Span> spans) {

    private static final TermText EMPTY = new TermText(List.of());
    private static final String SLOT = "{}";

    public TermText {
        spans = List.copyOf(spans);
    }

    /** One run of a text. */
    public sealed interface Span permits Plain, Mention {
    }

    /** Words that name nothing. */
    public record Plain(String text) implements Span {
    }

    /**
     * A term named in the text, shown as its icon and title.
     *
     * @param detail what the term means here, when that is more than its own sentence
     */
    public record Mention(Term term, Optional<TermText> detail) implements Span {

        /** What a tooltip on this mention says. */
        public TermText meaning() {
            return this.detail.orElseGet(this.term::describe);
        }
    }

    public static TermText empty() {
        return EMPTY;
    }

    public static TermText of(String plain) {
        return plain.isEmpty() ? EMPTY : new TermText(List.of(new Plain(plain)));
    }

    public static TermText of(Term term) {
        return new TermText(List.of(new Mention(term, Optional.empty())));
    }

    /** {@code term}, whose tooltip here says {@code detail} instead of the term's own sentence. */
    public static TermText of(Term term, TermText detail) {
        return new TermText(List.of(new Mention(term, Optional.of(detail))));
    }

    /**
     * {@code template} with each {@code "{}"} replaced by the next of {@code terms} as a mention.
     *
     * @throws IllegalArgumentException when the slots and the terms don't pair up
     */
    public static TermText format(String template, Term... terms) {
        TermText text = EMPTY;
        int from = 0;
        for (Term term : terms) {
            int slot = template.indexOf(SLOT, from);
            if (slot < 0) {
                throw new IllegalArgumentException("More terms than slots in: " + template);
            }
            text = text.plus(template.substring(from, slot)).plus(term);
            from = slot + SLOT.length();
        }
        if (template.indexOf(SLOT, from) >= 0) {
            throw new IllegalArgumentException("More slots than terms in: " + template);
        }
        return text.plus(template.substring(from));
    }

    /** {@code parts} with {@code separator} between each two; empty parts are left out. */
    public static TermText join(String separator, List<TermText> parts) {
        TermText text = EMPTY;
        for (TermText part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            text = text.isEmpty() ? part : text.plus(separator).plus(part);
        }
        return text;
    }

    public TermText plus(TermText more) {
        if (more.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return more;
        }
        List<Span> joined = new ArrayList<>(this.spans);
        Optional<String> last = plainText(joined.getLast());
        Optional<String> first = plainText(more.spans.getFirst());
        if (last.isPresent() && first.isPresent()) {
            joined.set(joined.size() - 1, new Plain(last.get() + first.get()));
            joined.addAll(more.spans.subList(1, more.spans.size()));
        } else {
            joined.addAll(more.spans);
        }
        return new TermText(joined);
    }

    public TermText plus(String plain) {
        return this.plus(of(plain));
    }

    public TermText plus(Term term) {
        return this.plus(of(term));
    }

    public boolean isEmpty() {
        return this.spans.isEmpty();
    }

    /** The text with each mention as its title: for tests, logs and button faces. */
    public String plain() {
        StringBuilder text = new StringBuilder();
        for (Span span : this.spans) {
            text.append(switch (span) {
                case Plain plain -> plain.text();
                case Mention mention -> mention.term().title();
            });
        }
        return text.toString();
    }

    /** Every term mentioned, in order. */
    public List<Term> mentions() {
        return this.spans.stream().flatMap(span -> switch (span) {
            case Mention mention -> Stream.of(mention.term());
            case Plain _ -> Stream.<Term>empty();
        }).toList();
    }

    /** The first letter upper-case. */
    public TermText capitalized() {
        Optional<String> first = this.isEmpty() ? Optional.empty() : plainText(this.spans.getFirst());
        if (first.isEmpty() || first.get().isEmpty()) {
            return this;
        }
        String text = first.get();
        List<Span> spans = new ArrayList<>(this.spans);
        spans.set(0, new Plain(text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1)));
        return new TermText(spans);
    }

    /** A plain span's words; empty for a mention. */
    private static Optional<String> plainText(Span span) {
        return switch (span) {
            case Plain plain -> Optional.of(plain.text());
            case Mention _ -> Optional.empty();
        };
    }

    /** Capitalized, ending in a full stop unless it already ends a sentence. */
    public TermText asSentence() {
        TermText capitalized = this.capitalized();
        if (this.isEmpty()) {
            return capitalized;
        }
        String plain = this.plain();
        char last = plain.charAt(plain.length() - 1);
        return last == '.' || last == '!' || last == '?' ? capitalized : capitalized.plus(".");
    }
}
