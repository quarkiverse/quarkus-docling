package io.quarkiverse.docling;

import java.io.IOException;
import java.net.URISyntaxException;

import jakarta.ws.rs.Path;

import org.assertj.core.util.Files;

import io.quarkiverse.docling.tests.DoclingServiceTester;

@Path("/docling")
public class DoclingResource {
    private final DoclingServiceTester doclingServiceTester;

    public DoclingResource(DoclingServiceTester doclingServiceTester) {
        this.doclingServiceTester = doclingServiceTester;
    }

    @Path("/health")
    public void isHealthy() {
        this.doclingServiceTester.health();
    }

    @Path("/convertBytes")
    public void convertBytes() throws IOException {
        this.doclingServiceTester.convertBytes();
    }

    @Path("/convertFile")
    public void convertFile() throws IOException, URISyntaxException {
        this.doclingServiceTester.convertFile();
    }

    @Path("/convertFromUri")
    public void convertFromUri() {
        this.doclingServiceTester.convertFromUri();
    }

    @Path("/convertFromBase64")
    public void convertFromBase64() throws IOException {
        this.doclingServiceTester.convertFromBase64();
    }

    @Path("/convertNullFile")
    public void convertNullFile() {
        this.doclingServiceTester.convertNullFile();
    }

    @Path("/convertNonExistingFile")
    public void convertNonExistingFile() {
        this.doclingServiceTester.convertNonExistingFile(Files.newTemporaryFolder().toPath());
    }

    @Path("/convertNonFile")
    public void convertNonFile() {
        this.doclingServiceTester.convertNonFile(Files.newTemporaryFolder().toPath());
    }
}
