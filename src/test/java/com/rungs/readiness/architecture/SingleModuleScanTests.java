package com.rungs.readiness.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * AC-H (REQ-010): the repository is one Maven module with no Node toolchain. The violation fixtures are
 * built in a temporary directory rather than committed, because a committed nested {@code pom.xml} could be
 * imported by an IDE as a second project (a deliberate deviation from REQ-011).
 */
class SingleModuleScanTests {

    private static final String POM = "pom.xml";

    private static final String NODE_MODULES = "node_modules";

    /** Node toolchain manifests and lockfiles, forbidden at any depth. */
    private static final Set<String> FORBIDDEN_FILE_NAMES =
            Set.of("package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml");

    /**
     * Directory names skipped at any depth: VCS metadata, IDE state, and agent tooling state ({@code .claude}
     * holds agent worktrees, each a full repository copy with its own {@code pom.xml}).
     */
    private static final Set<String> SKIPPED_DIRECTORY_NAMES = Set.of(".git", ".idea", ".vscode", ".claude");

    /** Root-relative directories skipped: build output and the committed violation fixtures of other scans. */
    private static final Set<Path> SKIPPED_RELATIVE_PATHS =
            Set.of(Path.of("target"), Path.of("src", "test", "resources", "archfixtures"));

    @Test
    void repositoryIsASingleMavenModuleWithoutNodeToolchain() throws IOException {
        Path root = Path.of("").toAbsolutePath().normalize();
        assertThat(root.resolve("pom.xml"))
                .as("working directory must be the project root, but was %s", root)
                .isRegularFile();

        List<String> violations = findViolations(root);

        assertThat(violations).as("AC-H violations:%n%s", String.join("\n", violations)).isEmpty();
    }

    @Test
    void rootPomAloneIsAllowed(@TempDir Path root) throws IOException {
        write(root, "pom.xml");
        write(root, "src/main/resources/static/js/app.js");

        assertThat(findViolations(root)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
        "src/main/resources/static/package.json", "web/yarn.lock", "module/pom.xml", "a/b/pom.xml"
    })
    void flagsForbiddenFile(String relativePath, @TempDir Path root) throws IOException {
        write(root, "pom.xml");
        write(root, relativePath);

        assertThat(findViolations(root)).containsExactly(relativePath + ": AC-H forbids " + fileName(relativePath));
    }

    @ParameterizedTest
    @ValueSource(strings = {"node_modules", "src/main/resources/static/node_modules"})
    void flagsNodeModulesDirectoryOnceWithoutDescending(String relativePath, @TempDir Path root) throws IOException {
        write(root, "pom.xml");
        write(root, relativePath + "/htmx/package.json");

        assertThat(findViolations(root)).containsExactly(relativePath + "/: AC-H forbids node_modules/");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "target", ".git", ".idea", ".vscode", ".claude", ".claude/worktrees/agent-x",
        "docs/.claude", "src/test/resources/archfixtures"
    })
    void skipsBuildOutputToolingStateAndFixtures(String skippedDirectory, @TempDir Path root) throws IOException {
        write(root, "pom.xml");
        write(root, skippedDirectory + "/pom.xml");
        write(root, skippedDirectory + "/package.json");
        Files.createDirectories(root.resolve(skippedDirectory).resolve("node_modules"));

        assertThat(findViolations(root)).isEmpty();
    }

    @Test
    void doesNotSkipDirectoriesMerelyNamedLikeBuildOutputBelowTheRoot(@TempDir Path root) throws IOException {
        write(root, "pom.xml");
        write(root, "module/target/pom.xml");

        assertThat(findViolations(root)).containsExactly("module/target/pom.xml: AC-H forbids pom.xml");
    }

    /**
     * Walks the tree under {@code root} and returns one entry per forbidden file or directory, naming its
     * root-relative path and the constraint.
     */
    static List<String> findViolations(Path root) throws IOException {
        List<String> violations = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (dir.equals(root)) {
                    return FileVisitResult.CONTINUE;
                }
                String name = dir.getFileName().toString();
                Path relative = root.relativize(dir);
                if (SKIPPED_DIRECTORY_NAMES.contains(name) || SKIPPED_RELATIVE_PATHS.contains(relative)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (name.equals(NODE_MODULES)) {
                    violations.add(displayPath(relative) + "/: AC-H forbids " + NODE_MODULES + "/");
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Path relative = root.relativize(file);
                String name = file.getFileName().toString();
                boolean nestedPom = name.equals(POM) && relative.getNameCount() > 1;
                if (FORBIDDEN_FILE_NAMES.contains(name) || nestedPom) {
                    violations.add(displayPath(relative) + ": AC-H forbids " + name);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return violations;
    }

    private static String displayPath(Path relative) {
        return relative.toString().replace('\\', '/');
    }

    private static String fileName(String relativePath) {
        return Path.of(relativePath).getFileName().toString();
    }

    private static void write(Path root, String relativePath) throws IOException {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{}\n", StandardCharsets.UTF_8);
    }
}
