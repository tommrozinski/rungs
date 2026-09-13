package com.rungs.readiness.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * 034 REQ-001: {@code VERSIONS.md} records the vendored htmx file's real SHA-256, its pinned version, source, and
 * license, so a swapped-in file is caught.
 */
class VendorVersionsTests {

    private static final Path VENDOR = Path.of("src", "main", "resources", "static", "js", "vendor");

    private static final Pattern SHA_256 = Pattern.compile("SHA-256: `([0-9a-f]{64})`");

    @Test
    void recordedShaMatchesTheVendoredHtmxFile() throws IOException, NoSuchAlgorithmException {
        String versions = Files.readString(VENDOR.resolve("VERSIONS.md"));
        Matcher matcher = SHA_256.matcher(versions);
        assertThat(matcher.find()).as("VERSIONS.md names a SHA-256").isTrue();
        String recorded = matcher.group(1);

        byte[] bytes = Files.readAllBytes(VENDOR.resolve("htmx.min.js"));
        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));

        assertThat(recorded).isEqualTo(actual);
    }

    @Test
    void versionsFileNamesThePinnedVersionAndTheZeroClauseBsdLicense() throws IOException {
        String versions = Files.readString(VENDOR.resolve("VERSIONS.md"));

        assertThat(versions).contains("2.0.10").contains("0BSD");
    }
}
