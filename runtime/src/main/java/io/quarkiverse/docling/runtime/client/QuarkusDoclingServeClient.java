package io.quarkiverse.docling.runtime.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.chunk.request.HierarchicalChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.api.health.HealthCheckResponse;

/**
 * A Quarkus REST Client interface for interacting with the Docling Serve API.
 * This client extends the DoclingServeApi interface and includes endpoints for health checks
 * and document conversion from a source.
 *
 * This interface uses JAX-RS annotations to define the resource paths, HTTP methods, and media types
 * for requests and responses.
 */
@Path("/")
public interface QuarkusDoclingServeClient extends DoclingServeApi {
    @Override
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    HealthCheckResponse health();

    @Override
    @Path("/v1/convert/source")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    ConvertDocumentResponse convertSource(ConvertDocumentRequest request);

    @Override
    @Path("/v1/chunk/hierarchical/source")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    ChunkDocumentResponse chunkSourceWithHierarchicalChunker(HierarchicalChunkDocumentRequest request);

    @Override
    @Path("/v1/chunk/hybrid/source")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    ChunkDocumentResponse chunkSourceWithHybridChunker(HybridChunkDocumentRequest request);

    @Override
    default <T extends DoclingServeApi, B extends DoclingApiBuilder<T, B>> DoclingApiBuilder<T, B> toBuilder() {
        throw new UnsupportedOperationException("This operation is not supported by the QuarkusDoclingServeClient");
    }
}
