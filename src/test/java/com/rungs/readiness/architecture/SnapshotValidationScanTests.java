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

/**
 * Story 011 review finding: every caller validates a snapshot through
 * {@code com.rungs.readiness.application.validation.SnapshotValidator}, the one entry point, rather than annotating
 * a controller method with {@code @Valid} or {@code @Validated}. A cheap source scan of the inbound web adapters
 * stands in for a full ArchUnit rule (which would need to reason about annotated parameter types).
 */
class SnapshotValidationScanTests {

    private static final Path INBOUND_WEB = Path.of("adapter", "in", "web");

    private static final Pattern FORBIDDEN = Pattern.compile("@Valid\\b|@Validated\\b");

    /** Block and line comments, stripped before scanning so prose mentioning these annotations doesn't count. */
    private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/|//[^\n]*", Pattern.DOTALL);

    @Test
    void noInboundWebControllerAnnotatesASnapshotWithValidOrValidated() throws IOException {
        Path root = Path.of("").toAbsolutePath().normalize();
        assertThat(root.resolve("pom.xml"))
                .as("working directory must be the project root, but was %s", root)
                .isRegularFile();

        Path start = root.resolve(Path.of("src", "main", "java", "com", "rungs", "readiness")).resolve(INBOUND_WEB);
        List<String> violations = new ArrayList<>();
        if (Files.isDirectory(start)) {
            try (Stream<Path> paths = Files.walk(start)) {
                for (Path file : paths.filter(Files::isRegularFile).sorted().toList()) {
                    String content = withoutComments(Files.readString(file, StandardCharsets.UTF_8));
                    Matcher matcher = FORBIDDEN.matcher(content);
                    while (matcher.find()) {
                        long line = content.chars().limit(matcher.start()).filter(c -> c == '\n').count() + 1;
                        violations.add(file.getFileName() + ":" + line + ": validates directly instead of through "
                                + "SnapshotValidator");
                    }
                }
            }
        }

        assertThat(violations).as("direct validation in inbound web controllers:%n%s",
                String.join("\n", violations)).isEmpty();
    }

    /** Blanks out comments, keeping every newline, so line numbers of any remaining match stay accurate. */
    private static String withoutComments(String content) {
        Matcher comment = COMMENT.matcher(content);
        StringBuilder result = new StringBuilder();
        int previousEnd = 0;
        while (comment.find()) {
            result.append(content, previousEnd, comment.start());
            comment.group().chars().forEach(symbol -> result.append(symbol == '\n' ? '\n' : ' '));
            previousEnd = comment.end();
        }
        result.append(content, previousEnd, content.length());
        return result.toString();
    }
}
