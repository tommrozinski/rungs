package com.rungs.readiness.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** 024 REQ-002, REQ-004, REQ-006: the stylesheet's tokens match the design board and name meaning, not hues. */
class DesignTokensTests {

    private static final Path BOARD = Path.of("prd", "design-board.html");

    private static final Path STYLESHEET = Path.of("src", "main", "resources", "static", "css", "rungs.css");

    /** A color token declaration; group 1 is the name and group 2 the value. */
    private static final Pattern COLOR_TOKEN = Pattern.compile("(--[\\w-]+):\\s*(#[0-9A-Fa-f]{3,8})\\s*;");

    private static final Pattern ROOT_BLOCK = Pattern.compile(":root\\s*\\{[^}]*}");

    @Test
    void everyBoardColorTokenIsCopiedWithAnIdenticalValue() throws IOException {
        Map<String, String> boardColors = colorTokens(firstRootBlock(BOARD));

        assertThat(boardColors).hasSize(20);
        assertThat(colorTokens(firstRootBlock(STYLESHEET))).containsExactlyEntriesOf(boardColors);
    }

    @Test
    void definesSemanticSpacingRadiusElevationAndTypeTokens() throws IOException {
        String root = firstRootBlock(STYLESHEET);

        assertThat(root).contains(
                "--status-satisfied: var(--green-600);",
                "--status-in-progress: var(--gold-500);",
                "--status-not-started: var(--ink-500);",
                "--status-skipped: var(--blue-600);",
                "--validation-error: var(--red-600);",
                "--space-1: 0.25rem;", "--space-2: 0.5rem;", "--space-3: 0.75rem;", "--space-4: 1rem;",
                "--space-5: 1.375rem;", "--space-6: 2rem;", "--space-7: 2.75rem;",
                "--r-sm: 6px;", "--r-md: 10px;", "--r-lg: 16px;", "--r-pill: 999px;",
                "--elevation-flat: none;",
                "--display-size: 2.125rem;", "--h1-size: 1.5rem;", "--h2-size: 1.0625rem;",
                "--body-size: 0.875rem;", "--small-size: 0.78125rem;");
        assertThat(Pattern.compile("--(?:elevation|shadow)-[\\w-]+:").matcher(root).results().count())
                .as("exactly two elevations")
                .isEqualTo(2);
    }

    @Test
    void selfHostsInterWithSwapAndTabularNumeralsUtility() throws IOException {
        String css = Files.readString(STYLESHEET, StandardCharsets.UTF_8);

        assertThat(css).contains("url(\"../fonts/InterVariable.woff2\")", "font-display: swap;",
                "font-weight: 100 900;", ".num { font-variant-numeric: tabular-nums; }");
        assertThat(STYLESHEET.resolveSibling(Path.of("..", "fonts", "InterVariable.woff2")).normalize()).isRegularFile();
        assertThat(STYLESHEET.resolveSibling(Path.of("..", "fonts", "LICENSE.txt")).normalize()).isRegularFile();
    }

    private static String firstRootBlock(Path file) throws IOException {
        Matcher matcher = ROOT_BLOCK.matcher(Files.readString(file, StandardCharsets.UTF_8));
        assertThat(matcher.find()).as("%s has a :root block", file).isTrue();
        return matcher.group();
    }

    private static Map<String, String> colorTokens(String block) {
        Map<String, String> tokens = new LinkedHashMap<>();
        COLOR_TOKEN.matcher(block).results().forEach(match -> tokens.put(match.group(1), match.group(2)));
        return tokens;
    }
}
