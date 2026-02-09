package io.quarkiverse.docling.tests;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Disabled
class DoclingServiceTests {
    @Inject
    DoclingServiceTester doclingServiceTester;

    @Test
    void health() {
        this.doclingServiceTester.health();
    }

    @Test
    void convertBytes() throws IOException {
        this.doclingServiceTester.convertBytes();
    }

    @Test
    void convertFile() throws IOException, URISyntaxException {
        this.doclingServiceTester.convertFile();
    }

    @Test
    void convertFromUri() {
        this.doclingServiceTester.convertFromUri();
    }

    @Test
    void convertFromBase64() throws IOException {
        this.doclingServiceTester.convertFromBase64();
    }

    @Test
    void convertNullFile() {
        this.doclingServiceTester.convertNullFile();
    }

    @Test
    void convertNonExistingFile(@TempDir Path tempDir) {
        this.doclingServiceTester.convertNonExistingFile(tempDir);
    }

    @Test
    void convertNonFile(@TempDir Path tempDir) {
        this.doclingServiceTester.convertNonFile(tempDir);
    }
}
