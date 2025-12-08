package io.quarkiverse.docling.runtime.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.chunk.request.HierarchicalChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.request.source.FileSource;
import ai.docling.serve.api.convert.request.source.HttpSource;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;

/**
 * Service class for interacting with the Docling API. Provides methods for document conversion
 * and health status checks of the external API service.
 */
public class DoclingService {
    private final DoclingServeApi doclingServeApi;

    /**
     * Defines the types of document chunking strategies available for processing documents.
     *
     * The chunking strategies determine how a document is segmented into smaller parts:
     * - HIERARCHICAL: A structured approach that organizes the document into a tree-like
     * hierarchy of nested chunks based on the document's logical structure.
     * - HYBRID: A mixed approach that combines multiple strategies for chunking
     * to suit specific processing or output requirements.
     */
    public enum ChunkType {
        HIERARCHICAL,
        HYBRID
    }

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
     * Processes a hybrid chunking operation on a document accessible via a URI.
     *
     * @param uri the URI of the document to be chunked. The resource should be reachable by the Docling server.
     * @param outputFormat the desired output format for the chunked document.
     * @return the response containing the result of the hybrid chunking operation.
     */
    public ChunkDocumentResponse hybridChunkFromUri(URI uri, OutputFormat outputFormat) {
        var httpSource = HttpSource.builder()
                .url(uri)
                .build();

        var options = ConvertDocumentOptions.builder()
                .toFormat(outputFormat)
                .build();

        var conversionRequest = HybridChunkDocumentRequest.builder()
                .source(httpSource)
                .options(options)
                .build();

        return this.doclingServeApi.chunkSourceWithHybridChunker(conversionRequest);
    }

    /**
     * Processes a hierarchical chunking operation on a document accessible via a URI.
     *
     * @param uri the URI of the document to be chunked. The resource should be reachable by the Docling server.
     * @param outputFormat the desired output format for the chunked document.
     * @return the response containing the result of the hierarchical chunking operation.
     */
    public ChunkDocumentResponse hierarchicalChunkFromUri(URI uri, OutputFormat outputFormat) {
        var httpSource = HttpSource.builder()
                .url(uri)
                .build();

        var options = ConvertDocumentOptions.builder()
                .toFormat(outputFormat)
                .build();

        var conversionRequest = HierarchicalChunkDocumentRequest.builder()
                .source(httpSource)
                .options(options)
                .build();

        return this.doclingServeApi.chunkSourceWithHierarchicalChunker(conversionRequest);
    }

    /**
     * Converts a document from a InputStream to given output format.
     * This method reads all InputStream into a byte[].
     *
     * @param content as input stream.
     * @param filename of input. Used for detecting input format.
     * @param outputFormat of the parsed document.
     * @return The response from docling serve.
     * @throws IOException when a problem reading the InputStream
     */
    public ConvertDocumentResponse convertFromInputStream(InputStream content, String filename, OutputFormat outputFormat)
            throws IOException {
        return convertFromBytes(content.readAllBytes(), filename, outputFormat);
    }

    /**
     * Processes a chunking operation on a document provided as an input stream.
     * This method reads all InputStream into a byte[].
     *
     * @param content the input stream representing the document content to be chunked.
     * @param filename the name of the input file. This is used to infer the file type of the document.
     * @param outputFormat the desired output format for the chunked document.
     * @param chunkType the type of chunking strategy to apply. It determines how the document will be segmented.
     * @return the response containing the result of the chunking operation.
     * @throws IOException when a problem reading the InputStream
     */
    public ChunkDocumentResponse chunkFromInputStream(InputStream content, String filename, OutputFormat outputFormat,
            ChunkType chunkType) throws IOException {
        return chunkFromBytes(content.readAllBytes(), filename, outputFormat, chunkType);
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
     * Processes a chunking operation on a document provided as a byte array.
     *
     * @param content the byte array representing the document content to be chunked.
     * @param filename the name of the input file. This is used to infer the file type of the document.
     * @param outputFormat the desired output format for the chunked document.
     * @param chunkType the type of chunking strategy to apply. It determines how the document will be segmented.
     * @return the response containing the result of the chunking operation.
     */
    public ChunkDocumentResponse chunkFromBytes(byte[] content, String filename, OutputFormat outputFormat,
            ChunkType chunkType) {
        String base64Document = Base64.getEncoder().encodeToString(content);

        return chunkFromBase64(base64Document, filename, outputFormat, chunkType);
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
     * Processes a hybrid chunking operation on a file and converts it into a specified output format.
     *
     * @param file the path of the file to be chunked. Must not be null, must exist, and must be a regular file.
     * @param outputFormat the desired output format for the chunked document.
     * @return the response containing the result of the hybrid chunking operation.
     * @throws IOException if an I/O error occurs while reading the file.
     * @throws IllegalArgumentException if the file is null, does not exist, or is not a regular file.
     */
    public ChunkDocumentResponse chunkFileHybrid(Path file, OutputFormat outputFormat) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }

        if (!Files.exists(file)) {
            throw new IllegalArgumentException("file does not exist");
        }

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("file %s is not a regular file".formatted(file));
        }

        return chunkFromBytes(Files.readAllBytes(file), file.getFileName().toString(), outputFormat, ChunkType.HYBRID);
    }

    /**
     * Processes a hierarchical chunking operation on a file and converts it into a specified output format.
     *
     * @param file the path of the file to be chunked. Must not be null, must exist, and must be a regular file.
     * @param outputFormat the desired output format for the chunked document.
     * @return the response containing the result of the hierarchical chunking operation.
     * @throws IOException if an I/O error occurs while reading the file.
     * @throws IllegalArgumentException if the file is null, does not exist, or is not a regular file.
     */
    public ChunkDocumentResponse chunkFileHierarchical(Path file, OutputFormat outputFormat) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }

        if (!Files.exists(file)) {
            throw new IllegalArgumentException("file does not exist");
        }

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("file %s is not a regular file".formatted(file));
        }

        return chunkFromBytes(Files.readAllBytes(file), file.getFileName().toString(), outputFormat, ChunkType.HIERARCHICAL);
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
     * Processes a chunking operation on a document provided as a Base64-encoded string.
     *
     * @param base64Content the Base64 string representing the document content to be chunked.
     *        The string should be a valid Base64 encoding of the document.
     * @param filename the name of the input file. This is used to infer the file type of the document.
     * @param outputFormat the desired output format for the chunked document.
     * @param chunkType the type of chunking strategy to apply. It determines how the document will be segmented.
     *        Supported values are defined in {@code DoclingService.ChunkType}.
     * @return the response containing the result of the chunking operation.
     */
    public ChunkDocumentResponse chunkFromBase64(String base64Content, String filename, OutputFormat outputFormat,
            DoclingService.ChunkType chunkType) {
        var options = ConvertDocumentOptions.builder()
                .toFormat(outputFormat)
                .build();

        var fileSource = FileSource.builder()
                .filename(filename)
                .base64String(base64Content)
                .build();

        return switch (chunkType) {
            case HYBRID -> this.doclingServeApi.chunkSourceWithHybridChunker(
                    HybridChunkDocumentRequest.builder()
                            .source(fileSource)
                            .options(options)
                            .build());

            case HIERARCHICAL -> this.doclingServeApi.chunkSourceWithHierarchicalChunker(
                    HierarchicalChunkDocumentRequest.builder()
                            .source(fileSource)
                            .options(options)
                            .build());
        };
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
