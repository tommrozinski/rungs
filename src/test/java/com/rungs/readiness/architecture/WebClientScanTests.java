package com.rungs.readiness.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * AC-G (REQ-009): the web client is a viewer. Templates and static files hold no arithmetic, number parsing
 * or formatting, or data fetching, and hand-written JavaScript stays within its line budget.
 */
class WebClientScanTests {

    private static final Path FIXTURES = Path.of("src", "test", "resources", "archfixtures", "ac-g");

    /** Directories scanned, relative to the resources root. */
    private static final List<String> SCANNED_DIRECTORIES = List.of("templates", "static");

    /** Vendored libraries (htmx), relative to the resources root; not hand-written, so never scanned or budgeted. */
    private static final Path VENDOR = Path.of("static", "js", "vendor");

    /** Parsing, formatting, and fetching, matched against whole file contents. */
    private static final List<ForbiddenPattern> FORBIDDEN_PATTERNS = List.of(
            new ForbiddenPattern("parseFloat", Pattern.compile("\\bparseFloat\\b"), false),
            new ForbiddenPattern("parseInt", Pattern.compile("\\bparseInt\\b"), false),
            new ForbiddenPattern("Number(", Pattern.compile("\\bNumber\\s*\\("), false),
            new ForbiddenPattern("toFixed", Pattern.compile("\\btoFixed\\b"), false),
            new ForbiddenPattern("Math.", Pattern.compile("\\bMath\\s*\\."), false),
            new ForbiddenPattern("fetch(", Pattern.compile("\\bfetch\\s*\\("), false),
            new ForbiddenPattern("XMLHttpRequest", Pattern.compile("\\bXMLHttpRequest\\b"), false),
            new ForbiddenPattern("Thymeleaf #numbers", Pattern.compile("#numbers\\b"), false),
            new ForbiddenPattern("<script> without src",
                    Pattern.compile("<script\\b(?![^>]*\\s(?:th:)?src\\s*=)[^>]*>", Pattern.CASE_INSENSITIVE), true),
            new ForbiddenPattern("inline on* event handlers",
                    Pattern.compile("(?<![\\w-])on[a-z]+\\s*=", Pattern.CASE_INSENSITIVE), true),
            new ForbiddenPattern("htmx hx-on inline handlers",
                    Pattern.compile("\\bhx-on(?:[:-][\\w:.-]*)?\\s*=", Pattern.CASE_INSENSITIVE), true));

    /** A {@code ${...}} or {@code *{...}} expression; group 1 is its opening symbol and group 2 its body. */
    private static final Pattern EXPRESSION = Pattern.compile("([$*])\\{([^}]*)}");

    /** A {@code th:*} attribute; group 1 or 2 is its double- or single-quoted value. */
    private static final Pattern TH_ATTRIBUTE = Pattern.compile("\\bth:[\\w-]+\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");

    /** Thymeleaf's word operators, as standalone words rather than property names such as {@code .division}. */
    private static final String WORD_OPERATOR = "(?<![.\\w])(?:div|mod)(?![\\w(])";

    /** An arithmetic operator between two expressions, such as {@code ${a} - ${b}} or {@code ${a} div *{b}}. */
    private static final Pattern BETWEEN_EXPRESSIONS =
            Pattern.compile("}\\s*(?:[-+*/%]|" + WORD_OPERATOR + ")\\s*[$*]\\{");

    /** Single- or double-quoted string literals, removed before the arithmetic check so 'is-current' passes. */
    private static final Pattern STRING_LITERAL = Pattern.compile("'(?:[^'\\\\]|\\\\.)*'|\"(?:[^\"\\\\]|\\\\.)*\"");

    private static final Pattern ARITHMETIC = Pattern.compile("[-+*/%]|" + WORD_OPERATOR);

    /** Most lines of hand-written JavaScript the client may hold in total. */
    private static final long SCRIPT_LINE_BUDGET = 200;

    @Test
    void flagsEveryForbiddenPatternInCommittedFixtureAndIgnoresVendoredFiles() throws IOException {
        assertThat(findViolations(FIXTURES.resolve("violations"))).containsExactlyInAnyOrder(
                "templates/forbidden.html:4: AC-G forbids Thymeleaf #numbers",
                "templates/forbidden.html:5: AC-G forbids arithmetic inside ${...}",
                "templates/forbidden.html:6: AC-G forbids inline on* event handlers",
                "templates/forbidden.html:7: AC-G forbids <script> without src",
                "static/js/forbidden.js:1: AC-G forbids parseFloat",
                "static/js/forbidden.js:2: AC-G forbids parseInt",
                "static/js/forbidden.js:3: AC-G forbids Number(",
                "static/js/forbidden.js:4: AC-G forbids toFixed",
                "static/js/forbidden.js:5: AC-G forbids Math.",
                "static/js/forbidden.js:6: AC-G forbids fetch(",
                "static/js/forbidden.js:7: AC-G forbids XMLHttpRequest");
    }

    @Test
    void realWebClientHasNoLogic() throws IOException {
        Path root = Path.of("").toAbsolutePath().normalize();
        assertThat(root.resolve("pom.xml"))
                .as("working directory must be the project root, but was %s", root)
                .isRegularFile();

        List<String> violations = findViolations(root.resolve(Path.of("src", "main", "resources")));

        assertThat(violations).as("AC-G violations:%n%s", String.join("\n", violations)).isEmpty();
    }

    @Test
    void ignoresHyphensInsideStringLiteralsAndScriptTagsWithSrc() throws IOException {
        assertThat(findViolations(FIXTURES.resolve("clean"))).isEmpty();
    }

    @Test
    void missingDirectoriesHoldNothingToScan(@TempDir Path root) throws IOException {
        assertThat(findViolations(root)).isEmpty();
    }

    @Test
    void failsWhenHandWrittenJavaScriptExceedsBudgetAndNamesFiles() throws IOException {
        assertThat(findViolations(FIXTURES.resolve("over-budget"))).containsExactly(
                "static/js: AC-G hand-written JavaScript totals 201 lines, over the 200-line budget "
                        + "(static/js/first.js, static/js/second.js)");
    }

    @Test
    void allowsExactlyTheBudgetAndDoesNotCountVendoredLines(@TempDir Path root) throws IOException {
        write(root, "static/js/app.js", "// line\n".repeat(200));
        write(root, "static/js/vendor/htmx.min.js", "// vendored\n".repeat(500));

        assertThat(findViolations(root)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
        "templates/page.html | <p th:text=\"${#numbers.formatInteger(n, 3)}\">x</p> | Thymeleaf #numbers",
        "templates/page.html | <p th:text=\"${a + b}\">x</p>                          | arithmetic inside ${...}",
        "templates/page.html | <p th:text=\"${a * b}\">x</p>                          | arithmetic inside ${...}",
        "templates/page.html | <p th:text=\"${a / b}\">x</p>                          | arithmetic inside ${...}",
        "templates/page.html | <p th:text=\"${a % b}\">x</p>                          | arithmetic inside ${...}",
        "templates/page.html | <div th:onclick=\"go()\">x</div>                        | inline on* event handlers",
        "templates/page.html | <SCRIPT type=\"module\">go()</SCRIPT>                  | <script> without src",
        "templates/page.html | <script data-src=\"x\">inline()</script>                | <script> without src",
        "templates/page.html | <p th:text=\"*{income - expenses}\">x</p>               | arithmetic inside *{...}",
        "templates/page.html | <p th:text=\"${a div b}\">x</p>                        | arithmetic inside ${...}",
        "templates/page.html | <p th:text=\"*{a mod b}\">x</p>                        | arithmetic inside *{...}",
        "templates/page.html | <p th:text=\"${income} - ${expenses}\">x</p>           | arithmetic between expressions",
        "templates/page.html | <p th:text=\"${a} + *{b}\">x</p>                       | arithmetic between expressions",
        "templates/page.html | <p th:with=\"t=${a} div ${b}\">x</p>                   | arithmetic between expressions",
        "templates/page.html | <button hx-on:click=\"go()\">x</button>                | htmx hx-on inline handlers",
        "templates/page.html | <form hx-on::after-request=\"reset()\">x</form>        | htmx hx-on inline handlers",
        "templates/page.html | <button hx-on-click=\"go()\">x</button>                | htmx hx-on inline handlers",
        "templates/page.html | <button data-hx-on:click=\"go()\">x</button>           | htmx hx-on inline handlers",
        "static/js/app.js    | const n = parseFloat(v);                                | parseFloat",
        "static/js/app.js    | const n = Number.parseInt(v);                           | parseInt",
        "static/js/app.js    | const n = Number (v);                                   | Number(",
        "static/js/app.js    | s = n.toFixed(2);                                       | toFixed",
        "static/js/app.js    | const m = Math.max(1, 2);                               | Math.",
        "static/js/app.js    | fetch ('/v1/assessments');                              | fetch(",
        "static/css/app.css  | /* new XMLHttpRequest() */                              | XMLHttpRequest"
    })
    void flagsEachForbiddenPattern(String path, String content, String label, @TempDir Path root) throws IOException {
        write(root, path, content + "\n");

        assertThat(findViolations(root)).containsExactly(path + ":1: AC-G forbids " + label);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<li th:classappend=\"${stage.current ? 'is-current' : ''}\">x</li>",
        "<script th:src=\"@{/js/app.js}\" defer></script>",
        "<button type=\"button\" hx-post=\"/assess\">Assess</button>",
        "<meta name=\"description\" content=\"stage one\">",
        "<p th:text=\"${stage.label}\">x</p>",
        "<p th:text=\"${stage.division}\" th:title=\"*{modeLabel}\">x</p>",
        "<p th:text=\"'Stage ' + ${stage.number}\">x</p>",
        "<p th:text=\"${stage.done} ? 'is-done' : 'is-open'\">x</p>",
        "<a th:href=\"@{/stages/{n}/detail(n=${stage.number})}\">x</a>"
    })
    void allowsViewerMarkup(String content, @TempDir Path root) throws IOException {
        write(root, "templates/page.html", content + "\n");

        assertThat(findViolations(root)).isEmpty();
    }

    @Test
    void markupOnlyPatternsDoNotApplyToScripts(@TempDir Path root) throws IOException {
        write(root, "static/js/app.js", "const one = 1;\nconst html = '<script>';\n");

        assertThat(findViolations(root)).isEmpty();
    }

    private static void write(Path root, String relativePath, String content) throws IOException {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    /**
     * Scans {@code templates/} and {@code static/} under {@code resourcesRoot}, skipping {@code static/js/vendor/},
     * and returns one entry per violation, naming the file, line, and constraint.
     */
    static List<String> findViolations(Path resourcesRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        List<String> scripts = new ArrayList<>();
        long scriptLines = 0;
        for (Path file : scannedFiles(resourcesRoot)) {
            String displayPath = resourcesRoot.relativize(file).toString().replace('\\', '/');
            String content;
            try {
                content = Files.readString(file, StandardCharsets.UTF_8);
            } catch (CharacterCodingException notUtf8) {
                // Binary asset such as an image or font: nothing a writer could put logic in.
                continue;
            }
            boolean script = displayPath.endsWith(".js");
            for (ForbiddenPattern forbidden : FORBIDDEN_PATTERNS) {
                if (script && forbidden.markupOnly()) {
                    continue;
                }
                Matcher matcher = forbidden.pattern().matcher(content);
                while (matcher.find()) {
                    violations.add(violation(displayPath, content, matcher.start(), forbidden.label()));
                }
            }
            Matcher expression = EXPRESSION.matcher(content);
            while (expression.find()) {
                String withoutLiterals = STRING_LITERAL.matcher(expression.group(2)).replaceAll("");
                if (ARITHMETIC.matcher(withoutLiterals).find()) {
                    violations.add(violation(displayPath, content, expression.start(),
                            "arithmetic inside " + expression.group(1) + "{...}"));
                }
            }
            Matcher attribute = TH_ATTRIBUTE.matcher(content);
            while (attribute.find()) {
                String value = attribute.group(1) != null ? attribute.group(1) : attribute.group(2);
                String withoutLiterals = STRING_LITERAL.matcher(value).replaceAll("");
                if (BETWEEN_EXPRESSIONS.matcher(withoutLiterals).find()) {
                    violations.add(violation(displayPath, content, attribute.start(), "arithmetic between expressions"));
                }
            }
            if (script) {
                scriptLines += content.lines().count();
                scripts.add(displayPath);
            }
        }
        if (scriptLines > SCRIPT_LINE_BUDGET) {
            violations.add("static/js: AC-G hand-written JavaScript totals " + scriptLines + " lines, over the "
                    + SCRIPT_LINE_BUDGET + "-line budget (" + String.join(", ", scripts) + ")");
        }
        return violations;
    }

    /** Regular files under the scanned directories, in path order; missing directories hold nothing to scan. */
    private static List<Path> scannedFiles(Path resourcesRoot) throws IOException {
        List<Path> files = new ArrayList<>();
        for (String directory : SCANNED_DIRECTORIES) {
            Path start = resourcesRoot.resolve(directory);
            if (!Files.isDirectory(start)) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(start)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> !resourcesRoot.relativize(path).startsWith(VENDOR))
                        .sorted()
                        .forEach(files::add);
            }
        }
        return files;
    }

    private static String violation(String displayPath, String content, int offset, String label) {
        long lineNumber = content.chars().limit(offset).filter(c -> c == '\n').count() + 1;
        return displayPath + ":" + lineNumber + ": AC-G forbids " + label;
    }

    /**
     * A pattern the web client must not contain. Markup-only patterns (script tags, inline handlers) are not
     * applied to {@code .js} files, where a name such as {@code one = 1} would otherwise read as a handler.
     */
    private record ForbiddenPattern(String label, Pattern pattern, boolean markupOnly) {
    }
}
