package io.quarkiverse.docling.runtime.client;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import ai.docling.api.serve.DoclingServeApi;
import ai.docling.api.serve.convert.request.ConvertDocumentRequest;
import ai.docling.api.serve.convert.request.options.ConvertDocumentOptions;
import ai.docling.api.serve.convert.request.options.OutputFormat;
import ai.docling.api.serve.convert.request.source.FileSource;
import ai.docling.api.serve.convert.request.source.HttpSource;
import ai.docling.api.serve.convert.response.ConvertDocumentResponse;

/**
 * Service class for interacting with the Docling API. Provides methods for document conversion
 * and health status checks of the external API service.
 */
public class DoclingService {
    private final DoclingServeApi doclingServeApi;

    /**
     * Enumerates the possible statuses with an optional associated string representation.
     */
    public enum Status {
        OK("ok"),
        NOT_OK;

        private final String statusText;

        Status(String statusText) {
            this.statusText = statusText;
        }

        Status() {
            this(null);
        }

        public static Optional<Status> from(String statusText) {
            return Optional.ofNullable(statusText)
                    .flatMap(st -> Arrays.stream(values())
                            .filter(status -> st.equals(status.statusText))
                            .findFirst());
        }
    }

    public DoclingService(DoclingServeApi doclingServeApi) {
        this.doclingServeApi = doclingServeApi;
    }

    /**
     * Converts a document from a URI to given output format.
     *
     * @param uri of document to convert. It should be reachable from docling server.
     * @param outputFormat of the parsed document.
     * @return The response from docling serve.
     */
    public ConvertDocumentResponse convertFromUri(URI uri, OutputFormat outputFormat) {
        var httpSource = HttpSource.builder()
                .url(uri)
                .build();

        var options = ConvertDocumentOptions.builder()
                .toFormat(outputFormat)
                .build();

        var conversionRequest = ConvertDocumentRequest.builder()
                .source(httpSource)
                .options(options)
                .build();

        return this.doclingServeApi.convertSource(conversionRequest);
    }

    /**
     * Converts a document from a byte[] to given output format.
     *
     * @param content as chunk of bytes
     * @param filename of input. Used for detecting input format.
     * @param outputFormat of the parsed document.
     * @return The response from docling serve.
     */
    public ConvertDocumentResponse convertFromBytes(byte[] content, String filename, OutputFormat outputFormat) {
        String base64Document = Base64.getEncoder().encodeToString(content);

        return convertFromBase64(base64Document, filename, outputFormat);
    }

    /**
     * Converts a document from a file to the specified output format.
     *
     * @param file the path of the file to be converted. Must not be null, must exist, and must be a regular file.
     * @param outputFormat the desired output format for the parsed document.
     * @return the response from the Docling service after converting the document.
     * @throws IOException if an I/O error occurs while reading the file.
     * @throws IllegalArgumentException if the file is null, does not exist, or is not a regular file.
     */
    public ConvertDocumentResponse convertFile(Path file, OutputFormat outputFormat) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }

        if (!Files.exists(file)) {
            throw new IllegalArgumentException("file does not exist");
        }

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("file %s is not a regular file".formatted(file));
        }

        return convertFromBytes(Files.readAllBytes(file), file.getFileName().toString(), outputFormat);
    }

    /**
     * Converts a document from a Base64 string to given output format.
     *
     * @param base64Content
     * @param filename of input. Used for detecting input format.
     * @param outputFormat of the parsed document.
     * @return The response from docling serve.
     */
    public ConvertDocumentResponse convertFromBase64(String base64Content, String filename, OutputFormat outputFormat) {
        var options = ConvertDocumentOptions.builder()
                .toFormat(outputFormat)
                .build();

        var fileSource = FileSource.builder()
                .filename(filename)
                .base64String(base64Content)
                .build();

        var conversionRequest = ConvertDocumentRequest.builder()
                .source(fileSource)
                .options(options)
                .build();

        return this.doclingServeApi.convertSource(conversionRequest);
    }

    /**
     * Checks the health status of the external Docling API service.
     *
     * @return {@code true} if the health status returned from the service is "ok"; {@code false} otherwise.
     */
    public boolean isHealthy() {
        return Status.from(this.doclingServeApi.health().getStatus())
                .map(status -> status == Status.OK)
                .orElse(false);
    }
}
