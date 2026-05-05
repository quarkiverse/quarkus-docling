package io.quarkiverse.docling.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import jakarta.enterprise.context.ApplicationScoped;

import ai.docling.serve.api.convert.request.options.OutputFormat;
import io.quarkiverse.docling.AssertionHelper;
import io.quarkiverse.docling.runtime.client.DoclingService;

@ApplicationScoped
public class DoclingServiceTester {
    private final DoclingService doclingService;

    public DoclingServiceTester(DoclingService doclingService) {
        this.doclingService = doclingService;
    }

    public void health() {
        assertThat(this.doclingService.isHealthy())
                .isTrue();
    }

    public void convertBytes() throws IOException {
        var storyPdf = Path.of("src/main/resources/story.pdf");
        var pdfBytes = Files.readAllBytes(storyPdf);
        var response = this.doclingService.convertFromBytes(pdfBytes, "story.pdf", OutputFormat.MARKDOWN);

        AssertionHelper.assertStoryPdfResponse(response);
    }

    public void convertFile() throws IOException, URISyntaxException {
        var response = this.doclingService.convertFile(Path.of("src", "main", "resources", "story.pdf"), OutputFormat.MARKDOWN);
        AssertionHelper.assertStoryPdfResponse(response);
    }

    public void convertFromUri() {
        var response = this.doclingService.convertFromUri(URI.create("https://docs.quarkiverse.io/quarkus-docling/dev"),
                OutputFormat.MARKDOWN);

        AssertionHelper.assertStoryPdfResponse(response, "dev");
    }

    public void convertFromBase64() throws IOException {
        var storyPdf = Path.of("src/main/resources/story.pdf");
        var pdfContent = Base64.getEncoder().encodeToString(Files.readAllBytes(storyPdf));
        var response = this.doclingService.convertFromBase64(pdfContent, "story.pdf", OutputFormat.MARKDOWN);

        AssertionHelper.assertStoryPdfResponse(response);
    }

    public void convertNullFile() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> this.doclingService.convertFile(null, OutputFormat.MARKDOWN))
                .withMessage("file cannot be null");
    }

    public void convertNonExistingFile(Path tempDir) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> this.doclingService.convertFile(tempDir.resolve("file.pdf"), OutputFormat.MARKDOWN))
                .withMessage("file does not exist");
    }

    public void convertNonFile(Path tempDir) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> this.doclingService.convertFile(tempDir, OutputFormat.MARKDOWN))
                .withMessage("file %s is not a regular file", tempDir);
    }
}
