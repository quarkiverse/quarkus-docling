package io.quarkiverse.docling.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import io.quarkiverse.docling.runtime.client.DoclingService;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DoclingTests {
    @Inject
    DoclingService doclingService;

    @Test
    void health() {
        assertThat(this.doclingService.isHealthy())
                .isTrue();
    }

    @Test
    void convertBytes() throws IOException {
        var storyPdf = Path.of("src/test/resources/story.pdf");
        var pdfBytes = Files.readAllBytes(storyPdf);
        var response = this.doclingService.convertFromBytes(pdfBytes, "story.pdf", OutputFormat.MARKDOWN);

        assertStoryPdfResponse(response);
    }

    @Test
    void convertFile() throws IOException {
        var storyPdf = Path.of("src/test/resources/story.pdf");
        var response = this.doclingService.convertFile(storyPdf, OutputFormat.MARKDOWN);

        assertStoryPdfResponse(response);
    }

    @Test
    void convertFromUri() {
        var response = this.doclingService.convertFromUri(URI.create("https://docs.quarkiverse.io/quarkus-docling/dev"),
                OutputFormat.MARKDOWN);

        assertStoryPdfResponse(response, "dev");
    }

    @Test
    void convertFromBase64() throws IOException {
        var storyPdf = Path.of("src/test/resources/story.pdf");
        var pdfContent = Base64.getEncoder().encodeToString(Files.readAllBytes(storyPdf));
        var response = this.doclingService.convertFromBase64(pdfContent, "story.pdf", OutputFormat.MARKDOWN);

        assertStoryPdfResponse(response);
    }

    @Test
    void convertNullFile() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> this.doclingService.convertFile(null, OutputFormat.MARKDOWN))
                .withMessage("file cannot be null");
    }

    @Test
    void convertNonExistingFile(@TempDir Path tempDir) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> this.doclingService.convertFile(tempDir.resolve("file.pdf"), OutputFormat.MARKDOWN))
                .withMessage("file does not exist");
    }

    @Test
    void convertNonFile(@TempDir Path tempDir) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> this.doclingService.convertFile(tempDir, OutputFormat.MARKDOWN))
                .withMessage("file %s is not a regular file", tempDir);
    }

    private static void assertStoryPdfResponse(ConvertDocumentResponse response) {
        assertStoryPdfResponse(response, "story.pdf");
    }

    private static void assertStoryPdfResponse(ConvertDocumentResponse response, String filename) {
        assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.getStatus()).as("response status")
                            .isNotEmpty();

                    assertThat(r.getDocument())
                            .as("response document")
                            .isNotNull()
                            .satisfies(doc -> {
                                assertThat(doc.getFilename()).isEqualTo(filename);
                                assertThat(doc.getMarkdownContent()).isNotEmpty();
                            });

                    if (r.getProcessingTime() != null) {
                        assertThat(r.getProcessingTime()).isPositive();
                    }
                });
    }
}
