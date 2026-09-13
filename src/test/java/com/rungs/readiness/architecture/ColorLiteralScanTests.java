package com.rungs.readiness.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 024 REQ-007: color literals live only in the stylesheet's token block. A hex, {@code rgb(}/{@code rgba(} or
 * {@code hsl(}/{@code hsla(} literal fails anywhere in {@code static/css/**.css} outside the first {@code :root { … }}
 * block of each file, and anywhere in {@code templates/}.
 *
 * <p>Rules that keep non-colors out:
 *
 * <ul>
 *   <li>A hex literal is {@code #} plus exactly 3, 4, 6 or 8 hex digits ending at a word boundary, not preceded by a
 *       word character or {@code &} (so {@code &#10003;} and {@code page#abc} are not colors).
 *   <li>In CSS, comments are ignored, and a hex literal counts only in a property value: the next <code>{</code>,
 *       <code>;</code> or <code>}</code> after it is a declaration end, not a rule's opening <code>{</code>. A selector
 *       such as <code>#abc {</code> or {@code #main} is not a value, so an id that happens to be hex digits passes.
 *   <li>In templates, a hex literal that opens an {@code href}, {@code src} or {@code action} value (such as
 *       {@code href="#add"}) is a URL fragment, not a color.
 * </ul>
 */
class ColorLiteralScanTests {

    private static final Path STYLESHEETS = Path.of("static", "css");

    private static final Path TEMPLATES = Path.of("templates");

    /** Group 1 is the digits; only the lengths in {@link #HEX_COLOR_LENGTHS} are colors. */
    private static final Pattern HEX = Pattern.compile("(?<![&\\w])#([0-9a-fA-F]{3,8})\\b");

    private static final Set<Integer> HEX_COLOR_LENGTHS = Set.of(3, 4, 6, 8);

    /** A {@code #} that opens a URL attribute value, which makes what follows a fragment identifier. */
    private static final Pattern URL_FRAGMENT_START =
            Pattern.compile("\\b(?:href|src|action)\\s*=\\s*[\"']$", Pattern.CASE_INSENSITIVE);

    /** Group 1 is the function name. */
    private static final Pattern COLOR_FUNCTION =
            Pattern.compile("(?<![\\w-])(rgba?|hsla?)\\s*\\(", Pattern.CASE_INSENSITIVE);

    private static final Pattern CSS_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private static final Pattern ROOT_BLOCK = Pattern.compile(":root\\s*\\{[^}]*}");

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
        "static/css/app.css  | .a { color: #fff; }                          | hex color #fff",
        "static/css/app.css  | .a { color: #ffff; }                         | hex color #ffff",
        "static/css/app.css  | .a { color: #0B5CAB; }                       | hex color #0B5CAB",
        "static/css/app.css  | .a { color: #0B5CAB80 }                      | hex color #0B5CAB80",
        "static/css/app.css  | .a { color: rgb(0, 0, 0); }                  | rgb( color",
        "static/css/app.css  | .a { color: RGBA(0, 0, 0, .5); }             | rgb( color",
        "static/css/app.css  | .a { color: hsl(0 0% 0%); }                  | hsl( color",
        "static/css/app.css  | .a { color: hsla(0, 0%, 0%, .5); }           | hsl( color",
        "static/css/app.css  | .a:hover { border: 1px solid #abc; }         | hex color #abc",
        "static/css/nested/b.css | .a { background: #EEE }                  | hex color #EEE",
        "templates/page.html | <svg><rect fill=\"#abc\"/></svg>               | hex color #abc",
        "templates/page.html | <p th:classappend=\"${a} ? '#DDE3E9'\">x</p>   | hex color #DDE3E9",
        "templates/page.html | <p data-tint=\"rgba(1,2,3,.4)\">x</p>          | rgb( color",
        "templates/page.html | <p data-tint=\"hsl(1 2% 3%)\">x</p>            | hsl( color"
    })
    void flagsEachColorLiteral(String path, String content, String label, @TempDir Path root) throws IOException {
        write(root, path, content + "\n");

        assertThat(findViolations(root)).containsExactly(path + ":1: color literal outside :root: " + label);
    }

    @Test
    void allowsLiteralsInsideFirstRootBlockOnlyAndReportsLines(@TempDir Path root) throws IOException {
        write(root, "static/css/app.css", """
                :root {
                  --blue-600: #0B5CAB;
                  --shadow: 0 1px 2px rgba(16,38,59,.06);
                }
                .a { color: var(--blue-600); }
                :root {
                  --late: #FFFFFF;
                }
                """);

        assertThat(findViolations(root)).containsExactly(
                "static/css/app.css:7: color literal outside :root: hex color #FFFFFF");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "#main { padding: 0; }",
        "#abc, #face { margin: 0; }",
        ".a #bad:hover { margin: 0; }",
        "/* was #fff, now a token */ .a { color: var(--white); }",
        ".a { color: #12345; }",
        ".a { color: #1234567; }",
        ".a { background: url(img.svg#abcdef); }",
        ".a { border-color: var(--gold-500); }",
        ".bg-rgb { margin: 0; }"
    })
    void ignoresCssThatIsNotAColorLiteral(String content, @TempDir Path root) throws IOException {
        write(root, "static/css/app.css", content + "\n");

        assertThat(findViolations(root)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<a href=\"#add\">Skip</a>",
        "<a th:href=\"#top\">Top</a>",
        "<a href='#main'>Main</a>",
        "<span>&#10003;</span>",
        "<a href=\"/stages#cafe\">x</a>",
        "<p th:text=\"#{stage.title}\">x</p>",
        "<p th:text=\"${#strings.isEmpty(a)}\">x</p>",
        "<p>Stage #2 of 7</p>"
    })
    void ignoresMarkupThatIsNotAColorLiteral(String content, @TempDir Path root) throws IOException {
        write(root, "templates/page.html", content + "\n");

        assertThat(findViolations(root)).isEmpty();
    }

    @Test
    void doesNotScanOtherStaticFiles(@TempDir Path root) throws IOException {
        write(root, "static/js/app.js", "const c = '#fff';\n");
        write(root, "static/css/notes.txt", "#fff\n");

        assertThat(findViolations(root)).isEmpty();
    }

    @Test
    void realStylesheetsAndTemplatesKeepColorsInTokens() throws IOException {
        Path root = Path.of("").toAbsolutePath().normalize();
        assertThat(root.resolve("pom.xml"))
                .as("working directory must be the project root, but was %s", root)
                .isRegularFile();

        List<String> violations = findViolations(root.resolve(Path.of("src", "main", "resources")));

        assertThat(violations).as("color literals outside :root:%n%s", String.join("\n", violations)).isEmpty();
    }

    /** Scans stylesheets and templates under {@code resourcesRoot}; one entry per literal, naming file and line. */
    static List<String> findViolations(Path resourcesRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : files(resourcesRoot.resolve(STYLESHEETS), ".css")) {
            String content = blankOut(blankOut(read(file), CSS_COMMENT, false), ROOT_BLOCK, true);
            scan(resourcesRoot, file, content, true, violations);
        }
        for (Path file : files(resourcesRoot.resolve(TEMPLATES), ".html")) {
            scan(resourcesRoot, file, read(file), false, violations);
        }
        return violations;
    }

    private static void scan(Path resourcesRoot, Path file, String content, boolean css, List<String> violations) {
        String displayPath = resourcesRoot.relativize(file).toString().replace('\\', '/');
        Matcher hex = HEX.matcher(content);
        while (hex.find()) {
            boolean color = HEX_COLOR_LENGTHS.contains(hex.group(1).length());
            boolean excluded = css ? !inCssValue(content, hex.end()) : opensUrl(content, hex.start());
            if (color && !excluded) {
                violations.add(violation(displayPath, content, hex.start(), "hex color " + hex.group()));
            }
        }
        Matcher function = COLOR_FUNCTION.matcher(content);
        while (function.find()) {
            String family = function.group(1).substring(0, 3).toLowerCase();
            violations.add(violation(displayPath, content, function.start(), family + "( color"));
        }
    }

    /** Whether the text from {@code offset} next reaches a declaration end rather than a rule's opening brace. */
    private static boolean inCssValue(String content, int offset) {
        for (int i = offset; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                return false;
            }
            if (c == ';' || c == '}') {
                return true;
            }
        }
        return true;
    }

    private static boolean opensUrl(String content, int hashOffset) {
        int from = Math.max(0, hashOffset - 40);
        return URL_FRAGMENT_START.matcher(content.substring(from, hashOffset)).find();
    }

    /** Replaces the first match ({@code firstOnly}) or every match with spaces, keeping newlines for line numbers. */
    private static String blankOut(String content, Pattern pattern, boolean firstOnly) {
        StringBuilder result = new StringBuilder(content);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            for (int i = matcher.start(); i < matcher.end(); i++) {
                if (result.charAt(i) != '\n') {
                    result.setCharAt(i, ' ');
                }
            }
            if (firstOnly) {
                break;
            }
        }
        return result.toString();
    }

    private static List<Path> files(Path start, String extension) throws IOException {
        if (!Files.isDirectory(start)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(start)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(extension))
                    .sorted()
                    .toList();
        }
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private static String violation(String displayPath, String content, int offset, String label) {
        long lineNumber = content.chars().limit(offset).filter(c -> c == '\n').count() + 1;
        return displayPath + ":" + lineNumber + ": color literal outside :root: " + label;
    }

    private static void write(Path root, String relativePath, String content) throws IOException {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
