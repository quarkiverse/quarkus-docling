package io.quarkiverse.docling.runtime.client;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;

public interface QuarkusDoclingServeTaskApi {
    @Path("/v1/status/poll/{taskId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    TaskStatusPollResponse pollTaskStatus(@PathParam("taskId") String taskId, @QueryParam("wait") long waitTime,
            @BeanParam ApiMetadata metadata);

    @Path("/v1/result/{taskId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ConvertDocumentResponse convertTaskResult(@PathParam("taskId") String taskId, @BeanParam ApiMetadata metadata);

    @Path("/v1/result/{taskId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ChunkDocumentResponse chunkTaskResult(@PathParam("taskId") String taskId, @BeanParam ApiMetadata metadata);
}
