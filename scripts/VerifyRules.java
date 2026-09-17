import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

    /**
     * The domain packages CLAUDE.md 2.1 requires to be free of any Swing or AWT dependency.
     */
    private static final String[] HEADLESS_PACKAGES = {
            "board", "cell", "damage", "economy", "effect", "enemy",
            "level", "projectile", "tower", "util", "wave"
    };
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

    private VerifyRules() {
    }

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

        rules.add(enumConstantsAreUpperSnakeCase());

        rules.add(fieldsDeclareTheirOwner());

        rules.add(docsNameRealTypes());

        // Last, and built from the names above: it checks that this file and CLAUDE.md agree
        // about which rules exist.
        rules.add(checksAreDocumented(rules.stream().map(Check::name).toList()));

        int failed = 0;
        for (Check rule : rules) {
            List<String> violations = rule.violations();
            if (violations.isEmpty()) {
                System.out.printf("rules: %-26s OK%n", rule.name());
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
        System.out.printf("%nAll %d rules pass.%n", rules.size());
        reportRuleInventory(rules.size());
    }

    /**
     * Prints how much of CLAUDE.md is enforced here and how much is prose.
     * <p>
     * This is the number worth watching, and the reason it is printed rather than counted by
     * hand: "commits touching CLAUDE.md" cannot tell a genuine rule change from a correction of
     * something that was never true from a narrative paragraph, and it punishes paying down
     * debt. Unverified rules are what actually generate churn - a rule that exists only as prose
     * can be wrong, can drift, and invites an explanatory paragraph beside it.
     * <p>
     * The rule count is a heuristic and says so: it counts bolded lead-ins, which is how this
     * file states a rule, and no parser of prose is exact. The enforced and judgement counts are
     * exact. Nothing here fails the build - a budget on prose size would just be met by writing
     * longer lines.
     */
    private static void reportRuleInventory(int enforced) throws IOException {
        Path doc = Paths.get("CLAUDE.md");
        if (!Files.isRegularFile(doc)) {
            return;
        }
        List<String> lines = Files.readAllLines(doc, StandardCharsets.UTF_8);
        Pattern boldedRule = Pattern.compile("^\\s*(?:[-*]\\s+|\\d+\\.\\s+)?\\*\\*\\S");
        long stated = lines.stream().filter(l -> boldedRule.matcher(l).find()).count();
        long judgement = lines.stream()
                .filter(l -> l.toLowerCase(Locale.ROOT).contains("judgement, not a grep"))
                .count();
        System.out.printf("docs:  CLAUDE.md states ~%d rules - %d enforced above, "
                        + "%d marked judgement, ~%d prose only.%n",
                stated, enforced, judgement, Math.max(0, stated - enforced - judgement));
        System.out.printf("       Prose-only rules are the ones that drift. "
                + "See CLAUDE.md 10 question 1.%n");
    }

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

    /**
     * Every constant of every {@code enum} must be {@code UPPER_SNAKE_CASE}, like any other
     * constant (CLAUDE.md 6).
     * <p>
     * This is separate from {@code no-lowercase-constants} because that rule greps for
     * {@code static final}, and an enum constant is written with neither keyword - so the build
     * reported OK for a long time with eleven lowercase constants in the tree
     * ({@code TowerFactory.Type.first} and friends, {@code Cell.HighlightType.none}). A rule
     * whose check cannot see the largest enum in the codebase is not enforced, it is decorative.
     * <p>
     * The scan is bracket-depth based rather than regex-only: from an {@code enum X \{} line it
     * reads constants until the first {@code ;} or the closing brace, which is exactly where an
     * enum's constant list ends.
     */
    private static Check enumConstantsAreUpperSnakeCase() {
        Pattern enumStart = Pattern.compile("\\benum\\s+[A-Z]\\w*\\s*(implements\\s+[\\w.,<> ]+)?\\{");
        Pattern constant = Pattern.compile("^([A-Za-z_]\\w*)\\s*(\\(|,|;|$)");
        return new SourceCheck("enum-constants-upper-snake", "CLAUDE.md 6",
                "an enum constant is a constant: UPPER_SNAKE_CASE, like every other one",
                List.of(MAIN, TEST)) {
            @Override
            void inspect(Path file, List<String> lines, List<String> found) {
                for (int i = 0; i < lines.size(); i++) {
                    if (!enumStart.matcher(lines.get(i)).find()) {
                        continue;
                    }
                    for (int j = i + 1; j < lines.size(); j++) {
                        String line = lines.get(j).trim();
                        if (line.isEmpty() || line.startsWith("//") || line.startsWith("*")
                                || line.startsWith("/*") || line.startsWith("@")) {
                            continue;
                        }
                        if (line.startsWith("}")) {
                            break;
                        }
                        Matcher m = constant.matcher(line);
                        if (m.find()) {
                            String name = m.group(1);
                            if (!name.equals(name.toUpperCase())) {
                                found.add(file + ":" + (j + 1) + "  enum constant `" + name
                                        + "` is not UPPER_SNAKE_CASE");
                            }
                        }
                        if (line.contains(";")) {
                            break;
                        }
                    }
                }
            }
        };
    }

    /**
     * A class holding mutable state must say which thread owns it (CLAUDE.md 3).
     * <p>
     * Concretely: any non-final, non-volatile instance field obliges its class to carry
     * {@code @ThreadConfined}. That turns §3's central rule - the thread that owns mutable state
     * publishes it, the other reads only what was published - from prose into a question the
     * author has to answer once per class. It is the check the codebase most needed: §3 names
     * three bug shapes by hand, and all three were in the tree while every rule reported OK.
     * <p>
     * A field that genuinely crosses threads has no honest answer here, and that is the point -
     * it belongs behind a {@code volatile}, an {@code Atomic*}, or an immutable snapshot, each of
     * which satisfies this rule by not being a plain mutable field in the first place.
     */
    private static Check fieldsDeclareTheirOwner() {
        // A field declaration, as opposed to a method, a compact record constructor or a nested
        // type: it has a modifier, a type, a name, and then either "=" or ";" - never "(" first.
        Pattern field = Pattern.compile(
                "^\\s{1,8}(private|protected|public)\\s+(?!static)[\\w.<>,\\[\\]\\s]*?\\s"
                        + "[a-z]\\w*\\s*(=[^=]|;)");
        return new SourceCheck("fields-declare-their-owner", "CLAUDE.md 3",
                "a class with mutable state must declare its owning thread with @ThreadConfined",
                List.of(MAIN)) {
            @Override
            void inspect(Path file, List<String> lines, List<String> found) {
                boolean declaresOwner = lines.stream().anyMatch(l -> l.contains("@ThreadConfined"));
                if (declaresOwner) {
                    return;
                }
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    String trimmed = line.trim();
                    if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                        continue;
                    }
                    if (line.contains(" final ") || line.contains(" volatile ")
                            || line.contains("static")) {
                        continue;
                    }
                    if (field.matcher(line).find()) {
                        found.add(file + ":" + (i + 1) + "  " + trimmed
                                + "   (mutable, so the class needs @ThreadConfined)");
                    }
                }
            }
        };
    }

    /**
     * {@code CLAUDE.md} and this file must agree about which rules exist, in both directions.
     * <p>
     * Forwards: every check here is named in {@code CLAUDE.md}, so the build cannot enforce a
     * rule nobody has been told about. Backwards: every check name cited there is real, so a
     * citation cannot outlive the check it points at.
     * <p>
     * This is what lets a rule cite its check <em>by name</em> rather than by restating the
     * check's own regex. Nine citations used to carry a copy of the pattern, and at least two
     * had already drifted from what the check actually did - {@code no-static-random} scans
     * tests as well as main, and the doc claimed only main. A name is one word and cannot be
     * subtly wrong; a duplicated regex is neither.
     */
    private static Check checksAreDocumented(List<String> checkNames) {
        return new Check() {
            @Override
            public String name() {
                return "checks-are-documented";
            }

            @Override
            public String section() {
                return "CLAUDE.md 10";
            }

            @Override
            public String why() {
                return "CLAUDE.md and VerifyRules must name the same set of rules";
            }

            @Override
            public List<String> violations() throws IOException {
                Path doc = Paths.get("CLAUDE.md");
                if (!Files.isRegularFile(doc)) {
                    return List.of();
                }
                String text = Files.readString(doc, StandardCharsets.UTF_8);
                List<String> found = new ArrayList<>();

                for (String check : checkNames) {
                    if (!text.contains("`" + check + "`")) {
                        found.add("CLAUDE.md never mentions the `" + check
                                + "` check - a rule the build enforces should be written down");
                    }
                }

                // Only blockquote lines, because that is what a citation is: the convention
                // stated at the top of CLAUDE.md is "> `check-name`" under the rule. Scanning
                // the whole document instead would flag every kebab-case string in it - the
                // `game-loop` thread and the `run-jtd` skill both read like check names and
                // are not.
                Set<String> known = new HashSet<>(checkNames);
                known.add("checks-are-documented");
                Pattern citation = Pattern.compile("`([a-z][a-z0-9]*(?:-[a-z0-9]+){1,4})`");
                List<String> lines = Files.readAllLines(doc, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    if (!lines.get(i).stripLeading().startsWith(">")) {
                        continue;
                    }
                    Matcher m = citation.matcher(lines.get(i));
                    while (m.find()) {
                        String cited = m.group(1);
                        if (!known.contains(cited)) {
                            found.add(doc + ":" + (i + 1) + "  cites `" + cited
                                    + "`, which is not a check in scripts/VerifyRules.java");
                        }
                    }
                }
                return found;
            }
        };
    }

    /**
     * The root {@code CLAUDE.md} and every per-package one.
     */
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

    /**
     * One verifiable rule. Most are greps ({@link Rule}); a few need real logic.
     */
    private interface Check {
        String name();

        String section();

        String why();

        List<String> violations() throws IOException;
    }

    /**
     * A rule needing real per-file logic rather than a single line-matching regex.
     */
    private abstract static class SourceCheck implements Check {

        private final String name;
        private final String section;
        private final String why;
        private final List<Path> roots;

        SourceCheck(String name, String section, String why, List<Path> roots) {
            this.name = name;
            this.section = section;
            this.why = why;
            this.roots = roots;
        }

        /**
         * Adds one entry to {@code found} per violation in {@code file}.
         */
        abstract void inspect(Path file, List<String> lines, List<String> found) throws IOException;

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
        public List<String> violations() throws IOException {
            List<String> found = new ArrayList<>();
            for (Path root : this.roots) {
                if (!Files.isDirectory(root)) {
                    continue;
                }
                try (Stream<Path> files = Files.walk(root)) {
                    for (Path file : files.filter(Files::isRegularFile)
                            .filter(f -> f.toString().endsWith(".java"))
                            .toList()) {
                        this.inspect(file, Files.readAllLines(file, StandardCharsets.UTF_8), found);
                    }
                }
            }
            return found;
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

        /**
         * Ignores matches on comment lines - see this class's note on the heuristic.
         */
        Rule skippingComments() {
            this.lineFilter = line -> {
                String trimmed = line.trim();
                return !trimmed.startsWith("//") && !trimmed.startsWith("*") && !trimmed.startsWith("/*");
            };
            return this;
        }

        /**
         * Exempts a subtree, for a rule CLAUDE.md scopes rather than applies everywhere.
         */
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
}
