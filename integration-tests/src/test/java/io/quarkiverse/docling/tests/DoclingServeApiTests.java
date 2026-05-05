package io.quarkiverse.docling.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.docling.core.DoclingDocument;
import ai.docling.core.DoclingDocument.DocItemLabel;
import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.chunk.request.HierarchicalChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.options.HierarchicalChunkerOptions;
import ai.docling.serve.api.chunk.request.options.HybridChunkerOptions;
import ai.docling.serve.api.chunk.response.Chunk;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.clear.request.ClearConvertersRequest;
import ai.docling.serve.api.clear.request.ClearResultsRequest;
import ai.docling.serve.api.clear.response.ClearResponse;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.ImageRefMode;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.request.options.TableFormerMode;
import ai.docling.serve.api.convert.request.source.HttpSource;
import ai.docling.serve.api.convert.request.target.ZipTarget;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.api.convert.response.InBodyConvertDocumentResponse;
import ai.docling.serve.api.convert.response.ResponseType;
import ai.docling.serve.api.convert.response.ZipArchiveConvertDocumentResponse;
import ai.docling.serve.api.health.HealthCheckResponse;
import ai.docling.serve.api.task.request.TaskStatusPollRequest;
import ai.docling.serve.api.util.FileUtils;
import ai.docling.serve.api.validation.ValidationError;
import ai.docling.serve.api.validation.ValidationErrorContext;
import ai.docling.serve.api.validation.ValidationErrorDetail;
import ai.docling.serve.api.validation.ValidationException;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;

@QuarkusTest
class DoclingServeApiTests {
    @Inject
    DoclingServeApi doclingServeApi;

    @Nested
    class ClearTests {
        @Test
        void shouldClearConvertersSuccessfully() {
            var response = doclingServeApi.clearConverters(ClearConvertersRequest.builder().build());

            assertThat(response)
                    .isNotNull()
                    .extracting(ClearResponse::getStatus)
                    .isEqualTo("ok");
        }

        @Test
        void shouldClearResultsSuccessfully() {
            var response = doclingServeApi.clearResults(ClearResultsRequest.builder().build());

            assertThat(response)
                    .isNotNull()
                    .extracting(ClearResponse::getStatus)
                    .isEqualTo("ok");
        }
    }

    @Nested
    class TaskTests {
        @Test
        void pollInvalidTaskId() {
            var request = TaskStatusPollRequest.builder()
                    .taskId("someInvalidTaskId")
                    .build();

            assertThatThrownBy(() -> doclingServeApi.pollTaskStatus(request))
                    .asInstanceOf(InstanceOfAssertFactories.throwable(WebApplicationException.class))
                    .extracting(ex -> ex.getResponse().getStatus())
                    .isEqualTo(404);
        }
    }

    @Nested
    class HealthTests {
        @Test
        void shouldSuccessfullyCallHealthEndpoint() {
            HealthCheckResponse response = doclingServeApi.health();

            assertThat(response)
                    .isNotNull()
                    .extracting(HealthCheckResponse::getStatus)
                    .isEqualTo("ok");
        }
    }

    @Nested
    class ConvertTests {
        private static InBodyConvertDocumentResponse assertConvertInBodySource(ConvertDocumentResponse response) {
            return assertThat(response)
                    .isNotNull()
                    .asInstanceOf(InstanceOfAssertFactories.type(InBodyConvertDocumentResponse.class))
                    .actual();
        }

        static void assertConvertHttpSource(ConvertDocumentResponse res) {
            var response = assertConvertInBodySource(res);

            assertThat(response.getStatus()).isNotEmpty();
            assertThat(response.getDocument()).isNotNull();
            assertThat(response.getDocument().getFilename()).isNotEmpty();

            if (response.getProcessingTime() != null) {
                assertThat(response.getProcessingTime()).isPositive();
            }

            assertThat(response.getDocument().getMarkdownContent()).isNotEmpty();
        }

        static void assertZipArchiveEntries(InputStream inputStream, Set<String> expectedEntries) {
            var actualEntries = new TreeSet<>();

            try (var zipInputStream = new ZipInputStream(inputStream)) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    actualEntries.add(entry.getName());
                    Log.infof("Found entry in ZIP: %s (size: %d bytes)", entry.getName(), entry.getSize());
                    zipInputStream.closeEntry();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            assertThat(actualEntries)
                    .containsExactlyInAnyOrderElementsOf(expectedEntries);
        }

        @Test
        void shouldThrowValidationError() {
            var file = Path.of("src", "main", "resources", "story.pdf");

            assertThat(file)
                    .exists()
                    .isRegularFile();

            var source = HttpSource.builder()
                    .url(file.toUri())
                    .build();

            var options = ConvertDocumentOptions.builder()
                    .toFormat(OutputFormat.MARKDOWN)
                    .build();

            var request = ConvertDocumentRequest.builder()
                    .source(source)
                    .options(options)
                    .build();

            assertThatThrownBy(() -> doclingServeApi.convertSource(request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageStartingWith("An error occurred while making request to ")
                    .asInstanceOf(InstanceOfAssertFactories.throwable(ValidationException.class))
                    .extracting(ValidationException::getValidationError)
                    .isNotNull()
                    .extracting(ValidationError::getErrorDetails)
                    .asInstanceOf(InstanceOfAssertFactories.list(ValidationErrorDetail.class))
                    .singleElement()
                    .usingRecursiveComparison()
                    .isEqualTo(
                            ValidationErrorDetail.builder()
                                    .type("url_scheme")
                                    .message("URL scheme should be 'http' or 'https'")
                                    .locations(List.of("body", "sources", 0, "http", "url"))
                                    .context(
                                            ValidationErrorContext.builder()
                                                    .expectedSchemes("'http' or 'https'")
                                                    .build())
                                    .build());
        }

        @Test
        void shouldConvertHttpSourceSuccessfully() {
            var request = ConvertDocumentRequest.builder()
                    .source(HttpSource.builder().url(URI.create("https://docs.arconia.io/arconia-cli/latest/development/dev/"))
                            .build())
                    .build();

            var response = doclingServeApi.convertSource(request);
            assertConvertHttpSource(response);
        }

        @Test
        void shouldConvertMultipleFileSourcesAsync() {
            var files = new Path[] {
                    Path.of("src", "main", "resources", "2408.09869.pdf"),
                    Path.of("src", "main", "resources", "story.pdf")
            };

            var requestBuilder = ConvertDocumentRequest.builder();

            FileUtils.createFileSources(files)
                    .forEach(requestBuilder::source);

            var response = doclingServeApi.convertSourceAsync(requestBuilder.build()).toCompletableFuture().join();

            assertThat(response)
                    .isNotNull()
                    .asInstanceOf(InstanceOfAssertFactories.type(ZipArchiveConvertDocumentResponse.class))
                    .satisfies(r -> assertZipArchiveEntries(r.getInputStream(), Set.of("2408.09869.md", "story.md")))
                    .extracting(
                            ZipArchiveConvertDocumentResponse::getFileName,
                            ZipArchiveConvertDocumentResponse::getResponseType)
                    .containsExactly(
                            "converted_docs.zip",
                            ResponseType.ZIP_ARCHIVE);
        }

        @Test
        void shouldConvertMultipleFileSources() {
            var files = new Path[] {
                    Path.of("src", "main", "resources", "2408.09869.pdf"),
                    Path.of("src", "main", "resources", "story.pdf")
            };

            var requestBuilder = ConvertDocumentRequest.builder();

            FileUtils.createFileSources(files)
                    .forEach(requestBuilder::source);

            var response = doclingServeApi.convertSource(requestBuilder.build());

            assertThat(response)
                    .isNotNull()
                    .asInstanceOf(InstanceOfAssertFactories.type(ZipArchiveConvertDocumentResponse.class))
                    .satisfies(r -> assertZipArchiveEntries(r.getInputStream(), Set.of("2408.09869.md", "story.md")))
                    .extracting(
                            ZipArchiveConvertDocumentResponse::getFileName,
                            ZipArchiveConvertDocumentResponse::getResponseType)
                    .containsExactly(
                            "converted_docs.zip",
                            ResponseType.ZIP_ARCHIVE);
        }

        @Test
        void shouldConvertSingleFileSourceWithZipTargetAndReferencedImageExportModeAsync() {
            var requestBuilder = ConvertDocumentRequest
                    .builder()
                    .target(ZipTarget.builder().build())
                    .options(
                            ConvertDocumentOptions.builder()
                                    .imageExportMode(ImageRefMode.REFERENCED)
                                    .build());

            FileUtils.createFileSources(Path.of("src", "main", "resources", "2408.09869.pdf"))
                    .forEach(requestBuilder::source);

            var response = doclingServeApi.convertSourceAsync(requestBuilder.build()).toCompletableFuture().join();

            assertThat(response)
                    .isNotNull()
                    .asInstanceOf(InstanceOfAssertFactories.type(ZipArchiveConvertDocumentResponse.class))
                    .satisfies(r -> assertZipArchiveEntries(r.getInputStream(), Set.of("2408.09869.md", "artifacts/",
                            "artifacts/image_000000_4f05ea6de89ce20493a5d9cc2305a4feb948c7bb794d7b81ee29554ec56b8445.png")))
                    .extracting(
                            ZipArchiveConvertDocumentResponse::getFileName,
                            ZipArchiveConvertDocumentResponse::getResponseType)
                    .containsExactly(
                            "converted_docs.zip",
                            ResponseType.ZIP_ARCHIVE);
        }

        @Test
        void shouldConvertSingleFileSourceWithZipTargetAndReferencedImageExportMode() {
            var requestBuilder = ConvertDocumentRequest
                    .builder()
                    .target(ZipTarget.builder().build())
                    .options(
                            ConvertDocumentOptions.builder()
                                    .imageExportMode(ImageRefMode.REFERENCED)
                                    .build());

            FileUtils.createFileSources(Path.of("src", "main", "resources", "2408.09869.pdf"))
                    .forEach(requestBuilder::source);

            var response = doclingServeApi.convertSource(requestBuilder.build());

            assertThat(response)
                    .isNotNull()
                    .asInstanceOf(InstanceOfAssertFactories.type(ZipArchiveConvertDocumentResponse.class))
                    .satisfies(r -> assertZipArchiveEntries(r.getInputStream(), Set.of("2408.09869.md", "artifacts/",
                            "artifacts/image_000000_4f05ea6de89ce20493a5d9cc2305a4feb948c7bb794d7b81ee29554ec56b8445.png")))
                    .extracting(
                            ZipArchiveConvertDocumentResponse::getFileName,
                            ZipArchiveConvertDocumentResponse::getResponseType)
                    .containsExactly(
                            "converted_docs.zip",
                            ResponseType.ZIP_ARCHIVE);
        }

        @Test
        void shouldConvertFileSuccessfully() {
            var response = assertConvertInBodySource(
                    doclingServeApi.convertFiles(Path.of("src", "main", "resources", "story.pdf")));

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isNotEmpty();
            assertThat(response.getDocument()).isNotNull();
            assertThat(response.getDocument().getFilename()).isEqualTo("story.pdf");

            if (response.getProcessingTime() != null) {
                assertThat(response.getProcessingTime()).isPositive();
            }

            assertThat(response.getDocument().getMarkdownContent()).isNotEmpty();
        }

        @Test
        void shouldHandleConversionWithDifferentDocumentOptions() {
            var options = ConvertDocumentOptions.builder()
                    .doOcr(true)
                    .includeImages(true)
                    .tableMode(TableFormerMode.FAST)
                    .documentTimeout(Duration.ofMinutes(1))
                    .build();

            var request = ConvertDocumentRequest.builder()
                    .source(HttpSource.builder().url(URI.create("https://docs.arconia.io/arconia-cli/latest/development/dev/"))
                            .build())
                    .options(options)
                    .build();

            var response = assertConvertInBodySource(doclingServeApi.convertSource(request));

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isNotEmpty();
            assertThat(response.getDocument()).isNotNull();
        }

        @Test
        void shouldHandleResponseWithDoclingDocument() {
            ConvertDocumentOptions options = ConvertDocumentOptions.builder()
                    .toFormat(OutputFormat.JSON)
                    .build();

            ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                    .source(HttpSource.builder().url(URI.create("https://docs.arconia.io/arconia-cli/latest/development/dev/"))
                            .build())
                    .options(options)
                    .build();

            var response = assertConvertInBodySource(doclingServeApi.convertSource(request));

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isNotEmpty();
            assertThat(response.getDocument()).isNotNull();

            DoclingDocument doclingDocument = response.getDocument().getJsonContent();
            assertThat(doclingDocument).isNotNull();
            assertThat(doclingDocument.getName()).isNotEmpty();
            assertThat(doclingDocument.getTexts().get(0).getLabel()).isEqualTo(DocItemLabel.TITLE);
        }

        @Test
        void shouldConvertSourceAsync() {
            ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                    .source(HttpSource.builder().url(URI.create("https://docs.arconia.io/arconia-cli/latest/development/dev/"))
                            .build())
                    .build();

            var response = assertConvertInBodySource(doclingServeApi.convertSourceAsync(request).toCompletableFuture().join());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isNotEmpty();
            assertThat(response.getDocument()).isNotNull();
            assertThat(response.getDocument().getMarkdownContent()).isNotEmpty();
        }

        @Test
        void shouldConvertFileAsync() {
            var response = assertConvertInBodySource(Uni.createFrom().completionStage(
                    doclingServeApi.convertFilesAsync(Path.of("src", "main", "resources", "story.pdf")))
                    .await().atMost(Duration.ofSeconds(10)));

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isNotEmpty();
            assertThat(response.getDocument()).isNotNull();
            assertThat(response.getDocument().getFilename()).isEqualTo("story.pdf");
            assertThat(response.getDocument().getMarkdownContent()).isNotEmpty();
        }

        @Test
        void shouldHandleAsyncConversionWithDifferentDocumentOptions() {
            ConvertDocumentOptions options = ConvertDocumentOptions.builder()
                    .doOcr(true)
                    .includeImages(true)
                    .tableMode(TableFormerMode.FAST)
                    .documentTimeout(Duration.ofMinutes(1))
                    .build();

            ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                    .source(HttpSource.builder().url(URI.create("https://docs.arconia.io/arconia-cli/latest/development/dev/"))
                            .build())
                    .options(options)
                    .build();

            var response = assertConvertInBodySource(doclingServeApi.convertSourceAsync(request).toCompletableFuture().join());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isNotEmpty();
            assertThat(response.getDocument()).isNotNull();
        }

        @Test
        void shouldChainAsyncOperations() {
            ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                    .source(HttpSource.builder().url(URI.create("https://docs.arconia.io/arconia-cli/latest/development/dev/"))
                            .build())
                    .build();

            // Test chaining with thenApply
            String markdownContent = doclingServeApi.convertSourceAsync(request)
                    .thenApply(ConvertTests::assertConvertInBodySource)
                    .thenApply(response -> response.getDocument().getMarkdownContent())
                    .toCompletableFuture().join();

            assertThat(markdownContent).isNotEmpty();
        }

        @Test
        void convertFilesNullFiles() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.convertFiles())
                    .withMessage("files cannot be null or empty");
        }

        @Test
        void convertFilesEmptyFiles() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.convertFiles(new Path[] {}))
                    .withMessage("files cannot be null or empty");
        }

        @Test
        void convertNonExistentFile() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.convertFiles(Path.of("src", "main", "resources", "file1234.pdf")))
                    .withMessage("File (src/main/resources/file1234.pdf) does not exist");
        }

        @Test
        void convertFilesNotRegularFile() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.convertFiles(Path.of("src", "main", "resources")))
                    .withMessage("File (src/main/resources) is not a regular file");
        }

        @Test
        void convertFilesAsyncNullFiles() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.convertFilesAsync())
                    .withMessage("files cannot be null or empty");
        }

        @Test
        void convertFilesAsyncEmptyFiles() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.convertFilesAsync(new Path[] {}))
                    .withMessage("files cannot be null or empty");
        }

        @Test
        void convertAsyncNonExistentFile() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.convertFilesAsync(Path.of("src", "main", "resources", "file1234.pdf")))
                    .withMessage("File (src/main/resources/file1234.pdf) does not exist");
        }

        @Test
        void convertAsyncFilesNotRegularFile() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.convertFilesAsync(Path.of("src", "main", "resources")))
                    .withMessage("File (src/main/resources) is not a regular file");
        }
    }

    @Nested
    class ChunkTests {
        @Test
        void chunkHierarchicalNonExistentFile() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi
                            .chunkFilesWithHierarchicalChunker(Path.of("src", "main", "resources", "file1234.pdf")))
                    .withMessage("File (src/main/resources/file1234.pdf) does not exist");
        }

        @Test
        void chunkHierarchicalFilesNotRegularFile() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.chunkFilesWithHierarchicalChunker(Path.of("src", "main", "resources")))
                    .withMessage("File (src/main/resources) is not a regular file");
        }

        @Test
        void chunkFilesHierarchicalNullFiles() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.chunkFilesWithHierarchicalChunker())
                    .withMessage("files cannot be null or empty");
        }

        @Test
        void chunkFilesHierarchicalEmptyFiles() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.chunkFilesWithHierarchicalChunker(new Path[] {}))
                    .withMessage("files cannot be null or empty");
        }

        @Test
        void chunkHierarchicalAsyncNonExistentFile() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi
                            .chunkFilesWithHierarchicalChunkerAsync(Path.of("src", "main", "resources", "file1234.pdf")))
                    .withMessage("File (src/main/resources/file1234.pdf) does not exist");
        }

        @Test
        void chunkHierarchicalAsyncFilesNotRegularFile() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(
                            () -> doclingServeApi.chunkFilesWithHierarchicalChunkerAsync(Path.of("src", "main", "resources")))
                    .withMessage("File (src/main/resources) is not a regular file");
        }

        @Test
        void chunkFilesHierarchicalAsyncNullFiles() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.chunkFilesWithHierarchicalChunkerAsync())
                    .withMessage("files cannot be null or empty");
        }

        @Test
        void chunkFilesHierarchicalAsyncEmptyFiles() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.chunkFilesWithHierarchicalChunkerAsync(new Path[] {}))
                    .withMessage("files cannot be null or empty");
        }

        @Test
        void chunkHybridNonExistentFile() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi
                            .chunkFilesWithHybridChunker(Path.of("src", "main", "resources", "file1234.pdf")))
                    .withMessage("File (src/main/resources/file1234.pdf) does not exist");
        }

        @Test
        void chunkHybridFilesNotRegularFile() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.chunkFilesWithHybridChunker(Path.of("src", "main", "resources")))
                    .withMessage("File (src/main/resources) is not a regular file");
        }

        @Test
        void chunkHybridAsyncNonExistentFile() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi
                            .chunkFilesWithHybridChunkerAsync(Path.of("src", "main", "resources", "file1234.pdf")))
                    .withMessage("File (src/main/resources/file1234.pdf) does not exist");
        }

        @Test
        void chunkHybridAsyncFilesNotRegularFile() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.chunkFilesWithHybridChunkerAsync(Path.of("src", "main", "resources")))
                    .withMessage("File (src/main/resources) is not a regular file");
        }

        @Test
        void chunkFilesHybridNullFiles() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.chunkFilesWithHybridChunker())
                    .withMessage("files cannot be null or empty");
        }

        @Test
        void chunkFilesHybridEmptyFiles() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.chunkFilesWithHybridChunker(new Path[] {}))
                    .withMessage("files cannot be null or empty");
        }

        @Test
        void chunkFilesHybridAsyncNullFiles() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.chunkFilesWithHybridChunkerAsync())
                    .withMessage("files cannot be null or empty");
        }

        @Test
        void chunkFilesHybridAsyncEmptyFiles() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> doclingServeApi.chunkFilesWithHybridChunkerAsync(new Path[] {}))
                    .withMessage("files cannot be null or empty");
        }

        @Test
        void shouldChainHybridAsyncOperations() {
            var options = ConvertDocumentOptions.builder()
                    .toFormat(OutputFormat.JSON)
                    .build();

            var request = HybridChunkDocumentRequest.builder()
                    .source(HttpSource.builder().url(URI.create("https://docs.arconia.io/arconia-cli/latest/development/dev/"))
                            .build())
                    .options(options)
                    .includeConvertedDoc(true)
                    .chunkingOptions(HybridChunkerOptions.builder()
                            .includeRawText(true)
                            .useMarkdownTables(true)
                            .maxTokens(10000)
                            .tokenizer("sentence-transformers/all-MiniLM-L6-v2")
                            .build())
                    .build();

            // Test chaining with thenApply
            var chunks = doclingServeApi.chunkSourceWithHybridChunkerAsync(request)
                    .thenApply(ChunkDocumentResponse::getChunks)
                    .toCompletableFuture().join();

            assertThat(chunks)
                    .isNotEmpty()
                    .allMatch(chunk -> !chunk.getText().isEmpty());
        }

        @Test
        void shouldChainHierarchicalAsyncOperations() {
            var options = ConvertDocumentOptions.builder()
                    .toFormat(OutputFormat.JSON)
                    .build();

            var request = HierarchicalChunkDocumentRequest.builder()
                    .source(HttpSource.builder().url(URI.create("https://docs.arconia.io/arconia-cli/latest/development/dev/"))
                            .build())
                    .options(options)
                    .includeConvertedDoc(true)
                    .chunkingOptions(HierarchicalChunkerOptions.builder()
                            .includeRawText(true)
                            .useMarkdownTables(true)
                            .build())
                    .build();

            // Test chaining with thenApply
            var chunks = doclingServeApi.chunkSourceWithHierarchicalChunkerAsync(request)
                    .thenApply(ChunkDocumentResponse::getChunks)
                    .toCompletableFuture().join();

            assertThat(chunks)
                    .isNotEmpty()
                    .allMatch(chunk -> !chunk.getText().isEmpty());
        }

        @Test
        void shouldChunkDocumentWithHierarchicalChunkerAsync() {
            ConvertDocumentOptions options = ConvertDocumentOptions.builder()
                    .toFormat(OutputFormat.JSON)
                    .build();

            HierarchicalChunkDocumentRequest request = HierarchicalChunkDocumentRequest.builder()
                    .source(HttpSource.builder().url(URI.create("https://docs.arconia.io/arconia-cli/latest/development/dev/"))
                            .build())
                    .options(options)
                    .includeConvertedDoc(true)
                    .chunkingOptions(HierarchicalChunkerOptions.builder()
                            .includeRawText(true)
                            .useMarkdownTables(true)
                            .build())
                    .build();

            ChunkDocumentResponse response = doclingServeApi.chunkSourceWithHierarchicalChunkerAsync(request)
                    .toCompletableFuture().join();

            assertThat(response).isNotNull();
            assertThat(response.getChunks()).isNotEmpty();
            assertThat(response.getDocuments()).isNotEmpty();
            assertThat(response.getProcessingTime()).isNotNull();

            List<Chunk> chunks = response.getChunks();
            assertThat(chunks).allMatch(chunk -> !chunk.getText().isEmpty());
        }

        @Test
        void shouldChunkFilesWithHierarchicalChunker() {
            var options = ConvertDocumentOptions.builder()
                    .toFormat(OutputFormat.JSON)
                    .build();

            var request = HierarchicalChunkDocumentRequest.builder()
                    .options(options)
                    .includeConvertedDoc(true)
                    .chunkingOptions(HierarchicalChunkerOptions.builder()
                            .includeRawText(true)
                            .useMarkdownTables(true)
                            .build())
                    .build();

            var response = doclingServeApi.chunkFilesWithHierarchicalChunker(request,
                    Path.of("src", "main", "resources", "story.pdf"));

            assertThat(response).isNotNull();
            assertThat(response.getChunks()).isNotEmpty();
            assertThat(response.getDocuments()).isNotEmpty();
            assertThat(response.getProcessingTime()).isNotNull();

            List<Chunk> chunks = response.getChunks();
            assertThat(chunks).allMatch(chunk -> !chunk.getText().isEmpty());
        }

        @Test
        void shouldChunkFilesWithHierarchicalChunkerAsync() {
            var options = ConvertDocumentOptions.builder()
                    .toFormat(OutputFormat.JSON)
                    .build();

            var request = HierarchicalChunkDocumentRequest.builder()
                    .options(options)
                    .includeConvertedDoc(true)
                    .chunkingOptions(HierarchicalChunkerOptions.builder()
                            .includeRawText(true)
                            .useMarkdownTables(true)
                            .build())
                    .build();

            var response = doclingServeApi
                    .chunkFilesWithHierarchicalChunkerAsync(request, Path.of("src", "main", "resources", "story.pdf"))
                    .toCompletableFuture().join();

            assertThat(response).isNotNull();
            assertThat(response.getChunks()).isNotEmpty();
            assertThat(response.getDocuments()).isNotEmpty();
            assertThat(response.getProcessingTime()).isNotNull();

            List<Chunk> chunks = response.getChunks();
            assertThat(chunks).allMatch(chunk -> !chunk.getText().isEmpty());
        }

        @Test
        void shouldChunkDocumentWithHybridChunkerAsync() {
            ConvertDocumentOptions options = ConvertDocumentOptions.builder()
                    .toFormat(OutputFormat.JSON)
                    .build();

            HybridChunkDocumentRequest request = HybridChunkDocumentRequest.builder()
                    .source(HttpSource.builder().url(URI.create("https://docs.arconia.io/arconia-cli/latest/development/dev/"))
                            .build())
                    .options(options)
                    .includeConvertedDoc(true)
                    .chunkingOptions(HybridChunkerOptions.builder()
                            .includeRawText(true)
                            .useMarkdownTables(true)
                            .maxTokens(10000)
                            .tokenizer("sentence-transformers/all-MiniLM-L6-v2")
                            .build())
                    .build();

            ChunkDocumentResponse response = doclingServeApi.chunkSourceWithHybridChunkerAsync(request).toCompletableFuture()
                    .join();

            assertThat(response).isNotNull();
            assertThat(response.getChunks()).isNotEmpty();
            assertThat(response.getDocuments()).isNotEmpty();
            assertThat(response.getProcessingTime()).isNotNull();

            List<Chunk> chunks = response.getChunks();
            assertThat(chunks).allMatch(chunk -> !chunk.getText().isEmpty());
        }

        @Test
        void shouldChunkDocumentWithHierarchicalChunker() {
            ConvertDocumentOptions options = ConvertDocumentOptions.builder()
                    .toFormat(OutputFormat.JSON)
                    .build();

            HierarchicalChunkDocumentRequest request = HierarchicalChunkDocumentRequest.builder()
                    .source(HttpSource.builder().url(URI.create("https://docs.arconia.io/arconia-cli/latest/development/dev/"))
                            .build())
                    .options(options)
                    .includeConvertedDoc(true)
                    .chunkingOptions(HierarchicalChunkerOptions.builder()
                            .includeRawText(true)
                            .useMarkdownTables(true)
                            .build())
                    .build();

            ChunkDocumentResponse response = doclingServeApi.chunkSourceWithHierarchicalChunker(request);

            assertThat(response).isNotNull();
            assertThat(response.getChunks()).isNotEmpty();
            assertThat(response.getDocuments()).isNotEmpty();
            assertThat(response.getProcessingTime()).isNotNull();

            List<Chunk> chunks = response.getChunks();
            assertThat(chunks).allMatch(chunk -> !chunk.getText().isEmpty());
        }

        @Test
        void shouldChunkDocumentWithHybridChunker() {
            ConvertDocumentOptions options = ConvertDocumentOptions.builder()
                    .toFormat(OutputFormat.JSON)
                    .build();

            HybridChunkDocumentRequest request = HybridChunkDocumentRequest.builder()
                    .source(HttpSource.builder().url(URI.create("https://docs.arconia.io/arconia-cli/latest/development/dev/"))
                            .build())
                    .options(options)
                    .includeConvertedDoc(true)
                    .chunkingOptions(HybridChunkerOptions.builder()
                            .includeRawText(true)
                            .useMarkdownTables(true)
                            .maxTokens(10000)
                            .tokenizer("sentence-transformers/all-MiniLM-L6-v2")
                            .build())
                    .build();

            ChunkDocumentResponse response = doclingServeApi.chunkSourceWithHybridChunker(request);

            assertThat(response).isNotNull();
            assertThat(response.getChunks()).isNotEmpty();
            assertThat(response.getDocuments()).isNotEmpty();
            assertThat(response.getProcessingTime()).isNotNull();

            List<Chunk> chunks = response.getChunks();
            assertThat(chunks).allMatch(chunk -> !chunk.getText().isEmpty());
        }

        @Test
        void shouldChunkFileWithHybridChunker() {
            var options = ConvertDocumentOptions.builder()
                    .toFormat(OutputFormat.JSON)
                    .build();

            var request = HybridChunkDocumentRequest.builder()
                    .options(options)
                    .includeConvertedDoc(true)
                    .chunkingOptions(HybridChunkerOptions.builder()
                            .includeRawText(true)
                            .useMarkdownTables(true)
                            .maxTokens(10000)
                            .tokenizer("sentence-transformers/all-MiniLM-L6-v2")
                            .build())
                    .build();

            var response = doclingServeApi.chunkFilesWithHybridChunker(request,
                    Path.of("src", "main", "resources", "story.pdf"));

            assertThat(response).isNotNull();
            assertThat(response.getChunks()).isNotEmpty();
            assertThat(response.getDocuments()).isNotEmpty();
            assertThat(response.getProcessingTime()).isNotNull();

            List<Chunk> chunks = response.getChunks();
            assertThat(chunks).allMatch(chunk -> !chunk.getText().isEmpty());
        }

        @Test
        void shouldChunkFileWithHybridChunkerAsync() {
            var options = ConvertDocumentOptions.builder()
                    .toFormat(OutputFormat.JSON)
                    .build();

            var request = HybridChunkDocumentRequest.builder()
                    .options(options)
                    .includeConvertedDoc(true)
                    .chunkingOptions(HybridChunkerOptions.builder()
                            .includeRawText(true)
                            .useMarkdownTables(true)
                            .maxTokens(10000)
                            .tokenizer("sentence-transformers/all-MiniLM-L6-v2")
                            .build())
                    .build();

            var response = doclingServeApi
                    .chunkFilesWithHybridChunkerAsync(request, Path.of("src", "main", "resources", "story.pdf"))
                    .toCompletableFuture()
                    .join();

            assertThat(response).isNotNull();
            assertThat(response.getChunks()).isNotEmpty();
            assertThat(response.getDocuments()).isNotEmpty();
            assertThat(response.getProcessingTime()).isNotNull();

            List<Chunk> chunks = response.getChunks();
            assertThat(chunks).allMatch(chunk -> !chunk.getText().isEmpty());
        }

        @Test
        void shouldThrowValidationErrorHierarchicalChunker() {
            var file = Path.of("src", "main", "resources", "story.pdf");

            assertThat(file)
                    .exists()
                    .isRegularFile();

            var source = HttpSource.builder()
                    .url(file.toUri())
                    .build();

            var options = ConvertDocumentOptions.builder()
                    .toFormat(OutputFormat.MARKDOWN)
                    .build();

            var request = HierarchicalChunkDocumentRequest.builder()
                    .source(source)
                    .options(options)
                    .build();

            assertThatThrownBy(() -> doclingServeApi.chunkSourceWithHierarchicalChunker(request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageStartingWith("An error occurred while making request to ")
                    .asInstanceOf(InstanceOfAssertFactories.throwable(ValidationException.class))
                    .extracting(ValidationException::getValidationError)
                    .isNotNull()
                    .extracting(ValidationError::getErrorDetails)
                    .asInstanceOf(InstanceOfAssertFactories.list(ValidationErrorDetail.class))
                    .singleElement()
                    .usingRecursiveComparison()
                    .isEqualTo(
                            ValidationErrorDetail.builder()
                                    .type("url_scheme")
                                    .message("URL scheme should be 'http' or 'https'")
                                    .locations(List.of("body", "sources", 0, "http", "url"))
                                    .context(
                                            ValidationErrorContext.builder()
                                                    .expectedSchemes("'http' or 'https'")
                                                    .build())
                                    .build());
        }

        @Test
        void shouldThrowValidationErrorHybridChunker() {
            var file = Path.of("src", "main", "resources", "story.pdf");

            assertThat(file)
                    .exists()
                    .isRegularFile();

            var source = HttpSource.builder()
                    .url(file.toUri())
                    .build();

            var options = ConvertDocumentOptions.builder()
                    .toFormat(OutputFormat.MARKDOWN)
                    .build();

            var request = HybridChunkDocumentRequest.builder()
                    .source(source)
                    .options(options)
                    .build();

            assertThatThrownBy(() -> doclingServeApi.chunkSourceWithHybridChunker(request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageStartingWith("An error occurred while making request to ")
                    .asInstanceOf(InstanceOfAssertFactories.throwable(ValidationException.class))
                    .extracting(ValidationException::getValidationError)
                    .isNotNull()
                    .extracting(ValidationError::getErrorDetails)
                    .asInstanceOf(InstanceOfAssertFactories.list(ValidationErrorDetail.class))
                    .singleElement()
                    .usingRecursiveComparison()
                    .isEqualTo(
                            ValidationErrorDetail.builder()
                                    .type("url_scheme")
                                    .message("URL scheme should be 'http' or 'https'")
                                    .locations(List.of("body", "sources", 0, "http", "url"))
                                    .context(
                                            ValidationErrorContext.builder()
                                                    .expectedSchemes("'http' or 'https'")
                                                    .build())
                                    .build());
        }
    }
}
