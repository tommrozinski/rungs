package com.rungs.readiness.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
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
 * 025 REQ-003 and its security requirement: templates never use unescaped output ({@code th:utext}) or inline
 * styles ({@code style=}, {@code th:style=}, or a {@code <style>} element), which also keeps a strict
 * Content-Security-Policy possible.
 */
class TemplateMarkupScanTests {

    private static final Path TEMPLATES = Path.of("templates");

    private static final List<Forbidden> FORBIDDEN = List.of(
            new Forbidden("th:utext", Pattern.compile("\\bth:utext\\s*=", Pattern.CASE_INSENSITIVE)),
            new Forbidden("inline style", Pattern.compile("(?<![\\w:-])(?:th:)?style\\s*=", Pattern.CASE_INSENSITIVE)),
            new Forbidden("<style> element", Pattern.compile("<style\\b", Pattern.CASE_INSENSITIVE)));

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
        "<p th:utext=\"${message}\">x</p>          | th:utext",
        "<p style=\"color: red\">x</p>              | inline style",
        "<p STYLE='margin:0'>x</p>                  | inline style",
        "<p th:style=\"${css}\">x</p>               | inline style",
        "<p th:attr=\"style=${css}\">x</p>          | inline style",
        "<style>p { margin: 0; }</style>            | <style> element"
    })
    void flagsEachForbiddenConstruct(String content, String label, @TempDir Path root) throws IOException {
        write(root, "templates/page.html", content + "\n");

        assertThat(findViolations(root)).containsExactly("templates/page.html:1: forbids " + label);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<p th:text=\"${message}\">x</p>",
        "<p data-style=\"compact\">x</p>",
        "<link rel=\"stylesheet\" th:href=\"@{/css/rungs.css}\">",
        "<p class=\"stylesheet-note\">No style here</p>"
    })
    void allowsEscapedOutputAndStylesheetLinks(String content, @TempDir Path root) throws IOException {
        write(root, "templates/page.html", content + "\n");

        assertThat(findViolations(root)).isEmpty();
    }

    @Test
    void realTemplatesUseEscapedOutputAndNoInlineStyles() throws IOException {
        Path root = Path.of("").toAbsolutePath().normalize();
        assertThat(root.resolve("pom.xml"))
                .as("working directory must be the project root, but was %s", root)
                .isRegularFile();

        List<String> violations = findViolations(root.resolve(Path.of("src", "main", "resources")));

        assertThat(violations).as("template markup violations:%n%s", String.join("\n", violations)).isEmpty();
    }

    /** Scans {@code templates/} under {@code resourcesRoot}; one entry per violation, naming file and line. */
    static List<String> findViolations(Path resourcesRoot) throws IOException {
        Path start = resourcesRoot.resolve(TEMPLATES);
        if (!Files.isDirectory(start)) {
            return List.of();
        }
        List<Path> files;
        try (Stream<Path> paths = Files.walk(start)) {
            files = paths.filter(Files::isRegularFile).sorted().toList();
        }
        List<String> violations = new ArrayList<>();
        for (Path file : files) {
            String displayPath = resourcesRoot.relativize(file).toString().replace('\\', '/');
            String content = Files.readString(file, StandardCharsets.UTF_8);
            for (Forbidden forbidden : FORBIDDEN) {
                Matcher matcher = forbidden.pattern().matcher(content);
                while (matcher.find()) {
                    long line = content.chars().limit(matcher.start()).filter(c -> c == '\n').count() + 1;
                    violations.add(displayPath + ":" + line + ": forbids " + forbidden.label());
                }
            }
        }
        return violations;
    }

    private static void write(Path root, String relativePath, String content) throws IOException {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private record Forbidden(String label, Pattern pattern) {
    }
}
