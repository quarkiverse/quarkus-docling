package io.quarkiverse.docling.runtime.client;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.chunk.request.HierarchicalChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.clear.request.ClearConvertersRequest;
import ai.docling.serve.api.clear.request.ClearResultsRequest;
import ai.docling.serve.api.clear.response.ClearResponse;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.api.health.HealthCheckResponse;
import ai.docling.serve.api.task.request.TaskResultRequest;
import ai.docling.serve.api.task.request.TaskStatusPollRequest;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;
import ai.docling.serve.api.util.ValidationUtils;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * Implementation of the {@link DoclingServeApi} interface using a Quarkus-based client.
 * This class provides methods to interact with the Docling Serve API for various functionalities
 * such as chunking, document conversion, clearing resources, polling task status, and health checks.
 *
 * <p>
 * The implementation manages both synchronous and asynchronous API calls and relies on the
 * {@link QuarkusDoclingServeClient} for the actual communication with the API endpoints.
 *
 * <p>
 * The polling interval and timeout duration for asynchronous operations are configurable during
 * instantiation of this class.
 */
public class QuarkusDoclingServeApi implements DoclingServeApi {
    private static final Logger LOG = Logger.getLogger(QuarkusDoclingServeApi.class);
    private final QuarkusDoclingServeClient client;
    private final DoclingRuntimeConfig config;
    private final ApiMetadata apiMetadata;

    private enum TaskResultType {
        CHUNK,
        CONVERT
    }

    private QuarkusDoclingServeApi(QuarkusDoclingServeApiBuilder builder) {
        this.client = ValidationUtils.ensureNotNull(builder.client, "client");
        this.config = ValidationUtils.ensureNotNull(builder.config, "config");
        this.apiMetadata = ApiMetadata.builder()
                .apiKey(this.config.apiKey().orElse(null))
                .build();
    }

    public static QuarkusDoclingServeApiBuilder builder() {
        return new QuarkusDoclingServeApiBuilder();
    }

    @Override
    public ChunkDocumentResponse chunkSourceWithHierarchicalChunker(HierarchicalChunkDocumentRequest request) {
        return this.client.chunkSourceWithHierarchicalChunker(request, this.apiMetadata);
    }

    @Override
    public ChunkDocumentResponse chunkSourceWithHybridChunker(HybridChunkDocumentRequest request) {
        return this.client.chunkSourceWithHybridChunker(request, this.apiMetadata);
    }

    @Override
    public CompletionStage<ChunkDocumentResponse> chunkSourceWithHierarchicalChunkerAsync(
            HierarchicalChunkDocumentRequest request) {
        return executeAsync(() -> this.client.submitChunkSourceWithHierarchicalChunkerAsync(request, this.apiMetadata),
                TaskResultType.CHUNK);
    }

    @Override
    public CompletionStage<ChunkDocumentResponse> chunkSourceWithHybridChunkerAsync(HybridChunkDocumentRequest request) {
        return executeAsync(() -> this.client.submitChunkSourceWithHybridChunkerAsync(request, this.apiMetadata),
                TaskResultType.CHUNK);
    }

    @Override
    public ClearResponse clearConverters(ClearConvertersRequest request) {
        return this.client.clearConverters(this.apiMetadata);
    }

    @Override
    public ClearResponse clearResults(ClearResultsRequest request) {
        return this.client.clearResults(request.getOlderThen().toSeconds(), this.apiMetadata);
    }

    @Override
    public ConvertDocumentResponse convertSource(ConvertDocumentRequest request) {
        return this.client.convertSource(request, this.apiMetadata);
    }

    @Override
    public CompletionStage<ConvertDocumentResponse> convertSourceAsync(ConvertDocumentRequest request) {
        return executeAsync(() -> this.client.submitConvertSourceAsync(request, this.apiMetadata), TaskResultType.CONVERT);
    }

    @Override
    public HealthCheckResponse health() {
        return this.client.health(this.apiMetadata);
    }

    @Override
    public TaskStatusPollResponse pollTaskStatus(TaskStatusPollRequest request) {
        return this.client.pollTaskStatus(request.getTaskId(), request.getWaitTime().toSeconds(), this.apiMetadata);
    }

    @Override
    public ConvertDocumentResponse convertTaskResult(TaskResultRequest request) {
        return this.client.convertTaskResult(request.getTaskId(), this.apiMetadata);
    }

    @Override
    public ChunkDocumentResponse chunkTaskResult(TaskResultRequest request) {
        return this.client.chunkTaskResult(request.getTaskId(), this.apiMetadata);
    }

    private <O> CompletionStage<O> executeAsync(Supplier<Uni<TaskStatusPollResponse>> asyncActionSupplier,
            TaskResultType taskResultType) {
        return asyncActionSupplier.get()
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .<O> flatMap(taskResponse -> {
                    LOG.infof("Started async conversion with task ID: %s", taskResponse.getTaskId());

                    var startTime = System.currentTimeMillis();
                    return pollTaskUntilComplete(taskResponse, startTime, taskResultType);
                })
                .subscribeAsCompletionStage();
    }

    private <O> Uni<O> pollTaskUntilComplete(TaskStatusPollResponse statusPollResponse, long startTime,
            TaskResultType taskResultType) {
        var taskId = statusPollResponse.getTaskId();

        // Check if we've timed out
        if (System.currentTimeMillis() - startTime > this.config.asyncTimeout().toMillis()) {
            return Uni.createFrom().failure(
                    new RuntimeException(
                            "Async conversion timed out after %s for task: %s".formatted(this.config.asyncTimeout(), taskId)));
        }

        // Poll the task status
        return Uni.createFrom().item(() -> this.client.pollTaskStatus(taskId, 0, this.apiMetadata))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .flatMap(statusResponse -> pollTaskStatus(statusResponse, startTime, taskResultType));
    }

    @SuppressWarnings("unchecked")
    private <O> Uni<O> pollTaskStatus(TaskStatusPollResponse statusResponse, long startTime, TaskResultType taskResultType) {
        var status = statusResponse.getTaskStatus();
        var taskId = statusResponse.getTaskId();
        LOG.debugf("Task %s status: %s", taskId, status);

        return switch (status) {
            case SUCCESS -> {
                LOG.infof("Task %s completed successfully", taskId);

                yield ((Uni<O>) switch (taskResultType) {
                    case CONVERT -> Uni.createFrom()
                            .item(() -> this.client.convertTaskResult(statusResponse.getTaskId(), this.apiMetadata));
                    case CHUNK ->
                        Uni.createFrom().item(() -> this.client.chunkTaskResult(statusResponse.getTaskId(), this.apiMetadata));
                })
                        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                        .emitOn(Infrastructure.getDefaultWorkerPool());
            }

            case FAILURE -> {
                var errorMessage = Optional.ofNullable(statusResponse.getTaskStatusMetadata())
                        .map(metadata -> "Task failed: %s".formatted(metadata))
                        .orElse("Task failed");

                yield Uni.createFrom().failure(
                        new RuntimeException("Async conversion failed for task %s: %s".formatted(taskId, errorMessage)));
            }

            default ->
                // Still in progress (PENDING or STARTED), schedule next poll after delay
                Uni.createFrom().item(() -> null)
                        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                        .emitOn(Infrastructure.getDefaultWorkerPool())
                        .onItem().delayIt().by(this.config.asyncPollInterval())
                        .flatMap(v -> pollTaskUntilComplete(statusResponse, startTime, taskResultType));
        };
    }

    @Override
    public <T extends DoclingServeApi, B extends DoclingApiBuilder<T, B>> DoclingApiBuilder<T, B> toBuilder() {
        throw new UnsupportedOperationException("This operation is not supported by the QuarkusDoclingServeClient");
    }

    public static class QuarkusDoclingServeApiBuilder
            implements DoclingApiBuilder<QuarkusDoclingServeApi, QuarkusDoclingServeApiBuilder> {
        private DoclingRuntimeConfig config;
        private QuarkusDoclingServeClient client;

        private QuarkusDoclingServeApiBuilder() {
        }

        public QuarkusDoclingServeApiBuilder config(DoclingRuntimeConfig config) {
            this.config = ValidationUtils.ensureNotNull(config, "config");
            return this;
        }

        public QuarkusDoclingServeApiBuilder client(QuarkusDoclingServeClient client) {
            this.client = ValidationUtils.ensureNotNull(client, "client");
            return this;
        }

        public QuarkusDoclingServeApiBuilder timeout(Duration timeout) {
            throw new UnsupportedOperationException("This operation is not supported by the QuarkusDoclingServeClient");
        }

        @Override
        public QuarkusDoclingServeApiBuilder baseUrl(URI baseUrl) {
            throw new UnsupportedOperationException("This operation is not supported by the QuarkusDoclingServeClient");
        }

        @Override
        public QuarkusDoclingServeApiBuilder apiKey(String apiKey) {
            throw new UnsupportedOperationException("This operation is not supported by the QuarkusDoclingServeClient");
        }

        @Override
        public QuarkusDoclingServeApiBuilder logRequests(boolean logRequests) {
            throw new UnsupportedOperationException("This operation is not supported by the QuarkusDoclingServeClient");
        }

        @Override
        public QuarkusDoclingServeApiBuilder logResponses(boolean logResponses) {
            throw new UnsupportedOperationException("This operation is not supported by the QuarkusDoclingServeClient");
        }

        @Override
        public QuarkusDoclingServeApiBuilder prettyPrint(boolean prettyPrint) {
            throw new UnsupportedOperationException("This operation is not supported by the QuarkusDoclingServeClient");
        }

        @Override
        public QuarkusDoclingServeApiBuilder asyncPollInterval(Duration asyncPollInterval) {
            throw new UnsupportedOperationException("This operation is not supported by the QuarkusDoclingServeClient");
        }

        @Override
        public QuarkusDoclingServeApiBuilder asyncTimeout(Duration asyncTimeout) {
            throw new UnsupportedOperationException("This operation is not supported by the QuarkusDoclingServeClient");
        }

        @Override
        public QuarkusDoclingServeApi build() {
            return new QuarkusDoclingServeApi(this);
        }
    }
}
