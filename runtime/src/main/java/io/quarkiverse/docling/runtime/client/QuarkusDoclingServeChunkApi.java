package io.quarkiverse.docling.runtime.client;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import ai.docling.serve.api.chunk.request.HierarchicalChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;
import io.smallrye.mutiny.Uni;

public interface QuarkusDoclingServeChunkApi {
    @Path("/v1/chunk/hierarchical/source")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    ChunkDocumentResponse chunkSourceWithHierarchicalChunker(HierarchicalChunkDocumentRequest request,
            @BeanParam ApiMetadata metadata);

    @Path("/v1/chunk/hybrid/source")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    ChunkDocumentResponse chunkSourceWithHybridChunker(HybridChunkDocumentRequest request, @BeanParam ApiMetadata metadata);

    @Path("/v1/chunk/hierarchical/source/async")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<TaskStatusPollResponse> submitChunkSourceWithHierarchicalChunkerAsync(HierarchicalChunkDocumentRequest request,
            @BeanParam ApiMetadata metadata);

    @Path("/v1/chunk/hybrid/source/async")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<TaskStatusPollResponse> submitChunkSourceWithHybridChunkerAsync(HybridChunkDocumentRequest request,
            @BeanParam ApiMetadata metadata);
}
