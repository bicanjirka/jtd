import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Fails the build on any violation of a greppable rule from CLAUDE.md.
 * <p>
 * Run by {@code mvn verify} through exec-maven-plugin, and standalone with
 * {@code java scripts/VerifyRules.java} (single-file source launcher - there is nothing to
 * compile and no classpath to set up). It is deliberately a Java program rather than a shell
 * script so that {@code mvn verify} behaves identically on Windows, macOS and Linux without
 * depending on a POSIX shell being on PATH.
 * <p>
 * Every rule here corresponds to a numbered rule in CLAUDE.md and states its section. A rule
 * belongs in this file only if a violation of it is mechanically detectable; rules that need
 * judgement stay prose in CLAUDE.md and are enforced in review.
 * <p>
 * Comment filtering is a heuristic: a line whose first non-whitespace characters are
 * {@code //}, {@code *} or <code>/*</code> is treated as a comment. That is exact for this
 * codebase's formatting and is why rules like no-instanceof can assert "returns only
 * comments" without hand-maintaining an exclusion list.
 */
public final class VerifyRules {

    private static final Path MAIN = Paths.get("src/main/java");
    private static final Path TEST = Paths.get("src/test/java");

    /** The domain packages CLAUDE.md 2.1 requires to be free of any Swing or AWT dependency. */
    private static final String[] HEADLESS_PACKAGES = {
            "board", "cell", "damage", "economy", "effect", "enemy",
            "level", "projectile", "tower", "util", "wave"
    };

    public static void main(String[] args) throws IOException {
        List<Check> rules = new ArrayList<>();

        rules.add(Rule.of("engine-is-headless", "CLAUDE.md 2.1",
                "domain packages must not import Swing or AWT",
                "^import (javax\\.swing|java\\.awt)", headlessRoots()).skippingComments());

        rules.add(Rule.of("render-has-no-awt", "CLAUDE.md 2.4",
                "td.ui.render describes frames; it must not import AWT",
                "^import java\\.awt", List.of(MAIN.resolve("td/ui/render"))).skippingComments());

        rules.add(Rule.of("no-instanceof", "CLAUDE.md 5 rule 11",
                "branch on domain type through a visitor, not instanceof",
                "\\binstanceof\\b", List.of(MAIN)).skippingComments());

        rules.add(Rule.of("no-null-return-in-engine", "CLAUDE.md 5",
                "engine and domain code models absence as a value, not null "
                        + "(td.ui frame builders are the one scoped exception)",
                "return null\\s*;", List.of(MAIN))
                .skippingComments()
                .excludingPath("td/ui/"));

        rules.add(Rule.of("no-wildcard-imports", "CLAUDE.md 6",
                "an explicit import once shadowed a real java.util.List / java.awt.List collision",
                "^import .*\\.\\*\\s*;", List.of(MAIN, TEST)).skippingComments());

        rules.add(Rule.of("no-lowercase-type-names", "CLAUDE.md 6",
                "types are UpperCamelCase",
                "^\\s*(public |protected |private )?(static )?(final )?"
                        + "(class|interface|enum|record)\\s+[a-z]", List.of(MAIN, TEST))
                .skippingComments());

        rules.add(Rule.of("no-lowercase-constants", "CLAUDE.md 6",
                "constants are UPPER_SNAKE_CASE (serialVersionUID is exempt - the JVM fixes that name)",
                "\\bstatic final +[A-Za-z_][\\w.<>\\[\\], ]*? +(?!serialVersionUID\\b)[a-z]\\w*\\s*=",
                List.of(MAIN, TEST)).skippingComments());

        rules.add(Rule.of("no-broad-catch", "CLAUDE.md 6",
                "catch Exception, never Throwable or Error - an Error means the JVM is in trouble",
                "catch\\s*\\(\\s*(Throwable|Error)\\b", List.of(MAIN, TEST)).skippingComments());

        rules.add(Rule.of("no-unchecked-catch", "CLAUDE.md 6",
                "test the condition instead of catching an unchecked exception as control flow "
                        + "(NumberFormatException around a parse is the one legitimate shape)",
                "catch\\s*\\(\\s*(NullPointerException|ClassCastException"
                        + "|(Array)?IndexOutOfBoundsException)\\b", List.of(MAIN, TEST))
                .skippingComments());

        rules.add(Rule.of("no-static-random", "CLAUDE.md 5",
                "randomness is injected through td.util.RandomSource, so a run can be reproduced",
                "Math\\.random\\s*\\(", List.of(MAIN, TEST)).skippingComments());

        rules.add(Rule.of("no-inline-todo", "CLAUDE.md 6",
                "gaps belong in TODO.md, which carries a Where and an Approach for each",
                "(?i)//\\s*TODO", List.of(MAIN, TEST)));

        rules.add(Rule.of("no-raw-junit-assert", "CLAUDE.md 7",
                "tests assert through AssertJ's assertThat, never JUnit's bare assertions",
                "org\\.junit\\.jupiter\\.api\\.Assertions"
                        + "|(?<![.\\w])assert(Equals|True|False|Null|NotNull|Same)\\s*\\(",
                List.of(TEST)).skippingComments());

        rules.add(Rule.of("no-etched-border", "CLAUDE.md 8",
                "borders come from td.ui.Hud; an etched border's shading is the L&F's to choose",
                "createEtchedBorder", List.of(MAIN)).skippingComments());

        rules.add(docsNameRealTypes());

        int failed = 0;
        int pending = 0;
        for (Check rule : rules) {
            List<String> violations = rule.violations();
            if (violations.isEmpty()) {
                System.out.printf("rules: %-26s OK%s%n", rule.name(),
                        rule.pendingReason() == null ? "" : "   (pending rule now clean - make it enforcing)");
            } else if (rule.pendingReason() != null) {
                pending++;
                System.out.printf("rules: %-26s PENDING  %d left  (%s)%n",
                        rule.name(), violations.size(), rule.pendingReason());
                for (String v : violations) {
                    System.out.printf("       %s%n", v);
                }
            } else {
                failed++;
                System.out.printf("rules: %-26s FAIL  (%s)%n", rule.name(), rule.section());
                System.out.printf("       %s%n", rule.why());
                for (String v : violations) {
                    System.out.printf("       %s%n", v);
                }
            }
        }

        if (failed > 0) {
            System.out.printf("%n%d rule%s violated. See CLAUDE.md.%n", failed, failed == 1 ? "" : "s");
            System.exit(1);
        }
        System.out.printf("%nAll enforcing rules pass%s.%n",
                pending == 0 ? "" : "; " + pending + " pending (tracked in TODO.md)");
    }

    /**
     * Types a doc may name that are not files in this repository: JDK and Swing types, and the
     * one inner class the enemy package's doc refers to. Anything else backticked in a
     * {@code CLAUDE.md} must exist as a source file.
     */
    private static final Set<String> KNOWN_EXTERNAL_TYPES = Set.of(
            "ArrayList", "CardLayout", "ClassCastException", "CopyOnWriteArrayList", "Error",
            "Exception", "Graphics2D", "GridBagLayout", "HashMap", "IndexOutOfBoundsException",
            "LinkedHashMap", "NullPointerException", "NumberFormatException", "Optional",
            "Runnable", "Shape", "Throwable",
            // an inner class of DefinedEnemyMob, so it has no file of its own
            "MobAbilityContext",
            // prose, not a type - the naming rule's own example
            "UpperCamelCase");

    /**
     * Every backticked {@code UpperCamelCase} name in a {@code CLAUDE.md} must be a real source
     * file. This is the drift that repeatedly survived manual review: a doc goes on citing a
     * class long after it is renamed or deleted, and nothing compiles the prose.
     * <p>
     * The candidate pattern requires a lowercase second character, which keeps {@code ALL_CAPS}
     * enum constants and single letters out of the match.
     * <p>
     * Deliberately scoped to {@code CLAUDE.md} files and not to {@code docs/ARCHITECTURE.md}.
     * That document's job is history, so naming a class that no longer exists - {@code Context},
     * the god object the composition root replaced - is correct there. Widening this check to it
     * would mean allowlisting every deleted class forever, which fights what the document is for.
     */
    private static Check docsNameRealTypes() {
        return new Check() {
            @Override
            public String name() {
                return "docs-name-real-types";
            }

            @Override
            public String section() {
                return "CLAUDE.md 10";
            }

            @Override
            public String why() {
                return "a CLAUDE.md names a type that no longer exists - rename it or drop the sentence";
            }

            @Override
            public List<String> violations() throws IOException {
                Set<String> sourceNames = new HashSet<>();
                for (Path root : List.of(MAIN, TEST)) {
                    if (!Files.isDirectory(root)) {
                        continue;
                    }
                    try (Stream<Path> files = Files.walk(root)) {
                        files.filter(Files::isRegularFile)
                                .map(f -> f.getFileName().toString())
                                .filter(n -> n.endsWith(".java"))
                                .forEach(n -> sourceNames.add(n.substring(0, n.length() - 5)));
                    }
                }
                Pattern candidate = Pattern.compile("`([A-Z][a-z][A-Za-z0-9]*)`");
                List<String> found = new ArrayList<>();
                for (Path doc : docFiles()) {
                    List<String> lines = Files.readAllLines(doc, StandardCharsets.UTF_8);
                    for (int i = 0; i < lines.size(); i++) {
                        Matcher m = candidate.matcher(lines.get(i));
                        while (m.find()) {
                            String type = m.group(1);
                            if (!sourceNames.contains(type) && !KNOWN_EXTERNAL_TYPES.contains(type)) {
                                found.add(doc + ":" + (i + 1) + "  names `" + type + "`, which has no source file");
                            }
                        }
                    }
                }
                return found;
            }
        };
    }

    /** The root {@code CLAUDE.md} and every per-package one. */
    private static List<Path> docFiles() throws IOException {
        List<Path> docs = new ArrayList<>();
        docs.add(Paths.get("CLAUDE.md"));
        try (Stream<Path> files = Files.walk(MAIN)) {
            files.filter(Files::isRegularFile)
                    .filter(f -> "CLAUDE.md".equals(f.getFileName().toString()))
                    .forEach(docs::add);
        }
        return docs;
    }

    private static List<Path> headlessRoots() {
        List<Path> roots = new ArrayList<>();
        for (String pkg : HEADLESS_PACKAGES) {
            roots.add(MAIN.resolve("td").resolve(pkg));
        }
        return roots;
    }

    /** One verifiable rule. Most are greps ({@link Rule}); a few need real logic. */
    private interface Check {
        String name();

        String section();

        String why();

        List<String> violations() throws IOException;

        /** Non-null while a rule is adopted but not yet satisfied - see {@link Rule#pending}. */
        default String pendingReason() {
            return null;
        }
    }

    private static final class Rule implements Check {

        private final String name;
        private final String section;
        private final String why;
        private final Pattern pattern;
        private final List<Path> roots;
        private Predicate<String> lineFilter = line -> true;
        private Predicate<Path> pathFilter = path -> true;
        private String pendingReason;

        private Rule(String name, String section, String why, String regex, List<Path> roots) {
            this.name = name;
            this.section = section;
            this.why = why;
            this.pattern = Pattern.compile(regex);
            this.roots = roots;
        }

        static Rule of(String name, String section, String why, String regex, List<Path> roots) {
            return new Rule(name, section, why, regex, roots);
        }

        /** Ignores matches on comment lines - see this class's note on the heuristic. */
        Rule skippingComments() {
            this.lineFilter = line -> {
                String trimmed = line.trim();
                return !trimmed.startsWith("//") && !trimmed.startsWith("*") && !trimmed.startsWith("/*");
            };
            return this;
        }

        /**
         * Reports remaining violations and their shrinking count without failing the build, for a
         * rule the codebase has adopted but not yet finished complying with. The named TODO.md
         * entry is the work that makes it enforcing; delete this call once the count reaches zero.
         */
        Rule pending(String todoEntry) {
            this.pendingReason = todoEntry;
            return this;
        }

        /** Exempts a subtree, for a rule CLAUDE.md scopes rather than applies everywhere. */
        Rule excludingPath(String fragment) {
            String normalized = fragment.replace('/', java.io.File.separatorChar);
            this.pathFilter = path -> !path.toString().contains(normalized);
            return this;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public String section() {
            return this.section;
        }

        @Override
        public String why() {
            return this.why;
        }

        @Override
        public String pendingReason() {
            return this.pendingReason;
        }

        @Override
        public List<String> violations() throws IOException {
            List<String> found = new ArrayList<>();
            for (Path root : this.roots) {
                if (!Files.isDirectory(root)) {
                    continue;
                }
                try (Stream<Path> files = Files.walk(root)) {
                    for (Path file : files.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".java"))
                            .filter(this.pathFilter)
                            .toList()) {
                        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                        for (int i = 0; i < lines.size(); i++) {
                            String line = lines.get(i);
                            if (this.lineFilter.test(line) && this.pattern.matcher(line).find()) {
                                found.add(file + ":" + (i + 1) + "  " + line.trim());
                            }
                        }
                    }
                }
            }
            return found;
        }
    }

    private VerifyRules() {
    }
}
